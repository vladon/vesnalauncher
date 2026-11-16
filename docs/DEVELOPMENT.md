# Development Workflow

Vesna Launcher follows Trunk-Based Development (TBD): `main` is the single trunk,
all work integrates into it quickly, and it must always build and release.

## Branching

- `main` is the only long-lived branch. Do not create `develop` or long-lived
  release branches.
- Work happens in short-lived branches merged (squash) within about a day.
  Branch naming: `<type>/<short-topic>`, for example
  `feat/drag-and-drop`, `fix/dock-icon-refresh`, `chore/gradle-upgrade`.
- Keep `main` buildable at all times. If a feature is unfinished, gate it behind
  an incomplete-but-safe internal implementation (e.g. disabled menu entry), not
  by leaving `main` broken.

## Commits

Conventional Commits, lowercase, imperative mood:

- `feat: implement application drawer`
- `fix: handle removed applications`
- `refactor: simplify workspace state management`
- `test: add launcher repository tests`
- `docs: document clean-room development policy`
- `ci: configure android build workflow`

Scope is optional (`feat(drawer): …`). Commit messages, PR titles/descriptions,
documentation, and code comments are written in English. Russian is allowed only
in app localization resources (`values-ru/strings.xml`).

## Pull requests

- Small, independently reviewable changes are preferred over large dumps.
- CI must be green (build + unit tests + lint) before merging.
- Squash-merge to keep history linear; one logical change per squash commit.
- `main` is protected against force pushes and deletion.
- Never commit generated artifacts: no APKs, no `build/` output, no local
  `local.properties`.

## CI quality gates

`.github/workflows/android.yml` runs on every push to `main` and on every pull
request:

1. `./gradlew assembleDebug` — the project compiles.
2. `./gradlew testDebugUnitTest` — JVM unit tests pass.
3. `./gradlew lintDebug` — Android Lint reports no errors.

A pull request is mergeable only when all three gates pass.

## Versioning and releases

- Semantic versioning starting at `0.1.0`; `versionName`/`versionCode` live in
  `app/build.gradle.kts` and change in the commit that cuts a release.
- Releases are tagged directly on `main` (`v0.1.0`, `v0.2.0`, …). No permanent
  release branches or maintenance branches.
- Pushing a `v*` tag triggers the CI release job, which builds the APK and
  attaches it to the GitHub Release. Until 1.0 the distributed artifact is the
  debug build; release-signed builds are introduced with 1.0. Never push APK
  files to the repository.
- Pre-1.0 versions may break layout compatibility between releases; starting
  with 1.0, breaking changes require a major version bump.

## Keeping `main` releasable

- Run the relevant checks locally before pushing:
  `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Prefer many small merges over one big integration.
- If `main` breaks, fixing it takes priority over all other work.
