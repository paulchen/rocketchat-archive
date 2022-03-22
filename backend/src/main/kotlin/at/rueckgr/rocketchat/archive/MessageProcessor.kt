package at.rueckgr.rocketchat.archive

import org.apache.commons.text.StringEscapeUtils

object MessageProcessor {
    private val URL_PATTERN = """(?:(?:ftp|http)[s]*://|www\.)[^.]+\.[^ \n"]+""".toRegex(RegexOption.IGNORE_CASE)

    fun process(message: String): String {
        val escaped = StringEscapeUtils.escapeHtml4(message)

        return escaped.replace(URL_PATTERN) { "<a href=\"${it.value}\" target=\"_blank\">${it.value}</a>" }
    }
}
