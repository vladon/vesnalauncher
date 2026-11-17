# Specification Template

Copy to `specifications/<area>.md`. A specification records approved,
implementation-independent behavior — never Nova implementation details,
asset reuse, or verbatim proprietary text.

```
### <AREA>-<NNN> — <Feature name>

- Description: <original functional description>
- Preconditions: <required state, settings, permissions>
- Trigger: <user or system event>
- Expected behavior: <precise observable result>
- State transitions: <before → after states>
- Configuration parameters: <options, defaults, ranges — as observed>
- Boundary conditions: <min/max/invalid cases>
- Interactions: <dependencies/conflicts with other features>
- Persistence: <what survives restart/reboot>
- Android integration: <relevant public platform behavior>
- Evidence: OBS-<AREA>-<NNN> / EVI-<AREA>-<NNN>
- Confidence: Observed / Partially Observed / Inferred / Not Tested
- Vesna requirements: <numbered R<AREA>-<NNN>.x requirements>
- Open questions: <unresolved items; link reviews/OPEN_QUESTIONS.md>
```
