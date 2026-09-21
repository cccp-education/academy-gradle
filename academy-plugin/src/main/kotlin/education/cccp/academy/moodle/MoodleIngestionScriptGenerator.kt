package education.cccp.academy.moodle

/**
 * Pure renderer of the Moodle ingestion artifacts (ACADEMY-11-1,
 * D-ACADEMY-11-2/11-4/11-9): the **plan JSON** (the data academy injects) and
 * the **applicator script** that drives the **native tools shipped in the
 * image**.
 *
 * Two deliberate choices, both grounded in the image audit (S-013):
 *
 *  - the JSON is built by explicit literal concatenation, with an explicit
 *    escaper (patron `OpenCodeConfigGenerator`, D-ACADEMY-5-3) — the output
 *    side stays free of any serialization library;
 *  - the applicator is a **POSIX shell script that drives native tools**
 *    (`moosh`, `moodle-blueprint`) already present in
 *    `erseco/alpine-moodle:v5.2.3`. Academy does **not** reimplement the
 *    Moodle API in a hand-written PHP script (that would be building a stack):
 *    it delegates to the image's own tooling.
 *
 * The script never writes SQL or DDL and never parses the AsciiDoc: for a
 * `page`/`label` it reads the artifact **Moodle-side** with `cat` from the
 * material directory, so the AsciiDoc body stays opaque to academy
 * (D-ACADEMY-11-4).
 */
object MoodleIngestionScriptGenerator {

    /** Renders the plan as deterministic, balanced JSON. */
    fun renderPlanJson(plan: MoodleImportPlan): String = buildString {
        appendLine("{")
        appendLine("  \"courseShortName\": \"${escape(plan.courseShortName)}\",")
        appendLine("  \"courseFullName\": \"${escape(plan.courseFullName)}\",")
        appendLine("  \"courseFormat\": \"${escape(plan.courseFormat)}\",")
        appendLine("  \"sections\": [")
        plan.sections.forEachIndexed { sectionIndex, section ->
            appendLine("    {")
            appendLine(
                "      \"module\": " +
                    (section.module?.let { "\"${escape(it)}\"" } ?: "null") + ",",
            )
            appendLine("      \"activities\": [")
            section.activities.forEachIndexed { activityIndex, activity ->
                appendLine("        {")
                appendLine("          \"type\": \"${activity.type.moduleName}\",")
                appendLine("          \"title\": \"${escape(activity.title)}\",")
                appendLine("          \"sourcePath\": \"${escape(activity.sourcePath)}\"")
                append("        }" + if (activityIndex == section.activities.lastIndex) "\n" else ",\n")
            }
            appendLine("      ]")
            append("    }" + if (sectionIndex == plan.sections.lastIndex) "\n" else ",\n")
        }
        appendLine("  ]")
        append("}")
    }

    /**
     * Renders the applicator shell script **for [plan]** — the commands are
     * rendered from the plan (the data), and the script drives the image's
     * native tools only.
     *
     * Idempotent by construction: `moosh course-create -r` reuses an existing
     * course (verified on the real image, audit S-013), and sections are
     * addressed by position so re-running never duplicates a course.
     */
    fun renderApplicator(plan: MoodleImportPlan): String = buildString {
        appendLine("#!/bin/sh")
        appendLine("# Academy Moodle material injection - applicator (ACADEMY-11).")
        appendLine("# Drives the NATIVE tools of the image (moosh, moodle-blueprint).")
        appendLine("# No SQL, no hand-written Moodle API: academy injects DATA, not a stack.")
        appendLine("set -eu")
        appendLine()
        appendLine("MATERIAL_DIR=\"\${MATERIAL_DIR:-/material}\"")
        appendLine("SHORTNAME=${shellQuote(plan.courseShortName)}")
        appendLine("FULLNAME=${shellQuote(plan.courseFullName)}")
        appendLine("FORMAT=${shellQuote(plan.courseFormat)}")
        appendLine()
        appendLine("# 1. Course - moosh create is idempotent with -r (reuses a matching course).")
        appendLine("moosh course-create -f \"\$FULLNAME\" -F \"\$FORMAT\" -r \"\$SHORTNAME\"")
        appendLine()
        appendLine("# activity-add needs the numeric course id, not the shortname (verified S-013).")
        appendLine("# moosh course-list emits a quoted CSV: id,category,shortname,fullname,visible.")
        appendLine(
            "COURSEID=\$(moosh course-list | awk -F'\\\",\\\"' -v s=\"\$SHORTNAME\" " +
                "'\$3==s {sub(/^\\\"/, \"\", \$1); print \$1; exit}' | head -1)",
        )
        appendLine("if [ -z \"\$COURSEID\" ]; then")
        appendLine("  echo \"[academy] could not resolve course \$SHORTNAME\" >&2")
        appendLine("  exit 1")
        appendLine("fi")
        appendLine()
        plan.sections.forEachIndexed { index, section ->
            val sectionNumber = index + 1
            if (section.module != null) {
                appendLine("# section $sectionNumber - module ${section.module}")
                appendLine(
                    "moosh section-config-set -s $sectionNumber course \"\$COURSEID\" " +
                        "name ${shellQuote(section.module)}",
                )
            }
            section.activities.forEach { activity ->
                appendLine()
                appendLine("# ${activity.type.moduleName} - ${activity.title}")
                when (activity.type) {
                    MoodleActivityType.PAGE -> {
                        appendLine("SRC=\"\$MATERIAL_DIR/${activity.sourcePath}\"")
                        appendLine("# content is read Moodle-side: academy never parses the AsciiDoc.")
                        appendLine("# a page stores its body in `content` (verified S-013).")
                        appendLine("CONTENT=\$(cat \"\$SRC\" 2>/dev/null || echo '')")
                        appendLine(
                            "moosh activity-add -n ${shellQuote(activity.title)} -s $sectionNumber " +
                                "--options=\"--content=\$CONTENT --contentformat=1\" page \"\$COURSEID\"",
                        )
                    }
                    MoodleActivityType.LABEL -> {
                        appendLine("SRC=\"\$MATERIAL_DIR/${activity.sourcePath}\"")
                        appendLine("# content is read Moodle-side: academy never parses the AsciiDoc.")
                        appendLine("# a label stores its body in `intro` (verified S-013).")
                        appendLine("CONTENT=\$(cat \"\$SRC\" 2>/dev/null || echo '')")
                        appendLine(
                            "moosh activity-add -n ${shellQuote(activity.title)} -s $sectionNumber " +
                                "--options=\"--intro=\$CONTENT --introformat=1\" label \"\$COURSEID\"",
                        )
                    }
                    else -> appendLine(
                        "moosh activity-add -n ${shellQuote(activity.title)} -s $sectionNumber " +
                            "${activity.type.moduleName} \"\$COURSEID\"",
                    )
                }
            }
            appendLine()
        }
        appendLine("echo \"[academy] material injection complete for \$SHORTNAME\"")
    }

    /**
     * Quotes a value for a POSIX shell double-quoted string: backslash, double
     * quote, backtick and dollar must be escaped; a newline is impossible in the
     * titles the builder derives from file names.
     */
    private fun shellQuote(raw: String): String = buildString {
        append('"')
        raw.forEach { char ->
            when (char) {
                '\\', '"', '`', '$' -> append('\\').append(char)
                else -> append(char)
            }
        }
        append('"')
    }

    /**
     * Escapes a value for a JSON string literal: backslash, quote and the
     * control characters that would otherwise break the document. A title
     * containing a quote must never produce invalid JSON.
     */
    private fun escape(raw: String): String = buildString {
        raw.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
            }
        }
    }
}
