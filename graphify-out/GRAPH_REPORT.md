# Graph Report - vesnalauncher  (2026-11-19)

## Corpus Check
- 70 files · ~43,414 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 42 file(s) not represented in the graph (top: .xml 33, (none) 3, .properties 2)
- WARNING: 42 of 127 files seen (33%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 697 nodes · 1414 edges · 30 communities (16 shown, 14 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 154 edges (avg confidence: 0.78)
- Token cost: 4,500 input · 1,400 output

## Graph Freshness
- Built from commit: `cec5f126`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Platform App Discovery
- Folder Persistence
- Architecture & README Docs
- Home Activity Orchestration
- DockView
- Resolver & Seeding
- DockOrderingTest
- Clean-Room & Process Docs
- About Screen UI
- CI Release Workflow
- Workspace View Internals
- Grid Canvas Geometry
- DrawerView
- LauncherApp
- WorkspaceItemDao
- App Search
- Current Home Screenshot
- DrawerAppsAdapter.kt
- Pixel Reference Screenshot
- Home Grid Screenshot
- Work Profile Reference
- AppIconView.kt
- LauncherUiState (isolated)
- LauncherDragLayer.kt
- CellContent

## God Nodes (most connected - your core abstractions)
1. `LauncherApp` - 83 edges
2. `WorkspaceRepository` - 36 edges
3. `WorkspaceView` - 31 edges
4. `LauncherController` - 31 edges
5. `HomeActivity` - 30 edges
6. `AppKey` - 25 edges
7. `DrawerView` - 21 edges
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

## Communities (30 total, 14 thin omitted)

### Community 0 - "Platform App Discovery"
Cohesion: 0.05
Nodes (11): AppDiscovery, AppLauncher, LauncherAppsAppDiscovery, HomeRoleChecker, PackageChangeMonitor, PackageEvent, Added, Removed (+3 more)

### Community 1 - "Folder Persistence"
Cohesion: 0.09
Nodes (4): FolderDao, FolderEntity, LauncherDatabase, toEntity()

### Community 2 - "Architecture & README Docs"
Cohesion: 0.11
Nodes (51): AboutScreen, AppDiscovery, AppIconView, AppKey, AppLauncher, AppSearch, CellContent, DockOrdering (+43 more)

### Community 3 - "Home Activity Orchestration"
Cohesion: 0.06
Nodes (10): FolderDialog, HomeActivity, DragSource, LauncherMessage, DOCK_FULL, WORKSPACE_FULL, LauncherUiState, Container (+2 more)

### Community 5 - "Resolver & Seeding"
Cohesion: 0.16
Nodes (7): AppKey, toDomain(), Folder, WorkspaceItem, WorkspaceResolver, WorkspaceResolverFolderTest, WorkspaceResolverTest

### Community 7 - "Clean-Room & Process Docs"
Cohesion: 0.06
Nodes (33): Architecture Overview, SettingsActivity, AOSP (including Launcher3), Independent Development Policy, License Compliance (MIT + Apache 2.0), Nova Launcher (reference example), android.yml CI Workflow, CI Quality Gates (+25 more)

### Community 8 - "About Screen UI"
Cohesion: 0.05
Nodes (15): AboutScreen(), VesnaTheme(), SettingsActivity, Android Adaptive Icon, Android handoff and acceptance, Components, Logo, Material 3 color (+7 more)

### Community 9 - "CI Release Workflow"
Cohesion: 0.06
Nodes (35): name, runs-on, steps, RELEASE_KEYSTORE_B64, VESNA_RELEASE_KEY_ALIAS, VESNA_RELEASE_KEY_PASSWORD, VESNA_RELEASE_KEYSTORE, VESNA_RELEASE_STORE_PASSWORD (+27 more)

### Community 12 - "DrawerView"
Cohesion: 0.07
Nodes (3): DrawerSearchTest, DrawerView, SimpleOnItemTouchListener

### Community 13 - "LauncherApp"
Cohesion: 0.08
Nodes (5): LauncherApp, LauncherController, SeedPlanner, WorkspaceRepository, SeedPlannerTest

### Community 16 - "Current Home Screenshot"
Cohesion: 0.29
Nodes (10): Bottom Dock Row: Calendar, Camera, Chrome, Clock, Folder Icon on Workspace (brown folder, contents not visible), Gesture Navigation Pill (no button bar), Android Home Launcher Layout Pattern (workspace grid + dock + gesture nav), Home Screen Screenshot (1080x2400, current state capture), Launcher Identity: suspected Vesna Launcher build under test (unverified), Status Bar: 12:48, shield badge, 5G signal, battery charging, Dark Abstract Wallpaper (mountain silhouette) (+2 more)

### Community 17 - "DrawerAppsAdapter.kt"
Cohesion: 0.15
Nodes (4): DrawerAppsAdapter, Holder, FolderContentsAdapter, Holder

### Community 19 - "Pixel Reference Screenshot"
Cohesion: 0.33
Nodes (8): 4-Column App Grid Workspace, Gesture Navigation Pill, App Icons with Text Labels, Android Launcher Home Screen Screenshot, Persistent Bottom Dock (4 Fixed Apps), Status Bar (Time, 5G, Charging Battery), Circular Themed Icon Set, On-Workspace Folder

### Community 20 - "Home Grid Screenshot"
Cohesion: 0.28
Nodes (9): App Icon Grid Layout (5 rows: Contacts/Drive/Files, Gmail/Google/Maps/Messages, Folder/Voice Access/Safety, Settings/Carrier/YouTube, Clock/Calendar), Carrier App Icon (label reads 'TMoble' - SIM-card-with-gear glyph), Bottom Dock (Play Store, Camera, Chrome), Home Screen Folder Icon (brown folder preview with 3 app dots), Gesture Navigation Pill (bottom white handle), Vesna Launcher Home Screen Design Reference (stock-style launcher layout captured in repo), Android Home Screen Screenshot (.cur.png), Status Bar (12:36, shield icon, 5G, signal bars, charging battery) (+1 more)

### Community 21 - "Work Profile Reference"
Cohesion: 0.38
Nodes (7): Android Home Screen Screenshot, App Icon Grid (4 columns, icon-over-label rows), Bottom Dock / Hotseat (Calendar, Camera, Chrome, Clock), Google App Icons (Contacts, Drive, Files, Gmail, Google, Maps, Messages, Phone, YouTube, Chrome, Calendar, Camera, Clock), Vesna Launcher Home Screen Layout Reference, Status Bar (time 1:09, 5G, battery charging), Work Folder (work-profile folder tile)

### Community 29 - "CellContent"
Cohesion: 0.40
Nodes (3): CellContent, App, FolderCell

## Ambiguous Edges - Review These
- `Home Screen Screenshot (1080x2400, current state capture)` → `Launcher Identity: suspected Vesna Launcher build under test (unverified)`  [AMBIGUOUS]
  .now.png · relation: references
- `Carrier App Icon (label reads 'TMoble' - SIM-card-with-gear glyph)` → `Android Home Screen Screenshot (.cur.png)`  [AMBIGUOUS]
  .cur.png · relation: references

## Knowledge Gaps
- **33 isolated node(s):** `Added`, `Removed`, `DOCK_FULL`, `WORKSPACE_FULL`, `DOCK` (+28 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 176 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `LauncherApp` connect `LauncherApp` to `Platform App Discovery`, `Folder Persistence`, `Home Activity Orchestration`, `DockView`, `Resolver & Seeding`, `DockOrderingTest`, `Workspace View Internals`, `DrawerView`, `App Search`, `DrawerAppsAdapter.kt`, `WorkspaceView.kt`, `AppIconView.kt`, `CellContent`?**
  _High betweenness centrality (0.294) - this node is a cross-community bridge._
- **What connects `Added`, `Removed`, `DOCK_FULL` to the rest of the system?**
  _33 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Platform App Discovery` be split into smaller, more focused modules?**
  _Cohesion score 0.05245901639344262 - nodes in this community are weakly interconnected._
- **Why does `WorkspaceRepository` connect `LauncherApp` to `Platform App Discovery`, `Folder Persistence`, `Resolver & Seeding`, `DockOrderingTest`, `WorkspaceItemDao`?**
  _High betweenness centrality (0.059) - this node is a cross-community bridge._
- **Should `Folder Persistence` be split into smaller, more focused modules?**
  _Cohesion score 0.08558558558558559 - nodes in this community are weakly interconnected._

### Low-confidence Hints
_AMBIGUOUS edges — the extractor was unsure. Verify before acting on these._

- **What is the exact relationship between `Home Screen Screenshot (1080x2400, current state capture)` and `Launcher Identity: suspected Vesna Launcher build under test (unverified)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Carrier App Icon (label reads 'TMoble' - SIM-card-with-gear glyph)` and `Android Home Screen Screenshot (.cur.png)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._