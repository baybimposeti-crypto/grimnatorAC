package com.grimnatorac.utils.item;

import com.grimnatorac.player.GrimPlayer;
import com.grimnatorac.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class UnsupportedItem extends ItemBehaviour {

    public static final UnsupportedItem INSTANCE = new UnsupportedItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, GrimPlayer player, InteractionHand hand) {
        return false;
    }

}
