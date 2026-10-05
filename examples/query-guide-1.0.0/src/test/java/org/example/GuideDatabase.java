package org.example;

import org.example.generated.VolanClient;

public class GuideDatabase {
    public static VolanClient open(boolean seed) {
        var db = VolanClient.builder().url("jdbc:sqlite::memory:").build();
        try {
            db.rawExecute("PRAGMA foreign_keys = ON");
            db.rawExecute("""
                CREATE TABLE "User" (
                    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                    "email" TEXT NOT NULL UNIQUE,
                    "name" TEXT
                )
                """);
            db.rawExecute("""
                CREATE TABLE "Post" (
                    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                    "title" TEXT NOT NULL,
                    "views" INTEGER NOT NULL DEFAULT 0,
                    "authorId" INTEGER NOT NULL REFERENCES "User"("id") ON DELETE CASCADE
                )
                """);
            if (seed) {
                var alice = db.getUser().create(d -> { d.setEmail("alice@example.org"); d.setName("Alice"); });
                var bob = db.getUser().create(d -> { d.setEmail("bob@example.org"); d.setName("Bob"); });
                db.getUser().create(d -> d.setEmail("unknown@example.org"));
                for (var index = 0; index < 25; index++) {
                    var value = index;
                    db.getPost().create(d -> {
                        d.setTitle("Volan " + value);
                        d.setViews(value * 10);
                        d.setAuthorId(value % 2 == 0 ? alice.getId() : bob.getId());
                    });
                }
            }
            return db;
        } catch (RuntimeException failure) {
            db.close();
            throw failure;
        }
    }
}
