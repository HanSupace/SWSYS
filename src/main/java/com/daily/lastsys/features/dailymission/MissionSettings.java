package com.daily.lastsys.features.dailymission;

public record MissionSettings(
        String mode,
        String lifeStage,
        String environmentType,
        String conditionType
) {
    public static MissionSettings defaults() {
        return new MissionSettings("PLAIN", "ANY", "ANY", "NORMAL");
    }

    public boolean ruleBased() {
        return "RULE_BASED".equals(mode);
    }
}
