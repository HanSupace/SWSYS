package com.daily.lastsys.features.dailymission;

public record DailyMissionResponse(
        String id,
        int slotIndex,
        String text,
        boolean completed,
        int rerollCount,
        int remainingRerolls,
        boolean rerollAvailable
) {
}
