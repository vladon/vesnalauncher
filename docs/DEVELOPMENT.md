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
- CI must be green (build + unit tests + lint + androidTest compilation) before
  merging.
- Squash-merge to keep history linear; one logical change per squash commit.
- `main` is protected against force pushes and deletion.
- Never commit generated artifacts: no APKs, no `build/` output, no local
  `local.properties`.

## Review and merge authorization

- The repository owner grants **standing authorization** to review and merge
  **eligible, scoped** pull requests: small, in-scope changes whose required
  checks pass. Scope must match the task the change was made under (for
  orchestrated work, the task's allowed-file list).
- The required checks (build, JVM unit tests, lint, androidTest compilation —
  see below) are mandatory for every merge. **No administrative bypass**:
  override buttons, admin merges with failing checks, or skip-directives are
  not permitted, ever.
- **Release authorization remains separate.** Merge authorization never implies
  release authorization: cutting a release tag, publishing a GitHub Release, or
  distributing an APK requires its own explicit owner approval at the time of
  the release.

## Orchestration worker handoff

Work may be dispatched to worker agents with a scoped task. The handoff
contract:

- The task defines an explicit set of allowed files; workers change nothing
  outside it and keep unrelated local files untouched.
- Workers use ordinary `git add` / `git commit` (a single Conventional Commit is
  preferred) and report completion with the changed files **and** the
  verification they ran.
- The coordinator verifies delivered artifacts before treating work as done:
  inspect `git show` for the delivered commit and run
  `git diff HEAD^ HEAD --check` for whitespace errors, and spot-check internal
  Markdown links for documentation tasks.

## CI quality gates

`.github/workflows/android.yml` runs on every push to `main` and on every pull
request:

1. `./gradlew assembleDebug` — the project compiles.
2. `./gradlew testDebugUnitTest` — JVM unit tests pass.
3. `./gradlew lintDebug` — Android Lint reports no errors.
4. `./gradlew assembleDebugAndroidTest` — the `androidTest` sources compile into
   `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

Gate 4 is a compile gate only: it proves the instrumentation test sources build,
but it does not execute any test. GitHub CI has no emulator attached, so
instrumentation tests never run there. On a shared host, verify the intended
Vesna QA emulator's name and pin every device command to its serial. Build
the APKs with the Gradle assembly tasks above, then run:

```text
adb -s <serial> emu avd name
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <serial> shell am instrument -w dev.vladon.vesna.launcher.test/androidx.test.runner.AndroidJUnitRunner
```

Keep connected Gradle tasks for hosts where the intended QA device is the
only attached device. An unscoped task can select every attached target
and remove the launcher package and its data during teardown.
[VESNA-QA-003E](engineering/verification/VESNA-QA-003E.md#operational-findings)
records the shared-host incident and recovery.

When QA runs on a fresh emulator, treat the device as safe to test only after
boot is completed (`sys.boot_completed=1`) and core system services are stable
— e.g. consecutive samples showing an unchanged `surfaceflinger`/`system_server`
PID and a responsive package service, as practiced in
[VESNA-QA-006](engineering/verification/VESNA-QA-006-API36-1.md); installing or
instrumenting earlier can conflate boot churn with app failures.

A pull request is mergeable only when all four gates pass.

## Versioning and releases

- Semantic versioning starting at `0.1.0`; `versionName`/`versionCode` live in
  `app/build.gradle.kts` and change in the commit that cuts a release.
- Releases are tagged directly on `main` (`v0.1.0`, `v0.2.0`, …). No permanent
  release branches or maintenance branches.
- Pushing a `v*` tag triggers the CI release job, which builds the **signed**
  release APK, verifies the signature with `apksigner`, and attaches it to the
  GitHub Release. Signing material lives only in repository secrets
  (`RELEASE_KEYSTORE_B64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
  `RELEASE_KEY_PASSWORD`); the keystore never enters the repository or CI logs.
  Never push APK files to the repository.
- Pre-1.0 versions may break layout compatibility between releases; starting
  with 1.0, breaking changes require a major version bump.

## Keeping `main` releasable

- Run the relevant checks locally before pushing:
  `./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`.
- Prefer many small merges over one big integration.
- If `main` breaks, fixing it takes priority over all other work.
