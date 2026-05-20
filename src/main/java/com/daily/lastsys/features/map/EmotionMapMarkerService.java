package com.daily.lastsys.features.map;

import org.springframework.stereotype.Service;

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
}
