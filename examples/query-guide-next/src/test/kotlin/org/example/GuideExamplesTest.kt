package org.example

import org.example.generated.VolanClient
import io.github.thirtyeighttwentysix.volan.runtime.*
import kotlin.test.*

class GuideExamplesTest {
    @Test
    fun aggregate() {
        GuideDatabase.open(true).use { db ->
            val stats = db.post.aggregate {
                where { views gt 0 }
                count()
                sum { views }
                average { views }
                minimum { views }
                maximum { views }
            }
            println("Posts: ${stats.count}")
            println("Views: ${stats.sumOfViews}")
            println("Average: ${stats.averageOfViews}")
            assertEquals(24L, stats.count)
            assertEquals(0, stats.sumOfViews!!.compareTo(java.math.BigDecimal("3000")))
            assertEquals(125.0, stats.averageOfViews)
            assertEquals(10, stats.minimumOfViews)
            assertEquals(240, stats.maximumOfViews)
        }
    }

    @Test
    fun grouping() {
        GuideDatabase.open(true).use { db ->
            val groups = db.post.groupBy {
                where { views gt 0 }
                by { authorId }
                count()
                sum { views }
                having { count gt 1L }
                orderBy {
                    authorId.asc()
                }
                take = 10
            }
            groups.forEach { group ->
                println("${group.authorId}: ${group.count} posts, ${group.sumOfViews} views")
            }
            assertEquals(2, groups.size)
            assertEquals(24L, groups.sumOf { it.count })
        }
    }

    @Test
    fun filter_range() {
        GuideDatabase.open(true).use { db ->
            val posts = db.post.findMany {
                where {
                    views.between(100, 1000)
                    title contains "Volan"
                }
                orderBy { id.asc() }
                take = 20
            }
            assertEquals(15, posts.size)
            assertTrue(posts.all { it.views in 100..1000 && "Volan" in it.title })
        }
    }

    @Test
    fun filter_text_null() {
        GuideDatabase.open(true).use { db ->
            val users = db.user.findMany {
                where {
                    email.ignoringCase() endsWith "@example.org"
                    name.isNotNull()
                }
                orderBy { id.asc() }
                take = 20
            }
            assertEquals(2, users.size)
        }
    }

    @Test
    fun filter_groups() {
        GuideDatabase.open(true).use { db ->
            val users = db.user.findMany {
                where {
                    email endsWith "@example.org"
                    or {
                        name eq "Alice"
                        name.isNull()
                    }
                    not { email startsWith "blocked" }
                }
                orderBy { id.asc() }
            }
            assertEquals(setOf("Alice", null), users.map { it.name }.toSet())
        }
    }

    @Test
    fun filter_relations() {
        GuideDatabase.open(true).use { db ->
            val authors = db.user.findMany {
                where { posts { some { views gt 100 } } }
                orderBy { id.asc() }
            }
            val posts = db.post.findMany {
                where { author { matches { email endsWith "@example.org" } } }
                orderBy { id.asc() }
            }
            assertEquals(2, authors.size)
            assertEquals(25, posts.size)
        }
    }

    @Test
    fun ordering() {
        GuideDatabase.open(true).use { db ->
            val posts = db.post.findMany {
                orderBy {
                    views.desc()
                    id.asc()
                }
                take = 20
            }
            assertEquals(20, posts.size)
            assertEquals(240, posts.first().views)
        }
    }

    @Test
    fun offset_page() {
        GuideDatabase.open(true).use { db ->
            val pageNumber = 2
            val pageSize = 10
            val page = db.post.findMany {
                orderBy { id.asc() }
                take = pageSize
                skip = (pageNumber - 1) * pageSize
            }
            assertEquals((11..20).toList(), page.map { it.id })
        }
    }

    @Test
    fun cursor_page() {
        GuideDatabase.open(true).use { db ->
            val firstPage = db.post.findMany {
                orderBy { id.asc() }
                take = 10
            }
            val lastId = firstPage.lastOrNull()?.id
            val nextPage = if (lastId == null) {
                emptyList()
            } else {
                db.post.findMany {
                    cursor(id = lastId)
                    take = 10
                }
            }
            assertEquals((1..10).toList(), firstPage.map { it.id })
            assertEquals((11..20).toList(), nextPage.map { it.id })
        }
    }

    @Test
    fun projection() {
        GuideDatabase.open(true).use { db ->
            val choices = db.user.projectMany {
                select { id; email }
                orderBy { id.asc() }
                take = 20
            }
            choices.forEach { choice -> println("${choice.id}: ${choice.email}") }
            assertEquals(3, choices.size)
            assertFailsWith<VolanFieldNotSelectedException> { choices.first().name }
        }
    }

    @Test
    fun distinct() {
        GuideDatabase.open(true).use { db ->
            val authors = db.post.projectMany {
                select { authorId }
                distinct { authorId }
            }
            val authorIds = authors.map { it.authorId }
            assertEquals(setOf(1, 2), authorIds.toSet())
        }
    }

    @Test
    fun raw_read() {
        GuideDatabase.open(true).use { db ->
            val emailToFind = "alice@example.org"
            val emails = db.rawQuery(
                """SELECT "email" FROM "User" WHERE "email" = ?""",
                listOf(emailToFind),
            ) { row -> row.getString("email") }
            assertEquals(listOf("alice@example.org"), emails)
        }
    }

    @Test
    fun raw_nullable() {
        GuideDatabase.open(true).use { db ->
            data class UserLabel(val email: String, val name: String?)

            val labels = db.rawQuery(
                """SELECT "email", "name" FROM "User" ORDER BY "id" LIMIT 20""",
                emptyList(),
            ) { row ->
                UserLabel(row.getString("email"), row.getStringOrNull("name"))
            }
            assertEquals(3, labels.size)
            assertNull(labels.last().name)
        }
    }

    @Test
    fun raw_write() {
        GuideDatabase.open(true).use { db ->
            val affected = db.rawExecute(
                """UPDATE "User" SET "name" = ? WHERE "email" = ?""",
                listOf("Alice", "alice@example.org"),
            )
            println("Updated rows: $affected")
            assertEquals(1L, affected)
        }
    }

    @Test
    fun reading_list() {
        GuideDatabase.open(true).use { db ->
            val users = db.user.findMany {
                where { email endsWith "@example.org" }
                orderBy { id.asc() }
                take = 20
            }
            users.forEach { user -> println(user.email) }
            assertEquals(3, users.size)
        }
    }

    @Test
    fun reading_first() {
        GuideDatabase.open(true).use { db ->
            val first = db.user.findFirst {
                where { name.isNotNull() }
                orderBy { id.asc() }
            }
            println(first?.email ?: "No matching user")
            assertEquals(1, first?.id)
        }
    }

    @Test
    fun reading_unique() {
        GuideDatabase.open(true).use { db ->
            val userId = 1
            val user = db.user.findUnique {
                where { id eq userId }
            }
            println(user?.email ?: "User not found")
            assertEquals("alice@example.org", user?.email)
        }
    }

    @Test
    fun reading_summary() {
        GuideDatabase.open(true).use { db ->
            val total = db.user.count {
                where { email endsWith "@example.org" }
            }
            val emailTaken = db.user.exists {
                where { email eq "alice@example.org" }
            }
            println("Users: $total, email taken: $emailTaken")
            assertEquals(3L, total)
            assertTrue(emailTaken)
        }
    }

    @Test
    fun create_one() {
        GuideDatabase.open(false).use { db ->
            val created = db.user.create {
                email = "alice@example.org"
                name = "Alice"
            }
            println("Created user ${created.id}: ${created.email}")
            assertEquals("Alice", created.name)
            assertEquals(1L, db.user.count())
        }
    }

    @Test
    fun create_many() {
        GuideDatabase.open(false).use { db ->
            val inserted = db.user.createMany {
                row { email = "one@example.org" }
                row { email = "two@example.org"; name = "Two" }
            }
            println("Inserted rows: $inserted")
            assertEquals(2L, inserted)
            assertEquals(2L, db.user.count())
        }
    }

    @Test
    fun upsert() {
        GuideDatabase.open(false).use { db ->
            val user = db.user.upsert {
                where { email eq "subscriber@example.org" }
                create { email = "subscriber@example.org"; name = "Subscriber" }
                update { name = "Subscriber" }
            }
            assertEquals("subscriber@example.org", user.email)
            assertEquals(1L, db.user.count())
        }
    }

    @Test
    fun delete_one() {
        GuideDatabase.open(false).use { db ->
            val created = db.user.create { email = "remove@example.org" }
            val deleted = db.user.delete {
                where { id eq created.id }
            }
            println("Deleted user: ${deleted.email}")
            assertEquals("remove@example.org", deleted.email)
            assertEquals(0L, db.user.count())
        }
    }

    @Test
    fun delete_many() {
        GuideDatabase.open(true).use { db ->
            db.user.create { email = "old@temporary.example.org" }
            val deletedCount = db.user.deleteMany {
                where { email endsWith "@temporary.example.org" }
            }
            println("Deleted users: $deletedCount")
            assertEquals(1L, deletedCount)
            assertEquals(3L, db.user.count())
        }
    }

    @Test
    fun nested_create() {
        GuideDatabase.open(false).use { db ->
            val author = db.user.create {
                email = "author@example.org"
                posts.create { title = "First post" }
                posts.create { title = "Second post" }
            }
            val loaded = db.user.findUniqueOrThrow {
                where { id eq author.id }
                include { posts { orderBy { id.asc() } } }
            }
            println(loaded.posts.map { it.title })
            assertEquals(listOf("First post", "Second post"), loaded.posts.map { it.title })
            assertFailsWith<VolanRelationNotLoadedException> { author.posts }
        }
    }

    @Test
    fun nested_connect() {
        GuideDatabase.open(false).use { db ->
            val existingAuthor = db.user.create { email = "existing-author@example.org" }
            val post = db.post.create {
                title = "Connected post"
                author.connect { id eq existingAuthor.id }
            }
            println(post.authorId)
            assertEquals(existingAuthor.id, post.authorId)
            assertEquals(1L, db.user.count())
        }
    }

    @Test
    fun nested_connect_or_create() {
        GuideDatabase.open(false).use { db ->
            val post = db.post.create {
                title = "Shared author"
                author.connectOrCreate {
                    email = "shared-author@example.org"
                    name = "Shared author"
                }
            }
            println(post.authorId)
            assertEquals(1L, db.user.count())
            val another = db.post.create { title = "Another"; author.connectOrCreate { email = "shared-author@example.org"; name = "Ignored" } }
            assertEquals(post.authorId, another.authorId)
            assertEquals("Shared author", db.user.findUniqueOrThrow { where { id eq another.authorId } }.name)
        }
    }

    @Test
    fun nested_update() {
        GuideDatabase.open(false).use { db ->
            val author = db.user.create {
                email = "nested-edit@example.org"
                posts.create { title = "Before" }
            }
            db.user.update {
                where { id eq author.id }
                data {
                    posts.update {
                        where { title eq "Before" }
                        data { title = "After" }
                    }
                }
            }
            assertEquals(1L, db.post.count { where { title eq "After" } })
            assertEquals(0L, db.post.count { where { title eq "Before" } })
        }
    }

    @Test
    fun update_one() {
        GuideDatabase.open(false).use { db ->
            val created = db.user.create { email = "edit@example.org"; name = "Before" }
            val updated = db.user.update {
                where { id eq created.id }
                data { name = "After" }
            }
            println(updated.name)
            assertEquals("After", updated.name)
            assertEquals("edit@example.org", updated.email)
        }
    }

    @Test
    fun update_null() {
        GuideDatabase.open(false).use { db ->
            val created = db.user.create { email = "clear@example.org"; name = "Alice" }
            val cleared = db.user.update {
                where { id eq created.id }
                data { name = null }
            }
            println(cleared.name)
            assertNull(cleared.name)
            assertEquals("clear@example.org", cleared.email)
        }
    }

    @Test
    fun update_many() {
        GuideDatabase.open(true).use { db ->
            val affected = db.user.updateMany {
                where { name.isNull() }
                data { name = "Unnamed" }
            }
            println("Updated rows: $affected")
            assertEquals(1L, affected)
            assertEquals(1L, db.user.count { where { name eq "Unnamed" } })
        }
    }


    @Test
    fun cursorRestrictionsAndInclusiveBoundary() {
        GuideDatabase.open(true).use { db ->
            assertFailsWith<VolanUnsupportedException> {
                db.post.findMany { cursor(id = 10); orderBy { id.asc() }; take = 10 }
            }
            val inclusive = db.post.findMany { cursor(id = 10, inclusive = true); take = 2 }
            assertEquals(listOf(10, 11), inclusive.map { it.id })
        }
    }

    @Test
    fun emptyAggregatesAndRelationQuantifiers() {
        GuideDatabase.open(false).use { db ->
            val empty = db.post.aggregate { count(); sum { views }; average { views } }
            assertEquals(0L, empty.count)
            assertNull(empty.sumOfViews)
            assertNull(empty.averageOfViews)
            val onlyCount = db.post.aggregate { count() }
            assertFailsWith<VolanAggregateNotAskedException> { onlyCount.sumOfViews }
            db.user.create { email = "empty@example.org" }
            assertEquals(1L, db.user.count { where { posts { every { views gt 100 } } } })
            assertEquals(0L, db.user.count { where { posts { some { views gt 100 } } } })
        }
    }

    @Test
    fun nestedFailureRollsBackParent() {
        GuideDatabase.open(false).use { db ->
            assertFailsWith<VolanNotFoundException> {
                db.post.create { title = "Missing author"; author.connect { id eq -1 } }
            }
            assertEquals(0L, db.post.count())
            assertFailsWith<VolanUniqueConstraintException> {
                db.user.create {
                    email = "rollback@example.org"
                    posts.create { id = 50; title = "First" }
                    posts.create { id = 50; title = "Duplicate" }
                }
            }
            assertEquals(0L, db.user.count())
            assertEquals(0L, db.post.count())
        }
    }

    @Test
    fun deletingParentCascadesWithoutLoadingChildren() {
        GuideDatabase.open(false).use { db ->
            val author = db.user.create { email = "cascade@example.org"; posts.create { title = "Dependent" } }
            assertEquals(1L, db.post.count())
            db.user.delete { where { id eq author.id } }
            assertEquals(0L, db.post.count())
        }
    }

    @Test
    fun bulkDefaultsAndConflictsRemainTransactional() {
        GuideDatabase.open(false).use { db ->
            val author = db.user.create { email = "bulk@example.org" }
            val inserted = db.post.createMany {
                row { title = "Default"; authorId = author.id }
                row { title = "Explicit"; authorId = author.id; views = 15 }
            }
            assertEquals(2L, inserted)
            assertEquals(listOf(0, 15), db.post.findMany { orderBy { id.asc() } }.map { it.views })
            assertFailsWith<VolanUniqueConstraintException> {
                db.user.createMany { row { email = "duplicate@example.org" }; row { email = "duplicate@example.org" } }
            }
            assertEquals(1L, db.user.count())
        }
    }
}
