# Volan documentation

This repository publishes Russian and English documentation at https://volan.mintlify.app.

- Edit documentation pages, examples and assets through this GitHub repository. Use the Mintlify dashboard for SEO, branding, search and deployment settings.
- Keep the same topics and navigation order in `ru/` and `en/`. Write complete translations.
- Explain each operation's purpose, inputs, result and limits. Use natural prose and concrete application scenarios. Avoid marketing claims and unexplained lists of API names.
- Keep reader-facing content about the ORM. Omit task history, milestone bookkeeping and the process used to prepare the documentation.
- Preserve release boundaries: published alpha.2, standalone CLI preview and staged alpha.3 are different distributions. Verify availability before changing version claims.
- Align generator and runtime versions. Check actual API signatures in source or generated code and execute runnable examples.
- Keep `examples/first-query` identical to the code blocks in both quickstarts. Verify both entry points with its wrapper.
- Generation does not create tables. Runnable examples must create or migrate their tables and close owned resources.
- Apply the user's clear-design preferences: no gradients, neon, emoji, decorative separators or default Lucide icons. Use the existing Volan logo assets.
- Run `npx mint validate` and `npx mint broken-links` after MDX changes. Inspect desktop and mobile pages after publication.
- Preserve unrelated local changes and stage only files belonging to the task.

## Versioned documentation

- Snapshot published releases under `v<version>/<language>/`. Keep their examples and APIs scoped to their tagged sources. Change old releases only to correct documentation errors.
- Keep development content under `next/<language>/` and pin its source baseline in the build guide. Never label staged artifacts as published.
- Navigation is language, then version, then topic groups. Default to the latest published release. Preserve both selectors when adding a release.
- Archive the previous version when publishing a new release: add a new directory and version entry; do not overwrite a release snapshot. Add redirects for old unversioned URLs deliberately.
- Quickstarts must match their corresponding examples: first-query for alpha.2, first-query-alpha1 for alpha.1 and first-query-next for development. Use the matching generator and runtime.
- When changing query, raw SQL or transaction contracts, run the corresponding example's `test` task. Alpha.1 checks require a dedicated PostgreSQL database and explicit DATABASE_URL, DATABASE_USER and DATABASE_PASSWORD; the other examples use isolated SQLite memory databases. Keep verification commands in each example's README.

## Detailed guides

- Keep the detailed `schema/`, `querying/` and `writing/` guides in published alpha.2 and development complete in both languages. The alpha.1 archive retains its release-specific structure.
- Use the explanatory depth and small topic-focused examples of https://3826.mintlify.app as editorial context. Verify every transferred API claim against the selected release; that site contains obsolete Java calls, uniqueness guarantees and aggregate ordering examples.
- Keep guide snippets aligned with `examples/query-guide` for alpha.2 and `examples/query-guide-next` for development. Run their `test` tasks after changing snippets. Generate and compile the schema fragments as part of those builds.
- Use one User/Post schema across related guides. State when code is an application fragment, requires existing data, replaces a model or uses provider-specific SQL. Nested write results do not imply loaded relations.
