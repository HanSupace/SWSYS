(function () {
    var form = document.getElementById('recommendation-form');
    if (!form) return;
    var input = document.getElementById('recommendation-query');
    var message = document.getElementById('recommendation-message');
    var results = document.getElementById('recommendation-results');

    function escapeHtml(value) {
        return String(value || '').replace(/[&<>"']/g, function (character) {
            return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character];
        });
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        var query = input.value.trim();
        if (!query) {
            message.textContent = '찾고 싶은 감정이나 상황을 입력해주세요.';
            return;
        }
        message.textContent = '비슷한 기록을 찾고 있어요…';
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
            var labels = (criteria.emotions || []).concat(criteria.includeTags || []);
            var excluded = criteria.excludeTags || [];
            message.textContent = labels.length ? '검색 기준: ' + labels.join(', ') : '요청에서 검색 기준을 찾지 못했어요.';
            if (excluded.length) message.textContent += ' · 제외: ' + excluded.join(', ');
            results.innerHTML = data.records && data.records.length
                ? '<div class="recommendation-results-heading">비슷한 감정 기록 ' + data.records.length + '개</div>' + data.records.map(function (record) {
                    return '<article class="recommendation-card"><div><span class="recommendation-emotion">' + escapeHtml(record.emotionLabel) + '</span><span class="recommendation-location">' + escapeHtml(record.locationName) + '</span></div><strong>' + escapeHtml(record.title) + '</strong><p>' + escapeHtml(record.description || '') + '</p><small>' + escapeHtml(record.authorNickname) + '</small></article>';
                }).join('')
                : '<p class="recommendation-empty">조건에 맞는 기록이 아직 없어요.</p>';
            results.hidden = false;
        } catch (error) {
            message.textContent = error.message || '추천을 불러오지 못했습니다.';
        }
    });
})();
