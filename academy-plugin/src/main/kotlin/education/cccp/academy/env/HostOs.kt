package education.cccp.academy.env

/**
 * Operating system of the host running the installer (ACADEMY-12-1,
 * D-ACADEMY-12-8). A dedicated enum rather than reusing the installer
 * `TargetOs`: the environment context and the installer context are separate
 * bounded contexts, they must not share a type.
 *
 * [UNKNOWN] exists so an undetected host is a verdict, never a crash.
 */
enum class HostOs {
    LINUX,
    WINDOWS,
    MACOS,
    UNKNOWN;

    companion object {
        /**
         * Maps a JVM `os.name` system property to a [HostOs] (ACADEMY-12-2). Purely
         * textual (no I/O), so the mapping itself is unit-testable; an unrecognized
         * name is [UNKNOWN], never a crash.
         */
        fun fromOsName(osName: String?): HostOs {
            val name = osName?.lowercase() ?: return UNKNOWN
            return when {
                name.contains("win") -> WINDOWS
                name.contains("mac") || name.contains("darwin") -> MACOS
                name.contains("nux") || name.contains("nix") || name.contains("aix") -> LINUX
                else -> UNKNOWN
            }
        }
    }
}
