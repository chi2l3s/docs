package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaQueryContractTest {
    @Test
    void parameterizedRead() {
        try (var connection = org.example.generated.VolanClient.builder().url("jdbc:sqlite::memory:").build()) {
            connection.transaction(db -> {
                db.rawExecute("""
                    CREATE TEMP TABLE "User" (
                "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                "email" TEXT NOT NULL UNIQUE,
                "name" TEXT
            )
                    """);
                db.getUser().create(data -> data.setEmail("alice@example.org"));
                var emailToFind = "alice@example.org";
                var emails = db.rawQuery(
                    "SELECT \"email\" FROM \"User\" WHERE \"email\" = ?",
                    java.util.List.of(emailToFind),
                    row -> row.getString("email")
                );
                assertEquals(java.util.List.of("alice@example.org"), emails);
                return null;
            });
        }
    }
}
