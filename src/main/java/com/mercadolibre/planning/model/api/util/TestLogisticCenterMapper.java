package com.mercadolibre.planning.model.api.util;

import java.util.Map;

public class TestLogisticCenterMapper {
    private static final Map<String, String> CAP5_TO_PACK_WHS = Map.of(
            "MXTP01", "MXCD01",
            "MXTP02", "MXCD02",
            "MXTP03", "MXCD03",
            "MXTP04", "MXCD04",
            "MXTP05", "MXNL01"
    );

    private TestLogisticCenterMapper() {
    }

    // This method is only for test in MLM and should be deleted in the future
    public static String toRealLogisticCenter(final String logisticCenterId) {
        return CAP5_TO_PACK_WHS.getOrDefault(logisticCenterId, logisticCenterId);
    }
}
