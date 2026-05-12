package com.daily.lastsys.service;

import com.daily.lastsys.dto.DailyMissionDayResponse;
import com.daily.lastsys.dto.DailyMissionListResponse;
import com.daily.lastsys.dto.DailyMissionResponse;
import com.daily.lastsys.repository.DailyMissionRepository;
import com.daily.lastsys.repository.DailyMissionRepository.DailyMissionSeed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
public class DailyMissionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DAILY_MISSION_COUNT = 5;
    private static final int MISSION_COMPLETE_XP = 20;
    private static final List<DailyMissionSeed> MISSION_POOL = List.of(
            new DailyMissionSeed("thanks-message", "가족이나 친구에게 고마웠던 점을 하나 메시지로 보내기"),
            new DailyMissionSeed("hold-door", "문이나 엘리베이터 앞에서 뒤 사람을 위해 잠깐 기다려주기"),
            new DailyMissionSeed("pick-trash", "내 주변에 보이는 작은 쓰레기 하나 줍기"),
            new DailyMissionSeed("warm-greeting", "오늘 만나는 사람 한 명에게 먼저 따뜻하게 인사하기"),
            new DailyMissionSeed("self-kindness", "고생한 나 자신에게 다정한 말 한마디 남기기"),
            new DailyMissionSeed("seat-kindness", "대중교통이나 대기 공간에서 필요한 사람에게 자리 양보하기"),
            new DailyMissionSeed("compliment", "누군가에게 진심 어린 칭찬 한마디 건네기"),
            new DailyMissionSeed("listen", "상대 이야기를 끊지 않고 끝까지 들어주기"),
            new DailyMissionSeed("share-snack", "간식이나 음료를 주변 사람과 나누기"),
            new DailyMissionSeed("clean-shared", "공용 공간에서 내가 쓰지 않은 자리까지 조금 정리하기"),
            new DailyMissionSeed("thank-worker", "가게나 시설 직원에게 또렷하게 감사 인사하기"),
            new DailyMissionSeed("check-in", "요즘 힘들어 보였던 사람에게 안부 묻기"),
            new DailyMissionSeed("positive-comment", "온라인에서 좋은 댓글이나 응원 한마디 남기기"),
            new DailyMissionSeed("quiet-help", "누군가 부탁하기 전에 필요한 일을 조용히 도와주기"),
            new DailyMissionSeed("donation-box", "작은 금액이라도 기부함이나 모금에 참여하기")
    );

    private final DailyMissionRepository dailyMissionRepository;
    private final UserProgressService userProgressService;

    public DailyMissionService(DailyMissionRepository dailyMissionRepository, UserProgressService userProgressService) {
        this.dailyMissionRepository = dailyMissionRepository;
        this.userProgressService = userProgressService;
    }

    @Transactional
    public DailyMissionListResponse getTodayMissions(Long userId) {
        LocalDate today = LocalDate.now(APP_ZONE);
        int successCount = dailyMissionRepository.findSuccessCount(userId, today);
        return new DailyMissionListResponse(
                getMissionItems(userId, today, successCount),
                successCount,
                userProgressService.getProgress(userId)
        );
    }

    @Transactional
    public DailyMissionListResponse completeMission(Long userId, String missionKey) {
        LocalDate today = LocalDate.now(APP_ZONE);
        boolean availableToday = pickMissions(userId, today).stream()
                .anyMatch(mission -> mission.key().equals(missionKey));

        if (!availableToday) {
            return getTodayMissions(userId);
        }

        boolean completed = dailyMissionRepository.increaseSuccessCount(userId, today, DAILY_MISSION_COUNT);

        if (completed) {
            userProgressService.addXp(userId, MISSION_COMPLETE_XP);
        }

        return getTodayMissions(userId);
    }

    @Transactional(readOnly = true)
    public List<DailyMissionDayResponse> getMonthlySuccessCounts(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDateExclusive = startDate.plusMonths(1);

        return dailyMissionRepository.findSuccessCounts(userId, startDate, endDateExclusive).stream()
                .map(day -> new DailyMissionDayResponse(day.date(), day.successCount()))
                .toList();
    }

    private List<DailyMissionResponse> getMissionItems(Long userId, LocalDate today, int successCount) {
        List<DailyMissionSeed> missions = pickMissions(userId, today);
        int completedCount = Math.min(successCount, missions.size());
        List<DailyMissionResponse> responses = new ArrayList<>();

        for (int index = 0; index < missions.size(); index += 1) {
            responses.add(toResponse(missions.get(index), index < completedCount));
        }

        return responses;
    }

    private List<DailyMissionSeed> pickMissions(Long userId, LocalDate today) {
        List<DailyMissionSeed> shuffledMissions = new ArrayList<>(MISSION_POOL);
        long seed = Objects.hash(userId, today);
        Collections.shuffle(shuffledMissions, new Random(seed));
        return shuffledMissions.subList(0, DAILY_MISSION_COUNT);
    }

    private DailyMissionResponse toResponse(DailyMissionSeed mission, boolean completed) {
        return new DailyMissionResponse(
                mission.key(),
                mission.text(),
                completed
        );
    }
}
