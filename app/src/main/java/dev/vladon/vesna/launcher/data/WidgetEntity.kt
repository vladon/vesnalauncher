package dev.vladon.vesna.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A committed widget placement (#183 phase 3 / ENG-102): a bound AppWidget
 * with its provider identity, stable page ID and top-left cell.
 *
 * Created only after the host ID is bound (never before); deleted before
 * the host ID is released (ADR-WIDGET-1 ordering).
 */
@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appWidgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val pageId: Long,
    val cellIndex: Int,
    val spanX: Int = 2,
    val spanY: Int = 2,
    val userSerial: Long = 0,
)
