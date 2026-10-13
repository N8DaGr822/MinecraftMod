# Development workflow

- Do not create, package, copy as a new build, or version-bump a JAR unless the user explicitly asks for a build/release. Ordinary edits, fixes, tests and readiness discussions do not authorize packaging.
- During iteration, compile with `gradlew.bat classes clientClasses --max-workers=1` and run targeted checks. Retain the default incremental build directory; avoid routine `clean` builds and checkpoint output directories.
- When the user explicitly requests a JAR, use `gradlew.bat playBuild --max-workers=1`. Keep one current playable JAR directly in `build/libs`, source JARs in `build/libs/sources`, and older artifacts in labeled subfolders of `build/libs/archive`. Update `build/libs/README.md` with the build contents and SHA-256. Do not change the release version without user approval; distinguish requested development builds with a descriptive build label in the index.
- Run tests relevant to the change. Use JUnit `--tests` selectors, `runGameTest -PgameTestFilter=<registered test ID>` for specific server behavior, and the applicable asset validator for artwork. Broaden coverage for changes to shared systems.
- Do not run the entire suite for every reversible asset or localized code edit. Clearly distinguish a play build, targeted checks and full verification when reporting results.
- Before pushing, run `gradlew.bat fullCheck --rerun-tasks --max-workers=1` without filters or exclusions and resolve failures. Do not push merely because a play build succeeded.
- See `docs/testing-workflow.md` for commands and limitations. Live gameplay and visual checks complement automated tests.
