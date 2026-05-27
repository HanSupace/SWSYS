package com.daily.lastsys.features.emotionrecord;

import java.time.format.DateTimeFormatter;

public record EmotionRecordResponse(
        Long id,
        String title,
        String content,
        String summary,
        String createdAtText
) {
    private static final DateTimeFormatter CREATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static EmotionRecordResponse from(EmotionRecord record) {
        String normalizedTitle = normalizeTitle(record.title());
        String normalizedContent = record.content() == null ? "" : record.content();

        return new EmotionRecordResponse(
                record.id(),
                normalizedTitle,
                normalizedContent,
                summarize(normalizedContent),
                record.createdAt().format(CREATED_AT_FORMATTER)
        );
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "제목 없는 기록";
        }
        return title.strip();
    }

    private static String summarize(String content) {
        String compactContent = content.replaceAll("\\s+", " ").strip();
        if (compactContent.isBlank()) {
            return "내용이 없습니다.";
        }
        if (compactContent.length() <= 60) {
            return compactContent;
        }
        return compactContent.substring(0, 60) + "...";
    }
}
