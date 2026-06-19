package com.daily.lastsys.features.map;

import com.daily.lastsys.common.ApiValidationException;
import com.daily.lastsys.features.emotion.EmotionCatalog;
import com.daily.lastsys.features.emotion.EmotionCatalog.EmotionMeta;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EmotionMapMarkerService {

    private final EmotionMapMarkerRepository emotionMapMarkerRepository;

    public EmotionMapMarkerService(EmotionMapMarkerRepository emotionMapMarkerRepository) {
        this.emotionMapMarkerRepository = emotionMapMarkerRepository;
    }

    public List<EmotionMapMarkerResponse> findMarkers(Long userId) {
        return emotionMapMarkerRepository.findAll(userId).stream()
                .map(this::normalize)
                .toList();
    }

    public EmotionMapMarkerResponse createMarker(Long userId, EmotionMapMarkerRequest request) {
        return emotionMapMarkerRepository.save(userId, validateAndNormalize(request));
    }

    @Transactional
    public EmotionMapRecordDetailResponse updateMarker(Long userId, Long markerId, EmotionMapMarkerRequest request) {
        EmotionMapMarkerRequest normalizedRequest = validateAndNormalize(request);
        boolean updated = emotionMapMarkerRepository.updateById(userId, markerId, normalizedRequest);

        if (!updated) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 기록만 수정할 수 있습니다.");
        }

        return normalize(emotionMapMarkerRepository.findDetail(userId, markerId));
    }

    @Transactional
    public void deleteMarker(Long userId, Long markerId) {
        boolean deleted = emotionMapMarkerRepository.deleteById(userId, markerId);

        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 기록만 삭제할 수 있습니다.");
        }
    }

    public EmotionMapRecordDetailResponse findRecordDetail(Long userId, Long markerId) {
        return normalize(emotionMapMarkerRepository.findDetail(userId, markerId));
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

    private EmotionMapMarkerRequest validateAndNormalize(EmotionMapMarkerRequest request) {
        List<String> errors = new ArrayList<>();
        String title = trim(request.title());
        String locationName = trim(request.locationName());
        String description = trim(request.description());
        String emotionLabel = trim(request.emotionLabel());
        Optional<EmotionMeta> emotion = EmotionCatalog.findByLabel(emotionLabel);

        if (emotion.isEmpty()) {
            errors.add("감정을 선택해주세요.");
        }

        if (title.isBlank()) {
            errors.add("제목을 입력해주세요.");
        } else if (title.length() > 24) {
            errors.add("제목은 최대 24자까지 입력 가능합니다.");
        }

        if (locationName.isBlank()) {
            errors.add("위치를 입력해주세요.");
        } else if (locationName.length() > 80) {
            errors.add("위치는 최대 80자까지 입력 가능합니다.");
        }

        if (description.isBlank()) {
            errors.add("내용을 입력해주세요.");
        } else if (description.length() > 2000) {
            errors.add("내용은 최대 2000자까지 입력 가능합니다.");
        }

        if (!errors.isEmpty()) {
            throw new ApiValidationException(errors);
        }

        EmotionMeta emotionMeta = emotion.orElseThrow();
        return new EmotionMapMarkerRequest(
                request.latitude(),
                request.longitude(),
                emotionMeta.label(),
                emotionMeta.color(),
                title,
                locationName,
                description
        );
    }

    private EmotionMapMarkerResponse normalize(EmotionMapMarkerResponse marker) {
        return new EmotionMapMarkerResponse(
                marker.id(),
                marker.latitude(),
                marker.longitude(),
                marker.authorNickname(),
                marker.emotionLabel(),
                EmotionCatalog.colorForLabelOrDefault(marker.emotionLabel(), marker.emotionColor()),
                marker.title(),
                marker.locationName(),
                marker.description(),
                marker.createdAt(),
                marker.own()
        );
    }

    private EmotionMapRecordDetailResponse normalize(EmotionMapRecordDetailResponse detail) {
        return new EmotionMapRecordDetailResponse(
                detail.id(),
                detail.userId(),
                detail.authorNickname(),
                detail.latitude(),
                detail.longitude(),
                detail.emotionLabel(),
                EmotionCatalog.colorForLabelOrDefault(detail.emotionLabel(), detail.emotionColor()),
                detail.title(),
                detail.locationName(),
                detail.content(),
                detail.createdAt(),
                detail.updatedAt(),
                detail.own(),
                detail.likeCount(),
                detail.commentCount(),
                detail.likedByMe()
        );
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
