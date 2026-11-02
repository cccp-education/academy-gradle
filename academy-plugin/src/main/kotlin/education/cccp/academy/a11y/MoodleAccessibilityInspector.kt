package education.cccp.academy.a11y

/**
 * ACADEMY-15 (GBL-011-5) — structural checks on one rendered Moodle page.
 *
 * Regex-based on the server HTML (no DOM, no browser): deterministic and
 * sufficient for the signals a theme or a plugin breaks first. It does not
 * measure focus order, contrast, ARIA live regions or JavaScript behaviour —
 * those need a browser and are declared as limits by [MoodleA11yAuditor].
 */
object MoodleAccessibilityInspector {
    private val HTML_TAG = Regex("<html\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val LANG = Regex("\\blang\\s*=", RegexOption.IGNORE_CASE)
    private val IMG = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ALT = Regex("\\balt\\s*=", RegexOption.IGNORE_CASE)
    private val H1 = Regex("<h1\\b", RegexOption.IGNORE_CASE)
    private val TABLE = Regex("<table\\b", RegexOption.IGNORE_CASE)
    private val TH = Regex("<th\\b", RegexOption.IGNORE_CASE)
    private val CONTROL = Regex("<(input|select|textarea)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val INPUT_TYPE = Regex("\\btype\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val ID = Regex("\\bid\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val LABEL_FOR = Regex("<label\\b[^>]*\\bfor\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val ACCESSIBLE_NAME = Regex("\\b(aria-label|aria-labelledby|title)\\s*=", RegexOption.IGNORE_CASE)
    private val NON_LABELLED_INPUT_TYPES = setOf("hidden", "submit", "button", "reset", "image")

    /** Findings for the page [html] served at [location] (absolute URL). */
    fun inspect(
        html: String,
        location: String,
    ): List<A11yFinding> {
        val findings = mutableListOf<A11yFinding>()

        val htmlTag = HTML_TAG.find(html)?.value
        if (htmlTag == null || !LANG.containsMatchIn(htmlTag)) {
            findings +=
                finding(
                    location = location,
                    rule = A11yRules.MOODLE_LANG_MISSING,
                    severity = A11ySeverity.ERROR,
                    message = "the html element has no lang attribute",
                )
        }

        val imagesWithoutAlt = IMG.findAll(html).count { !ALT.containsMatchIn(it.value) }
        if (imagesWithoutAlt > 0) {
            findings +=
                finding(
                    location = location,
                    rule = A11yRules.MOODLE_IMAGE_ALT_MISSING,
                    severity = A11ySeverity.ERROR,
                    message = "$imagesWithoutAlt image(s) without an alt attribute",
                )
        }

        val labelledBy = LABEL_FOR.findAll(html).map { it.groupValues[1] }.toSet()
        val unlabelledControls =
            CONTROL.findAll(html).count { control ->
                val tag = control.value
                val type =
                    INPUT_TYPE
                        .find(tag)
                        ?.groupValues
                        ?.get(1)
                        ?.lowercase()
                val isInput = tag.startsWith("<input", ignoreCase = true)
                when {
                    isInput && type != null && type in NON_LABELLED_INPUT_TYPES -> false
                    ACCESSIBLE_NAME.containsMatchIn(tag) -> false
                    labelledById(tag, labelledBy) -> false
                    isWrappedInLabel(html, control.range.first) -> false
                    else -> true
                }
            }
        if (unlabelledControls > 0) {
            findings +=
                finding(
                    location = location,
                    rule = A11yRules.MOODLE_FORM_CONTROL_LABEL_MISSING,
                    severity = A11ySeverity.ERROR,
                    message = "$unlabelledControls form control(s) without an accessible name",
                )
        }

        if (TABLE.containsMatchIn(html) && !TH.containsMatchIn(html)) {
            findings +=
                finding(
                    location = location,
                    rule = A11yRules.MOODLE_TABLE_HEADER_MISSING,
                    severity = A11ySeverity.WARNING,
                    message = "a table has no th header cell",
                )
        }

        if (!H1.containsMatchIn(html)) {
            findings +=
                finding(
                    location = location,
                    rule = A11yRules.MOODLE_H1_MISSING,
                    severity = A11ySeverity.WARNING,
                    message = "the page has no first level heading",
                )
        }

        return findings
    }

    private fun labelledById(
        tag: String,
        labelledBy: Set<String>,
    ): Boolean {
        val id = ID.find(tag)?.groupValues?.get(1) ?: return false
        return id in labelledBy
    }

    private fun isWrappedInLabel(
        html: String,
        index: Int,
    ): Boolean {
        val prefix = html.substring(0, index)
        val lastOpen = prefix.lastIndexOf("<label", ignoreCase = true)
        if (lastOpen < 0) return false
        val lastClose = prefix.lastIndexOf("</label>", ignoreCase = true)
        return lastOpen > lastClose
    }

    private fun finding(
        location: String,
        rule: String,
        severity: A11ySeverity,
        message: String,
    ): A11yFinding =
        A11yFinding(
            deliverable = A11yDeliverable.MOODLE,
            rule = rule,
            severity = severity,
            location = location,
            message = message,
        )
}
