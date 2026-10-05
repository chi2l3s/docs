# First query — Volan 1.0.0

JDK 25, Gradle wrapper, Maven Central. Kotlin and Java programs use H2 and a reviewed SQL migration in `migrations/`. Contract tests also use isolated SQLite databases.

```shell
./gradlew test runKotlinExample runJavaExample
```

Both programs print `Alice: alice@example.org` and `Users: 1`.
