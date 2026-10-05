# Volan query guide examples

These examples use 0.1.0-alpha.2, SQLite in memory and JDK 25. Each test opens its own database and closes the client. The User/Post schema matches the version's documentation. Kotlin and Java tests execute the guide fragments and check their results; schema examples are generated and compiled in separate packages.

Run from this directory with the Gradle wrapper:

```bash
./gradlew test
```

```powershell
.\gradlew.bat test
```

The guide starts at https://volan.mintlify.app/v0.1.0-alpha.2/en/querying/reading and https://volan.mintlify.app/v0.1.0-alpha.2/ru/querying/reading. Fixtures create test tables before executing SQL; source generation alone does not create them. No database server or Docker is required.
