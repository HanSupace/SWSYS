package com.daily.lastsys.features.dailymission;

import java.time.LocalDate;

public record DailyMissionDayResponse(
        LocalDate date,
        String emotionLabel,
        String emotionColor
) {
}
