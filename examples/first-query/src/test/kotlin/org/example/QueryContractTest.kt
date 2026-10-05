package org.example

import io.github.thirtyeighttwentysix.volan.runtime.Isolation
import io.github.thirtyeighttwentysix.volan.runtime.RetryPolicy
import io.github.thirtyeighttwentysix.volan.runtime.VolanNotFoundException
import io.github.thirtyeighttwentysix.volan.runtime.VolanTransactionException
import org.example.generated.VolanClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryContractTest {
    private fun open(): VolanClient = VolanClient.builder().url("jdbc:sqlite::memory:").build()

    private fun createTable(db: VolanClient) {
        db.rawExecute("""
            CREATE TEMP TABLE "User" (
                "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                "email" TEXT NOT NULL UNIQUE,
                "name" TEXT
            )
        """.trimIndent())
    }

    @Test
    fun emptyResultsAndWriteScope() {
        open().use { db ->
            db.transaction { tx ->
                createTable(tx)
                assertEquals(emptyList(), tx.user.findMany())
                assertNull(tx.user.findUnique { where { id eq -1 } })
                assertEquals(0L, tx.user.count())
                assertFalse(tx.user.exists())
                assertNull(tx.user.projectFirst { select { email } })
                assertFailsWith<VolanNotFoundException> { tx.user.findUniqueOrThrow { where { id eq -1 } } }
                assertFailsWith<VolanNotFoundException> { tx.user.delete { where { id eq -1 } } }

                tx.user.create { email = "one@example.org"; name = "Shared" }
                tx.user.create { email = "two@example.org"; name = "Shared" }
                assertTrue(tx.user.findUnique { where { name eq "Shared" } } != null)
                tx.user.update { where { name eq "Shared" }; data { name = "Changed" } }
                assertEquals(2L, tx.user.count { where { name eq "Changed" } })
                tx.user.delete { where { name eq "Changed" } }
                assertEquals(0L, tx.user.count())
                assertEquals(0L, tx.user.deleteMany { where { id eq -1 } })
                assertFailsWith<VolanNotFoundException> {
                    tx.user.update { where { id eq -1 }; data { name = "Missing" } }
                }
            }
        }
    }

    @Test
    fun rawSqlBindsValuesAndMapsNullableColumns() {
        open().use { db ->
            db.transaction { db ->
                createTable(db)
                db.user.create { email = "alice@example.org" }
                val emailToFind = "alice@example.org"
                val emails = db.rawQuery(
                    """SELECT "email" FROM "User" WHERE "email" = ?""",
                    listOf(emailToFind),
                ) { row -> row.getString("email") }
                assertEquals(listOf("alice@example.org"), emails)
                val absent = db.rawQuery(
                    """SELECT "email" FROM "User" WHERE "email" = ?""",
                    listOf("missing' OR 1=1 --"),
                ) { row -> row.getString("email") }
                assertEquals(emptyList(), absent)
                val names = db.rawQuery("""SELECT "name" FROM "User" """, emptyList()) { it.getStringOrNull("name") }
                assertEquals(listOf(null), names)
                assertEquals(1L, db.rawExecute("""UPDATE "User" SET "name" = ? WHERE "email" = ?""", listOf("Alice", emailToFind)))
            }
        }
    }

    @Test
    fun documentedRetryParameters() {
        assertEquals(1, RetryPolicy.NONE.attempts)
        assertEquals(3, RetryPolicy.DEFAULT.attempts)
        assertEquals(20L, RetryPolicy.DEFAULT.initialDelay)
        assertEquals(2.0, RetryPolicy.DEFAULT.multiplier)
        open().use { db ->
            var attempts = 0
            val created = db.transaction(Isolation.DEFAULT, RetryPolicy(attempts = 3, initialDelay = 0L)) { tx ->
                createTable(tx)
                val row = tx.user.create { email = "retry@example.org" }
                attempts++
                if (attempts == 1) throw VolanTransactionException(true, "simulate retryable failure", null)
                assertEquals(1L, tx.user.count())
                row
            }
            assertEquals(2, attempts)
            assertEquals("retry@example.org", created.email)
        }
    }

    @Test
    fun applicationErrorsDoNotRetry() {
        open().use { db ->
            var attempts = 0
            assertFailsWith<IllegalStateException> {
                db.transaction(Isolation.DEFAULT, RetryPolicy.DEFAULT) {
                    attempts++
                    error("application failure")
                }
            }
            assertEquals(1, attempts)
        }
    }

    @Test
    fun nestedRetryDoesNotRunAnIndependentLoop() {
        open().use { db ->
            db.transaction { tx ->
                var attempts = 0
                assertFailsWith<VolanTransactionException> {
                    tx.transaction(Isolation.DEFAULT, RetryPolicy.DEFAULT) {
                        attempts++
                        throw VolanTransactionException(true, "nested failure", null)
                    }
                }
                assertEquals(1, attempts)
            }
        }
    }

    @Test
    fun documentationRetryExample() {
        open().use { db ->
            // The published example assumes the table already exists. PostgreSQL uses a
            // transaction-local fixture here so this test never writes permanent tables.
            db.transaction { db ->
                createTable(db)
                val emailToCreate = "transaction@example.org"
                val created = db.transaction(
                    isolation = Isolation.DEFAULT,
                    retry = RetryPolicy(attempts = 3, initialDelay = 20L, multiplier = 2.0),
                ) { tx ->
                    tx.user.create { email = emailToCreate }
                }
                assertEquals(emailToCreate, created.email)
                assertEquals(1L, db.user.count())
            }
        }
    }
}
