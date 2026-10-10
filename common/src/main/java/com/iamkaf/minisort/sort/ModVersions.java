package com.iamkaf.minisort.sort;

import org.jetbrains.annotations.Nullable;

/** Compares other mods' versions, for guarding against releases known to misbehave with Minisort. */
final class ModVersions {
    private ModVersions() {
    }

    /**
     * Whether the numeric part of a version such as {@code 6.2.1+26.3} is below the given floor. A mod that isn't
     * loaded (null) or a version that isn't numeric is not older.
     */
    static boolean olderThan(@Nullable String version, int... floor) {
        if (version == null) {
            return false;
        }
        String[] parts = version.split("[+-]", 2)[0].split("\\.");
        for (int i = 0; i < floor.length; i++) {
            int part;
            try {
                part = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            } catch (NumberFormatException notNumeric) {
                return false;
            }
            if (part != floor[i]) {
                return part < floor[i];
            }
        }
        return false;
    }
}
