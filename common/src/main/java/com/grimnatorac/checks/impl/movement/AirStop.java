package com.grimnatorac.checks.impl.movement;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.checks.type.PostPredictionCheck;
import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.anticheat.update.PredictionComplete;
import com.grimnatorac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import com.grimnatorac.GrimAPI;

/**
 * AirStop - Detects players freezing in mid-air with zero velocity
 *
 * <p>This check detects "air stop" or "air freeze" cheats where players
 * float motionless in the air without any visible velocity or movement.
 *
 * <p>Detection logic:
 * - Player must be in air (not on ground, not in liquid, not on ladder)
 * - Player must have near-zero velocity (< 0.001 on all axes)
 * - Player must maintain this state for 2+ seconds (40+ ticks)
 *
 * <p>On detection:
 * - Warns admins/ops with alert message
 * - Logs to server console
 * - Kicks player with "Geçersiz Hareket" (Invalid Movement) message in Turkish
 */
@CheckData(
    name = "AirStop",
    stableKey = "grimnatorac.movement.airstop",
    description = "Detects players freezing in mid-air with zero velocity",
    decay = 0.05
)
public class AirStop extends Check implements PostPredictionCheck {

    // Tick counter for how long player has been frozen in air
    private int frozenAirTicks = 0;

    // Threshold: 2 seconds = 40 ticks (20 ticks per second)
    private static final int FREEZE_THRESHOLD_TICKS = 40;

    // Velocity threshold: consider velocity "zero" if < 0.001 on all axes
    private static final double VELOCITY_THRESHOLD = 0.001;

    // Track if we've already kicked this player (prevent spam)
    private boolean hasKicked = false;

    public AirStop(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Skip if already kicked
        if (hasKicked) {
            return;
        }

        // Exemption: Permission bypass
        if (isExemptPermission()) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Player on ground
        if (player.onGround || player.lastOnGround) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Player in liquid (water/lava)
        if (player.wasTouchingWater || player.wasTouchingLava || player.wasEyeInWater) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Player on ladder/vine
        if (player.isClimbing) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Player in vehicle
        if (player.inVehicle()) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Player is flying (creative/spectator mode)
        if (player.isFlying || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
            || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Player recently teleported (give 20 ticks grace period)
        if (player.getSetbackTeleportUtil().getRequiredSetBack() != null) {
            frozenAirTicks = 0;
            return;
        }

        // Exemption: Elytra flying
        if (player.isGliding) {
            frozenAirTicks = 0;
            return;
        }

        // Check if player has near-zero velocity on all axes
        double velX = Math.abs(player.clientVelocity.getX());
        double velY = Math.abs(player.clientVelocity.getY());
        double velZ = Math.abs(player.clientVelocity.getZ());

        boolean isZeroVelocity = velX < VELOCITY_THRESHOLD
                              && velY < VELOCITY_THRESHOLD
                              && velZ < VELOCITY_THRESHOLD;

        if (isZeroVelocity) {
            // Player is frozen in air, increment counter
            frozenAirTicks++;

            // Check if threshold exceeded
            if (frozenAirTicks >= FREEZE_THRESHOLD_TICKS) {
                // DETECTION!
                double seconds = frozenAirTicks / 20.0;
                String verbose = String.format("frozen=%.1fs vel=%.4f,%.4f,%.4f",
                    seconds, velX, velY, velZ);

                flag(verbose);

                // Alert admins/ops
                alertStaff(seconds);

                // Kick player
                kickPlayer();

                // Mark as kicked to prevent spam
                hasKicked = true;

                // Reset counter
                frozenAirTicks = 0;
            }
        } else {
            // Player has movement, reset counter
            frozenAirTicks = 0;
        }
    }

    /**
     * Alerts staff members (ops and players with grimnatorac.alerts permission)
     * about the air stop detection.
     */
    private void alertStaff(double seconds) {
        String message = String.format(
            "%%prefix%% &b%s &7detected air stop: &cfrozen for %.1f seconds in mid-air",
            player.user.getName(),
            seconds
        );

        Component msg = MessageUtil.miniMessage(message);

        // Send alert using GrimAPI
        GrimAPI.INSTANCE.getAlertManager().sendAlert(msg, null);
    }

    /**
     * Kicks the player with a Turkish kick message explaining the violation.
     */
    private void kickPlayer() {
        if (!shouldModifyPackets()) {
            return;
        }

        // Turkish kick message with explanation
        String kickMessage = "§f§lBu sunucudan atıldınız.\n\n" +
            "§c§lSebep: §cGeçersiz Hareket\n" +
            "§7Havada hareketsiz durma tespit edildi.\n\n" +
            "§e§lAçıklama:\n" +
            "§eOyununuz " + String.format("%.1f", frozenAirTicks / 20.0) + " saniye boyunca havada\n" +
            "§ehavreketsiz kaldınız (hız = 0).\n" +
            "§eBu hile kullanımı veya bağlantı sorunu\n" +
            "§eolabilir.\n\n" +
            "§7Bu bir hata olduğunu düşünüyorsanız Discord'dan yetkililere ulaşın.";

        // Use MessageUtil to convert to Component and disconnect
        Component kickComponent = MessageUtil.miniMessage(kickMessage);
        player.disconnect(kickComponent);
    }
}
