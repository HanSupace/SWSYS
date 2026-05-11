document.addEventListener('DOMContentLoaded', () => {
    const calendarElement = document.getElementById('emotion-calendar');
    const selectedDateLabel = document.getElementById('selected-date-label');
    const calendarTitle = document.getElementById('calendar-title');
    const previousButton = document.getElementById('calendar-prev');
    const nextButton = document.getElementById('calendar-next');

    if (!calendarElement) {
        return;
    }

    const today = new Date();
    let visibleMonth = new Date(today.getFullYear(), today.getMonth(), 1);

    const renderCalendar = () => {
        updateCalendarTitle(calendarTitle, visibleMonth);
        renderFallbackCalendar(calendarElement, selectedDateLabel, visibleMonth, today);
    };

    previousButton?.addEventListener('click', () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1);
        renderCalendar();
    });

    nextButton?.addEventListener('click', () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1);
        renderCalendar();
    });

    renderCalendar();
});

function updateCalendarTitle(calendarTitle, date) {
    if (!calendarTitle) {
        return;
    }

    calendarTitle.textContent = `${date.getFullYear()}. ${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function updateSelectedDate(selectedDateLabel, selectedDate) {
    if (!selectedDateLabel) {
        return;
    }

    selectedDateLabel.textContent = `${selectedDate} 감정을 선택했습니다.`;
}

function renderFallbackCalendar(calendarElement, selectedDateLabel, visibleMonth, today) {
    const year = visibleMonth.getFullYear();
    const month = visibleMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const lastDate = new Date(year, month + 1, 0).getDate();
    const previousLastDate = new Date(year, month, 0).getDate();
    const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
    const cells = [];

    for (let index = firstDay - 1; index >= 0; index -= 1) {
        cells.push(`<button class="muted" type="button">${previousLastDate - index}</button>`);
    }

    for (let date = 1; date <= lastDate; date += 1) {
        const isToday = year === today.getFullYear() && month === today.getMonth() && date === today.getDate();
        const className = isToday ? 'today' : '';
        const isoDate = `${year}-${String(month + 1).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
        cells.push(`<button class="${className}" type="button" data-date="${isoDate}">${date}</button>`);
    }

    const remaining = (7 - (cells.length % 7)) % 7;

    for (let date = 1; date <= remaining; date += 1) {
        cells.push(`<button class="muted" type="button">${date}</button>`);
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

    calendarElement.addEventListener('click', (event) => {
        const selectedDateButton = event.target.closest('[data-date]');

        if (!selectedDateButton) {
            return;
        }

        const previousSelected = calendarElement.querySelector('.is-emotion-selected');

        if (previousSelected) {
            previousSelected.classList.remove('is-emotion-selected');
        }

        selectedDateButton.classList.add('is-emotion-selected');
        updateSelectedDate(selectedDateLabel, selectedDateButton.dataset.date);
    });
}

function toIsoDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
