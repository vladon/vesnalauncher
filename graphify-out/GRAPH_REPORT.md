# Graph Report - vesnalauncher  (2026-11-18)

## Corpus Check
- 62 files · ~18,896 words (11 changed files re-extracted in this update)
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 666 nodes · 1358 edges · 28 communities (15 shown, 13 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 154 edges (avg confidence: 0.78)
- Token cost: 0 input · 0 output self-reported (chunk agents rarely report counts; real spend higher — this update also burned 3 failed agent runs at ~$0.01 each before the reply-channel workaround)

## Community Hubs (Navigation)
- Platform App Discovery
- Folder Persistence
- Workspace View Internals
- Grid Canvas Geometry
- Drawer View Behavior
- Dock Ordering
- Folder Dialog UI
- App Search
- Drawer List Adapters
- Folder Contents Adapter
- Drawer Fling-to-Close
- Home Activity Orchestration
- Icon Rendering & Framework
- Resolver & Seeding
- App Model & Controller
- About Screen UI
- CI Release Workflow
- Architecture & README Docs
- Current Home Screenshot
- Pixel Reference Screenshot
- Home Grid Screenshot
- Work Profile Reference
- LauncherUiState (isolated)
- Clean-Room & Process Docs

## God Nodes (most connected - your core abstractions)
1. `LauncherApp` - 81 edges
2. `WorkspaceRepository` - 36 edges
3. `WorkspaceView` - 31 edges
4. `LauncherController` - 31 edges
5. `HomeActivity` - 30 edges
6. `AppKey` - 23 edges
7. `DrawerView` - 20 edges
8. `WorkspaceItem` - 20 edges
9. `Query: what does LauncherApp connect (corrected)` - 20 edges
10. `Query: which docs are not tied to code (corrected)` - 19 edges

## Surprising Connections (you probably didn't know these)
- `Speed First Principle` --conceptually_related_to--> `HomeActivity`  [INFERRED]
  PRODUCT.md → docs/architecture/OVERVIEW.md
- `Live Package Updates` --references--> `PackageChangeMonitor`  [INFERRED]
  README.md → docs/architecture/OVERVIEW.md
- `CI Release Pipeline` --semantically_similar_to--> `Release Process`  [INFERRED] [semantically similar]
  README.md → docs/DEVELOPMENT.md
- `Query: what does LauncherApp connect` --conceptually_related_to--> `AppIconView`  [INFERRED]
  graphify-out/memory/query_20261117_225540_0cb8e4aa_what_does_launcherapp_connect.md → docs/architecture/OVERVIEW.md
- `Query: what does LauncherApp connect (corrected)` --conceptually_related_to--> `AppIconView`  [INFERRED]
  graphify-out/memory/query_20261117_225625_5c27a6ec_what_does_launcherapp_connect.md → docs/architecture/OVERVIEW.md

## Import Cycles
- None detected.

## Communities (28 total, 13 thin omitted)

### Community 0 - "Platform App Discovery"
Cohesion: 0.05
Nodes (11): AppDiscovery, AppLauncher, LauncherAppsAppDiscovery, HomeRoleChecker, PackageChangeMonitor, PackageEvent, Added, Removed (+3 more)

### Community 1 - "Folder Persistence"
Cohesion: 0.06
Nodes (7): LauncherDatabase, FolderDao, FolderEntity, WorkspaceItemDao, WorkspaceItemEntity, toDomain(), toEntity()

### Community 10 - "Workspace View Internals"
Cohesion: 0.10
Nodes (4): WorkspaceView, CellContent, App, FolderCell

### Community 3 - "Home Activity Orchestration"
Cohesion: 0.08
Nodes (9): Folder, HomeActivity, DragSource, Container, LauncherMessage, DOCK, WORKSPACE, DOCK_FULL (+1 more)

### Community 4 - "Icon Rendering & Framework"
Cohesion: 0.06
Nodes (3): AppIconView, DockView, LauncherDragLayer

### Community 5 - "Resolver & Seeding"
Cohesion: 0.11
Nodes (7): SeedPlanner, SeedPlannerTest, AppKey, WorkspaceItem, WorkspaceResolver, WorkspaceResolverFolderTest, WorkspaceResolverTest

### Community 6 - "App Model & Controller"
Cohesion: 0.09
Nodes (4): LauncherApp, LauncherController, LauncherUiState, WorkspaceRepository

### Community 8 - "About Screen UI"
Cohesion: 0.06
Nodes (3): SettingsActivity, AboutScreen(), VesnaTheme()

### Community 9 - "CI Release Workflow"
Cohesion: 0.06
Nodes (35): name, runs-on, steps, RELEASE_KEYSTORE_B64, VESNA_RELEASE_KEY_ALIAS, VESNA_RELEASE_KEY_PASSWORD, VESNA_RELEASE_KEYSTORE, VESNA_RELEASE_STORE_PASSWORD (+27 more)

### Community 2 - "Architecture & README Docs"
Cohesion: 0.11
Nodes (51): Query: what does LauncherApp connect, Query: what does LauncherApp connect (corrected), LauncherUiState, Path query: trace LauncherUiState -> LauncherController -> WorkspaceResolver, PackageEvent (Added/Removed/Replaced), Query: what connects Added/Removed/WORKSPACE_FULL to the system, LauncherDatabase, Query: Explain WorkspaceRepository (+43 more)

### Community 16 - "Current Home Screenshot"
Cohesion: 0.29
Nodes (10): Android Home Launcher Layout Pattern (workspace grid + dock + gesture nav), Bottom Dock Row: Calendar, Camera, Chrome, Clock, Folder Icon on Workspace (brown folder, contents not visible), Gesture Navigation Pill (no button bar), Home Screen Screenshot (1080x2400, current state capture), Launcher Identity: suspected Vesna Launcher build under test (unverified), Status Bar: 12:48, shield badge, 5G signal, battery charging, Dark Abstract Wallpaper (mountain silhouette) (+2 more)

### Community 19 - "Pixel Reference Screenshot"
Cohesion: 0.33
Nodes (8): 4-Column App Grid Workspace, Gesture Navigation Pill, App Icons with Text Labels, Persistent Bottom Dock (4 Fixed Apps), Status Bar (Time, 5G, Charging Battery), Circular Themed Icon Set, On-Workspace Folder, Android Launcher Home Screen Screenshot

### Community 20 - "Home Grid Screenshot"
Cohesion: 0.28
Nodes (9): App Icon Grid Layout (5 rows: Contacts/Drive/Files, Gmail/Google/Maps/Messages, Folder/Voice Access/Safety, Settings/Carrier/YouTube, Clock/Calendar), Carrier App Icon (label reads 'TMoble' - SIM-card-with-gear glyph), Bottom Dock (Play Store, Camera, Chrome), Home Screen Folder Icon (brown folder preview with 3 app dots), Gesture Navigation Pill (bottom white handle), Vesna Launcher Home Screen Design Reference (stock-style launcher layout captured in repo), Status Bar (12:36, shield icon, 5G, signal bars, charging battery), Dark Abstract Wallpaper (diagonal pale wave band over black) (+1 more)

### Community 21 - "Work Profile Reference"
Cohesion: 0.38
Nodes (7): Vesna Launcher Home Screen Layout Reference, Android Home Screen Screenshot, App Icon Grid (4 columns, icon-over-label rows), Bottom Dock / Hotseat (Calendar, Camera, Chrome, Clock), Google App Icons (Contacts, Drive, Files, Gmail, Google, Maps, Messages, Phone, YouTube, Chrome, Calendar, Camera, Clock), Status Bar (time 1:09, 5G, battery charging), Work Folder (work-profile folder tile)

### Community 7 - "Clean-Room & Process Docs"
Cohesion: 0.06
Nodes (33): Live Package Updates, AOSP (including Launcher3), License Compliance (MIT + Apache 2.0), Nova Launcher (reference example), Vesna Launcher, Independent Development Policy, android.yml CI Workflow, CI Quality Gates (+25 more)

## Ambiguous Edges - Review These
- `Home Screen Screenshot (1080x2400, current state capture)` → `Launcher Identity: suspected Vesna Launcher build under test (unverified)`  [AMBIGUOUS]
  .now.png · relation: references
- `Carrier App Icon (label reads 'TMoble' - SIM-card-with-gear glyph)` → `Android Home Screen Screenshot (.cur.png)`  [AMBIGUOUS]
  .cur.png · relation: references

## Knowledge Gaps
- **33 isolated node(s):** `Added`, `Removed`, `DOCK`, `WORKSPACE`, `DOCK_FULL` (+28 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 166 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `LauncherApp` connect `App Model & Controller` to `Platform App Discovery`, `Folder Persistence`, `Home Activity Orchestration`, `Icon Rendering & Framework`, `Resolver & Seeding`, `Workspace View Internals`, `Grid Canvas Geometry`, `Drawer View Behavior`, `Dock Ordering`, `Folder Dialog UI`, `App Search`, `Drawer List Adapters`, `Folder Contents Adapter`?**
  _High betweenness centrality (0.298) - this node is a cross-community bridge._
- **What connects `Added`, `Removed`, `DOCK` to the rest of the system?**
  _33 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Platform App Discovery` be split into smaller, more focused modules?**
  _Cohesion score 0.05310734463276836 - nodes in this community are weakly interconnected._
- **Why does `WorkspaceRepository` connect `App Model & Controller` to `Platform App Discovery`, `Folder Persistence`, `Home Activity Orchestration`, `Resolver & Seeding`, `Grid Canvas Geometry`, `Dock Ordering`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `Folder Persistence` be split into smaller, more focused modules?**
  _Cohesion score 0.0576271186440678 - nodes in this community are weakly interconnected._

### Low-confidence Hints
_AMBIGUOUS edges — the extractor was unsure. Verify before acting on these._

- **What is the exact relationship between `Home Screen Screenshot (1080x2400, current state capture)` and `Launcher Identity: suspected Vesna Launcher build under test (unverified)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Carrier App Icon (label reads 'TMoble' - SIM-card-with-gear glyph)` and `Android Home Screen Screenshot (.cur.png)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._

## Update notes (2026-11-18, --update audit)

- **Forced overwrite justification:** the #479-family guard refused the write over 3 missing semantic ids. Two are verified renames of the same concepts under the new extraction's id normalization (`docs_architecture_overview_architecture_overview` → `..._architectureoverview`; `docs_architecture_overview_room_database` → `..._roomdatabase`) — no content lost.
- **`LauncherUiState` doc node was a genuine extraction miss** (OVERVIEW names it in Data flow): re-injected by hand from the verified doc/code text (`LauncherController.kt`), not present in the subagent chunk.
- **10 `graphify-out/memory/query_*.md` entries were cached as partial** (read 1–3 lines short). Tail inspection shows the unread lines are cited-node lists already represented as graph edges, so the partial markers were cleared on evidence — no re-extraction queued.
- **Token cost basis:** chunk self-reports only; this update also spent 3 failed agent runs (~$0.01 each) on a flaky write channel before the JSON-via-reply workaround.