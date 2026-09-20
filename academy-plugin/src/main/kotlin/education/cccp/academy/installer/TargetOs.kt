package education.cccp.academy.installer

/**
 * Target operating systems supported by the installer generator
 * (ACADEMY-1 socle). Each target maps to its distribution script and its
 * platform sub-directory under the output directory.
 *
 * @property scriptName installer file name for the platform
 * @property dirName platform sub-directory under the output directory
 */
enum class TargetOs(
    val scriptName: String,
    val dirName: String,
) {
    LINUX("install.sh", "linux"),
    WINDOWS("install.bat", "windows"),
    MACOS("install.sh", "macos"),
}