package dev.vladon.vesna.launcher.search

import java.text.Normalizer

/** Normalizes search comparisons without changing the text shown to the user. */
internal fun searchKey(text: String): String {
    if (text.isEmpty()) return text
    val ascii = text.all { it <= '\u007f' }
    val input = if (ascii) text else Normalizer.normalize(text, Normalizer.Form.NFKD)
    val key = StringBuilder(input.length)
    var index = 0
    var pendingSpace = false
    var base = -1
    while (index < input.length) {
        val cp = input.codePointAt(index)
        index += Character.charCount(cp)
        if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
            pendingSpace = key.isNotEmpty()
            base = -1
            continue
        }
        val mark = !ascii && isMark(cp)
        // Latin accents and the commonly omitted dots in ё are optional.
        // Other marks can distinguish letters (й/и, ガ/カ) and must stay.
        if (mark && base >= 0 &&
            (Character.UnicodeScript.of(base) == Character.UnicodeScript.LATIN ||
                (base == 'е'.code && cp == '\u0308'.code))
        ) continue

        if (pendingSpace) {
            key.append(' ')
            pendingSpace = false
        }
        if (mark) {
            key.appendCodePoint(cp)
        } else {
            // Simple, locale-independent case folding also unifies both forms of Greek sigma.
            base = Character.toLowerCase(Character.toUpperCase(cp))
            key.appendCodePoint(base)
        }
    }
    val folded = key.toString()
    return if (ascii) folded else Normalizer.normalize(folded, Normalizer.Form.NFC)
}

private fun isMark(cp: Int): Boolean = when (Character.getType(cp)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt() -> true
    else -> false
}
