package com.grimnatorac.platform.fabric.initables;

import com.grimnatorac.GrimAPI;
import com.grimnatorac.manager.init.start.StartableInitable;
import com.grimnatorac.manager.init.stop.StoppableInitable;
import com.grimnatorac.platform.fabric.utils.metrics.MetricsFabric;
import com.grimnatorac.utils.anticheat.Constants;

public class FabricBStats implements StartableInitable, StoppableInitable {

    private MetricsFabric metricsFabric;

    @Override
    public void start() {
        try {
            metricsFabric = new MetricsFabric(GrimAPI.INSTANCE.getGrimPlugin(), Constants.BSTATS_PLUGIN_ID);
        } catch (Exception ignored) {}
    }

    @Override
    public void stop() {
        if (metricsFabric != null)
            metricsFabric.shutdown();
    }
}
