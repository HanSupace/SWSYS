package com.daily.lastsys.features.map;

import com.daily.lastsys.features.emotion.EmotionCatalog;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class HealingSpotService {

    private static final double EARTH_RADIUS_IN_METERS = 6371000;
    private static final double CLUSTER_RADIUS_IN_METERS = 250;
    private static final double DISTANCE_SCORE_DECAY_IN_METERS = 5000;
    private static final double MIN_POSITIVE_RATIO = 0.60;
    private static final double NEGATIVE_SCORE_WEIGHT = 0.5;
    private static final int MIN_CLUSTER_SIZE = 2;
    private static final int SEARCH_LIMIT = 3;
    private static final Set<String> POSITIVE_EMOTIONS = Set.copyOf(EmotionCatalog.positiveLabels());

    private final HealingSpotRepository healingSpotRepository;

    public HealingSpotService(HealingSpotRepository healingSpotRepository) {
        this.healingSpotRepository = healingSpotRepository;
    }

    public List<HealingSpotResponse> findHealingSpots(BigDecimal latitude, BigDecimal longitude) {
        List<HealingSpotRepository.HealingSpotCandidate> candidates =
                healingSpotRepository.findNearbyEmotionSpots(latitude, longitude);

        return cluster(candidates).stream()
                .map(members -> summarizeCluster(latitude, longitude, members))
                .filter(cluster -> cluster.positiveRatio() >= MIN_POSITIVE_RATIO)
                .sorted(Comparator
                        .comparingDouble(HealingSpotCluster::score).reversed()
                        .thenComparing(Comparator.comparingInt(HealingSpotCluster::positiveCount).reversed())
                        .thenComparingDouble(HealingSpotCluster::distanceInMeters))
                .limit(SEARCH_LIMIT)
                .map(cluster -> new HealingSpotResponse(
                        formatDistance(cluster.distanceInMeters()),
                        emotionEmoji(cluster.emotionLabel()),
                        cluster.locationName(),
                        cluster.latitude(),
                        cluster.longitude(),
                        cluster.positiveCount()
                ))
                .toList();
    }

    private List<List<HealingSpotRepository.HealingSpotCandidate>> cluster(
            List<HealingSpotRepository.HealingSpotCandidate> candidates
    ) {
        boolean[] visited = new boolean[candidates.size()];
        List<List<HealingSpotRepository.HealingSpotCandidate>> clusters = new ArrayList<>();

        for (int start = 0; start < candidates.size(); start++) {
            if (visited[start]) {
                continue;
            }

            List<HealingSpotRepository.HealingSpotCandidate> members = new ArrayList<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            visited[start] = true;
            queue.add(start);

            while (!queue.isEmpty()) {
                int currentIndex = queue.removeFirst();
                HealingSpotRepository.HealingSpotCandidate current = candidates.get(currentIndex);
                members.add(current);

                for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                    if (visited[candidateIndex]) {
                        continue;
                    }

                    HealingSpotRepository.HealingSpotCandidate candidate = candidates.get(candidateIndex);
                    if (distanceInMeters(
                            current.latitude(), current.longitude(),
                            candidate.latitude(), candidate.longitude()
                    ) <= CLUSTER_RADIUS_IN_METERS) {
                        visited[candidateIndex] = true;
                        queue.addLast(candidateIndex);
                    }
                }
            }

            if (members.size() >= MIN_CLUSTER_SIZE) {
                clusters.add(members);
            }
        }

        return clusters;
    }

    private HealingSpotCluster summarizeCluster(
            BigDecimal currentLatitude,
            BigDecimal currentLongitude,
            List<HealingSpotRepository.HealingSpotCandidate> members
    ) {
        BigDecimal centerLatitude = BigDecimal.valueOf(members.stream()
                .mapToDouble(member -> member.latitude().doubleValue())
                .average()
                .orElseThrow());
        BigDecimal centerLongitude = BigDecimal.valueOf(members.stream()
                .mapToDouble(member -> member.longitude().doubleValue())
                .average()
                .orElseThrow());
        double distance = distanceInMeters(
                currentLatitude, currentLongitude, centerLatitude, centerLongitude
        );
        List<HealingSpotRepository.HealingSpotCandidate> positiveMembers = members.stream()
                .filter(member -> isPositive(member.emotionLabel()))
                .toList();
        List<HealingSpotRepository.HealingSpotCandidate> negativeMembers = members.stream()
                .filter(member -> !isPositive(member.emotionLabel()))
                .toList();
        int positiveUserCount = (int) positiveMembers.stream()
                .map(HealingSpotRepository.HealingSpotCandidate::userId)
                .distinct()
                .count();
        int negativeUserCount = (int) negativeMembers.stream()
                .map(HealingSpotRepository.HealingSpotCandidate::userId)
                .distinct()
                .count();
        double positiveScore = positiveMembers.size() + (positiveUserCount * 2.0);
        double negativeScore = negativeMembers.size() + (negativeUserCount * 2.0);
        double sentimentScore = positiveScore - (negativeScore * NEGATIVE_SCORE_WEIGHT);
        double score = sentimentScore / (1 + (distance / DISTANCE_SCORE_DECAY_IN_METERS));
        double positiveRatio = (double) positiveMembers.size() / members.size();

        return new HealingSpotCluster(
                distance,
                dominantEmotion(positiveMembers),
                representativeLocationName(members, centerLatitude, centerLongitude),
                centerLatitude,
                centerLongitude,
                positiveMembers.size(),
                score,
                positiveRatio
        );
    }

    private boolean isPositive(String emotionLabel) {
        return POSITIVE_EMOTIONS.contains(emotionLabel);
    }

    private String dominantEmotion(List<HealingSpotRepository.HealingSpotCandidate> members) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        members.forEach(member -> counts.merge(member.emotionLabel(), 1, Integer::sum));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private String representativeLocationName(
            List<HealingSpotRepository.HealingSpotCandidate> members,
            BigDecimal centerLatitude,
            BigDecimal centerLongitude
    ) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        members.stream()
                .map(HealingSpotRepository.HealingSpotCandidate::locationName)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .forEach(name -> counts.merge(name, 1, Integer::sum));
        int highestCount = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Set<String> mostFrequentNames = new HashSet<>();
        counts.forEach((name, count) -> {
            if (count == highestCount) {
                mostFrequentNames.add(name);
            }
        });

        return members.stream()
                .filter(member -> member.locationName() != null && !member.locationName().isBlank())
                .filter(member -> mostFrequentNames.contains(member.locationName().trim()))
                .min(Comparator.comparingDouble(member -> distanceInMeters(
                        centerLatitude, centerLongitude, member.latitude(), member.longitude()
                )))
                .map(member -> member.locationName().trim())
                .orElse("이름 없는 위치");
    }

    private double distanceInMeters(
            BigDecimal firstLatitude,
            BigDecimal firstLongitude,
            BigDecimal secondLatitude,
            BigDecimal secondLongitude
    ) {
        double firstLatRadians = Math.toRadians(firstLatitude.doubleValue());
        double secondLatRadians = Math.toRadians(secondLatitude.doubleValue());
        double latitudeDelta = secondLatRadians - firstLatRadians;
        double longitudeDelta = Math.toRadians(secondLongitude.doubleValue() - firstLongitude.doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(firstLatRadians) * Math.cos(secondLatRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double normalized = Math.min(1, Math.max(0, haversine));
        return EARTH_RADIUS_IN_METERS * 2
                * Math.atan2(Math.sqrt(normalized), Math.sqrt(1 - normalized));
    }

    private record HealingSpotCluster(
            double distanceInMeters,
            String emotionLabel,
            String locationName,
            BigDecimal latitude,
            BigDecimal longitude,
            int positiveCount,
            double score,
            double positiveRatio
    ) {
    }

    public List<LikedSpotResponse> findLikedSpots() {
        return healingSpotRepository.findMostLikedSpots()
                .stream()
                .map(candidate -> new LikedSpotResponse(
                        candidate.id(),
                        displayName(candidate.locationName(), candidate.title()),
                        emotionEmoji(candidate.emotionLabel()),
                        candidate.emotionLabel(),
                        candidate.likeCount(),
                        displayDescription(candidate.description(), candidate.locationName()),
                        candidate.latitude(),
                        candidate.longitude()
                ))
                .toList();
    }

    private String formatDistance(double distanceInMeters) {
        long roundedMeters = Math.round(distanceInMeters);

        if (roundedMeters < 1000) {
            return roundedMeters + "m";
        }

        double kilometers = roundedMeters / 1000.0;
        return String.format(Locale.US, "%.1fkm", kilometers);
    }

    private String emotionEmoji(String emotionLabel) {
        return EmotionCatalog.findByLabel(emotionLabel)
                .map(EmotionCatalog.EmotionMeta::icon)
                .orElse("🙂");
    }

    private String displayName(String locationName, String title) {
        if (locationName != null && !locationName.isBlank()) {
            return locationName.trim();
        }

        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        return "이름 없는 위치";
    }

    private String displayDescription(String description, String locationName) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }

        if (locationName != null && !locationName.isBlank()) {
            return locationName.trim();
        }

        return "";
    }
}
