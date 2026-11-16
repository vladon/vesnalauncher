# Independent Development Policy

Vesna Launcher is developed independently. This document defines what that means
in practice: which sources of requirements and knowledge are acceptable, and
which uses of third-party products are prohibited.

> **Note on terminology.** This is *not* a formal organizational clean room in the
> strict legal sense (a physically and contractually segregated team that never
> sees the reference product at all). The same team both defines requirements and
> writes the code. We therefore call this an **independent development process**:
> the implementation must never be derived from proprietary implementations of
> other products.

## Purpose

Vesna Launcher ships under the MIT license. To keep the project legally clean and
ethically sound, every line of code, every resource, and every design decision
must originate from our own work or from properly licensed public sources.

## Third-party launchers (e.g. Nova Launcher)

Third-party launchers may be used **only as functional and UX references**:

**Allowed**

- Using a launcher yourself and noting *what* a feature does from a user's point
  of view (e.g. "the drawer opens with a swipe-up gesture and lists all apps in
  alphabetical order").
- Expressing such observations as requirements in PRODUCT.md, GitHub issues, and
  design documents, written in our own words.
- Deciding independently *how* to implement a feature, using public Android APIs.

**Prohibited**

- Copying, transcribing, or adapting any source code of a proprietary launcher.
- Decompiling, disassembling, or otherwise reverse-engineering launcher binaries
  (APK, DEX, native libraries), including automated analysis of resources or code.
- Extracting or reusing graphics, icons, layouts, strings, animations, or any
  other assets from a proprietary launcher.
- Reproducing file formats, database schemas, or protocol designs copied from a
  proprietary implementation.
- Basing our documentation or specs on extracted internal documentation of
  another product.

## Acceptable sources of requirements and knowledge

- Official Android documentation and API references (developer.android.com).
- The Android Open Source Project (AOSP), including its sample code and the
  AOSP Launcher3 source, under its applicable open-source licenses (with
  attribution and license compliance).
- Publicly available official documentation of libraries we use (AndroidX,
  Room, Compose, Kotlin).
- First-hand usage of published launchers, expressed as functional requirements
  (see above).
- Product decisions made by this project's maintainers and contributors.

## Prohibited sources

- Any proprietary or restricted-licensed source code.
- Decomiled or disassembled artifacts of any third-party app.
- Confidential documentation, internal design docs, or leaked materials of
  third parties.

## Practical rules for contributors

1. When proposing a feature inspired by another launcher, describe the *user
   visible behavior*, not an implementation: "when X happens, the launcher
   shows Y" is fine; "call class Z like product P does" is not.
2. If you have accidentally reviewed proprietary launcher code, disclose it in
   the relevant issue or pull request so maintainers can evaluate independence.
3. All committed code must be written for this project. Do not commit code
   generated from prompts that quoted proprietary implementations.
4. Third-party libraries must be widely used under permissive licenses
   (Apache 2.0 / MIT) and declared in the Gradle build files.
5. Documentation and code comments must not reference proprietary internals of
   other products.

## License compliance

- Vesna Launcher code is MIT-licensed (see [LICENSE](../../LICENSE)).
- Dependencies keep their own licenses; MIT code combined with Apache 2.0
  libraries is compatible and standard for Android projects.
- The name "Vesna Launcher" and its branding belong to this project; do not use
  third-party product names in app resources, package names, or marketing
  materials.
