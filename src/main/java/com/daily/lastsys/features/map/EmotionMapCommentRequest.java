package com.daily.lastsys.features.map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmotionMapCommentRequest(
        @NotBlank
        @Size(max = 300)
        String content
) {
}
