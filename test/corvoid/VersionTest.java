package corvoid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VersionTest {

    private int compare(String v1, String v2) {
        return new Version(v1).compareTo(new Version(v2));
    }

    private void assertOrder(String... versions) {
        for (int i = 0; i < versions.length - 1; i++) {
            String v1 = versions[i];
            String v2 = versions[i + 1];
            assertTrue(compare(v1, v2) < 0, v1 + " should be less than " + v2);
            assertTrue(compare(v2, v1) > 0, v2 + " should be greater than " + v1);
        }
    }

    private void assertEqualsVersion(String v1, String v2) {
        assertEquals(0, compare(v1, v2), v1 + " should be equal to " + v2);
        assertEquals(0, compare(v2, v1), v2 + " should be equal to " + v1);
        assertEquals(new Version(v1), new Version(v2));
    }

    @Test
    public void testKeyExamples() {
        // 1 == 1.0 == 1-0 (after normalization effects)
        assertEqualsVersion("1", "1.0");
        assertEqualsVersion("1", "1-0");
        assertEqualsVersion("1.0", "1-0");

        // 1-rc < 1 < 1-sp
        assertOrder("1-rc", "1", "1-sp");

        // 1-ga == 1-final == 1-release == 1
        assertEqualsVersion("1-ga", "1");
        assertEqualsVersion("1-final", "1");
        assertEqualsVersion("1-release", "1");
        assertEqualsVersion("1-ga", "1-final");

        // 1.0.RC2 < 1.0-RC3 < 1.0.1
        assertOrder("1.0.RC2", "1.0-RC3", "1.0.1");

        // rc < rc1 < rc2
        assertOrder("rc", "rc1", "rc2");
    }

    @Test
    public void testNumeric() {
        assertOrder("1", "2", "10");
        assertOrder("1.0", "1.1");
        // INT < LONG < BIGINT
        // 9 digits, 10 digits, 19 digits
        assertOrder("999999999", "1000000000"); 
        assertOrder("999999999999999999", "1000000000000000000");
    }

    @Test
    public void testQualifiers() {
        assertOrder("1-alpha", "1-beta", "1-milestone", "1-rc", "1-snapshot", "1", "1-sp", "1-unknown");
        assertEqualsVersion("1-cr", "1-rc");
    }

    @Test
    public void testTransitions() {
        assertEqualsVersion("1alpha", "1-alpha");
        assertEqualsVersion("1.alpha", "1-alpha");
        assertOrder("1rc1", "1rc2");
        assertOrder("1-rc1", "1-rc2");
        assertEqualsVersion("1rc1", "1-rc1");
    }

    @Test
    public void testSpecialCases() {
        // X-1 merges into X1
        assertEqualsVersion("rc-1", "rc1");
    }

    @Test
    public void testNormalization() {
        assertEqualsVersion("1..2", "1.0.2");
        assertEqualsVersion("1--2", "1-0-2");
    }

    @Test
    public void testEmptyAndNull() {
        assertEqualsVersion("", "");
        assertOrder("", "1");
    }

    @Test
    public void testComplexTransitions() {
        assertOrder("1.0-alpha-1", "1.0-alpha-2");
        assertOrder("1.0-alpha-1", "1.0-beta-1");
    }

    @Test
    public void testDeeplyNested() {
        assertOrder("1.1.1.1.1", "1.1.1.1.2");
        assertEqualsVersion("1.0.0.0.0", "1");
    }
}
