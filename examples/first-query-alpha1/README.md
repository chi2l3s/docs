# Volan 0.1.0-alpha.1 first query

JDK 25; the wrapper uses Gradle 9.8.0.

Start a dedicated PostgreSQL example database:

```bash
docker run --name volan-alpha1-example -e POSTGRES_PASSWORD=volan -e POSTGRES_USER=volan -e POSTGRES_DB=volan_example -p 127.0.0.1:55433:5432 -d postgres:17.10
```

Wait until `docker exec volan-alpha1-example pg_isready -U volan -d volan_example` reports ready. The default example credentials are local demo values. To use another dedicated database, set DATABASE_URL, DATABASE_USER and DATABASE_PASSWORD. Both entry points use a temporary PostgreSQL table with ON COMMIT DROP.

```bash
./gradlew runKotlinExample runJavaExample
```

On Windows use `.\gradlew.bat`. Both tasks print:

```text
Alice: alice@example.org
Users: 1
```

Remove only this dedicated example container when finished: `docker rm -f volan-alpha1-example`.

[English guide](https://volan.mintlify.app/v0.1.0-alpha.1/en/quickstart)
[Русская инструкция](https://volan.mintlify.app/v0.1.0-alpha.1/ru/quickstart)

## Contract checks

Use the dedicated PostgreSQL example database above. Tests require explicit environment variables and only create temporary tables within transactions:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:55433/volan_example
export DATABASE_USER=volan
export DATABASE_PASSWORD=volan
./gradlew test
```

PowerShell:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:55433/volan_example"
$env:DATABASE_USER = "volan"
$env:DATABASE_PASSWORD = "volan"
.\gradlew.bat test
```
