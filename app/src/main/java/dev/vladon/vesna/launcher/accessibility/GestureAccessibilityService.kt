package dev.vladon.vesna.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * The gesture-only accessibility service (#183): performs global actions
 * (the notification expansion fallback for the platforms that restrict the
 * status bar reflection) and nothing else. Reads no window content and
 * observes no accessibility events; the user enables it explicitly in the
 * system accessibility settings.
 */
class GestureAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    companion object {
        /** The connected instance, or null while the service is unbound. */
        internal var instance: GestureAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
