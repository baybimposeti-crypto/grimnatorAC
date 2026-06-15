package com.grimnatorac.checks.impl.misc;

import com.grimnatorac.checks.Check;
import com.grimnatorac.checks.CheckData;
import com.grimnatorac.player.GrimPlayer;

@CheckData(name = "TransactionOrder", stableKey = "grimnatorac.ping.invalid_transaction_order")
public class TransactionOrder extends Check {
    public TransactionOrder(GrimPlayer player) {
        super(player);
    }
}
