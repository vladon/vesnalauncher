package dev.vladon.vesna.launcher.settings

/** Home gestures a user can bind an action to (#183 phase 1). */
enum class GestureId(val preferenceKey: String) {
    SWIPE_DOWN("gesture_swipe_down"),
    DOUBLE_TAP("gesture_double_tap"),
    TWO_FINGER_SWIPE_UP("gesture_two_finger_swipe_up"),
    PINCH_IN("gesture_pinch_in"),
    PINCH_OUT("gesture_pinch_out");

    companion object {
        val ALL: List<GestureId> = entries.toList()
    }
}
