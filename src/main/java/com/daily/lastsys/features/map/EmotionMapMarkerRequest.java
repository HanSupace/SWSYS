package com.daily.lastsys.features.map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EmotionMapMarkerRequest(
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal longitude,

        @NotBlank
        @Size(max = 20)
        String emotionLabel,

        @NotBlank
        @Size(max = 20)
        String emotionColor,

        @NotBlank
        @Size(max = 24)
        String title,

        @NotBlank
        @Size(max = 80)
        String locationName,

        @NotBlank
        @Size(max = 2000)
        String description
) {
}
