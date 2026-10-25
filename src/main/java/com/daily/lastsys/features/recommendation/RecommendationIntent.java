package com.daily.lastsys.features.recommendation;

import java.util.List;

public record RecommendationIntent(List<String> emotions, List<String> includeTags, List<String> excludeTags) {
}
