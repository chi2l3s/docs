# Volan query guide examples

These examples use staged 0.1.0-alpha.3, SQLite in memory and JDK 25. Each test opens its own database and closes the client. The User/Post schema matches the version's documentation. Kotlin and Java tests execute the guide fragments and check their results; schema examples are generated and compiled in separate packages.

First stage the libraries through the [development build guide](https://volan.mintlify.app/next/en/build). Replace `ABSOLUTE/PATH/TO/release-repository` with your file Maven repository path. These libraries are not published in Maven Central.

Run from this directory with the Gradle wrapper:

```bash
./gradlew test "-PvolanRepository=file:///ABSOLUTE/PATH/TO/release-repository"
```

```powershell
.\gradlew.bat test "-PvolanRepository=file:///ABSOLUTE/PATH/TO/release-repository"
```

The guide starts at https://volan.mintlify.app/next/en/querying/reading and https://volan.mintlify.app/next/ru/querying/reading. Fixtures create test tables before executing SQL; source generation alone does not create them. No database server or Docker is required.
