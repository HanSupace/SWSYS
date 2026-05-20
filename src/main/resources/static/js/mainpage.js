document.addEventListener('DOMContentLoaded', () => {
    const calendarElement = document.getElementById('emotion-calendar');
    const calendarTitle = document.getElementById('calendar-title');
    const previousButton = document.getElementById('calendar-prev');
    const nextButton = document.getElementById('calendar-next');
    const missionPanel = document.getElementById('mission-panel');
    const missionToggle = document.querySelector('.floating-toggle');
    const missionClose = document.querySelector('.mission-close');
    const missionList = document.getElementById('mission-list');
    const missionConfirm = document.getElementById('mission-confirm');
    const missionConfirmSubmit = document.getElementById('mission-confirm-submit');
    const missionConfirmCloseTriggers = document.querySelectorAll('[data-mission-confirm-close]');
    const userLevel = document.getElementById('user-level');
    const userXp = document.getElementById('user-xp');
    const userXpBar = document.getElementById('user-xp-bar');
    const emotionButtonBox = document.getElementById('emotion-button-box');
    const emotionAddButton = document.getElementById('emotion-add-button');
    const emotionEditButton = document.getElementById('emotion-edit-button');
    let missionsLoaded = false;
    let missionSuccessCounts = new Map();
    let pendingMissionButton = null;
    let emotionEditMode = false;
    const customEmotionStorageKey = 'lastsys.customEmotions';
    const emotionLabelStorageKey = 'lastsys.emotionLabels';
    const selectedEmotionStorageKey = 'lastsys.selectedEmotion';

    const today = getKoreaToday();
    let visibleMonth = new Date(today.getFullYear(), today.getMonth(), 1);

    const getEmotionValue = (button) => button.dataset.emotion || button.textContent.trim();

    const loadCustomEmotions = () => {
        try {
            const emotions = JSON.parse(localStorage.getItem(customEmotionStorageKey) || '[]');

            if (!Array.isArray(emotions)) {
                return [];
            }

            const uniqueEmotions = new Set();

            return emotions
                .map((emotion) => String(emotion).trim())
                .filter((emotion) => {
                    if (!emotion || uniqueEmotions.has(emotion)) {
                        return false;
                    }

                    uniqueEmotions.add(emotion);
                    return true;
                });
        } catch (error) {
            return [];
        }
    };

    const saveCustomEmotions = (emotions) => {
        try {
            localStorage.setItem(customEmotionStorageKey, JSON.stringify(emotions));
        } catch (error) {
            // 저장 공간을 사용할 수 없는 환경에서는 화면에만 추가합니다.
        }
    };

    const loadEmotionLabels = () => {
        try {
            const labels = JSON.parse(localStorage.getItem(emotionLabelStorageKey) || '{}');

            if (!labels || Array.isArray(labels) || typeof labels !== 'object') {
                return {};
            }

            return labels;
        } catch (error) {
            return {};
        }
    };

    const saveEmotionLabels = (labels) => {
        try {
            localStorage.setItem(emotionLabelStorageKey, JSON.stringify(labels));
        } catch (error) {
            // 저장 공간을 사용할 수 없는 환경에서는 화면에만 반영합니다.
        }
    };

    const createEmotionButton = (emotion) => {
        const button = document.createElement('button');

        button.className = 'emotion-choice';
        button.type = 'button';
        button.dataset.customEmotion = 'true';
        button.dataset.emotion = emotion;
        button.setAttribute('aria-pressed', 'false');
        button.textContent = emotion;

        return button;
    };

    const updateEmotionButtonLabel = (button, emotion) => {
        button.dataset.emotion = emotion;
        button.textContent = emotion;
    };

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
        } catch (error) {
            // 선택 저장이 막혀도 버튼 선택 상태는 유지합니다.
        }
    };

    const restoreSelectedEmotion = () => {
        if (!emotionButtonBox) {
            return;
        }

        let selectedEmotion = '';

        try {
            selectedEmotion = localStorage.getItem(selectedEmotionStorageKey) || '';
        } catch (error) {
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

    const applyEmotionLabels = () => {
        if (!emotionButtonBox) {
            return;
        }

        const labels = loadEmotionLabels();

        emotionButtonBox.querySelectorAll('.emotion-choice[data-emotion-id]').forEach((button) => {
            const emotionId = button.dataset.emotionId;
            const emotion = String(labels[emotionId] || '').trim().slice(0, 12);

            if (emotion) {
                updateEmotionButtonLabel(button, emotion);
            }
        });
    };

    const findEmotionButtonByValue = (emotion, exceptButton = null) => {
        if (!emotionButtonBox) {
            return null;
        }

        return Array.from(emotionButtonBox.querySelectorAll('.emotion-choice'))
            .find((button) => button !== exceptButton && getEmotionValue(button) === emotion) || null;
    };

    const setEmotionEditMode = (isEditing) => {
        emotionEditMode = isEditing;
        emotionButtonBox?.classList.toggle('is-editing', isEditing);
        emotionEditButton?.classList.toggle('is-active', isEditing);
        emotionEditButton?.setAttribute('aria-pressed', String(isEditing));
        emotionEditButton?.setAttribute('aria-label', isEditing ? '감정 편집 완료' : '감정 편집');

        if (emotionEditButton) {
            emotionEditButton.textContent = isEditing ? '완료' : '편집';
        }
    };

    const editEmotionButton = (button) => {
        const currentEmotion = getEmotionValue(button);
        const emotion = window.prompt('수정할 감정을 입력해 주세요.', currentEmotion);
        const normalizedEmotion = String(emotion || '').trim().slice(0, 12);

        if (!normalizedEmotion || normalizedEmotion === currentEmotion) {
            return;
        }

        if (findEmotionButtonByValue(normalizedEmotion, button)) {
            window.alert('이미 있는 감정입니다.');
            return;
        }

        const wasSelected = button.classList.contains('is-selected');

        updateEmotionButtonLabel(button, normalizedEmotion);

        if (button.dataset.customEmotion === 'true') {
            const customEmotions = loadCustomEmotions();
            const customEmotionIndex = customEmotions.indexOf(currentEmotion);

            if (customEmotionIndex >= 0) {
                customEmotions[customEmotionIndex] = normalizedEmotion;
            } else {
                customEmotions.push(normalizedEmotion);
            }

            saveCustomEmotions(customEmotions);
        } else if (button.dataset.emotionId) {
            const labels = loadEmotionLabels();

            labels[button.dataset.emotionId] = normalizedEmotion;
            saveEmotionLabels(labels);
        }

        if (wasSelected) {
            setSelectedEmotion(button);
        }
    };

    const renderCustomEmotions = () => {
        if (!emotionButtonBox || !emotionAddButton) {
            return;
        }

        loadCustomEmotions().forEach((emotion) => {
            emotionButtonBox.insertBefore(createEmotionButton(emotion), emotionAddButton);
        });

        applyEmotionLabels();
        restoreSelectedEmotion();
    };

    const renderCalendar = () => {
        if (!calendarElement) {
            return;
        }

        updateCalendarTitle(calendarTitle, visibleMonth);
        renderFallbackCalendar(calendarElement, visibleMonth, today, missionSuccessCounts);
    };

    previousButton?.addEventListener('click', () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1);
        renderCalendar();
        loadMonthlySuccessCounts();
    });

    nextButton?.addEventListener('click', () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1);
        renderCalendar();
        loadMonthlySuccessCounts();
    });

    emotionEditButton?.addEventListener('click', () => {
        setEmotionEditMode(!emotionEditMode);
    });

    emotionButtonBox?.addEventListener('click', (event) => {
        const emotionButton = event.target.closest('.emotion-choice');

        if (!emotionButton || !emotionButtonBox.contains(emotionButton)) {
            return;
        }

        if (emotionEditMode) {
            editEmotionButton(emotionButton);
            return;
        }

        setSelectedEmotion(emotionButton);
    });

    emotionAddButton?.addEventListener('click', () => {
        if (!emotionButtonBox) {
            return;
        }

        const emotion = window.prompt('추가할 감정을 입력해 주세요.');
        const normalizedEmotion = String(emotion || '').trim().slice(0, 12);

        if (!normalizedEmotion) {
            return;
        }

        const existingButton = Array.from(emotionButtonBox.querySelectorAll('.emotion-choice'))
            .find((button) => getEmotionValue(button) === normalizedEmotion);

        if (existingButton) {
            setSelectedEmotion(existingButton);
            return;
        }

        const customEmotions = loadCustomEmotions();
        const nextCustomEmotions = [...customEmotions, normalizedEmotion];
        const emotionButton = createEmotionButton(normalizedEmotion);

        saveCustomEmotions(nextCustomEmotions);
        emotionButtonBox.insertBefore(emotionButton, emotionAddButton);
        setSelectedEmotion(emotionButton);
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
        const missionButton = event.target.closest('[data-mission-id]');

        if (!missionButton || missionButton.classList.contains('is-completed')) {
            return;
        }

        setMissionConfirmOpen(true, missionButton);
    });

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
        } catch (error) {
            missionButton.disabled = false;
            renderMissionMessage('미션 완료 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            missionConfirmSubmit.disabled = false;
        }
    });

    async function loadTodayMissions() {
        if (!missionList) {
            return;
        }

        renderMissionMessage('오늘의 미션을 불러오는 중입니다.');

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
        } catch (error) {
            renderMissionMessage('미션을 불러오지 못했습니다. 다시 열어 주세요.');
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
                <button class="${mission.completed ? 'is-completed' : ''}" type="button" data-mission-id="${mission.id}" ${mission.completed ? 'disabled' : ''}>
                    <span>${index + 1}</span>
                    <p>${escapeHtml(mission.text)}</p>
                    <strong>${mission.completed ? '성공' : '도전'}</strong>
                </button>
            </li>
        `).join('');
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

    async function loadMonthlySuccessCounts() {
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

            missionSuccessCounts = new Map((await response.json()).map((day) => [day.date, day.successCount]));
            renderCalendar();
        } catch (error) {
            missionSuccessCounts = new Map();
            renderCalendar();
        }
    }

    function updateTodaySuccessCount(todaySuccessCount) {
        if (typeof todaySuccessCount !== 'number') {
            return;
        }

        const todayKey = toIsoDate(today);
        missionSuccessCounts.set(todayKey, todaySuccessCount);

        if (visibleMonth.getFullYear() === today.getFullYear() && visibleMonth.getMonth() === today.getMonth()) {
            renderCalendar();
        }
    }

    renderCustomEmotions();
    renderCalendar();
    loadMonthlySuccessCounts();
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

function renderFallbackCalendar(calendarElement, visibleMonth, today, missionSuccessCounts) {
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
        const successCount = missionSuccessCounts.get(isoDate) || 0;
        const className = [
            isToday ? 'today' : '',
            getSuccessClassName(successCount)
        ].filter(Boolean).join(' ');
        const tooltip = `완료 미션 ${successCount}개`;
        cells.push(`
            <span
                class="calendar-day ${className}"
                data-tooltip="${tooltip}"
                aria-label="${isoDate} ${tooltip}"
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

function toIsoDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function getSuccessClassName(successCount) {
    if (successCount >= 5) {
        return 'mission-success-3';
    }

    if (successCount >= 3) {
        return 'mission-success-2';
    }

    if (successCount >= 1) {
        return 'mission-success-1';
    }

    return '';
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
