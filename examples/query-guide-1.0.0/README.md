# Guide examples — Volan 1.0.0

JDK 25; actual Maven Central artifacts. Run `./gradlew test` (`.\gradlew.bat test` on Windows).

The build generates and compiles the User/Post schema plus 12 schema fixtures. Fifty-eight checks exercise SQLite/H2 CRUD, filters, projections, aggregates, nested writes, composite cursors, optional/self/many-to-many relations, every scalar type, enum mapping, managed timestamps, savepoints, futures, coroutines, interceptors, metrics, migration apply/rollback/history/checksums and raw row mapping. Every database is isolated; fixtures use distinct packages.

PostgreSQL, MySQL and MariaDB server scenarios are outside this example suite. Public API documentation is separately checked against release ABI dumps by `tools/verify-stable.py` at the repository root.
