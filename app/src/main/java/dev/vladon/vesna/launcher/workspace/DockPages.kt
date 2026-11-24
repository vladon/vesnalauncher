package dev.vladon.vesna.launcher.workspace

/**
 * Pure arithmetic for the dock's capacity and paging. Pins live in one saved
 * order; a page shows a consecutive slice of it, so extra pins flow onto the
 * next page in order and a released slot pulls the rest back — the approved
 * overflow policy for #115. At most [MAX_PAGES] pages exist, which is what
 * makes a capacity reduction rejectable.
 */
object DockPages {

    /** The dock never splits into more than two pages. */
    const val MAX_PAGES = 2

    const val MIN_CAPACITY = 2

    const val MAX_CAPACITY = 8

    /** The shipped default: four favorite slots per page. */
    const val DEFAULT_CAPACITY = GridCanvas.DOCK_CAPACITY

    /** Slots a [capacityPerPage]-slot dock offers across all of its pages. */
    fun totalCapacity(capacityPerPage: Int): Int = capacityPerPage * MAX_PAGES

    /** False when a dock of [capacityPerPage] slots per page cannot hold every pin. */
    fun canHold(pinCount: Int, capacityPerPage: Int): Boolean =
        pinCount <= totalCapacity(capacityPerPage)

    /** Coerces a raw preference value into the supported capacity range. */
    fun coerce(capacity: Int): Int = capacity.coerceIn(MIN_CAPACITY, MAX_CAPACITY)

    /** Pages needed to show [pinCount] pins; a dock always has at least one. */
    fun pageCount(pinCount: Int, capacityPerPage: Int): Int = when {
        pinCount <= 0 -> 1
        else -> minOf(MAX_PAGES, (pinCount + capacityPerPage - 1) / capacityPerPage)
    }

    /** The consecutive slice of [items] shown on zero-based [page]. */
    fun <T> page(items: List<T>, page: Int, capacityPerPage: Int): List<T> =
        items.drop(page * capacityPerPage).take(capacityPerPage)

    /** Clamps a requested page into the pages the pins actually fill. */
    fun coercePage(page: Int, pinCount: Int, capacityPerPage: Int): Int =
        page.coerceIn(0, pageCount(pinCount, capacityPerPage) - 1)

    /** The saved-order slot behind a page-local column. */
    fun globalSlot(page: Int, localColumn: Int, capacityPerPage: Int): Int =
        page * capacityPerPage + localColumn
}
