/*
 * Development-only compatibility shim for ForgeGradle 3 / Minecraft 1.12.2.
 * The published mod does not include this source set.
 */
package net.minecraftforge.fml.relauncher;

public enum Side {
    CLIENT,
    SERVER;

    public boolean isServer() {
        return !isClient();
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
