package education.cccp.academy.a11y

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ACADEMY-15 (GBL-011-5) — static inspection of one rendered Moodle page.
 *
 * The box ships a real Moodle; its accessibility is measured on the HTML the
 * server actually returns. The checks are structural and browser-free (no JS,
 * no focus order, no session): they catch the regressions a theme change or a
 * plugin introduces on the pages a learner meets first.
 */
class MoodleAccessibilityInspectorTest {
    private val accessiblePage =
        """
        <!DOCTYPE html>
        <html lang="fr" dir="ltr">
          <head><title>Cours</title></head>
          <body>
            <h1>Connexion</h1>
            <img src="logo.png" alt="">
            <form>
              <label for="username">Nom d'utilisateur</label>
              <input id="username" name="username" type="text">
              <label for="password">Mot de passe</label>
              <input id="password" name="password" type="password">
              <input type="hidden" name="sesskey" value="x">
              <input type="submit" value="Connexion">
            </form>
            <table><thead><tr><th>Module</th></tr></thead><tbody><tr><td>SPG</td></tr></tbody></table>
          </body>
        </html>
        """.trimIndent()

    private fun inspect(html: String) = MoodleAccessibilityInspector.inspect(html, "http://localhost:8080/login/index.php")

    private fun findings(html: String) = inspect(html)

    @Test
    fun `an accessible page has no finding`() {
        assertTrue(findings(accessiblePage).isEmpty())
    }

    @Test
    fun `a page without lang degrades`() {
        val html = accessiblePage.replace("<html lang=\"fr\"", "<html")

        val finding = findings(html).single { it.rule == A11yRules.MOODLE_LANG_MISSING }

        assertEquals(A11ySeverity.ERROR, finding.severity)
        assertEquals(A11yDeliverable.MOODLE, finding.deliverable)
        assertEquals("http://localhost:8080/login/index.php", finding.location)
    }

    @Test
    fun `an image without alt degrades`() {
        val html = accessiblePage.replace("<img src=\"logo.png\" alt=\"\">", "<img src=\"logo.png\">")

        val finding = findings(html).single { it.rule == A11yRules.MOODLE_IMAGE_ALT_MISSING }

        assertEquals(A11ySeverity.ERROR, finding.severity)
    }

    @Test
    fun `a form control without an accessible name degrades`() {
        val html = accessiblePage.replace("<label for=\"username\">Nom d'utilisateur</label>", "")

        val finding = findings(html).single { it.rule == A11yRules.MOODLE_FORM_CONTROL_LABEL_MISSING }

        assertEquals(A11ySeverity.ERROR, finding.severity)
    }

    @Test
    fun `aria-label or title names a control`() {
        val aria =
            accessiblePage
                .replace("<label for=\"username\">Nom d'utilisateur</label>", "")
                .replace(
                    "<input id=\"username\" name=\"username\" type=\"text\">",
                    "<input id=\"username\" name=\"username\" type=\"text\" aria-label=\"Nom d'utilisateur\">",
                )
        val titled =
            accessiblePage
                .replace("<label for=\"password\">Mot de passe</label>", "")
                .replace(
                    "<input id=\"password\" name=\"password\" type=\"password\">",
                    "<input id=\"password\" name=\"password\" type=\"password\" title=\"Mot de passe\">",
                )

        assertTrue(findings(aria).none { it.rule == A11yRules.MOODLE_FORM_CONTROL_LABEL_MISSING })
        assertTrue(findings(titled).none { it.rule == A11yRules.MOODLE_FORM_CONTROL_LABEL_MISSING })
    }

    @Test
    fun `a wrapping label names a control`() {
        val html =
            accessiblePage
                .replace("<label for=\"username\">Nom d'utilisateur</label>", "")
                .replace(
                    "<input id=\"username\" name=\"username\" type=\"text\">",
                    "<label>Nom d'utilisateur <input name=\"username\" type=\"text\"></label>",
                )

        assertTrue(findings(html).none { it.rule == A11yRules.MOODLE_FORM_CONTROL_LABEL_MISSING })
    }

    @Test
    fun `a table without a header cell is a warning`() {
        val html = accessiblePage.replace("<thead><tr><th>Module</th></tr></thead>", "")

        val finding = findings(html).single { it.rule == A11yRules.MOODLE_TABLE_HEADER_MISSING }

        assertEquals(A11ySeverity.WARNING, finding.severity)
    }

    @Test
    fun `a page without a first level heading is a warning`() {
        val html = accessiblePage.replace("<h1>Connexion</h1>", "")

        val finding = findings(html).single { it.rule == A11yRules.MOODLE_H1_MISSING }

        assertEquals(A11ySeverity.WARNING, finding.severity)
    }

    @Test
    fun `hidden and submit controls are never required to carry a label`() {
        assertTrue(findings(accessiblePage).none { it.rule == A11yRules.MOODLE_FORM_CONTROL_LABEL_MISSING })
    }

    @Test
    fun `the inspection is deterministic`() {
        assertEquals(inspect(accessiblePage), inspect(accessiblePage))
    }
}
