package com.daily.lastsys.features.emotion;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmotionSummaryService {

    private final EmotionSummaryRepository emotionSummaryRepository;

    public EmotionSummaryService(EmotionSummaryRepository emotionSummaryRepository) {
        this.emotionSummaryRepository = emotionSummaryRepository;
    }

    public List<EmotionSummaryResponse> findRecentSummary(Long userId) {
        return emotionSummaryRepository.findRecentSummary(userId);
    }
}
