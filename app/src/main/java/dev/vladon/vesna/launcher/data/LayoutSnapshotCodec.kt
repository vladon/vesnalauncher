package dev.vladon.vesna.launcher.data

import dev.vladon.vesna.launcher.workspace.DockPages
import dev.vladon.vesna.launcher.workspace.GridCanvas
import dev.vladon.vesna.launcher.workspace.MAX_WORKSPACE_PAGES
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned layout snapshot for user-controlled export and restore
 * (ADR-BACKUP-1 part 1, issue #116). The codec is the only code that knows the
 * on-disk shape; [validate] enforces the version 1 rules documented there:
 * the whole file is valid or the whole file is rejected, with no partial
 * record skipping.
 *
 * Pages are carried as a rank list in display order; pins and folders
 * reference a page by its position in that list and folder members reference
 * a folder by its position in the folder list. Restore remaps these positions
 * onto freshly inserted Room rows inside one transaction, so no auto-generated
 * id ever crosses devices. Only personal-profile rows are carried; restore
 * maps everything onto the destination's personal profile.
 */
internal data class LayoutSnapshotV1(
    val columns: Int,
    val rows: Int,
    val seeded: Boolean,
    /** Page ranks in display order; position in this list is the page reference. */
    val pages: List<Int>,
    /** Dock pins in dock order. */
    val dock: List<DockPinSnapshot>,
    /** Folders in (page position, cell) order; index in this list is the folder reference. */
    val folders: List<FolderSnapshot>,
    /** Workspace pins, including folder members (position references). */
    val pins: List<PinSnapshot>,
) {
    /** Every distinct package name the snapshot carries, in stable order. */
    fun packages(): List<String> =
        (dock.map { it.packageName } + pins.map { it.packageName }).distinct()

    fun toJson(): String {
        val root = JSONObject()
        root.put(VERSION_KEY, FORMAT_VERSION)
        root.put(COLUMNS_KEY, columns)
        root.put(ROWS_KEY, rows)
        root.put(SEEDED_KEY, seeded)
        root.put(PAGES_KEY, JSONArray(pages))
        root.put(
            DOCK_KEY,
            JSONArray().apply {
                dock.forEach {
                    put(
                        JSONObject()
                            .put(PACKAGE_KEY, it.packageName)
                            .put(CLASS_KEY, it.className)
                            .putOpt(SHORTCUT_KEY, it.shortcutId)
                            .putOpt(CUSTOM_LABEL_KEY, it.customLabel)
                            .putOpt(SHORTCUT_LABEL_KEY, it.shortcutLabel)
                    )
                }
            },
        )
        root.put(
            FOLDERS_KEY,
            JSONArray().apply {
                folders.forEach {
                    put(
                        JSONObject()
                            .put(TITLE_KEY, it.title)
                            .put(PAGE_KEY, it.pagePosition)
                            .put(CELL_KEY, it.cell)
                    )
                }
            },
        )
        root.put(
            PINS_KEY,
            JSONArray().apply {
                pins.forEach {
                    put(
                        JSONObject()
                            .put(PACKAGE_KEY, it.packageName)
                            .put(CLASS_KEY, it.className)
                            .put(PAGE_KEY, it.pagePosition)
                            .put(CELL_KEY, it.cell)
                            .putOpt(FOLDER_KEY, it.folderIndex)
                            .putOpt(SHORTCUT_KEY, it.shortcutId)
                            .putOpt(CUSTOM_LABEL_KEY, it.customLabel)
                            .putOpt(SHORTCUT_LABEL_KEY, it.shortcutLabel)
                    )
                }
            },
        )
        return root.toString()
    }

    companion object {
        const val FORMAT_VERSION = 1
        private const val VERSION_KEY = "version"
        private const val COLUMNS_KEY = "columns"
        private const val ROWS_KEY = "rows"
        private const val SEEDED_KEY = "seeded"
        private const val PAGES_KEY = "pages"
        private const val DOCK_KEY = "dock"
        private const val FOLDERS_KEY = "folders"
        private const val PINS_KEY = "pins"
        private const val PACKAGE_KEY = "package"
        private const val CLASS_KEY = "class"
        private const val PAGE_KEY = "page"
        private const val CELL_KEY = "cell"
        private const val FOLDER_KEY = "folder"
        private const val TITLE_KEY = "title"
        private const val SHORTCUT_KEY = "shortcutId"
        private const val CUSTOM_LABEL_KEY = "customLabel"
        private const val SHORTCUT_LABEL_KEY = "shortcutLabel"

        /**
         * Parses and validates the version 1 rules. Throws
         * [LayoutSnapshotException] on unreadable JSON, an unsupported version,
         * or any structural violation; the caller never sees a partial model.
         */
        fun fromJson(text: String): LayoutSnapshotV1 {
            val root = try {
                JSONObject(text)
            } catch (error: Exception) {
                throw LayoutSnapshotException("Not a JSON object", error)
            }
            val version = root.optInt(VERSION_KEY, -1)
            if (version != FORMAT_VERSION) {
                throw LayoutSnapshotException("Unsupported snapshot version $version")
            }
            val columns = root.optInt(COLUMNS_KEY, -1)
            val rows = root.optInt(ROWS_KEY, -1)
            if (columns !in GridCanvas.MIN_COLUMNS..GridCanvas.MAX_COLUMNS ||
                rows !in GridCanvas.MIN_ROWS..GridCanvas.MAX_ROWS
            ) {
                throw LayoutSnapshotException("Grid ${columns}x${rows} is out of range")
            }
            val cells = columns * rows
            val pages = root.optJSONArray(PAGES_KEY)?.let { array ->
                List(array.length()) { array.optInt(it, -1) }
            } ?: throw LayoutSnapshotException("Missing pages array")
            if (pages.isEmpty() || pages.any { it < 0 } || pages.distinct().size != pages.size ||
                pages.size > MAX_WORKSPACE_PAGES
            ) {
                throw LayoutSnapshotException("Invalid page ranks")
            }

            val dock = root.optJSONArray(DOCK_KEY)?.let { array ->
                List(array.length()) {
                    readAppObject(array.getJSONObject(it), DOCK_KEY)
                }
            } ?: throw LayoutSnapshotException("Missing dock array")
            if (dock.size > DockPages.totalCapacity(DockPages.MAX_CAPACITY)) {
                throw LayoutSnapshotException("Dock holds ${dock.size} pins, over the hard bound")
            }

            val foldersJson = root.optJSONArray(FOLDERS_KEY) ?: throw LayoutSnapshotException("Missing folders array")
            val folders = List(foldersJson.length()) {
                val folder = foldersJson.getJSONObject(it)
                FolderSnapshot(
                    title = folder.optString(TITLE_KEY),
                    pagePosition = requirePageReference(folder, FOLDERS_KEY, pages),
                    cell = requireCell(folder, FOLDERS_KEY, cells),
                )
            }

            val pinsJson = root.optJSONArray(PINS_KEY) ?: throw LayoutSnapshotException("Missing pins array")
            val pins = List(pinsJson.length()) {
                val pin = pinsJson.getJSONObject(it)
                val folderIndex = pin.optInt(FOLDER_KEY, -1).takeIf { it >= 0 }
                if (folderIndex != null && folderIndex >= folders.size) {
                    throw LayoutSnapshotException("Pin references folder $folderIndex outside the file")
                }
                PinSnapshot(
                    packageName = pin.optString(PACKAGE_KEY),
                    className = pin.optString(CLASS_KEY),
                    pagePosition = requirePageReference(pin, PINS_KEY, pages),
                    cell = requireCell(pin, PINS_KEY, cells),
                    folderIndex = folderIndex,
                    shortcutId = pin.optString(SHORTCUT_KEY).takeIf { pin.has(SHORTCUT_KEY) },
                    customLabel = pin.optString(CUSTOM_LABEL_KEY).takeIf { pin.has(CUSTOM_LABEL_KEY) },
                    shortcutLabel = pin.optString(SHORTCUT_LABEL_KEY).takeIf { pin.has(SHORTCUT_LABEL_KEY) },
                )
            }

            val snapshot = LayoutSnapshotV1(columns, rows, root.optBoolean(SEEDED_KEY, false), pages, dock, folders, pins)
            snapshot.validateCollisions()
            return snapshot
        }

        private fun readAppObject(source: JSONObject, section: String): DockPinSnapshot =
            DockPinSnapshot(
                packageName = requireText(source, PACKAGE_KEY, section),
                className = requireText(source, CLASS_KEY, section),
                shortcutId = source.optString(SHORTCUT_KEY).takeIf { source.has(SHORTCUT_KEY) },
                customLabel = source.optString(CUSTOM_LABEL_KEY).takeIf { source.has(CUSTOM_LABEL_KEY) },
                shortcutLabel = source.optString(SHORTCUT_LABEL_KEY).takeIf { source.has(SHORTCUT_LABEL_KEY) },
            )

        private fun requirePageReference(source: JSONObject, section: String, pages: List<Int>): Int {
            val position = source.optInt(PAGE_KEY, -1)
            if (position !in pages.indices) {
                throw LayoutSnapshotException("$section references page position $position outside the file")
            }
            return position
        }

        private fun requireCell(source: JSONObject, section: String, cells: Int): Int {
            val cell = source.optInt(CELL_KEY, -1)
            if (cell !in 0 until cells) {
                throw LayoutSnapshotException("$section carries cell $cell outside the grid")
            }
            return cell
        }

        private fun requireText(source: JSONObject, key: String, section: String): String {
            val value = source.optString(key)
            if (value.isBlank()) {
                throw LayoutSnapshotException("$section entry misses $key")
            }
            return value
        }

        private fun LayoutSnapshotV1.validateCollisions() {
            val occupied = HashMap<Pair<Int, Int>, String>()
            folders.forEachIndexed { index, folder ->
                val key = folder.pagePosition to folder.cell
                val owner = occupied.put(key, "folder $index")
                if (owner != null) {
                    throw LayoutSnapshotException("$owner and folder $index share cell ${folder.cell}")
                }
            }
            pins.forEachIndexed { index, pin ->
                if (pin.folderIndex != null) return@forEachIndexed
                val key = pin.pagePosition to pin.cell
                val owner = occupied.put(key, "pin $index")
                if (owner != null) {
                    throw LayoutSnapshotException("$owner and pin $index share cell ${pin.cell}")
                }
            }
        }
    }
}

/** A dock pin in dock order. */
internal data class DockPinSnapshot(
    val packageName: String,
    val className: String,
    val shortcutId: String?,
    val customLabel: String?,
    val shortcutLabel: String?,
)

/** A folder occupying a workspace cell; position references resolve at restore. */
internal data class FolderSnapshot(
    val title: String,
    val pagePosition: Int,
    val cell: Int,
)

/** A workspace pin; folder members carry the folder list index and a folder position. */
internal data class PinSnapshot(
    val packageName: String,
    val className: String,
    val pagePosition: Int,
    val cell: Int,
    val folderIndex: Int?,
    val shortcutId: String?,
    val customLabel: String?,
    val shortcutLabel: String?,
)

/** The snapshot file is corrupt or written for another format version. */
internal class LayoutSnapshotException(message: String, cause: Throwable? = null) : Exception(message, cause)
