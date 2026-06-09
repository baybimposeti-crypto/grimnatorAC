package com.grimnatorac.predictionengine.movementtick;

import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.data.packetentity.PacketEntityRideable;
import com.grimnatorac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(GrimPlayer player) {
        super(player);
        this.movementInput = new Vector3dm(0, 0, 1);
    }

    @Override
    public float getSteeringSpeed() { // Vanilla multiples by 0.225f
        PacketEntityRideable pig = (PacketEntityRideable) player.compensatedEntities.self.getRiding();
        return (float) pig.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225f;
    }
}
