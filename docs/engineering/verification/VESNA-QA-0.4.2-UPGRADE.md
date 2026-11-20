# Signed release upgrade: Vesna 0.4.1 → 0.4.2

**Verdict:** Pass on the fresh Android 15/API 35 userdebug AVD `Vesna_Signed_Upgrade_QA` (`emulator-5572`). No other device was modified.

Both APKs came from GitHub Releases and `apksigner` verified the same Vesna certificate (`21a9fb9c090d2eafd34c0766b225f743225e87061ddc76922e4900f22a2e1c58`).

| Release | Version code | APK SHA-256 |
| --- | --- | --- |
| `0.4.1` | 6 | `1a19fa4e397b851d6d488e7c09281d7e96fd58049ce139e274c3a3ab39ea8b23` |
| `0.4.2` | 7 | `4cf18b367dbf6c4bc8be42136a7b8f0bcb225f394aec915ec3d5017568f7de42` |

1. Installed 0.4.1 and opened Home. Created a Drive/Files folder at cell 2 and moved Google from cell 4 to cell 14. The four seeded dock pins remained in place.
2. Force-stopped the launcher and captured the schema version, all 18 workspace/dock item rows, the folder table, and the `workspace_seeded` preference via read-only queries. Root was enabled only on the dedicated userdebug guest to read the release app's private data.
3. Installed 0.4.2 in place using serial-pinned `adb install -r`; did not uninstall or clear the app. Confirmed version code 7, cold-launched Home, and observed the folder, moved Google pin, and dock in the UI hierarchy.
4. Force-stopped and repeated the read-only capture. The two dumps matched byte-for-byte, including IDs, package/activity keys, positions, dock order, folder memberships, and the seeding flag. Both published versions use Room schema 3.

This verifies the personal-profile signed APK upgrade, not the separate local Room 3→4 migration or a device-to-device restore.
