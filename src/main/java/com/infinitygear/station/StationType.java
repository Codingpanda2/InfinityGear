package com.infinitygear.station;

public enum StationType {
    RUNIC_TABLE("runic-table"),
    FUSION_ALTAR("fusion-altar"),
    GEAR_FORGE("gear-forge");

    private final String configKey;
    StationType(String configKey) { this.configKey = configKey; }
    public String configKey() { return configKey; }
}
