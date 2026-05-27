package com.daily.lastsys.features.map;

public record EmotionMapLikeToggleResponse(
        boolean likedByMe,
        int likeCount
) {
}
