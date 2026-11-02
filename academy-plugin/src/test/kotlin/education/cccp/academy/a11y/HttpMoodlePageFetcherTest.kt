package education.cccp.academy.a11y

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * ACADEMY-15 (GBL-011-5) — the JDK HTTP adapter for the Moodle page fetcher.
 *
 * The adapter is the only network code of the audit: a status outside 2xx/3xx
 * or a connection failure returns `null`, which the auditor reports as an
 * unreachable page. It never throws into the Gradle task.
 */
class HttpMoodlePageFetcherTest {
    private var server: HttpServer? = null

    @AfterTest
    fun stop() {
        server?.stop(0)
    }

    private fun serve(
        status: Int,
        body: String,
    ): String {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/login/index.php") { exchange ->
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        http.start()
        server = http
        return "http://127.0.0.1:${http.address.port}/login/index.php"
    }

    @Test
    fun `a 200 response returns the body`() {
        val url = serve(200, "<html lang=\"fr\"></html>")
        val fetcher = HttpMoodlePageFetcher(timeoutSeconds = 5)

        assertEquals("<html lang=\"fr\"></html>", fetcher.fetch(url))
    }

    @Test
    fun `a server error returns null`() {
        val url = serve(500, "boom")
        val fetcher = HttpMoodlePageFetcher(timeoutSeconds = 5)

        assertNull(fetcher.fetch(url))
    }

    @Test
    fun `an unreachable host returns null`() {
        val fetcher = HttpMoodlePageFetcher(timeoutSeconds = 1)

        assertNull(fetcher.fetch("http://127.0.0.1:1/login/index.php"))
    }
}
