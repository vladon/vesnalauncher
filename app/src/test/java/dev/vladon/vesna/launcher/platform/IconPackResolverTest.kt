package dev.vladon.vesna.launcher.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The documented ADW appfilter mapping contract, verified against Arcticons. */
class IconPackResolverTest {

    @Test
    fun `parses component entries into package-class keys`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <resources>
                <!-- #C-pla -->
                <item component="ComponentInfo{li.yapp.appAF4B724D/li.yapp.sdk.YLSplashActivity}" drawable="c_pla"/>
                <item component="ComponentInfo{com.android.chrome/com.android.chrome.Main}" drawable="google_chrome"/>
            </resources>
        """.trimIndent()

        val map = parseAppfilter(xml)

        assertEquals(
            mapOf(
                "li.yapp.appAF4B724D/li.yapp.sdk.YLSplashActivity" to "c_pla",
                "com.android.chrome/com.android.chrome.Main" to "google_chrome",
            ),
            map,
        )
    }

    @Test
    fun `first entry wins for a duplicated component`() {
        val xml = """
            <resources>
                <item component="ComponentInfo{com.a.b/com.a.b.Main}" drawable="first"/>
                <item component="ComponentInfo{com.a.b/com.a.b.Main}" drawable="second"/>
            </resources>
        """.trimIndent()

        assertEquals("first", parseAppfilter(xml)["com.a.b/com.a.b.Main"])
    }

    @Test
    fun `skips malformed and unrelated lines`() {
        val xml = """
            <resources>
                <category title="New" />
                <item drawable="no_component_attribute" />
                <item component="ComponentInfo{only.package}" drawable="no_activity" />
                <item component="ComponentInfo{com.ok/com.ok.Main}" drawable="ok" />
                <item component="ComponentInfo{com.bad/com.bad.Main}" drawable="" />
            </resources>
        """.trimIndent()

        val map = parseAppfilter(xml)

        assertEquals("ok", map["com.ok/com.ok.Main"])
        assertTrue(map.keys.none { it.contains("only.package") || it.contains("com.bad") })
    }

    @Test
    fun `handles whitespace variations`() {
        val xml = """
            <resources>
                <item  component="ComponentInfo{com.space/com.space.Main}"    drawable="spaced"/>
            </resources>
        """.trimIndent()

        assertEquals("spaced", parseAppfilter(xml)["com.space/com.space.Main"])
    }
}
