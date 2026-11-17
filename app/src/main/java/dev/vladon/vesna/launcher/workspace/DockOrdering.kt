package dev.vladon.vesna.launcher.workspace

/**
 * Pure slot arithmetic for the dock's drag-and-drop. Dock entries are identified by
 * their ids and ordered by slot index; all functions return the desired new order as
 * a list of ids, which the repository then persists as sequential slot indexes.
 */
object DockOrdering {

    /**
     * The order after removing the entry at [fromSlot] and re-inserting it at
     * [toSlot] (0-based within the current size). Slot values are clamped to the
     * valid range, so an out-of-range target degrades to append.
     */
    fun moveTo(ids: List<Long>, fromSlot: Int, toSlot: Int): List<Long> {
        if (fromSlot !in ids.indices) return ids
        val mutable = ids.toMutableList()
        val id = mutable.removeAt(fromSlot)
        mutable.add(toSlot.coerceIn(0, mutable.size), id)
        return mutable
    }

    /**
     * The order after inserting [id] at [insertSlot] (clamped to the valid range).
     * Used when dragging an app from the workspace into the dock.
     */
    fun insertAt(ids: List<Long>, id: Long, insertSlot: Int): List<Long> {
        if (id in ids) return ids
        val mutable = ids.toMutableList()
        mutable.add(insertSlot.coerceIn(0, mutable.size), id)
        return mutable
    }
}
