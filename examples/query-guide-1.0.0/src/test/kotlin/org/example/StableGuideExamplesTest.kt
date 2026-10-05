package org.example

import io.github.thirtyeighttwentysix.volan.coroutines.CoroutineAccess
import io.github.thirtyeighttwentysix.volan.coroutines.suspendQuery
import io.github.thirtyeighttwentysix.volan.dialect.sqlite.SqliteDialect
import io.github.thirtyeighttwentysix.volan.ir.SchemaLoader
import io.github.thirtyeighttwentysix.volan.micrometer.MicrometerQueryInterceptor
import io.github.thirtyeighttwentysix.volan.migrate.*
import io.github.thirtyeighttwentysix.volan.runtime.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.example.generated.VolanClient
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.time.Instant
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*

/** Executable contracts for the stable-only guides; every database is isolated. */
class StableGuideExamplesTest {
    @Test fun savepointsRollbackOnlyInnerWork() {
        GuideDatabase.open(false).use { db ->
            db.transaction { tx ->
                tx.user.create { email = "outer@example.org" }
                assertFailsWith<IllegalStateException> {
                    tx.transaction { inner ->
                        inner.user.create { email = "inner@example.org" }
                        error("Undo inner work")
                    }
                }
                assertTrue(tx.user.exists { where { email eq "outer@example.org" } })
                assertFalse(tx.user.exists { where { email eq "inner@example.org" } })
            }
            assertEquals(1L, db.user.count())
        }
    }

    @Test fun outerFailureRollsBackSuccessfulSavepoint() {
        GuideDatabase.open(false).use { db ->
            assertFailsWith<IllegalStateException> {
                db.transaction { tx ->
                    tx.transaction { inner -> inner.user.create { email = "inner@example.org" } }
                    error("Undo all")
                }
            }
            assertEquals(0L, db.user.count())
        }
    }

    @Test fun coroutineReadsAndTransaction() = runBlocking {
        GuideDatabase.open(true).use { db ->
            val access = CoroutineAccess(Dispatchers.IO.limitedParallelism(4))
            val users = withTimeout(2_000) {
                db.suspendQuery(access) { user.findMany { take = 20 } }
            }
            assertEquals(3, users.size)
            val saved = db.suspendQuery {
                transaction { tx ->
                    val user = tx.user.create { email = "coroutine-tx@example.org" }
                    tx.post.create { title = "Welcome"; authorId = user.id }
                    user
                }
            }
            assertTrue(db.post.exists { where { authorId eq saved.id } })
            val created = db.user.suspendQuery { create { email = "new@example.org" } }
            assertEquals("new@example.org", created.email)
        }
    }

    @Test fun asyncTransactionAndWorkerOwnership() {
        val workers = Executors.newFixedThreadPool(2)
        try {
            VolanClient.builder().url("jdbc:sqlite::memory:").asyncExecutor(workers).build().use { db ->
                db.rawExecute("CREATE TABLE User(id INTEGER PRIMARY KEY, email TEXT NOT NULL UNIQUE, name TEXT)")
                val created = db.transactionAsync { tx -> tx.user.create { id = 1; email = "async@example.org" } }.join()
                assertEquals(created, db.user.findFirstAsync().join())
                assertEquals(1L, db.user.countAsync().join())
                db.transaction { tx ->
                    val failure = assertFailsWith<java.util.concurrent.CompletionException> { tx.user.countAsync().join() }
                    assertIs<IllegalStateException>(failure.cause)
                }
            }
            assertFalse(workers.isShutdown)
        } finally { workers.shutdown() }
    }

    @Test fun interceptorAndMetricsCountPhysicalStatements() {
        val registry = SimpleMeterRegistry()
        val operations = mutableListOf<QueryOperation>()
        val hook = object : QueryInterceptor {
            override fun <T> intercept(query: QueryContext, next: Supplier<T>): T {
                operations.add(query.operation)
                return next.get()
            }
        }
        VolanClient.builder().url("jdbc:sqlite::memory:")
            .interceptor(hook).interceptor(MicrometerQueryInterceptor(registry)).build().use { db ->
                db.rawExecute("CREATE TABLE User(id INTEGER PRIMARY KEY, email TEXT NOT NULL UNIQUE, name TEXT)")
                db.user.create { id = 1; email = "metric@example.org" }
                assertEquals(1L, db.user.count())
                assertTrue(operations.contains(QueryOperation.QUERY))
                val timers = registry.find("volan.query").timers()
                assertEquals(operations.size.toLong(), timers.sumOf { it.count() })
                assertTrue(timers.all { it.id.getTag("outcome") == "success" })
            }
    }

    @Test fun nestedDeleteAffectsOnlyRelatedRows() {
        GuideDatabase.open(true).use { db ->
            val authorId = db.post.findFirstOrThrow { where { views eq 0 } }.authorId
            db.user.update { where { id eq authorId }; data { posts.delete { views eq 0 } } }
            assertEquals(24L, db.post.count())
            assertEquals(3L, db.user.count())
        }
    }

    @Test fun typedRawMapperAndNulls() {
        data class UserEmail(val id: Int, val email: String)
        GuideDatabase.open(true).use { db ->
            val emails = db.rawQuery(
                "SELECT id, email FROM User WHERE id > ?", listOf(0),
                RowMapper { row -> UserEmail(row.getInt("id"), row.getString("email")) },
            )
            assertEquals(3, emails.size)
            val names = db.rawQuery("SELECT name FROM User", emptyList(), RowMapper { it.getStringOrNull("name") })
            assertEquals(1, names.count { it == null })
        }
    }

    @Test fun h2RuntimeCrud() {
        VolanClient.builder().url("jdbc:h2:mem:stableGuide").build().use { db ->
            db.rawExecute("CREATE TABLE \"User\"(\"id\" INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \"email\" VARCHAR NOT NULL UNIQUE, \"name\" VARCHAR)")
            val user = db.user.create { email = "h2@example.org" }
            assertEquals(user, db.user.findUnique { where { id eq user.id } })
            val updated = db.user.update { where { id eq user.id }; data { name = "H2" } }
            assertEquals("H2", updated.name)
            assertEquals(user.id, db.user.delete { where { id eq user.id } }.id)
            assertEquals(0L, db.user.count())
        }
    }

    @Test fun sqlitePushPlanPullAndNoop() {
        val schema = SchemaLoader.load("schema.volan", Files.readString(Path.of("schema.volan"))).schemaOrThrow()
        val sync = DatabaseSync(SqliteReader(), SqliteDialect)
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            val plan = sync.plan(connection, schema)
            assertFalse(plan.isEmpty)
            assertTrue(plan.toSql(SqliteDialect).contains("CREATE TABLE"))
            sync.push(connection, schema)
            assertTrue(sync.plan(connection, schema).isEmpty)
            assertTrue(sync.push(connection, schema).isEmpty)
            val pulled = SchemaLoader.load("pulled.volan", sync.pull(connection)).schemaOrThrow()
            assertEquals(setOf("User", "Post"), pulled.models.map { it.name }.toSet())
            assertTrue(sync.drift(connection, SchemaMapper.map(schema)).isEmpty)
        }
    }

    @Test fun migrationApplyChecksumsAndEditedHistory() {
        val root = Files.createTempDirectory("volan-stable-history-")
        val directory = MigrationDirectory(root)
        val migration = directory.write(Instant.parse("2026-10-06T00:00:00Z"), "initial", "CREATE TABLE History(id INTEGER PRIMARY KEY);\n")
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            val migrator = Migrator(directory)
            assertEquals(1, migrator.status(connection).pending.size)
            assertEquals(listOf(migration.id), migrator.apply(connection).map { it.id })
            assertTrue(migrator.status(connection).isUpToDate)
            assertTrue(migrator.apply(connection).isEmpty())
            Files.writeString(root.resolve(migration.id).resolve("migration.sql"), migration.sql + "-- edited\n")
            assertEquals(listOf(migration.id), migrator.status(connection).edited)
            assertFailsWith<VolanMigrationException> { migrator.apply(connection) }
        }
    }

    @Test fun failedSqliteMigrationRollsBackDdlAndJournal() {
        val directory = MigrationDirectory(Files.createTempDirectory("volan-stable-failure-"))
        directory.write(Instant.parse("2026-10-06T00:00:00Z"), "broken", "CREATE TABLE RollbackDemo(id INTEGER PRIMARY KEY); INSERT INTO Missing VALUES(1);")
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            val migrator = Migrator(directory)
            assertFailsWith<VolanMigrationException> { migrator.apply(connection) }
            assertTrue(migrator.status(connection).applied.isEmpty())
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='RollbackDemo'").use { result ->
                    result.next(); assertEquals(0, result.getInt(1))
                }
            }
        }
    }

    @Test fun checksumsNormalizeOnlyLineEndings() {
        assertEquals(MigrationFile.checksumOf("SELECT 1;\n"), MigrationFile.checksumOf("SELECT 1;\r\n"))
        assertNotEquals(MigrationFile.checksumOf("SELECT 1;"), MigrationFile.checksumOf("SELECT 2;"))
    }
}
