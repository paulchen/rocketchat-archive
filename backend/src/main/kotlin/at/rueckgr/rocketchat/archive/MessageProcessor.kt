package at.rueckgr.rocketchat.archive

import org.apache.commons.text.StringEscapeUtils

object MessageProcessor {
    private val URL_PATTERN = """(?:(?:ftp|http)[s]*://|www\.)[^.]+\.[^ \n")]+""".toRegex(RegexOption.IGNORE_CASE)

    fun process(message: String): String {
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var escaped = message

        // linkify messages
        escaped = StringEscapeUtils.escapeHtml4(message)
        escaped = escaped.replace(URL_PATTERN) { "<a href=\"${it.value}\" target=\"_blank\">${it.value}</a>" }

        // properly show multi-line messages
        escaped = escaped.replace("\n", "<br />")

        return escaped
    }
}
