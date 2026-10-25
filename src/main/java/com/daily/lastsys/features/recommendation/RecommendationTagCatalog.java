package com.daily.lastsys.features.recommendation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RecommendationTagCatalog {
    private static final Map<String, List<String>> SIMILAR_TAGS = new LinkedHashMap<>();

    static {
        addFamily("QUIET", "SECLUDED", "CALM", "SERENE");
        addFamily("REST", "HEALING", "COMFORT", "RELAXATION");
        addFamily("WALK", "STROLL", "HIKING");
        addFamily("NATURE", "GREENERY", "PARK", "FOREST");
        addFamily("SOCIAL", "FRIENDS", "CONVERSATION");
        addFamily("LIVELY", "ACTIVE", "ENERGETIC");
        addFamily("LOUD", "NOISY", "ROWDY");
        addFamily("CROWDED", "BUSY", "POPULAR");
        addFamily("WATERFRONT", "RIVER", "SEA", "LAKE");
        addFamily("INDOOR", "CAFE", "LIBRARY");
        addFamily("OUTDOOR", "MOUNTAIN", "GARDEN");
    }

    private RecommendationTagCatalog() {
    }

    private static void addFamily(String... codes) {
        List<String> members = List.of(codes);
        for (String code : codes) SIMILAR_TAGS.put(code, members);
    }

    public static List<String> allCodes() {
        return List.copyOf(SIMILAR_TAGS.keySet());
    }

    public static List<String> relatedCodes(String code) {
        return SIMILAR_TAGS.getOrDefault(code, List.of());
    }

    public static List<List<String>> relatedGroups(List<String> codes) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<List<String>> groups = new java.util.ArrayList<>();
        for (String code : codes) {
            List<String> group = relatedCodes(code);
            if (!group.isEmpty() && seen.add(group.getFirst())) groups.add(group);
        }
        return groups;
    }

    public static List<String> inferNegatedTags(String query) {
        String compact = query.replaceAll("\\s+", "").toLowerCase();
        if (compact.contains("아니라") || compact.contains("아니고")) return List.of();
        java.util.List<String> negativeTags = new java.util.ArrayList<>();
        if (Pattern.matches(".*(조용|한적|고요|차분|평온)(한)?(곳|장소|분위기)?(은|는|이|가)?(싫|별로|원하지|피하고싶).*", compact)
                || Pattern.matches(".*(조용|한적|고요|차분|평온)(하)?지않(고|은|아|다|는|게|지만).*", compact)) {
            negativeTags.add("QUIET");
        }
        if (Pattern.matches(".*(시끄럽|소음|소란)(지않(고|은|아|다|는|게|지만)|[은는이가]?(싫|별로|원하지|피하고싶)).*", compact)) {
            negativeTags.add("LOUD");
        }
        return List.copyOf(negativeTags);
    }
}
