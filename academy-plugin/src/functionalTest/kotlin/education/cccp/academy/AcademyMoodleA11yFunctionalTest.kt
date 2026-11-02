package education.cccp.academy

import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ACADEMY-15 (GBL-011-5) — functional TestKit guarantees for
 * `auditMoodleAccessibility`.
 *
 * The task really fetches a page over HTTP (a local server stands in for the
 * Moodle box) and writes the report. Four contracts are locked end-to-end:
 * the audit is opt-in, advisory by default, strict is the only way to fail, and
 * an unreachable box is a declared warning rather than a crash.
 */
class AcademyMoodleA11yFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    private val servers = mutableListOf<HttpServer>()

    @AfterEach
    fun stopServers() {
        servers.forEach { it.stop(0) }
        servers.clear()
    }

    private fun serve(body: String): String {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/login/index.php") { exchange ->
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        servers += server
        return "http://127.0.0.1:${server.address.port}"
    }

    private fun writeBuild(content: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"consumer-sample\"\n")
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    private fun optedIn(
        baseUrl: String,
        strict: Boolean = false,
    ): String =
        """
        plugins {
            id("education.cccp.academy")
        }

        academyInstaller {
            moodleA11yEnabled.set(true)
            moodleA11yStrict.set($strict)
            moodleA11yBaseUrl.set("$baseUrl")
            moodleA11yPaths.set(listOf("/login/index.php"))
        }
        """.trimIndent()

    private fun reportText(): String {
        val report = File(projectDir, "build/academy/a11y/moodle-accessibility-report.json")
        assertTrue(report.isFile, "expected the Moodle audit report to be written")
        return report.readText()
    }

    private fun runner(vararg args: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withArguments(*args)
            .withPluginClasspath()
            .forwardOutput()

    private val accessiblePage =
        """
        <!DOCTYPE html>
        <html lang="fr">
          <body>
            <h1>Connexion</h1>
            <label for="username">Nom</label>
            <input id="username" type="text">
          </body>
        </html>
        """.trimIndent()

    @Test
    fun `the Moodle audit is opt-in and skipped by default`() {
        writeBuild(
            """
            plugins {
                id("education.cccp.academy")
            }
            """.trimIndent(),
        )

        val result = runner("auditMoodleAccessibility").build()

        assertEquals(TaskOutcome.SKIPPED, result.task(":auditMoodleAccessibility")?.outcome)
        assertFalse(
            File(projectDir, "build/academy/a11y/moodle-accessibility-report.json").exists(),
            "a skipped audit writes no report",
        )
    }

    @Test
    fun `an accessible page is verified`() {
        val baseUrl = serve(accessiblePage)
        writeBuild(optedIn(baseUrl))

        val result = runner("auditMoodleAccessibility").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditMoodleAccessibility")?.outcome)
        assertTrue(reportText().contains("\"readiness\": \"VERIFIED\""))
    }

    @Test
    fun `a finding is advisory and written to the report`() {
        val baseUrl = serve("""<html><body>no lang</body></html>""")
        writeBuild(optedIn(baseUrl))

        val result = runner("auditMoodleAccessibility").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditMoodleAccessibility")?.outcome)
        val report = reportText()
        assertTrue(report.contains("\"readiness\": \"DEGRADED\""))
        assertTrue(report.contains("moodle-lang-missing"))
    }

    @Test
    fun `strict mode fails on an error finding`() {
        val baseUrl = serve("""<html><body>no lang</body></html>""")
        writeBuild(optedIn(baseUrl, strict = true))

        val result = runner("auditMoodleAccessibility").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":auditMoodleAccessibility")?.outcome)
        assertTrue(result.output.contains("strict mode"))
    }

    @Test
    fun `an unreachable box is a warning, not a failure`() {
        writeBuild(optedIn("http://127.0.0.1:1", strict = true))

        val result = runner("auditMoodleAccessibility").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":auditMoodleAccessibility")?.outcome)
        val report = reportText()
        assertTrue(report.contains("\"readiness\": \"DEGRADED\""))
        assertTrue(report.contains("moodle-page-unreachable"))
    }
}
