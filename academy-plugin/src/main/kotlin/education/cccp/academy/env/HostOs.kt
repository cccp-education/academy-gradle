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
    UNKNOWN,
}
