package com.daily.lastsys.features.popularity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PlaceSearchEventRequest(
        @Size(max = 100, message = "장소 식별자를 확인해주세요.")
        String placeId,

        @NotBlank(message = "장소 이름을 확인해주세요.")
        @Size(max = 80, message = "장소 이름은 80자까지 저장할 수 있습니다.")
        String placeName,

        @Size(max = 160, message = "장소 분류를 확인해주세요.")
        String categoryName,

        @NotNull(message = "장소 좌표를 확인해주세요.")
        @DecimalMin(value = "-90.0", message = "장소 좌표를 확인해주세요.")
        @DecimalMax(value = "90.0", message = "장소 좌표를 확인해주세요.")
        BigDecimal latitude,

        @NotNull(message = "장소 좌표를 확인해주세요.")
        @DecimalMin(value = "-180.0", message = "장소 좌표를 확인해주세요.")
        @DecimalMax(value = "180.0", message = "장소 좌표를 확인해주세요.")
        BigDecimal longitude
) {
}
