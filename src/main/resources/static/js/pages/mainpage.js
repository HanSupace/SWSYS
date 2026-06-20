document.addEventListener('DOMContentLoaded', () => {
    const calendarElement = document.getElementById('emotion-calendar');
    const missionPage = document.querySelector('[data-mission-page]');
    const calendarTitle = document.getElementById('calendar-title');
    const previousButton = document.getElementById('calendar-prev');
    const nextButton = document.getElementById('calendar-next');
    const missionPanel = document.getElementById('mission-panel');
    const missionToggle = document.querySelector('.floating-toggle');
    const missionClose = document.querySelector('.mission-close');
    const missionReroll = document.querySelector('.mission-reroll');
    const missionList = document.getElementById('mission-list');
    const missionConfirm = document.getElementById('mission-confirm');
    const missionConfirmSubmit = document.getElementById('mission-confirm-submit');
    const missionConfirmCloseTriggers = document.querySelectorAll('[data-mission-confirm-close]');
    const userLevel = document.getElementById('user-level');
    const userXp = document.getElementById('user-xp');
    const userXpBar = document.getElementById('user-xp-bar');
    const emotionButtonBox = document.getElementById('emotion-button-box');
    const missionProgressCount = document.getElementById('mission-progress-count');
    const missionProgressBar = document.getElementById('mission-progress-bar');
    let missionsLoaded = false;
    let calendarEmotionDays = new Map();
    let pendingMissionButton = null;
    const selectedEmotionStorageKey = 'plia.selectedEmotion';
    const legacySelectedEmotionStorageKey = 'lastsys.selectedEmotion';

    const today = getKoreaToday();
    let visibleMonth = new Date(today.getFullYear(), today.getMonth(), 1);

    const getEmotionValue = (button) => button.dataset.emotion || button.textContent.trim();

    const setSelectedEmotion = (selectedButton) => {
        if (!emotionButtonBox || !selectedButton) {
            return;
        }

        emotionButtonBox.querySelectorAll('.emotion-choice').forEach((button) => {
            const isSelected = button === selectedButton;

            button.classList.toggle('is-selected', isSelected);
            button.setAttribute('aria-pressed', String(isSelected));
        });

        try {
            localStorage.setItem(selectedEmotionStorageKey, getEmotionValue(selectedButton));
        } catch {
        }
    };

    const restoreSelectedEmotion = () => {
        if (!emotionButtonBox) {
            return;
        }

        let selectedEmotion = '';

        try {
            selectedEmotion = localStorage.getItem(selectedEmotionStorageKey)
                || localStorage.getItem(legacySelectedEmotionStorageKey)
                || '';
        } catch {
            selectedEmotion = '';
        }

        if (!selectedEmotion) {
            return;
        }

        const selectedButton = Array.from(emotionButtonBox.querySelectorAll('.emotion-choice'))
            .find((button) => getEmotionValue(button) === selectedEmotion);

        if (selectedButton) {
            setSelectedEmotion(selectedButton);
        }
    };

    const initializeEmotionButtons = () => {
        restoreSelectedEmotion();
    };

    const renderCalendar = () => {
        if (!calendarElement) {
            return;
        }

        updateCalendarTitle(calendarTitle, visibleMonth);
        renderFallbackCalendar(calendarElement, visibleMonth, today, calendarEmotionDays);
    };

    previousButton?.addEventListener('click', () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1);
        renderCalendar();
        loadMonthlyCalendarEmotions();
    });

    nextButton?.addEventListener('click', () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1);
        renderCalendar();
        loadMonthlyCalendarEmotions();
    });

    emotionButtonBox?.addEventListener('click', (event) => {
        const emotionButton = event.target.closest('.emotion-choice');

        if (!emotionButton || !emotionButtonBox.contains(emotionButton)) {
            return;
        }

        setSelectedEmotion(emotionButton);

        const mapUrl = new URL('/map', window.location.origin);
        const emotionId = emotionButton.dataset.emotionId;

        if (emotionId) {
            mapUrl.searchParams.set('emotionId', emotionId);
        }

        mapUrl.searchParams.set('locate', 'current');

        window.location.assign(mapUrl.toString());
    });

    const setMissionPanelOpen = (isOpen) => {
        if (!missionPanel || !missionToggle) {
            return;
        }

        missionPanel.classList.toggle('is-open', isOpen);
        missionPanel.setAttribute('aria-hidden', String(!isOpen));
        missionToggle.classList.toggle('is-active', isOpen);
        missionToggle.setAttribute('aria-expanded', String(isOpen));
        missionToggle.setAttribute('aria-label', isOpen ? '오늘의 선행 미션 닫기' : '오늘의 선행 미션 열기');

        if (isOpen && !missionsLoaded) {
            loadTodayMissions();
        }
    };

    missionToggle?.addEventListener('click', () => {
        setMissionPanelOpen(!missionPanel?.classList.contains('is-open'));
    });

    missionClose?.addEventListener('click', () => {
        setMissionPanelOpen(false);
    });

    missionReroll?.addEventListener('click', async () => {
        if (!missionList || missionReroll.disabled) {
            return;
        }

        missionReroll.disabled = true;
        renderMissionMessage('오늘의 미션을 새로 뽑는 중입니다.');

        try {
            const response = await fetch('/api/daily-missions/reroll', {
                method: 'POST',
                headers: {
                    Accept: 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('mission reroll failed');
            }

            missionsLoaded = true;
            renderMissionPayload(await response.json());
        } catch {
            renderMissionMessage('미션을 다시 뽑지 못했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            missionReroll.disabled = false;
        }
    });

    const setMissionConfirmOpen = (isOpen, missionButton = null) => {
        if (!missionConfirm) {
            return;
        }

        pendingMissionButton = isOpen ? missionButton : null;
        missionConfirm.classList.toggle('is-open', isOpen);
        missionConfirm.setAttribute('aria-hidden', String(!isOpen));

        if (isOpen) {
            missionConfirmSubmit?.focus();
        }
    };

    missionConfirmCloseTriggers.forEach((trigger) => {
        trigger.addEventListener('click', () => {
            setMissionConfirmOpen(false);
        });
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && missionConfirm?.classList.contains('is-open')) {
            setMissionConfirmOpen(false);
        }
    });

    missionList?.addEventListener('click', (event) => {
        const rerollButton = event.target.closest('[data-mission-reroll-slot]');

        if (rerollButton && missionList.contains(rerollButton)) {
            event.preventDefault();
            event.stopPropagation();
            rerollMissionSlot(rerollButton);
            return;
        }

        const missionButton = event.target.closest('[data-mission-id]');

        if (!missionButton || missionButton.classList.contains('is-completed')) {
            return;
        }

        setMissionConfirmOpen(true, missionButton);
    });

    async function rerollMissionSlot(rerollButton) {
        if (rerollButton.dataset.rerollAvailable !== 'true') {
            return;
        }

        rerollButton.dataset.rerollAvailable = 'false';
        rerollButton.classList.add('is-waiting');

        try {
            const response = await fetch(`/api/daily-missions/slots/${rerollButton.dataset.missionRerollSlot}/reroll`, {
                method: 'POST',
                headers: {
                    Accept: 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('mission slot reroll failed');
            }

            missionsLoaded = true;
            renderMissionPayload(await response.json());
        } catch {
            rerollButton.dataset.rerollAvailable = 'true';
            rerollButton.classList.remove('is-waiting');
            renderMissionMessage('미션을 다시 뽑지 못했습니다. 잠시 후 다시 시도해 주세요.');
        }
    }

    missionConfirmSubmit?.addEventListener('click', async () => {
        const missionButton = pendingMissionButton;

        if (!missionButton || missionButton.classList.contains('is-completed')) {
            setMissionConfirmOpen(false);
            return;
        }

        missionButton.disabled = true;
        missionConfirmSubmit.disabled = true;

        try {
            const response = await fetch(`/api/daily-missions/${missionButton.dataset.missionId}/complete`, {
                method: 'POST',
                headers: {
                    Accept: 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('mission complete failed');
            }

            renderMissionPayload(await response.json());
            setMissionConfirmOpen(false);
        } catch {
            missionButton.disabled = false;
            renderMissionMessage('미션 완료 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            missionConfirmSubmit.disabled = false;
        }
    });

    async function loadTodayMissions() {
        if (!missionList && !missionProgressCount && !missionProgressBar) {
            return;
        }

        if (missionList) {
            renderMissionMessage('오늘의 미션을 불러오는 중입니다.');
        }

        try {
            const response = await fetch('/api/daily-missions', {
                headers: {
                    Accept: 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('mission load failed');
            }

            missionsLoaded = true;
            renderMissionPayload(await response.json());
        } catch {
            if (missionList) {
                renderMissionMessage('미션을 불러오지 못했습니다. 다시 열어 주세요.');
            }
        }
    }

    function renderMissionPayload(payload) {
        renderMissions(payload.missions || []);
        renderProgress(payload.progress);
        updateTodaySuccessCount(payload.todaySuccessCount);
    }

    function renderMissions(missions) {
        if (!missionList) {
            return;
        }

        missionList.innerHTML = missions.map((mission, index) => `
            <li>
                <div class="mission-item ${mission.completed ? 'is-completed' : ''}">
                    <button class="mission-complete" type="button" data-mission-id="${mission.id}" ${mission.completed ? 'disabled' : ''}>
                        <span>${index + 1}</span>
                        <p>${escapeHtml(mission.text)}</p>
                        <strong>${mission.completed ? '성공' : '도전'}</strong>
                    </button>
                    ${renderMissionRerollButton(mission, index)}
                </div>
            </li>
        `).join('');
    }

    function renderMissionRerollButton(mission, index) {
        const remainingRerolls = Number.isFinite(Number(mission.remainingRerolls))
            ? Math.max(0, Number(mission.remainingRerolls))
            : Math.max(0, 3 - Number(mission.rerollCount || 0));
        const rerollAvailable = typeof mission.rerollAvailable === 'boolean'
            ? mission.rerollAvailable
            : !mission.completed && remainingRerolls > 0;
        const className = [
            'mission-item-reroll',
            rerollAvailable ? '' : 'is-disabled'
        ].filter(Boolean).join(' ');

        return `
                    <button class="${className}" type="button" data-mission-reroll-slot="${mission.slotIndex}" data-reroll-available="${rerollAvailable}" data-remaining-rerolls="${remainingRerolls}" aria-disabled="${!rerollAvailable}" aria-label="${index + 1}번 미션 다시 뽑기, 남은 횟수 ${remainingRerolls}회">
                        <span aria-hidden="true">↻</span>
                        <small>${remainingRerolls}/3</small>
                    </button>
        `;
    }

    function renderMissionMessage(message) {
        if (!missionList) {
            return;
        }

        missionList.innerHTML = `<li class="mission-message">${escapeHtml(message)}</li>`;
    }

    function renderProgress(progress) {
        if (!progress || !userLevel || !userXp || !userXpBar) {
            return;
        }

        userLevel.textContent = `Lv. ${progress.level}`;
        userXp.textContent = `${progress.currentXp} / ${progress.requiredXp} XP`;
        userXpBar.style.width = `${Math.max(0, Math.min(100, progress.progressPercent))}%`;
    }

    async function loadMonthlyCalendarEmotions() {
        if (!calendarElement) {
            return;
        }

        try {
            const response = await fetch(`/api/daily-missions/calendar?year=${visibleMonth.getFullYear()}&month=${visibleMonth.getMonth() + 1}`, {
                headers: {
                    Accept: 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('calendar load failed');
            }

            calendarEmotionDays = new Map((await response.json()).map((day) => [day.date, day]));

            renderCalendar();
        } catch {
            calendarEmotionDays = new Map();
            renderCalendar();
        }
    }

    function updateTodaySuccessCount(todaySuccessCount) {
        if (typeof todaySuccessCount !== 'number') {
            return;
        }

        updateMissionActionProgress(todaySuccessCount);
    }

    function updateMissionActionProgress(successCount = 0, totalCount = 5) {
        if (!missionProgressCount && !missionProgressBar) {
            return;
        }

        const completed = Math.max(0, Math.min(totalCount, Number(successCount) || 0));
        const percent = totalCount > 0 ? (completed / totalCount) * 100 : 0;

        if (missionProgressCount) {
            missionProgressCount.textContent = `${completed}/${totalCount}`;
        }

        if (missionProgressBar) {
            missionProgressBar.style.width = `${percent}%`;
        }
    }

    initializeEmotionButtons();
    updateMissionActionProgress();
    renderCalendar();
    loadMonthlyCalendarEmotions();

    if (missionPage || missionProgressCount || missionProgressBar) {
        loadTodayMissions();
    }
});

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function updateCalendarTitle(calendarTitle, date) {
    if (!calendarTitle) {
        return;
    }

    calendarTitle.textContent = `${date.getFullYear()}. ${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function renderFallbackCalendar(calendarElement, visibleMonth, today, calendarEmotionDays) {
    const year = visibleMonth.getFullYear();
    const month = visibleMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const lastDate = new Date(year, month + 1, 0).getDate();
    const previousLastDate = new Date(year, month, 0).getDate();
    const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
    const cells = [];

    for (let index = firstDay - 1; index >= 0; index -= 1) {
        cells.push(`<span class="calendar-day muted" aria-hidden="true">${previousLastDate - index}</span>`);
    }

    for (let date = 1; date <= lastDate; date += 1) {
        const isToday = year === today.getFullYear() && month === today.getMonth() && date === today.getDate();
        const isoDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
        const representativeEmotion = calendarEmotionDays.get(isoDate);
        const emotionStyle = calendarEmotionStyleFor(representativeEmotion?.emotionLabel, representativeEmotion?.emotionColor);
        const emotionLabel = representativeEmotion?.emotionLabel || '';
        const className = [
            isToday ? 'today' : '',
            emotionStyle ? 'has-emotion-record' : ''
        ].filter(Boolean).join(' ');
        const tooltip = emotionLabel ? `대표 감정 ${emotionLabel}` : '감정 기록 없음';
        const style = emotionStyle
            ? ` style="--calendar-emotion-bg: ${emotionStyle.background}; --calendar-emotion-border: ${emotionStyle.border}; --calendar-emotion-text: ${emotionStyle.color};"`
            : '';
        cells.push(`
            <span
                class="calendar-day ${className}"
                data-tooltip="${escapeHtml(tooltip)}"
                aria-label="${isoDate} ${escapeHtml(tooltip)}"
                ${style}
            >${date}</span>
        `);
    }

    const remaining = (7 - (cells.length % 7)) % 7;

    for (let date = 1; date <= remaining; date += 1) {
        cells.push(`<span class="calendar-day muted" aria-hidden="true">${date}</span>`);
    }

    calendarElement.innerHTML = `
        <div class="fallback-calendar" aria-label="감정 캘린더">
            <div class="fallback-weekdays">
                ${weekdays.map((day) => `<span>${day}</span>`).join('')}
            </div>
            <div class="fallback-days">
                ${cells.join('')}
            </div>
        </div>
    `;

}

function calendarEmotionStyleFor(label, value) {
    const color = String(value || '').trim();
    const fallbackColor = /^#[0-9a-fA-F]{6}$/.test(color) ? color : '';

    if (window.PLIA_EMOTION && typeof window.PLIA_EMOTION.metaForLabel === 'function') {
        const emotion = window.PLIA_EMOTION.metaForLabel(label, fallbackColor);

        if (emotion && emotion.color) {
            return {
                background: emotion.background || emotion.color,
                border: emotion.border || emotion.color,
                color: emotion.color
            };
        }
    }

    if (!fallbackColor) {
        return null;
    }

    return {
        background: fallbackColor,
        border: fallbackColor,
        color: '#FFFFFF'
    };
}

function getKoreaToday() {
    const parts = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'Asia/Seoul',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).formatToParts(new Date());
    const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));

    return new Date(Number(values.year), Number(values.month) - 1, Number(values.day));
}
