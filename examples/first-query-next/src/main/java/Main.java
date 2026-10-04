import org.example.generated.VolanClient;

void main() {
    try (var db = VolanClient.builder().url("jdbc:sqlite::memory:").build()) {
        db.rawExecute("""
            CREATE TABLE "User" (
                "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                "email" TEXT NOT NULL UNIQUE,
                "name" TEXT
            )
            """);

        var created = db.getUser().create(data -> {
            data.setEmail("alice@example.org");
            data.setName("Alice");
        });

        var found = db.getUser().findUnique(query ->
            query.where(where -> where.getId().eq(created.getId())));

        if (!created.equals(found)) {
            throw new IllegalStateException("The saved user was not returned");
        }
        IO.println(found.getName() + ": " + found.getEmail());
        IO.println("Users: " + db.getUser().count());
    }
}
