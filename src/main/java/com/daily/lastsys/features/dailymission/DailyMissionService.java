package com.daily.lastsys.features.dailymission;

import com.daily.lastsys.features.dailymission.DailyMissionRepository.DailyMissionSeed;
import com.daily.lastsys.features.emotion.EmotionCatalog;
import com.daily.lastsys.features.userprogress.UserProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@Service
public class DailyMissionService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DAILY_MISSION_COUNT = 5;
    private static final int MAX_SLOT_REROLL_COUNT = 3;
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
            new DailyMissionSeed("donation-box", "작은 금액이라도 기부함이나 모금에 참여하기"),
            new DailyMissionSeed("return-cart", "마트나 편의점 주변의 바구니나 카트를 제자리에 두기"),
            new DailyMissionSeed("kind-reply", "받은 메시지 하나에 조금 더 다정하게 답장하기"),
            new DailyMissionSeed("share-info", "누군가에게 도움이 될 만한 정보를 하나 공유하기"),
            new DailyMissionSeed("hold-elevator", "엘리베이터를 타기 전 뒤 사람을 한번 확인하고 기다려주기"),
            new DailyMissionSeed("thank-family", "가족이나 가까운 사람에게 오늘 고마운 점 하나 말하기")
    );
    private static final Map<String, List<DailyMissionSeed>> LIFE_STAGE_MISSIONS = Map.of(
            "STUDENT", List.of(
                    new DailyMissionSeed("student-help-friend", "친구가 어려워하는 과제나 공부 내용을 10분만 도와주기"),
                    new DailyMissionSeed("student-share-notes", "오늘 배운 내용 중 하나를 친구에게 쉽게 설명해주기"),
                    new DailyMissionSeed("student-study-habit", "내일의 공부 계획을 작게 하나 세우고 주변 친구도 응원하기"),
                    new DailyMissionSeed("student-greet-teacher", "선생님이나 조교에게 먼저 밝게 인사하기")
            ),
            "PRIMARY_SECONDARY_STUDENT", List.of(
                    new DailyMissionSeed("ps-student-include", "쉬는 시간에 혼자 있는 친구에게 먼저 말을 걸어보기"),
                    new DailyMissionSeed("ps-student-clean-class", "교실 바닥이나 책상 주변 쓰레기 하나 줍기"),
                    new DailyMissionSeed("ps-student-help-homework", "친구가 어려워하는 숙제나 준비물을 한번 도와주기"),
                    new DailyMissionSeed("ps-student-greet-teacher", "선생님께 먼저 밝게 인사하기"),
                    new DailyMissionSeed("ps-student-share-supply", "필요한 친구에게 필기구나 준비물을 잠깐 빌려주기"),
                    new DailyMissionSeed("ps-student-kind-word", "친구 한 명에게 기분 좋아지는 말 한마디 건네기")
            ),
            "UNIVERSITY_STUDENT", List.of(
                    new DailyMissionSeed("uni-share-notes", "강의 내용이나 과제 팁 하나를 동기에게 공유하기"),
                    new DailyMissionSeed("uni-check-team", "팀플 구성원 한 명에게 진행 상황을 배려 있게 확인하기"),
                    new DailyMissionSeed("uni-clean-seat", "강의실이나 도서관 자리를 떠나기 전 주변을 정리하기"),
                    new DailyMissionSeed("uni-greet-staff", "학과 사무실이나 학교 직원에게 감사 인사하기"),
                    new DailyMissionSeed("uni-include-peer", "혼자 있는 동기에게 식사나 이동을 가볍게 제안하기"),
                    new DailyMissionSeed("uni-help-freshman", "후배나 동기에게 학교 생활 팁 하나 알려주기")
            ),
            "WORKER", List.of(
                    new DailyMissionSeed("worker-thanks", "동료 한 명에게 오늘 고마웠던 일을 구체적으로 말하기"),
                    new DailyMissionSeed("worker-break", "바빠 보이는 동료에게 짧은 휴식이나 물 한 잔을 권하기"),
                    new DailyMissionSeed("worker-help-task", "동료의 작은 업무 하나를 먼저 도와줄 수 있는지 물어보기"),
                    new DailyMissionSeed("worker-positive-feedback", "회의나 대화에서 누군가의 좋은 의견을 짚어주기")
            ),
            "FREELANCER", List.of(
                    new DailyMissionSeed("freelancer-check-in", "혼자 일하는 지인에게 안부 메시지 보내기"),
                    new DailyMissionSeed("freelancer-kind-review", "도움 받은 자료나 서비스에 좋은 피드백 남기기"),
                    new DailyMissionSeed("freelancer-boundary", "나와 상대 모두를 위해 무리한 일정 하나를 정중히 조율하기")
            ),
            "CAREGIVER", List.of(
                    new DailyMissionSeed("caregiver-listen", "돌보는 사람의 이야기를 판단 없이 5분 들어주기"),
                    new DailyMissionSeed("caregiver-rest", "함께 있는 사람과 나를 위해 짧은 휴식 시간을 만들기"),
                    new DailyMissionSeed("caregiver-thanks-self", "오늘 버틴 나에게 고생했다는 말을 남기기")
            )
    );
    private static final Map<String, List<DailyMissionSeed>> ENVIRONMENT_MISSIONS = Map.of(
            "SCHOOL", List.of(
                    new DailyMissionSeed("school-clean-desk", "교실이나 강의실에서 내 자리 주변을 조금 정리하기"),
                    new DailyMissionSeed("school-include", "혼자 있는 친구에게 가볍게 말을 걸어보기"),
                    new DailyMissionSeed("school-lend", "필요한 사람에게 필기구나 자료를 빌려주기")
            ),
            "PRIMARY_SECONDARY_SCHOOL", List.of(
                    new DailyMissionSeed("ps-school-clean-board", "교실 칠판이나 주변 정리를 한번 도와주기"),
                    new DailyMissionSeed("ps-school-lunch-kind", "급식 줄에서 앞뒤 친구를 배려하며 기다리기"),
                    new DailyMissionSeed("ps-school-include-game", "놀이에 끼지 못한 친구에게 같이 하자고 말하기"),
                    new DailyMissionSeed("ps-school-help-teacher", "선생님이 정리하는 물건을 가볍게 도와드리기"),
                    new DailyMissionSeed("ps-school-safe-walk", "복도에서 뛰지 않고 주변 사람을 배려하며 걷기"),
                    new DailyMissionSeed("ps-school-share-umbrella", "비 오는 날 우산이나 여유 공간을 필요한 친구와 나누기")
            ),
            "UNIVERSITY", List.of(
                    new DailyMissionSeed("uni-env-library-seat", "도서관 자리를 깨끗하게 비우고 다음 사람을 배려하기"),
                    new DailyMissionSeed("uni-env-lab-clean", "실습실이나 동아리방 공용 물건 하나를 제자리에 두기"),
                    new DailyMissionSeed("uni-env-cafe-table", "학생식당이나 카페 테이블을 깨끗하게 정리하기"),
                    new DailyMissionSeed("uni-env-notice-share", "중요한 공지나 일정 하나를 놓친 사람에게 알려주기"),
                    new DailyMissionSeed("uni-env-door", "강의실 문이나 엘리베이터를 뒤 사람을 위해 잡아주기"),
                    new DailyMissionSeed("uni-env-campus-thanks", "경비, 청소, 행정 직원에게 감사 인사하기")
            ),
            "OFFICE", List.of(
                    new DailyMissionSeed("office-clean-shared", "공용 책상이나 탕비실을 내가 쓴 것보다 조금 더 정리하기"),
                    new DailyMissionSeed("office-appreciate", "보이지 않는 일을 한 사람에게 감사 표현하기"),
                    new DailyMissionSeed("office-door", "엘리베이터나 문 앞에서 뒤 사람을 기다려주기")
            ),
            "HOME", List.of(
                    new DailyMissionSeed("home-chore", "가족이나 룸메이트가 하기 전에 집안일 하나 먼저 하기"),
                    new DailyMissionSeed("home-kind-message", "가족에게 짧은 안부나 감사 메시지 보내기"),
                    new DailyMissionSeed("home-shared-space", "공용 공간 하나를 5분만 정리하기")
            ),
            "OUTSIDE", List.of(
                    new DailyMissionSeed("outside-pick-trash", "길에서 보이는 작은 쓰레기 하나 줍기"),
                    new DailyMissionSeed("outside-thank-worker", "가게나 시설 직원에게 또렷하게 감사 인사하기"),
                    new DailyMissionSeed("outside-yield", "대중교통이나 길에서 필요한 사람에게 먼저 양보하기")
            )
    );
    private static final Map<String, List<DailyMissionSeed>> CONDITION_MISSIONS = Map.of(
            "SAD", List.of(
                    new DailyMissionSeed("sad-walk", "햇빛이나 바람을 느끼며 10분 산책하기"),
                    new DailyMissionSeed("sad-check-in", "믿을 만한 사람에게 오늘 기분을 한 문장으로 보내기"),
                    new DailyMissionSeed("sad-self-kindness", "나 자신에게 다정한 말 한 문장을 적어두기")
            ),
            "TIRED", List.of(
                    new DailyMissionSeed("tired-water", "물을 한 잔 마시고 주변 사람에게도 챙기라고 말해주기"),
                    new DailyMissionSeed("tired-rest", "5분 쉬면서 급하지 않은 일 하나를 내려놓기"),
                    new DailyMissionSeed("tired-easy-help", "부담 없는 작은 친절 하나만 선택해서 하기")
            ),
            "STRESSED", List.of(
                    new DailyMissionSeed("stressed-breathe", "천천히 숨을 고른 뒤 누군가에게 날 선 말 대신 부드럽게 답하기"),
                    new DailyMissionSeed("stressed-priority", "오늘 할 일을 하나 줄이고 주변 사람에게도 무리하지 말라고 말해주기"),
                    new DailyMissionSeed("stressed-compliment", "가까운 사람에게 진심 어린 칭찬 한마디 건네기")
            ),
            "NORMAL", List.of(
                    new DailyMissionSeed("normal-greeting", "오늘 마주치는 사람 한 명에게 먼저 따뜻하게 인사하기"),
                    new DailyMissionSeed("normal-compliment", "누군가의 좋은 점을 발견해 바로 말해주기"),
                    new DailyMissionSeed("normal-share", "간식이나 작은 도움을 주변 사람과 나누기")
            )
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
        Set<String> completedMissionKeys = dailyMissionRepository.findCompletedMissionKeys(userId, today);
        return new DailyMissionListResponse(
                getMissionItems(userId, today, completedMissionKeys),
                successCount,
                userProgressService.getProgress(userId)
        );
    }

    @Transactional
    public DailyMissionListResponse completeMission(Long userId, String missionKey) {
        LocalDate today = LocalDate.now(APP_ZONE);
        boolean availableToday = pickMissions(userId, today).stream()
                .anyMatch(mission -> mission.seed().key().equals(missionKey));

        if (!availableToday) {
            return getTodayMissions(userId);
        }

        boolean completionInserted = dailyMissionRepository.insertMissionCompletion(userId, today, missionKey);
        boolean completed = completionInserted
                && dailyMissionRepository.increaseSuccessCount(userId, today, DAILY_MISSION_COUNT);

        if (completed) {
            userProgressService.addXp(userId, MISSION_COMPLETE_XP);
        }

        return getTodayMissions(userId);
    }

    @Transactional
    public DailyMissionListResponse rerollMissionSlot(Long userId, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= DAILY_MISSION_COUNT) {
            return getTodayMissions(userId);
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        Set<String> completedMissionKeys = dailyMissionRepository.findCompletedMissionKeys(userId, today);
        List<DailyMissionSlot> currentMissions = pickMissions(userId, today);
        boolean completedSlot = currentMissions.stream()
                .filter(mission -> mission.slotIndex() == slotIndex)
                .map(mission -> mission.seed().key())
                .anyMatch(completedMissionKeys::contains);

        if (completedSlot) {
            return getTodayMissions(userId);
        }

        int rerollCount = dailyMissionRepository.findSlotRerollCounts(userId, today).getOrDefault(slotIndex, 0);

        if (rerollCount >= MAX_SLOT_REROLL_COUNT) {
            return getTodayMissions(userId);
        }

        dailyMissionRepository.increaseSlotRerollCount(userId, today, slotIndex, MAX_SLOT_REROLL_COUNT);
        return getTodayMissions(userId);
    }

    @Transactional(readOnly = true)
    public MissionSettings getMissionSettings(Long userId) {
        return dailyMissionRepository.findMissionSettings(userId);
    }

    @Transactional
    public void saveMissionSettings(Long userId, MissionSettings settings) {
        dailyMissionRepository.saveMissionSettings(userId, sanitize(settings));
    }

    @Transactional(readOnly = true)
    public List<DailyMissionDayResponse> getMonthlyCalendarEmotions(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDateExclusive = startDate.plusMonths(1);

        return dailyMissionRepository.findRepresentativeEmotions(userId, startDate, endDateExclusive).stream()
                .map(day -> new DailyMissionDayResponse(
                        day.date(),
                        day.emotionLabel(),
                        EmotionCatalog.colorForLabelOrDefault(day.emotionLabel(), day.emotionColor())
                ))
                .toList();
    }

    private List<DailyMissionResponse> getMissionItems(Long userId, LocalDate today, Set<String> completedMissionKeys) {
        List<DailyMissionSlot> missions = pickMissions(userId, today);
        Map<Integer, Integer> slotRerollCounts = dailyMissionRepository.findSlotRerollCounts(userId, today);
        List<DailyMissionResponse> responses = new ArrayList<>();

        for (DailyMissionSlot mission : missions) {
            int rerollCount = Math.min(
                    slotRerollCounts.getOrDefault(mission.slotIndex(), 0),
                    MAX_SLOT_REROLL_COUNT
            );
            responses.add(toResponse(mission, completedMissionKeys.contains(mission.seed().key()), rerollCount));
        }

        return responses;
    }

    private List<DailyMissionSlot> pickMissions(Long userId, LocalDate today) {
        MissionSettings settings = dailyMissionRepository.findMissionSettings(userId);
        Map<Integer, Integer> slotRerollCounts = dailyMissionRepository.findSlotRerollCounts(userId, today);
        List<DailyMissionSeed> missionPool = missionPoolFor(settings);

        String[] currentPicks = new String[DAILY_MISSION_COUNT];

        for (int slotIndex = 0; slotIndex < DAILY_MISSION_COUNT; slotIndex++) {
            currentPicks[slotIndex] = pickForSlot(userId, today, settings, missionPool, slotIndex, 0, currentPicks);
        }

        for (int slotIndex = 0; slotIndex < DAILY_MISSION_COUNT; slotIndex++) {
            int rerolls = Math.min(slotRerollCounts.getOrDefault(slotIndex, 0), MAX_SLOT_REROLL_COUNT);
            for (int r = 1; r <= rerolls; r++) {
                currentPicks[slotIndex] = pickForSlot(userId, today, settings, missionPool, slotIndex, r, currentPicks);
            }
        }

        List<DailyMissionSlot> missions = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < DAILY_MISSION_COUNT; slotIndex++) {
            String key = currentPicks[slotIndex];
            DailyMissionSeed seedObj = missionPool.stream().filter(m -> m.key().equals(key)).findFirst().orElseThrow();
            missions.add(new DailyMissionSlot(slotIndex, seedObj));
        }

        return missions;
    }

    private String pickForSlot(Long userId, LocalDate today, MissionSettings settings, List<DailyMissionSeed> pool, int slotIndex, int reroll, String[] currentPicks) {
        List<DailyMissionSeed> candidates = new ArrayList<>(pool);
        long seed = Objects.hash(userId, today, settings, slotIndex, reroll);
        Collections.shuffle(candidates, new Random(seed));

        Set<String> excludes = new HashSet<>();
        for (String currentPick : currentPicks) {
            if (currentPick != null) {
                excludes.add(currentPick);
            }
        }

        for (DailyMissionSeed candidate : candidates) {
            if (!excludes.contains(candidate.key())) {
                return candidate.key();
            }
        }

        excludes.remove(currentPicks[slotIndex]);

        for (DailyMissionSeed candidate : candidates) {
            if (!excludes.contains(candidate.key())) {
                return candidate.key();
            }
        }

        return candidates.get(0).key();
    }

    private List<DailyMissionSeed> missionPoolFor(MissionSettings settings) {
        if (!settings.ruleBased()) {
            return MISSION_POOL;
        }

        LinkedHashMap<String, DailyMissionSeed> missions = new LinkedHashMap<>();
        addAll(missions, LIFE_STAGE_MISSIONS.get(settings.lifeStage()));
        addAll(missions, ENVIRONMENT_MISSIONS.get(settings.environmentType()));
        addAll(missions, CONDITION_MISSIONS.get(settings.conditionType()));
        addAll(missions, MISSION_POOL);
        return List.copyOf(missions.values());
    }

    private void addAll(LinkedHashMap<String, DailyMissionSeed> missions, List<DailyMissionSeed> seeds) {
        if (seeds == null) {
            return;
        }

        for (DailyMissionSeed seed : seeds) {
            missions.putIfAbsent(seed.key(), seed);
        }
    }

    private MissionSettings sanitize(MissionSettings settings) {
        return new MissionSettings(
                allowed(settings.mode(), List.of("PLAIN", "RULE_BASED"), "PLAIN"),
                allowed(settings.lifeStage(), List.of("ANY", "STUDENT", "PRIMARY_SECONDARY_STUDENT", "UNIVERSITY_STUDENT", "WORKER", "FREELANCER", "CAREGIVER"), "ANY"),
                allowed(settings.environmentType(), List.of("ANY", "SCHOOL", "PRIMARY_SECONDARY_SCHOOL", "UNIVERSITY", "OFFICE", "HOME", "OUTSIDE"), "ANY"),
                allowed(settings.conditionType(), List.of("NORMAL", "SAD", "TIRED", "STRESSED"), "NORMAL")
        );
    }

    private String allowed(String value, List<String> allowedValues, String fallback) {
        return allowedValues.contains(value) ? value : fallback;
    }

    private DailyMissionResponse toResponse(DailyMissionSlot mission, boolean completed, int rerollCount) {
        int remainingRerolls = Math.max(0, MAX_SLOT_REROLL_COUNT - rerollCount);
        return new DailyMissionResponse(
                mission.seed().key(),
                mission.slotIndex(),
                mission.seed().text(),
                completed,
                rerollCount,
                remainingRerolls,
                !completed && remainingRerolls > 0
        );
    }

    private record DailyMissionSlot(int slotIndex, DailyMissionSeed seed) {
    }
}
