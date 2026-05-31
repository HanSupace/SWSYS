package com.daily.lastsys.features.map;

import java.math.BigDecimal;

public record HealingSpotResponse(
        String distance,
        String emotion,
        String name,
        BigDecimal lat,
        BigDecimal lng
) {
}
