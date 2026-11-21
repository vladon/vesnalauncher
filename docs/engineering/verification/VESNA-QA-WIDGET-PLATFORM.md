# ENG-102 platform check: binding denial and interrupted allocation

**Verdict:** The isolated host recovered unbound IDs after process death and cleaned them up after Android binding denial on both test images. This is a platform check for [ADR-WIDGET-1](../../adr/0003-widget-host-lifecycle.md), not a test of a shipped Vesna widget feature.

| Device | Cold restart after deliberately orphaning an ID | Android system denial |
| --- | --- | --- |
| Android 8 (API 26), `Vesna_QA_MinSdk_API_26` | Reopened with `Owned=[]`; ID 4 released | Result contained no widget ID. The host still owned the ID before explicit cleanup, and `Owned=[]` after cleanup. |
| Android 15 (API 35), `Vesna_Pages_V4_QA` | Reopened with `Owned=[]`; ID 2 released | Result contained no widget ID. The host still owned the ID before explicit cleanup, and `Owned=[]` after cleanup. |

## Procedure and implications

- Installed two separate test APKs on read-only emulators: `dev.vesna.qa.widgethost` and `dev.vesna.qa.widgetprovider`. The provider declares a real Android configuration activity. Both packages are isolated from Vesna production code and data.
- Allocated an unbound ID in the test host, confirmed it was enumerated, then force-stopped the host. Relaunched with the same host key, enumerated its IDs, and explicitly deleted the orphan. No pending or committed fixture placement remained.
- Launched Android's system binding UI for that provider and dismissed it without granting access. The host used its locally tracked pending ID rather than relying on result extras, then deleted it. Repeated denial during teardown and confirmed that no IDs remained for the fixture host.
- On the API 26 image, `uiautomator dump` could fail in Android's accessibility layer while an unbound ID existed. Screenshots and host logs were used for that state. Check assistive technology on API 26 before claiming widget placement accessibility.

## Scope and evidence

One-time binding was not authorized in this run. Provider configuration cancellation and success, committed widget restart, package removal, actual Vesna Room occupancy, and restore were not tested. No permissions were granted. Both disposable devices were stopped; the attached phone was not touched. Source, build helper, captured screenshots and per-API logs are in `D:/orca/scratch/vesna-widget-lifecycle`.
