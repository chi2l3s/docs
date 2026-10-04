package org.example

import org.example.generated.VolanClient

fun main() {
    VolanClient.builder()
        .url(System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:55433/volan_example")
        .username(System.getenv("DATABASE_USER") ?: "volan")
        .password(System.getenv("DATABASE_PASSWORD") ?: "volan")
        .build().use { db ->
            db.transaction { tx ->
                tx.rawExecute("""
                    CREATE TEMP TABLE "User" (
                        "id" SERIAL PRIMARY KEY,
                        "email" TEXT NOT NULL UNIQUE,
                        "name" TEXT
                    ) ON COMMIT DROP
                """.trimIndent())
                val created = tx.user.create {
                    email = "alice@example.org"
                    name = "Alice"
                }
                val found = tx.user.findUnique { where { id eq created.id } }
                check(found == created)
                println("${found.name}: ${found.email}")
                println("Users: ${tx.user.count()}")
            }
        }
}
