package com.iamkaf.minisort.sort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModVersionsTest {
    @Test
    void readsTheNumericPartBeforeBuildMetadata() {
        assertTrue(ModVersions.olderThan("6.2.0+26.3", 6, 2, 1));
        assertTrue(ModVersions.olderThan("1.5.0", 6, 2, 1));
        assertFalse(ModVersions.olderThan("6.2.1+26.3", 6, 2, 1));
        assertFalse(ModVersions.olderThan("6.10.0+1.21.11", 6, 2, 1));
        assertFalse(ModVersions.olderThan("7.0", 6, 2, 1));
    }

    @Test
    void treatsAMissingOrUnreadableVersionAsSafe() {
        assertFalse(ModVersions.olderThan(null, 6, 2, 1));
        assertFalse(ModVersions.olderThan("${version}", 6, 2, 1));
    }
}
