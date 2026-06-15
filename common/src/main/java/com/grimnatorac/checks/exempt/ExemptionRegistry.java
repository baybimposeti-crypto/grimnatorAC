package com.grimnatorac.checks.exempt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ExemptionRegistry - Global registry for exemption providers
 *
 * <p>This registry allows platform-specific exemption providers to be
 * registered at plugin startup and queried by checks during runtime.
 *
 * <p>Thread-safe for registration and query operations.
 */
public class ExemptionRegistry {

    private static final List<ExemptionProvider> providers = new ArrayList<>();

    /**
     * Registers an exemption provider.
     * Should be called during plugin initialization.
     *
     * @param provider The exemption provider to register
     */
    public static synchronized void register(ExemptionProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
        }
    }

    /**
     * Unregisters an exemption provider.
     * Should be called during plugin shutdown.
     *
     * @param provider The exemption provider to unregister
     */
    public static synchronized void unregister(ExemptionProvider provider) {
        providers.remove(provider);
    }

    /**
     * Checks if a player UUID is exempt according to any registered provider.
     *
     * @param uuid The player UUID to check
     * @return true if any provider returns true, false otherwise
     */
    public static boolean isExempt(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        // Check all providers - if ANY returns true, player is exempt
        for (ExemptionProvider provider : providers) {
            try {
                if (provider.isExempt(uuid)) {
                    return true;
                }
            } catch (Throwable t) {
                // Silently ignore provider errors to avoid breaking checks
            }
        }

        return false;
    }

    /**
     * Clears all registered providers.
     * Used primarily for testing.
     */
    public static synchronized void clear() {
        providers.clear();
    }
}
