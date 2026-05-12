package com.daily.lastsys.dto;

import java.time.LocalDate;

public record DailyMissionDayResponse(
        LocalDate date,
        int successCount
) {
}
