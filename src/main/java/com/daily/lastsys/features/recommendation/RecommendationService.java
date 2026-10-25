package com.daily.lastsys.features.recommendation;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {
    private final OpenAiRecommendationTagger tagger;
    private final RecommendationRepository repository;
    public RecommendationService(OpenAiRecommendationTagger tagger, RecommendationRepository repository) { this.tagger = tagger; this.repository = repository; }
    public RecommendationResponse recommend(Long userId, RecommendationRequest request) {
        RecommendationIntent intent = tagger.parseQuery(request.query());
        classifyLegacyRecords();
        return new RecommendationResponse(intent, repository.search(userId, intent));
    }
    private void classifyLegacyRecords() {
        List<RecommendationTagDocument> documents = repository.findUntagged();
        if (documents.isEmpty()) return;
        Map<Long, List<String>> tags = tagger.classifyRecords(documents);
        for (RecommendationTagDocument document : documents) {
            List<String> classified = tags.get(document.markerId());
            if (classified != null) repository.replaceTags(document.markerId(), classified, true);
        }
    }
}
