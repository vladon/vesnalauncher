# VESNA-QA-PAGE-TRANSITIONS — page transition effect (#180, v0.4.98)

The page transition effect per the approved #180 scope: Home page changes
can animate with a slide (the outgoing page snapshot slides over the
incoming page), with today's instant switch kept as the default option;
both respect the system animator duration scale and the «Remove
animations» accessibility setting. The finger-follow drag is NOT part of
this phase (see Limitations). Build: debug 0.4.98 (versionCode 103) on
`Vesna_QA_Menu_API_35` (emulator-5560, Android 15/API 35, Russian locale,
dark theme).

## What is verified

| Scenario | Result |
| --- | --- |
| Settings surface | Settings › Home screen › «Эффект перехода страниц»: radio «Мгновенно (по умолчанию)» / «Скольжение» (`transition-radios-0498.png`); the choice persists in the prefs (`home_page_transition = slide` after the tap). |
| The slide transition fires | With Slide selected, a page change via the page controls animates: the outgoing-page bitmap slides over the incoming page for 300 ms (scaled by the system animator scale), then the overlay is removed (`transition-mid-0498.png` — captured during the window; `transition-after-0498.png` — the settled state, the HomeActivity in focus, no leftover overlay). |
| No-crash under animation | The page controls, page indicator and dock keep working during and after the animation; no Activity leak (the overlay is removed in `withEndAction`). |
| Instant default | Without the Slide selection the page change stays instant (the pre-change behaviour, covered by the existing suites). |

## Limitations

- Finger-follow during a drag is NOT implemented (the acceptance line
  «the pages follow the finger during a drag»): the transition animates
  on the commit of a page change (a swipe past the page threshold, the
  page controls, Go to page…). A full finger-follow requires the two-page
  pager refactor of WorkspaceView — recorded as the next phase of #180;
  this PR ships the transition-effect setting and the committed-change
  animation.
- The 50 ms-captured mid-animation frame shows the outgoing overlay mid-
  slide; frame-perfect capture tooling was not used.
- Release builds and physical devices need separate runs.

## Regression coverage

FolderDisplacementTest 3/3, PageAutoRemovalTest 5/5,
NotificationListenerBadgeTest 6/6 on the same build (the transition does
not affect the displacement, auto-removal or badge pipelines).
