package org.xiyu.onekeyminer.legacy.core;

import java.util.Objects;

/**
 * Version-neutral integer block coordinates.
 *
 * <p>Keeping coordinates outside a Minecraft mapping lets the traversal
 * algorithm be compiled and tested once for every legacy Forge target.</p>
 */
public final class LegacyPosition {
    private final int x;
    private final int y;
    private final int z;

    public LegacyPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public LegacyPosition offset(int dx, int dy, int dz) {
        return new LegacyPosition(x + dx, y + dy, z + dz);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LegacyPosition)) {
            return false;
        }
        LegacyPosition that = (LegacyPosition) other;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "LegacyPosition{" + x + "," + y + "," + z + "}";
    }
}
