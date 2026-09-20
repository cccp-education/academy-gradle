package education.cccp.academy.installer

/**
 * One generated file of the installer distribution — a single script per
 * target, rendered by the pure [InstallerScriptGenerator] (no I/O).
 *
 * @property relativePath script file name inside the platform directory
 * @property content rendered script text
 */
data class InstallerFile(
    val relativePath: String,
    val content: String,
)