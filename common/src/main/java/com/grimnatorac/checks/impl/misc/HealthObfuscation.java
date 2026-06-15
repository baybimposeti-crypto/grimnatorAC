package com.grimnatorac.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PacketCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HealthObfuscation — hides real entity health values from the client.
 *
 * <p>Health is stored in {@code ENTITY_METADATA} at index 9 for all living entities
 * (LivingEntity base class). When the server updates an entity's health it sends this
 * packet with only the changed entries.
 *
 * <ul>
 *   <li>Entities never attacked: health metadata index is replaced with MAX_HEALTH (20).</li>
 *   <li>First attack on entity: send 20.0 (client thinks entity is full health).</li>
 *   <li>Subsequent attacks: random float in (0.5, 20.0].</li>
 *   <li>Player's own health (UPDATE_HEALTH): also randomised.</li>
 * </ul>
 */
@CheckData(
        name = "HealthObfuscation",
        stableKey = "grimnatorac.misc.health_obfuscation",
        description = "Hides real entity health from clients to counter health-reading cheats",
        decay = 0,
        setback = Integer.MAX_VALUE,
        experimental = false
)
public class HealthObfuscation extends Check implements PacketCheck {

    /**
     * Health metadata index for all LivingEntity subclasses in Minecraft 1.17+.
     * This is index 9 in the LivingEntity data layout (base Entity = 0-7, LivingEntity starts at 8).
     */
    private static final int HEALTH_INDEX = 9;

    private static final float MAX_HEALTH = 20.0f;
    private static final float MIN_HEALTH = 0.5f;

    private final Random rng = new Random();

    /**
     * Entity IDs that this player has attacked at least once.
     * First attack → 20.0f, subsequent attacks → random.
     */
    private final Set<Integer> attackedOnce = ConcurrentHashMap.newKeySet();

    private boolean enabled;

    public HealthObfuscation(GrimPlayer player) {
        super(player);
    }

    // -----------------------------------------------------------------------
    // Inbound — track attacks
    // -----------------------------------------------------------------------

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!enabled) return;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack attack = new WrapperPlayClientAttack(event);
            attackedOnce.add(attack.getEntityId());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                attackedOnce.add(interact.getEntityId());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Outbound — spoof health packets
    // -----------------------------------------------------------------------

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!enabled) return;

        // Only intercept ENTITY_METADATA for other entities — never touch the player's own health
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            spoofEntityMetadata(event);
        }
        // UPDATE_HEALTH is intentionally NOT intercepted — the client must see their own real health
    }

    // -----------------------------------------------------------------------
    // Spoofing logic
    // -----------------------------------------------------------------------

    private void spoofEntityMetadata(PacketSendEvent event) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        int entityId = packet.getEntityId();

        // Don't touch the player's own metadata
        if (entityId == player.entityID) return;

        List<EntityData<?>> metadata = packet.getEntityMetadata();
        if (metadata == null || metadata.isEmpty()) return;

        // Health is index 9 for all LivingEntity types (post-1.17)
        EntityData<?> healthData = WatchableIndexUtil.getIndex(metadata, HEALTH_INDEX);

        // Only proceed if health is actually in this packet and is a Float
        if (healthData == null || healthData.getType() != EntityDataTypes.FLOAT) return;

        float fakeHealth;
        if (!attackedOnce.contains(entityId)) {
            // Never attacked — send 20.0 (full health, tells nothing)
            fakeHealth = MAX_HEALTH;
        } else {
            // Already attacked — send random value ≤ 20
            fakeHealth = MIN_HEALTH + rng.nextFloat() * (MAX_HEALTH - MIN_HEALTH);
        }

        // Replace the health entry in the list
        int idx = metadata.indexOf(healthData);
        metadata.set(idx, new EntityData<>(HEALTH_INDEX, EntityDataTypes.FLOAT, fakeHealth));
        packet.setEntityMetadata(metadata);

        // Mark for re-encode so the modified data is actually written to the packet bytes
        event.markForReEncode(true);
    }

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    @Override
    public void onReload(ConfigManager config) {
        this.enabled = config.getBooleanElse("HealthObfuscation.enabled", true);
    }
}
