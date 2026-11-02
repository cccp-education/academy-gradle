package education.cccp.academy.a11y

/**
 * ACADEMY-15 / GBL-011-5 — the box acceptance gate (pure).
 *
 * The machine verdict is the conjunction of the available layer audits:
 *  - no report at all = `MISSING` (nothing was measured);
 *  - any finding, a missing layer or a non-`VERIFIED` layer = `DEGRADED`;
 *  - every layer `VERIFIED` and no finding = `VERIFIED`.
 *
 * A missing layer is **declared** in the limits, never silently skipped. The
 * report always carries the limits no machine audit can lift: the usage test
 * with a concerned person and the opencode terminal.
 */
object AcademyA11yAcceptance {
    /** Limits that no machine audit can lift, always present in the report. */
    val LIMITS =
        listOf(
            "The acceptance is a machine audit; a usage test with a concerned person is required before " +
                "claiming accessibility (GBL-011)",
            "opencode terminal (TUI) accessibility is not measured by the box audits",
        )

    /** Aggregates the two layer reports into the box acceptance report. */
    fun accept(
        material: MaterialA11yReport?,
        moodle: MaterialA11yReport?,
    ): AcademyA11yReport {
        val layers =
            buildList {
                if (material != null) add(material.toLayer("material"))
                if (moodle != null) add(moodle.toLayer("moodle"))
            }
        val gapLimits =
            buildList {
                if (material == null) add("material audit report not provided: the material layer is unverified")
                if (moodle == null) add("moodle audit report not provided: the Moodle layer is unverified")
            }
        val layerLimits =
            layers
                .flatMap { it.limits }
                .filterNot { it.contains("opencode", ignoreCase = true) }
        val limits = (layerLimits + gapLimits + LIMITS).distinct()

        if (layers.isEmpty()) {
            return AcademyA11yReport(readiness = A11yReadiness.MISSING, limits = limits)
        }

        val readiness =
            when {
                layers.any { it.findings.isNotEmpty() } -> A11yReadiness.DEGRADED
                material == null || moodle == null -> A11yReadiness.DEGRADED
                layers.any { it.readiness != A11yReadiness.VERIFIED } -> A11yReadiness.DEGRADED
                else -> A11yReadiness.VERIFIED
            }
        return AcademyA11yReport(readiness = readiness, layers = layers, limits = limits)
    }

    private fun MaterialA11yReport.toLayer(name: String): A11yLayerReport =
        A11yLayerReport(
            layer = name,
            readiness = readiness,
            findings = findings,
            limits = limits,
        )
}
