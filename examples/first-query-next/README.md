# Volan 0.1.0-alpha.3 first query

JDK 25; the wrapper uses Gradle 9.8.0.

First stage the pinned Volan sources `d9c8906caba4bcd3ccec0dbfcdf4961c62b679e4` as described in the documentation. Supply their local repository URI:

```bash
./gradlew runKotlinExample runJavaExample -PvolanRepository=file:///absolute/path/volan/build/release-repository
```

On Windows use `.\gradlew.bat`. Both tasks print:

```text
Alice: alice@example.org
Users: 1
```

[English guide](https://volan.mintlify.app/next/en/quickstart)
[Русская инструкция](https://volan.mintlify.app/next/ru/quickstart)

## Contract checks

Run the query and transaction checks from this directory:

```bash
./gradlew test "-PvolanRepository=file:///absolute/path/build/release-repository"
```

On Windows use `.\gradlew.bat`. Tests use isolated in-memory SQLite databases. They verify empty results, parameter binding, broad write filters and transaction retry boundaries. The Java and Kotlin raw SQL examples compile and execute against the same generated client.
