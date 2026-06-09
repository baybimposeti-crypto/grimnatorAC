package com.grimnatorac.manager.init.stop;

import com.grimnatorac.manager.init.Initable;

public interface StoppableInitable extends Initable {
    void stop();
}
