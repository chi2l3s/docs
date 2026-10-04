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
