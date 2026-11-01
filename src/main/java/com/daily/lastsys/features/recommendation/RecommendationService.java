package com.daily.lastsys.features.recommendation;

import com.daily.lastsys.features.map.EmotionMapMarkerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private final OpenAiRecommendationTagger tagger;
    private final RecommendationRepository repository;
    public RecommendationService(OpenAiRecommendationTagger tagger, RecommendationRepository repository) { this.tagger = tagger; this.repository = repository; }
    public RecommendationResponse recommend(Long userId, RecommendationRequest request) {
        RecommendationIntent intent = tagger.parseQuery(request.query());
        classifyLegacyRecords();
        List<EmotionMapMarkerResponse> directlyMatched = repository.search(userId, intent);
        List<Long> semanticIds = List.of();
        try {
            semanticIds = tagger.rankRelevantRecords(request.query(), intent, repository.findRecommendationCandidates());
        } catch (RuntimeException exception) {
            log.warn("의미 기반 기록 추천을 완료하지 못해 태그 검색 결과를 사용합니다.", exception);
        }

        Map<Long, EmotionMapMarkerResponse> rankedResults = new LinkedHashMap<>();
        repository.findByIds(userId, semanticIds).forEach(record -> rankedResults.put(record.id(), record));
        directlyMatched.forEach(record -> rankedResults.putIfAbsent(record.id(), record));
        return new RecommendationResponse(intent, rankedResults.values().stream().limit(5).toList());
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
