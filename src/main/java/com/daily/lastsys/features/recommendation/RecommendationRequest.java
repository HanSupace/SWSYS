package com.daily.lastsys.features.recommendation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecommendationRequest(
        @NotBlank(message = "원하는 내용을 입력해주세요.")
        @Size(max = 500, message = "500자 이하로 입력해주세요.")
        String query
) {
}
