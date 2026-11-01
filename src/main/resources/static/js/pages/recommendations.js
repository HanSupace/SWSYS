(function () {
    var form = document.getElementById('recommendation-form');
    if (!form) return;
    var input = document.getElementById('recommendation-query');
    var message = document.getElementById('recommendation-message');
    var results = document.getElementById('recommendation-results');
    var tagLabels = {
        QUIET: '조용함', SECLUDED: '한적함', CALM: '차분함', SERENE: '고요함',
        REST: '휴식', HEALING: '힐링', COMFORT: '편안함', RELAXATION: '여유',
        WALK: '산책', STROLL: '산책', HIKING: '하이킹', NATURE: '자연',
        GREENERY: '녹음', PARK: '공원', FOREST: '숲', SOCIAL: '사람들과 어울림',
        FRIENDS: '친구', CONVERSATION: '대화', LIVELY: '활기', ACTIVE: '활동적',
        ENERGETIC: '에너지', LOUD: '시끄러움', NOISY: '소음', ROWDY: '소란',
        CROWDED: '붐빔', BUSY: '붐비는 곳', POPULAR: '인기', WATERFRONT: '물가',
        RIVER: '강', SEA: '바다', LAKE: '호수', INDOOR: '실내', CAFE: '카페',
        LIBRARY: '도서관', OUTDOOR: '야외', MOUNTAIN: '산', GARDEN: '정원'
    };

    function escapeHtml(value) {
        return String(value || '').replace(/[&<>"']/g, function (character) {
            return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character];
        });
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        var query = input.value.trim();
        if (!query) {
            message.textContent = '가고 싶은 분위기나 상황을 입력해주세요.';
            return;
        }
        message.textContent = '저장된 감정 기록에서 비슷한 분위기를 찾고 있어요…';
        results.hidden = true;
        try {
            var response = await fetch('/api/recommendations', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: query })
            });
            var data = await response.json();
            if (!response.ok) throw new Error(data.detail || data.message || '추천을 불러오지 못했습니다.');
            var criteria = data.criteria || {};
            var labels = (criteria.emotions || []).concat((criteria.includeTags || []).map(function (tag) { return tagLabels[tag] || tag; }));
            var excluded = (criteria.excludeTags || []).map(function (tag) { return tagLabels[tag] || tag; });
            message.textContent = labels.length ? '저장된 기록에서 검색 · 기준: ' + labels.join(', ') : '요청에서 검색 기준을 찾지 못했어요.';
            if (excluded.length) message.textContent += ' · 제외: ' + excluded.join(', ');
            results.innerHTML = data.records && data.records.length
                ? '<div class="recommendation-results-heading">비슷한 분위기의 저장 기록 ' + data.records.length + '개</div>' + data.records.map(function (record) {
                    return '<article class="recommendation-card"><div><span class="recommendation-emotion">' + escapeHtml(record.emotionLabel) + '</span><span class="recommendation-location">' + escapeHtml(record.locationName) + '</span></div><strong>' + escapeHtml(record.title) + '</strong><p>' + escapeHtml(record.description || '') + '</p><small>' + escapeHtml(record.authorNickname) + '</small></article>';
                }).join('')
                : '<p class="recommendation-empty">이 분위기에 맞는 기록이 저장된 장소를 아직 찾지 못했어요.</p>';
            results.hidden = false;
        } catch (error) {
            message.textContent = error.message || '추천을 불러오지 못했습니다.';
        }
    });
})();
