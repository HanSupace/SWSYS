package com.daily.lastsys.features.map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EmotionMapMarkerRequest(
        @NotNull(message = "위치 좌표를 확인해주세요.")
        @DecimalMin(value = "-90.0", message = "위치 좌표를 확인해주세요.")
        @DecimalMax(value = "90.0", message = "위치 좌표를 확인해주세요.")
        BigDecimal latitude,

        @NotNull(message = "위치 좌표를 확인해주세요.")
        @DecimalMin(value = "-180.0", message = "위치 좌표를 확인해주세요.")
        @DecimalMax(value = "180.0", message = "위치 좌표를 확인해주세요.")
        BigDecimal longitude,

        @NotBlank(message = "감정을 선택해주세요.")
        @Size(max = 20, message = "감정 이름을 확인해주세요.")
        String emotionLabel,

        @Size(max = 20, message = "감정 색상을 확인해주세요.")
        String emotionColor,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 24, message = "제목은 최대 24자까지 입력 가능합니다.")
        String title,

        @NotBlank(message = "위치를 입력해주세요.")
        @Size(max = 80, message = "위치는 최대 80자까지 입력 가능합니다.")
        String locationName,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 2000, message = "내용은 최대 2000자까지 입력 가능합니다.")
        String description
) {
}
