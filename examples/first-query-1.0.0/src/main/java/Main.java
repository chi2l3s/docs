import org.example.generated.VolanClient;
import io.github.thirtyeighttwentysix.volan.migrate.MigrationDirectory;
import io.github.thirtyeighttwentysix.volan.migrate.MigrationJournal;
import io.github.thirtyeighttwentysix.volan.migrate.Migrator;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;

void main() throws SQLException {
    var url = "jdbc:h2:mem:java-example;DB_CLOSE_DELAY=-1";
    try (var connection = DriverManager.getConnection(url)) {
        new Migrator(new MigrationDirectory(Path.of("migrations")), new MigrationJournal(), Clock.systemUTC())
            .apply(connection);
    }
    try (var db = VolanClient.builder().url(url).build()) {
        var created = db.getUser().create(data -> {
            data.setEmail("alice@example.org");
            data.setName("Alice");
        });
        var found = db.getUser().findUniqueOrThrow(query ->
            query.where(where -> where.getId().eq(created.getId())));
        if (!created.equals(found)) {
            throw new IllegalStateException("The saved user was not returned");
        }
        IO.println(found.getName() + ": " + found.getEmail());
        IO.println("Users: " + db.getUser().count());
    }
}
