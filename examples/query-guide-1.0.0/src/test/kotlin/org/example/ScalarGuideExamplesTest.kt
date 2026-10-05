package org.example

import io.github.thirtyeighttwentysix.volan.Json
import io.github.thirtyeighttwentysix.volan.dialect.h2.H2Dialect
import io.github.thirtyeighttwentysix.volan.ir.SchemaLoader
import io.github.thirtyeighttwentysix.volan.migrate.DatabaseSync
import io.github.thirtyeighttwentysix.volan.migrate.H2Reader
import org.example.schema.scalars.State
import org.example.schema.scalars.VolanClient
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.time.*
import java.util.UUID
import kotlin.test.*

class ScalarGuideExamplesTest {
    @Test fun scalarRoundTripDefaultsEnumMappingAndManagedTime() {
        val schema = SchemaLoader.load("12.volan", Files.readString(Path.of("schema-examples/12.volan"))).schemaOrThrow()
        val url = "jdbc:h2:mem:scalarGuide;DB_CLOSE_DELAY=-1"
        DriverManager.getConnection(url).use { connection -> DatabaseSync(H2Reader(), H2Dialect).push(connection, schema) }
        val instant = Instant.parse("2026-01-01T00:00:00Z")
        val fixed = Clock.fixed(instant, ZoneOffset.UTC)
        VolanClient.builder().url(url).clock(fixed).build().use { db ->
            val created = db.sample.create {
                id = 1
                label = "Measurement"
                amount = BigDecimal("12.50")
                ratio = 0.5
                score = 1.25f
                total = 5_000_000_000L
                occurredAt = instant
                eventDate = LocalDate.of(2026, 1, 1)
                eventTime = LocalTime.of(12, 30)
                token = UUID.fromString("85a42b5c-b3c0-4e40-bbed-88c40428f1ba")
                data = Json.of("""{"source":"sensor"}""")
                content = byteArrayOf(1, 2, 3)
                scores = listOf(10, 20)
            }
            val found = db.sample.findUniqueOrThrow { where { id eq 1 } }
            assertEquals(created, found)
            assertEquals(State.NEW, found.state)
            assertTrue(found.enabled)
            assertNull(found.note)
            assertEquals(instant, found.modifiedAt)
            assertEquals(0, found.amount.compareTo(BigDecimal("12.50")))
            assertEquals(listOf(10, 20), found.scores)
            assertContentEquals(byteArrayOf(1, 2, 3), found.content)
            val rawState = db.rawQuery("SELECT \"state\" FROM \"Sample\"", emptyList(), io.github.thirtyeighttwentysix.volan.runtime.RowMapper { it.getString("state") })
            assertEquals(listOf("new"), rawState)
            val updated = db.sample.update { where { id eq 1 }; data { state = State.DONE; note = "Checked" } }
            assertEquals(State.DONE, updated.state)
            assertEquals(instant, updated.modifiedAt)
        }
    }
}
