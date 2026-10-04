package org.example

import org.example.generated.VolanClient

fun main() {
    VolanClient.builder().url("jdbc:sqlite::memory:").build().use { db ->
        db.rawExecute("""
            CREATE TABLE "User" (
                "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                "email" TEXT NOT NULL UNIQUE,
                "name" TEXT
            )
        """.trimIndent())

        val created = db.user.create {
            email = "alice@example.org"
            name = "Alice"
        }

        val found = db.user.findUnique {
            where { id eq created.id }
        }

        check(found == created)
        println("${found.name}: ${found.email}")
        println("Users: ${db.user.count()}")
    }
}
