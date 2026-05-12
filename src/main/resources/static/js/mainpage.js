document.addEventListener('DOMContentLoaded', () => {
    const calendarElement = document.getElementById('emotion-calendar');
    const calendarTitle = document.getElementById('calendar-title');
    const previousButton = document.getElementById('calendar-prev');
    const nextButton = document.getElementById('calendar-next');
    const missionPanel = document.getElementById('mission-panel');
    const missionToggle = document.querySelector('.floating-toggle');
    const missionClose = document.querySelector('.mission-close');
    const missionList = document.getElementById('mission-list');
    const userLevel = document.getElementById('user-level');
    const userXp = document.getElementById('user-xp');
    const userXpBar = document.getElementById('user-xp-bar');
    let missionsLoaded = false;
    let missionSuccessCounts = new Map();

    const today = getKoreaToday();
    let visibleMonth = new Date(today.getFullYear(), today.getMonth(), 1);

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

    missionList?.addEventListener('click', async (event) => {
        const missionButton = event.target.closest('[data-mission-id]');

        if (!missionButton || missionButton.classList.contains('is-completed')) {
            return;
        }

        if (!confirm('미션 완료하셨습니까?')) {
            return;
        }

        missionButton.disabled = true;

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
        } catch (error) {
            missionButton.disabled = false;
            renderMissionMessage('미션 완료 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.');
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
