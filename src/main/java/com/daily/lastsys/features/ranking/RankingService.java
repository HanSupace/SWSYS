package com.daily.lastsys.features.ranking;

import com.daily.lastsys.features.userprogress.UserProgressRepository;
import com.daily.lastsys.features.userprogress.UserProgressService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RankingService {

    private static final int LEADERBOARD_SIZE = 10;

    private final UserProgressRepository userProgressRepository;
    private final UserProgressService userProgressService;

    public RankingService(
            UserProgressRepository userProgressRepository,
            UserProgressService userProgressService
    ) {
        this.userProgressRepository = userProgressRepository;
        this.userProgressService = userProgressService;
    }

    public RankingPageResponse getRanking(Long currentUserId) {
        List<UserProgressRepository.UserProgressSnapshot> snapshots = userProgressRepository.findLeaderboard();
        List<RankingEntryResponse> entries = new ArrayList<>(snapshots.size());
        RankingEntryResponse currentUser = null;
        Integer previousXp = null;
        int currentRank = 0;

        for (int index = 0; index < snapshots.size(); index += 1) {
            UserProgressRepository.UserProgressSnapshot snapshot = snapshots.get(index);
            if (previousXp == null || previousXp != snapshot.totalXp()) {
                currentRank = index + 1;
                previousXp = snapshot.totalXp();
            }

            boolean isCurrentUser = snapshot.userId().equals(currentUserId);
            RankingEntryResponse entry = new RankingEntryResponse(
                    currentRank,
                    snapshot.userId(),
                    snapshot.nickname(),
                    snapshot.totalXp(),
                    userProgressService.fromTotalXp(snapshot.totalXp()),
                    isCurrentUser
            );
            entries.add(entry);

            if (isCurrentUser) {
                currentUser = entry;
            }
        }

        List<RankingEntryResponse> leaders = entries.stream()
                .limit(LEADERBOARD_SIZE)
                .toList();
        boolean currentUserInLeaders = leaders.stream()
                .anyMatch(RankingEntryResponse::currentUser);

        return new RankingPageResponse(leaders, currentUser, currentUserInLeaders);
    }
}
