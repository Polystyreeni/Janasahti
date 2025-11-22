package com.example.wordgame.models;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.CheckForNull;

public class GameVersion implements Comparable<GameVersion> {
    private static final int NO_BETA = -1;
    private static final String BETA_PREFIX = "Beta";
    private final int major;
    private final int minor;
    private final int subMinor;
    private final int betaVersion;

    public GameVersion(int major, int minor, int subMinor) {
        this(major, minor, subMinor, NO_BETA);
    }

    public GameVersion(int major, int minor, int subMinor, int betaVersion) {
        this.major = major;
        this.minor = minor;
        this.subMinor = subMinor;
        this.betaVersion = betaVersion;
    }

    /**
     * Checks if game version are incompatible
     * @param version1 version 1
     * @param version2 version 2
     * @return true if versions are incompatible
     */
    public static boolean areVersionsIncompatible(GameVersion version1, GameVersion version2) {
        return version1.minor != version2.minor || version1.major != version2.major;
    }

    @NonNull
    @Override
    public String toString() {
        return betaVersion != NO_BETA
                ? String.format(Locale.ENGLISH, "%d.%d.%d.%s%d", major, minor, subMinor,
                    BETA_PREFIX, betaVersion)
                : String.format(Locale.ENGLISH, "%d.%d.%d", major, minor, subMinor);
    }

    @CheckForNull
    public static GameVersion valueOf(String versionStr) {
        if (versionStr == null) {
            return null;
        }
        try {
            final String[] nums = versionStr.split("\\.");
            int major = Integer.parseInt(nums[0]);
            int minor = Integer.parseInt(nums[1]);
            int subMinor = Integer.parseInt(nums[2]);
            int beta = nums.length == 4 ? parseBeta(nums[3]) : NO_BETA;

            return new GameVersion(major, minor, subMinor, beta);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof GameVersion)) {
            return false;
        }

        GameVersion otherVer = (GameVersion) other;
        return subMinor == otherVer.subMinor
                && minor == otherVer.minor
                && major == otherVer.major
                && betaVersion == otherVer.betaVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, subMinor, betaVersion);
    }

    @Override
    public int compareTo(GameVersion other) {
        if (this.major != other.major) {
            return this.major - other.major;
        } else if (this.minor != other.minor) {
            return this.minor - other.minor;
        } else if (this.subMinor != other.subMinor) {
            return this.subMinor - other.subMinor;
        } else {
            // Beta comparison - no beta always preferred, otherwise higher beta wins
            final boolean thisBeta = this.betaVersion != NO_BETA;
            final boolean otherBeta = other.betaVersion != NO_BETA;
            if (!thisBeta && otherBeta) {
                return 1;
            } else if (thisBeta && !otherBeta) {
                return -1;
            } else {
                return this.betaVersion - other.betaVersion;
            }
        }
    }

    public boolean isBeta() {
        return this.betaVersion != NO_BETA;
    }

    private static int parseBeta(String betaPart) {
        try {
            return Integer.parseInt(betaPart.replace(BETA_PREFIX, ""));
        } catch (NumberFormatException e) {
            return NO_BETA;
        }
    }
}
