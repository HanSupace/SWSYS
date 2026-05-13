package com.daily.lastsys.features.userprogress;

import org.springframework.stereotype.Service;

@Service
public class UserProgressService {

    private static final int BASE_REQUIRED_XP = 100;
    private static final int REQUIRED_XP_STEP = 30;

    private final UserProgressRepository userProgressRepository;

    public UserProgressService(UserProgressRepository userProgressRepository) {
        this.userProgressRepository = userProgressRepository;
    }

    public UserProgressResponse getProgress(Long userId) {
        return calculate(userProgressRepository.findTotalXp(userId));
    }

    public UserProgressResponse addXp(Long userId, int xp) {
        userProgressRepository.addXp(userId, xp);
        return getProgress(userId);
    }

    public UserProgressResponse fromTotalXp(int totalXp) {
        return calculate(totalXp);
    }

    private UserProgressResponse calculate(int totalXp) {
        int level = 1;
        int remainingXp = totalXp;
        int requiredXp = requiredXpFor(level);

        while (remainingXp >= requiredXp) {
            remainingXp -= requiredXp;
            level += 1;
            requiredXp = requiredXpFor(level);
        }

        int progressPercent = requiredXp == 0 ? 0 : Math.min(100, (remainingXp * 100) / requiredXp);
        return new UserProgressResponse(level, remainingXp, requiredXp, totalXp, progressPercent);
    }

    private int requiredXpFor(int level) {
        return BASE_REQUIRED_XP + ((level - 1) * REQUIRED_XP_STEP);
    }
}
