document.addEventListener('DOMContentLoaded', () => {
    let emotionChart = null;

    async function loadEmotionSummaryChart() {
        const canvas = document.getElementById('myEmotionChart');
        const emptyElement = document.getElementById('emotion-chart-empty');

        if (!canvas || !emptyElement) {
            return;
        }

        if (typeof Chart === 'undefined') {
            showChartEmpty(canvas, emptyElement, '그래프 라이브러리를 불러오지 못했습니다.', '네트워크 상태를 확인한 뒤 다시 시도해 주세요.');
            return;
        }

        const ctx = canvas.getContext('2d');

        try {
            const response = await fetch('/api/emotions/summary');

            if (!response.ok) {
                throw new Error('Failed to load emotion summary.');
            }

            const summary = await response.json();
            const labels = summary.map((item) => item.label);
            const data = summary.map((item) => item.count);

            if (data.length === 0) {
                showChartEmpty(canvas, emptyElement);
                return;
            }

            canvas.hidden = false;
            emptyElement.hidden = true;

            if (emotionChart) {
                emotionChart.destroy();
            }

            emotionChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels,
                    datasets: [{
                        label: '기록 수',
                        data,
                        backgroundColor: labels.map((label) => emotionColorFor(label)),
                        borderColor: labels.map((label) => emotionColorFor(label)),
                        borderWidth: 1,
                        borderRadius: 10,
                        borderSkipped: false,
                        barThickness: 18,
                        maxBarThickness: 22
                    }]
                },
                options: getChartOptions()
            });
        } catch (chartError) {
            console.error(chartError);
            showChartEmpty(canvas, emptyElement, '감정 요약을 불러오지 못했습니다.', '잠시 후 다시 확인해 주세요.');
        }
    }

    function getChartOptions() {
        const fontFamily = 'Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';

        return {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: {
                    top: 4,
                    right: 8,
                    bottom: 0,
                    left: 0
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0,
                        color: '#6F8278',
                        font: {
                            family: fontFamily,
                            size: 11,
                            weight: '700'
                        }
                    },
                    grid: {
                        color: 'rgba(26, 77, 57, 0.08)',
                        drawBorder: false
                    },
                    border: {
                        display: false
                    }
                },
                y: {
                    ticks: {
                        color: '#1C2622',
                        font: {
                            family: fontFamily,
                            size: 12,
                            weight: '800'
                        }
                    },
                    grid: {
                        display: false
                    },
                    border: {
                        display: false
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(26, 77, 57, 0.94)',
                    titleColor: '#FFFFFF',
                    bodyColor: '#FFFFFF',
                    borderColor: 'rgba(255, 255, 255, 0.18)',
                    borderWidth: 1,
                    cornerRadius: 12,
                    displayColors: false,
                    padding: 12,
                    titleFont: {
                        family: fontFamily,
                        size: 12,
                        weight: '900'
                    },
                    bodyFont: {
                        family: fontFamily,
                        size: 12,
                        weight: '700'
                    },
                    callbacks: {
                        label: (context) => `기록 ${context.parsed.x}회`
                    }
                }
            }
        };
    }

    function showChartEmpty(canvas, emptyElement, title = '아직 분석할 감정 기록이 없습니다.', message = '감정지도에 기록을 남기면 이곳에서 요약을 볼 수 있습니다.') {
        canvas.hidden = true;
        emptyElement.hidden = false;
        emptyElement.querySelector('strong').textContent = title;
        emptyElement.querySelector('span').textContent = message;
    }

    function emotionColorFor(label) {
        if (window.PLIA_EMOTION && typeof window.PLIA_EMOTION.colorFor === 'function') {
            return window.PLIA_EMOTION.colorFor(label, '#2F7650');
        }

        return '#2F7650';
    }

    function loadHealingSpots() {
        if (!navigator.geolocation) {
            renderHealingSpotMessage('GPS를 지원하지 않는 기기 또는 브라우저입니다.');
            return;
        }

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const params = new URLSearchParams({
                    lat: position.coords.latitude.toString(),
                    lng: position.coords.longitude.toString()
                });

                try {
                    const response = await fetch(`/api/spots/healing?${params.toString()}`);

                    if (!response.ok) {
                        throw new Error('Failed to load healing spots.');
                    }

                    renderHealingSpots(await response.json());
                } catch (fetchError) {
                    console.error(fetchError);
                    renderHealingSpotMessage('주변 힐링 스팟을 불러오지 못했습니다.');
                }
            },
            () => {
                renderHealingSpotMessage('위치 권한을 허용해 주세요.');
            }
        );
    }

    function renderHealingSpotMessage(message) {
        const listElement = document.getElementById('healing-spot-list');

        if (!listElement) {
            return;
        }

        listElement.innerHTML = `
            <li class="record-item record-empty">
                <div class="record-info">
                    <span class="record-date">${escapeHtml(message)}</span>
                </div>
            </li>
        `;
    }

    function renderHealingSpots(spots) {
        const listElement = document.getElementById('healing-spot-list');

        if (!listElement) {
            return;
        }

        listElement.innerHTML = '';

        if (spots.length === 0) {
            listElement.innerHTML = `
                <li class="record-item record-empty">
                    <div class="record-info">
                        <span class="record-emotion">주변에 아직 힐링 스팟이 없습니다.</span>
                        <span class="record-date">감정지도에 긍정적인 기록을 남기면 이곳에 표시됩니다.</span>
                    </div>
                </li>
            `;
            return;
        }

        spots.forEach((spot) => {
            const item = document.createElement('li');
            item.className = 'record-item';
            item.setAttribute('style', 'cursor: pointer;');
            item.tabIndex = 0;
            item.setAttribute('role', 'link');
            item.onclick = () => openSpotOnMap(spot);
            item.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openSpotOnMap(spot);
                }
            });
            item.innerHTML = `
                <div class="record-info">
                    <span class="record-date">현위치에서 ${escapeHtml(spot.distance)}</span>
                    <span class="record-emotion">
                        ${escapeHtml(spot.name)}
                    </span>
                </div>
            `;
            listElement.appendChild(item);
        });
    }

    function openSpotOnMap(spot) {
        window.location.href = '/map?lat=' + spot.lat + '&lng=' + spot.lng;
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    loadEmotionSummaryChart();
    loadHealingSpots();
});
