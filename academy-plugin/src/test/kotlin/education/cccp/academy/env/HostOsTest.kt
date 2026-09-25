package education.cccp.academy.env

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ACADEMY-12-2 — the textual OS mapping (D-ACADEMY-12-8): a JVM `os.name` to a
 * [HostOs]. Pure, no I/O, so the mapping is locked without a machine; an
 * unrecognized or absent name is [HostOs.UNKNOWN], never a crash.
 */
class HostOsTest {

    @Test
    fun `recognizes the three supported hosts from the jvm os name`() {
        assertEquals(HostOs.LINUX, HostOs.fromOsName("Linux"))
        assertEquals(HostOs.WINDOWS, HostOs.fromOsName("Windows 11"))
        assertEquals(HostOs.MACOS, HostOs.fromOsName("Mac OS X"))
    }

    @Test
    fun `an unrecognized or absent name is unknown`() {
        assertEquals(HostOs.UNKNOWN, HostOs.fromOsName("FreeBSD"))
        assertEquals(HostOs.UNKNOWN, HostOs.fromOsName(""))
        assertEquals(HostOs.UNKNOWN, HostOs.fromOsName(null))
    }

    @Test
    fun `matching is case-insensitive`() {
        assertEquals(HostOs.WINDOWS, HostOs.fromOsName("WINDOWS"))
        assertEquals(HostOs.LINUX, HostOs.fromOsName("linux"))
    }
}
