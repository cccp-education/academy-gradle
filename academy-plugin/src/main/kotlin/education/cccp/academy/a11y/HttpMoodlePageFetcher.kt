package education.cccp.academy.a11y

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * ACADEMY-15 (GBL-011-5) — the JDK HTTP adapter of [MoodlePageFetcher].
 *
 * A status outside 2xx/3xx, a timeout or a connection failure returns `null`;
 * the audit then declares the page unreachable. The adapter never throws into
 * the Gradle task — a box that is not running must not break the build.
 */
class HttpMoodlePageFetcher(
    private val timeoutSeconds: Long = 10,
) : MoodlePageFetcher {
    private val timeout: Duration = Duration.ofSeconds(timeoutSeconds)
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

    override fun fetch(url: String): String? =
        try {
            val request =
                HttpRequest
                    .newBuilder(URI.create(url))
                    .timeout(timeout)
                    .GET()
                    .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..399) response.body() else null
        } catch (_: Exception) {
            null
        }
}
