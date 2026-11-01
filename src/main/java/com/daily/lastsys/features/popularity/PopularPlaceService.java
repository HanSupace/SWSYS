package com.daily.lastsys.features.popularity;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class PopularPlaceService {
    private static final long CACHE_TTL_MILLIS = 3_000L;

    private final PopularPlaceRepository repository;
    private CachedPopularPlaces cache;

    public PopularPlaceService(PopularPlaceRepository repository) {
        this.repository = repository;
    }

    public synchronized List<PopularPlaceResponse> getPopularPlaces() {
        long now = System.currentTimeMillis();
        if (cache != null && cache.expiresAtMillis() > now) return cache.places();
        List<PopularPlaceResponse> places = repository.findPopularPlaces();
        cache = new CachedPopularPlaces(now + CACHE_TTL_MILLIS, places);
        return places;
    }

    public synchronized void recordSearch(Long userId, PlaceSearchEventRequest request) {
        String key = placeKey(request.placeName());
        if (key.isBlank()) return;
        if (repository.recordSearch(userId, request, key) > 0) cache = null;
    }

    public synchronized void invalidate() {
        cache = null;
    }

    private String placeKey(String placeName) {
        return (placeName == null ? "" : placeName.trim())
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private record CachedPopularPlaces(long expiresAtMillis, List<PopularPlaceResponse> places) {
    }
}
