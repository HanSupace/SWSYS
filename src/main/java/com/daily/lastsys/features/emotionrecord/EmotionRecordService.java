package com.daily.lastsys.features.emotionrecord;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmotionRecordService {

    private final EmotionRecordRepository emotionRecordRepository;

    public EmotionRecordService(EmotionRecordRepository emotionRecordRepository) {
        this.emotionRecordRepository = emotionRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getMyRecords(Long userId) {
        return emotionRecordRepository.findAllByUserId(userId).stream()
                .map(EmotionRecordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmotionRecordResponse> getMyRecord(Long userId, Long recordId) {
        return emotionRecordRepository.findByIdAndUserId(recordId, userId)
                .map(EmotionRecordResponse::from);
    }

    @Transactional
    public boolean deleteMyRecord(Long userId, Long recordId) {
        return emotionRecordRepository.deleteByIdAndUserId(recordId, userId);
    }
}
