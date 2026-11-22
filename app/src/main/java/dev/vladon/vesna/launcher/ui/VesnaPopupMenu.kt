package dev.vladon.vesna.launcher.ui

import android.graphics.Rect
import android.text.Layout
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.FrameLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import dev.vladon.vesna.launcher.R
import kotlin.math.ceil
import kotlin.math.roundToInt

/** An anchored, scrollable menu whose rows can grow to fit localized action labels. */
internal class VesnaPopupMenu(private val anchor: View) {
    private data class Row(
        val label: CharSequence,
        val description: CharSequence?,
        val checked: Boolean?,
        val onClick: () -> Unit,
    )

    private val rows = ArrayList<Row>()
    private val context = ContextThemeWrapper(anchor.context, R.style.ThemeOverlay_Vesna_PopupMenu)
    private val popup = ListPopupWindow(context)
    private val anchorListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit
        override fun onViewDetachedFromWindow(view: View) = dismiss()
    }

    fun add(@StringRes label: Int, onClick: () -> Unit) = add(context.getText(label), onClick = onClick)

    fun add(
        label: CharSequence,
        description: CharSequence? = null,
        checked: Boolean? = null,
        onClick: () -> Unit,
    ) {
        rows.add(Row(label, description, checked, onClick))
    }

    fun show(onDismiss: () -> Unit) {
        val frame = Rect()
        anchor.getWindowVisibleDisplayFrame(frame)
        if (!anchor.isShown || !anchor.isAttachedToWindow || frame.width() <= 0) {
            onDismiss()
            return
        }
        popup.anchorView = anchor
        popup.isModal = true
        popup.inputMethodMode = ListPopupWindow.INPUT_METHOD_NOT_NEEDED
        popup.setDropDownGravity(Gravity.START)
        popup.setBackgroundDrawable(context.getDrawable(R.drawable.vesna_popup_menu_background))
        popup.width = measureWidth(frame.width())
        popup.setAdapter(RowAdapter())
        popup.setOnItemClickListener { _, _, position, _ -> activate(rows[position]) }
        popup.setOnDismissListener {
            anchor.removeOnAttachStateChangeListener(anchorListener)
            onDismiss()
        }
        anchor.addOnAttachStateChangeListener(anchorListener)
        popup.show()
    }

    fun dismiss() = popup.dismiss()

    private fun activate(row: Row) {
        dismiss()
        row.onClick()
    }

    private fun measureWidth(available: Int): Int {
        val text = createLabel(checkable = false)
        val checkWidth = if (rows.any { it.checked != null }) {
            (createLabel(checkable = true) as CheckedTextView).checkMarkDrawable?.intrinsicWidth ?: 0
        } else {
            0
        }
        val padding = text.paddingLeft + text.paddingRight
        var width = dp(196)
        for (row in rows) {
            val labelWidth = ceil(Layout.getDesiredWidth(row.label, text.paint)).toInt()
            width = maxOf(width, labelWidth + padding + if (row.checked == null) 0 else checkWidth)
        }
        return width.coerceAtMost(available)
    }

    private fun createLabel(checkable: Boolean): TextView {
        val text = if (checkable) {
            CheckedTextView(context).apply {
                val indicator = TypedValue()
                context.theme.resolveAttribute(android.R.attr.listChoiceIndicatorSingle, indicator, true)
                setCheckMarkDrawable(indicator.resourceId)
            }
        } else {
            TextView(context)
        }
        return text.apply {
            setTextAppearance(R.style.TextAppearance_Vesna_PopupMenu)
            layoutDirection = anchor.layoutDirection
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            minHeight = dp(48)
            setPaddingRelative(dp(16), dp(8), dp(16), dp(8))
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()

    private inner class RowView(checkable: Boolean) : FrameLayout(context) {
        // Android's DropDownListView forces horizontal scrolling on TextView row roots.
        private val label = createLabel(checkable)
        private lateinit var boundRow: Row

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            label.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnClickListener { activate(boundRow) }
            isFocusable = false
            addView(label, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            layoutParams = AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        fun bind(row: Row) {
            boundRow = row
            label.text = row.label
            contentDescription = row.description
            if (label is CheckedTextView) label.isChecked = row.checked == true
        }

        @Suppress("DEPRECATION") // The boolean setter is available on every supported Android version.
        override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(info)
            info.text = label.text
            if (label is CheckedTextView) {
                info.className = CheckedTextView::class.java.name
                info.isCheckable = true
                info.isChecked = label.isChecked
            }
        }
    }

    private inner class RowAdapter : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(position: Int) = rows[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getViewTypeCount() = 2
        override fun getItemViewType(position: Int) = if (rows[position].checked == null) 0 else 1

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = getItem(position)
            return ((convertView as? RowView) ?: RowView(checkable = row.checked != null)).apply { bind(row) }
        }
    }
}
