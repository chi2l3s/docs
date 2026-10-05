package org.example

import org.example.generated.VolanClient
import io.github.thirtyeighttwentysix.volan.migrate.MigrationDirectory
import io.github.thirtyeighttwentysix.volan.migrate.Migrator
import java.nio.file.Path
import java.sql.DriverManager

fun main() {
    val url = "jdbc:h2:mem:kotlin-example;DB_CLOSE_DELAY=-1"
    DriverManager.getConnection(url).use { connection ->
        Migrator(MigrationDirectory(Path.of("migrations"))).apply(connection)
    }
    VolanClient.builder().url(url).build().use { db ->
        val created = db.user.create {
            email = "alice@example.org"
            name = "Alice"
        }
        val found = db.user.findUniqueOrThrow { where { id eq created.id } }
        check(found == created)
        println("${found.name}: ${found.email}")
        println("Users: ${db.user.count()}")
    }
}
