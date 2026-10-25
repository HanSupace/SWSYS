package com.daily.lastsys.features.recommendation;
public record RecommendationTagDocument(Long markerId, String text) {
    public static String textFor(String emotion, String title, String location, String description) { return String.join(" ", emotion, title, location, description == null ? "" : description); }
}
