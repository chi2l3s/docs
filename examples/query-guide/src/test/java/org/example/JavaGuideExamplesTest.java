package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JavaGuideExamplesTest {
    @Test
    void filter_java() {
        try (var db = GuideDatabase.open(true)) {
            var users = db.getUser().findMany(q -> {
                q.where(w -> {
                    w.getEmail().endsWith("@example.org");
                    w.or(alternative -> {
                        alternative.getName().eq("Alice");
                        alternative.getName().isNull();
                    });
                });
                q.orderBy(o -> o.getId().asc());
                q.setTake(20);
            });
            assertEquals(2, users.size());
        }
    }

    @Test
    void raw_read_java() {
        try (var db = GuideDatabase.open(true)) {
            var emailToFind = "alice@example.org";
            var emails = db.rawQuery(
                "SELECT \"email\" FROM \"User\" WHERE \"email\" = ?",
                java.util.List.of(emailToFind),
                row -> row.getString("email")
            );
            assertEquals(java.util.List.of("alice@example.org"), emails);
        }
    }

    @Test
    void reading_unique_java() {
        try (var db = GuideDatabase.open(true)) {
            var userId = 1;
            var user = db.getUser().findUnique(q -> {
                q.where(w -> w.getId().eq(userId));
            });
            System.out.println(user == null ? "User not found" : user.getEmail());
            assertEquals("alice@example.org", user.getEmail());
        }
    }

    @Test
    void create_one_java() {
        try (var db = GuideDatabase.open(false)) {
            var created = db.getUser().create(data -> {
                data.setEmail("alice@example.org");
                data.setName("Alice");
            });
            System.out.println("Created user " + created.getId() + ": " + created.getEmail());
            assertEquals("Alice", created.getName());
        }
    }

    @Test
    void delete_one_java() {
        try (var db = GuideDatabase.open(false)) {
            var created = db.getUser().create(data -> data.setEmail("remove@example.org"));
            var deleted = db.getUser().delete(q -> {
                q.where(w -> w.getId().eq(created.getId()));
            });
            System.out.println("Deleted user: " + deleted.getEmail());
            assertEquals(0L, db.getUser().count());
        }
    }

    @Test
    void nested_create_java() {
        try (var db = GuideDatabase.open(false)) {
            var author = db.getUser().create(data -> {
                data.setEmail("author@example.org");
                data.getPosts().create(post -> post.setTitle("First post"));
                data.getPosts().create(post -> post.setTitle("Second post"));
            });
            var loaded = db.getUser().findUniqueOrThrow(q -> {
                q.where(w -> w.getId().eq(author.getId()));
                q.include(i -> i.posts());
            });
            System.out.println(loaded.getPosts().size());
            assertEquals(2, loaded.getPosts().size());
        }
    }

    @Test
    void update_one_java() {
        try (var db = GuideDatabase.open(false)) {
            var created = db.getUser().create(data -> {
                data.setEmail("edit@example.org");
                data.setName("Before");
            });
            var updated = db.getUser().update(q -> {
                q.where(w -> w.getId().eq(created.getId()));
                q.data(data -> data.setName("After"));
            });
            System.out.println(updated.getName());
            assertEquals("After", updated.getName());
        }
    }

}
