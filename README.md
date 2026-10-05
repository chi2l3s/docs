# Volan documentation

Russian and English documentation: [volan.mintlify.app](https://volan.mintlify.app).

The default version is **1.0.0**, with separate alpha.1 and alpha.2 archives. The legacy `next/` URLs redirect to the stable release. Content, examples, assets and navigation live in this repository. Use the Mintlify dashboard for SEO and deployment settings.

## Preview and validate

```shell
npx mint dev
npx mint validate
npx mint broken-links
python tools/verify-stable.py --source /path/to/volan
```

The source checkout must contain the commit pinned in `tools/stable-coverage.json`. The verifier checks all public types and members against the release ABI, bilingual page/code parity, navigation, redirects and quickstart/project consistency. It does not measure runtime test coverage. Mintlify's Windows link scanner can report false positives when filesystem paths contain backslashes; normalize path separators in the scanner when investigating those reports.

## Execute stable examples

Use JDK 25 and the included Gradle wrappers. On Windows replace `./gradlew` with `.\gradlew.bat`.

```shell
cd examples/first-query-1.0.0
./gradlew test runKotlinExample runJavaExample
```

Both programs print `Alice: alice@example.org` and `Users: 1`.

```shell
cd examples/query-guide-1.0.0
./gradlew test
```

The guide project checks CRUD, filtering, projections, aggregates, relations, bulk/nested writes, raw mapping, transactions, futures, coroutines, metrics and migration history. Its schema fragments are generated and compiled as part of the build. SQLite databases are isolated; the H2 case uses its own memory database. These checks do not execute PostgreSQL, MySQL or MariaDB server scenarios.

## Publish

Changes merged into `main` deploy through the connected Mintlify GitHub integration. Add each new release under `v<version>/<language>/`; preserve archive URLs and version selectors. Follow [AGENTS.md](AGENTS.md) when editing.
