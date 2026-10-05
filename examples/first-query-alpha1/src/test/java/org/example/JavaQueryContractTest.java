package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaQueryContractTest {
    @Test
    void parameterizedRead() {
        try (var connection = org.example.generated.VolanClient.builder()
            .url(System.getenv("DATABASE_URL"))
            .username(System.getenv("DATABASE_USER"))
            .password(System.getenv("DATABASE_PASSWORD"))
            .build()) {
            connection.transaction(db -> {
                db.rawExecute("""
                    CREATE TEMP TABLE "User" (
                "id" SERIAL PRIMARY KEY,
                "email" TEXT NOT NULL UNIQUE,
                "name" TEXT
            ) ON COMMIT DROP
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
