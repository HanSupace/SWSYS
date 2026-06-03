package com.daily.lastsys.features.emotion;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class EmotionCatalog {

    private static final List<EmotionMeta> EMOTIONS = List.of(
            new EmotionMeta("default-happiness", "HAPPY", "happiness", "기쁨", "😊", "#765A08", "#FFF7D7", "rgba(171, 126, 9, 0.22)", true),
            new EmotionMeta("default-anticipation", "ANTICIPATION", "anticipation", "기대", "🌿", "#4F711F", "#ECF8DD", "rgba(89, 137, 36, 0.22)", true),
            new EmotionMeta("default-sadness", "SAD", "sadness", "슬픔", "☔", "#315C86", "#EAF3FF", "rgba(62, 111, 162, 0.22)", false),
            new EmotionMeta("default-anger", "ANGRY", "anger", "분노", "🔥", "#9D312B", "#FFF1F0", "rgba(196, 73, 64, 0.22)", false),
            new EmotionMeta("default-anxiety", "ANXIOUS", "anxiety", "불안", "…", "#5C477D", "#F3EEFF", "rgba(106, 78, 150, 0.22)", false),
            new EmotionMeta("default-embarrassment", "EMBARRASSED", "embarrassment", "당황", "?", "#8B3D66", "#FFEFF7", "rgba(177, 75, 120, 0.22)", false),
            new EmotionMeta("default-surprise", "SURPRISED", "surprise", "놀람", "✨", "#28746F", "#E8F9F7", "rgba(50, 137, 128, 0.22)", true),
            new EmotionMeta("default-irritation", "IRRITATED", "irritation", "짜증", "!", "#925021", "#FFF0E4", "rgba(181, 99, 31, 0.22)", false)
    );

    private EmotionCatalog() {
    }

    public static List<EmotionMeta> all() {
        return EMOTIONS;
    }

    public static List<String> positiveLabels() {
        return EMOTIONS.stream()
                .filter(EmotionMeta::positive)
                .map(EmotionMeta::label)
                .toList();
    }

    public static Optional<EmotionMeta> findByLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }

        String normalizedLabel = label.trim();
        return EMOTIONS.stream()
                .filter(emotion -> emotion.label().equals(normalizedLabel))
                .findFirst();
    }

    public static Optional<EmotionMeta> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        return EMOTIONS.stream()
                .filter(emotion -> emotion.code().equals(normalizedCode))
                .findFirst();
    }

    public static Optional<EmotionMeta> findByValue(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return EMOTIONS.stream()
                .filter(emotion -> emotion.value().equals(normalizedValue))
                .findFirst();
    }

    public static Optional<EmotionMeta> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        String normalizedId = id.trim();
        return EMOTIONS.stream()
                .filter(emotion -> emotion.id().equals(normalizedId))
                .findFirst();
    }

    public static String colorForLabelOrDefault(String label, String fallbackColor) {
        return findByLabel(label)
                .map(EmotionMeta::color)
                .orElse(fallbackColor);
    }

    public static boolean colorMatches(String label, String color) {
        return findByLabel(label)
                .map(emotion -> emotion.color().equalsIgnoreCase(String.valueOf(color).trim().toUpperCase(Locale.ROOT)))
                .orElse(false);
    }

    public record EmotionMeta(
            String id,
            String code,
            String value,
            String label,
            String icon,
            String color,
            String background,
            String border,
            boolean positive
    ) {
    }
}
