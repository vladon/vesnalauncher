# Vesna Launcher keeps its release build minified with R8.
#
# No custom keep rules are required today: Room, Compose, and AndroidX ship their own
# consumer rules, and the launcher reaches installed apps exclusively through public
# framework APIs (PackageManager / Intents) that R8 cannot touch.
#
# When adding reflection or serialization, document the required rules here.
