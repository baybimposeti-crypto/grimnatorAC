package com.grimnatorac.checks.exempt;

import java.util.UUID;

/**
 * ExemptionProvider - Platform-independent interface for check exemptions
 *
 * <p>This interface allows platform-specific implementations (e.g., Bukkit's
 * UkkaHandshakeManager) to provide exemption logic to the common anticheat
 * checks without creating circular dependencies between modules.
 *
 * <p>The exemption provider is registered at plugin startup and queried by
 * the Check base class to determine if a player should be exempt from checks.
 */
public interface ExemptionProvider {

    /**
     * Checks if a player UUID should be exempt from all anticheat checks.
     *
     * @param uuid The player UUID to check
     * @return true if player should be exempt, false otherwise
     */
    boolean isExempt(UUID uuid);
}
