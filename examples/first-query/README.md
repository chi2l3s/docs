# First query

This project uses published Volan `0.1.0-alpha.2` libraries and the matching generator. It creates a temporary SQLite database, saves Alice, reads the record by ID and closes the client.

Use JDK 25. From this directory, run either entry point:

```bash
./gradlew runKotlinExample
./gradlew runJavaExample
```

On Windows, use `gradlew.bat`. Each run prints:

```text
Alice: alice@example.org
Users: 1
```

The in-memory database is discarded after the run. For persistent data, configure a file or server database and apply migrations separately.

Read the complete guide in [Russian](https://volan.mintlify.app/v0.1.0-alpha.2/ru/quickstart) or [English](https://volan.mintlify.app/v0.1.0-alpha.2/en/quickstart).

## Contract checks

Run the query and transaction checks from this directory:

```bash
./gradlew test
```

On Windows use `.\gradlew.bat`. Tests use isolated in-memory SQLite databases. They verify empty results, parameter binding, broad write filters and transaction retry boundaries. The Java and Kotlin raw SQL examples compile and execute against the same generated client.
