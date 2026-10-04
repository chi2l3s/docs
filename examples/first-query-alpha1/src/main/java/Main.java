import org.example.generated.VolanClient;

void main() {
    var url = System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:55433/volan_example");
    var username = System.getenv().getOrDefault("DATABASE_USER", "volan");
    var password = System.getenv().getOrDefault("DATABASE_PASSWORD", "volan");
    try (var db = VolanClient.builder().url(url).username(username).password(password).build()) {
        db.transaction(tx -> {
            tx.rawExecute("""
                CREATE TEMP TABLE "User" (
                    "id" SERIAL PRIMARY KEY,
                    "email" TEXT NOT NULL UNIQUE,
                    "name" TEXT
                ) ON COMMIT DROP
                """);
            var created = tx.getUser().create(data -> {
                data.setEmail("alice@example.org");
                data.setName("Alice");
            });
            var found = tx.getUser().findUnique(query ->
                query.where(where -> where.getId().eq(created.getId())));
            if (!created.equals(found)) {
                throw new IllegalStateException("The saved user was not returned");
            }
            IO.println(found.getName() + ": " + found.getEmail());
            IO.println("Users: " + tx.getUser().count());
            return null;
        });
    }
}
