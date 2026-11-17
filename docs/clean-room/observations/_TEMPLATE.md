# Observation Template

Copy this file to `observations/<area>.md` and fill one entry per tested
behavior. Evidence IDs point into the restricted local capture store (never
committed). Do not commit Nova screenshots or verbatim UI text.

```
## OBS-<AREA>-<NNN> — <short behavior name>

- Date / Env: 2026-NN-NN / ENV-N
- Navigation path: Home ▸ … ▸ …
- Initial state: <relevant settings/current layout before the test>
- Action: <exact interaction performed>
- Expected (before running): <what the UI label/assumption suggested>
- Observed: <what actually happened>
- Variations: <other values tried and results>
- Persistence: <survives navigation / restart / reboot?>
- Dependencies: <settings/features this depends on or conflicts with>
- Evidence: EVI-<AREA>-<NNN> (screenshots, UI dumps)
- Confidence: Observed / Partially Observed / Inferred / Not Tested
- Open questions: <anything unverified>
```

Rules:

1. One behavior per OBS entry; keep entries small and factual.
2. Record negative results ("tap did nothing") — they are findings too.
3. Never mark Observed unless you actually ran the interaction and captured
   the result.
