package com.daily.lastsys.features.map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
public class HealingSpotController {

    private final HealingSpotService healingSpotService;

    public HealingSpotController(HealingSpotService healingSpotService) {
        this.healingSpotService = healingSpotService;
    }

    @GetMapping("/api/spots/healing")
    public List<HealingSpotResponse> healingSpots(
            @RequestParam("lat") @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @RequestParam("lng") @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
    ) {
        return healingSpotService.findHealingSpots(latitude, longitude);
    }
}
