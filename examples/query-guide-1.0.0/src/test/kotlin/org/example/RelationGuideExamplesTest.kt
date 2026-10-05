package org.example

import io.github.thirtyeighttwentysix.volan.dialect.sqlite.SqliteDialect
import io.github.thirtyeighttwentysix.volan.ir.SchemaLoader
import io.github.thirtyeighttwentysix.volan.migrate.DatabaseSync
import io.github.thirtyeighttwentysix.volan.migrate.SqliteReader
import io.github.thirtyeighttwentysix.volan.runtime.VolanRelationNotLoadedException
import org.example.schema.relationships.VolanClient
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.*

class RelationGuideExamplesTest {
    private fun open(): VolanClient {
        val schema = SchemaLoader.load("11.volan", Files.readString(Path.of("schema-examples/11.volan"))).schemaOrThrow()
        val file = Files.createTempFile("volan-relations-", ".db")
        file.toFile().deleteOnExit()
        val url = "jdbc:sqlite:$file"
        DriverManager.getConnection(url).use { connection -> DatabaseSync(SqliteReader(), SqliteDialect).push(connection, schema) }
        return VolanClient.builder().url(url).build()
    }

    @Test fun selfRelationConnectDisconnectAndSetNull() {
        open().use { db ->
            db.folder.create { id = 1 }
            db.folder.create { id = 2; parent.connect { id eq 1 } }
            val root = db.folder.findUniqueOrThrow {
                where { id eq 1 }
                include { children { include { children() } } }
            }
            assertEquals(2, root.children.single().id)
            assertTrue(root.children.single().children.isEmpty())
            db.folder.update { where { id eq 2 }; data { parent.disconnect() } }
            assertNull(db.folder.findUniqueOrThrow { where { id eq 2 } }.parentId)
            db.folder.update { where { id eq 2 }; data { parent.connect { id eq 1 } } }
            db.folder.delete { where { id eq 1 } }
            val child = db.folder.findUniqueOrThrow { where { id eq 2 }; include { parent() } }
            assertNull(child.parentId)
            assertNull(child.parent)
        }
    }

    @Test fun manyToManyReplacementPreservesTargets() {
        open().use { db ->
            db.tag.create { id = 1; label = "kotlin" }
            db.tag.create { id = 2; label = "sql" }
            val created = db.article.create {
                id = 10; title = "Relations"
                tags.connect { id eq 1 }
                tags.connect { id eq 2 }
            }
            assertFailsWith<VolanRelationNotLoadedException> { created.tags }
            db.article.update { where { id eq 10 }; data { tags.disconnect { id eq 1 } } }
            assertEquals(listOf(2), db.article.findUniqueOrThrow { where { id eq 10 }; include { tags() } }.tags.map { it.id })
            db.article.update { where { id eq 10 }; data { tags.`set` { row { id eq 1 } } } }
            val article = db.article.findUniqueOrThrow { where { id eq 10 }; include { tags() } }
            assertEquals("kotlin", article.tags.single().label)
            assertEquals(2L, db.tag.count())
            db.article.update { where { id eq 10 }; data { tags.`set` {} } }
            assertTrue(db.article.findUniqueOrThrow { where { id eq 10 }; include { tags() } }.tags.isEmpty())
            assertEquals(2L, db.tag.count())
        }
    }

    @Test fun compositeCursorFollowsOrderedKeyAndBoundary() {
        open().use { db ->
            db.membership.create { tenantId = 1; userId = 1; role = "reader" }
            db.membership.create { tenantId = 1; userId = 2; role = "writer" }
            db.membership.create { tenantId = 2; userId = 1; role = "reader" }
            val exact = db.membership.findUniqueOrThrow { where { tenantId eq 1; userId eq 2 } }
            val next = db.membership.findMany { cursor(tenantId = exact.tenantId, userId = exact.userId); take = 10 }
            assertEquals(2, next.single().tenantId)
            assertEquals(2, db.membership.findMany { cursor(1, 2, inclusive = true) }.size)
            assertEquals(1, db.membership.findMany { where { tenantId eq 1 }; cursor(1, 1) }.size)
        }
    }
}
