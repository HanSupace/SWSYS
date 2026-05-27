package com.daily.lastsys.features.map;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmotionMapMarkerService {

    private final EmotionMapMarkerRepository emotionMapMarkerRepository;

    public EmotionMapMarkerService(EmotionMapMarkerRepository emotionMapMarkerRepository) {
        this.emotionMapMarkerRepository = emotionMapMarkerRepository;
    }

    public List<EmotionMapMarkerResponse> findMarkers(Long userId) {
        return emotionMapMarkerRepository.findAll(userId);
    }

    public EmotionMapMarkerResponse createMarker(Long userId, EmotionMapMarkerRequest request) {
        return emotionMapMarkerRepository.save(userId, request);
    }

    @Transactional
    public void deleteMarker(Long userId, Long markerId) {
        boolean deleted = emotionMapMarkerRepository.deleteById(userId, markerId);

        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 기록만 삭제할 수 있습니다.");
        }
    }

    public EmotionMapRecordDetailResponse findRecordDetail(Long userId, Long markerId) {
        return emotionMapMarkerRepository.findDetail(userId, markerId);
    }

    @Transactional
    public EmotionMapLikeToggleResponse toggleLike(Long userId, Long markerId) {
        return emotionMapMarkerRepository.toggleLike(userId, markerId);
    }

    public List<EmotionMapCommentResponse> findComments(Long userId, Long markerId) {
        return emotionMapMarkerRepository.findComments(userId, markerId);
    }

    @Transactional
    public EmotionMapCommentResponse createComment(Long userId, Long markerId, EmotionMapCommentRequest request) {
        return emotionMapMarkerRepository.saveComment(userId, markerId, request);
    }
}
