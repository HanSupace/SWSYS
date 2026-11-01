(function () {
    var form = document.getElementById('recommendation-form');
    if (!form) return;

    var input = document.getElementById('recommendation-query');
    var message = document.getElementById('recommendation-message');
    var results = document.getElementById('recommendation-results');
    var requestId = 0;

    function escapeHtml(value) {
        return String(value || '').replace(/[&<>"']/g, function (character) {
            return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character];
        });
    }

    function mapUrl(record) {
        if (record.latitude == null || record.longitude == null) return '';
        var latitude = Number(record.latitude);
        var longitude = Number(record.longitude);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)
            || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return '';
        var url = new URL('/map', window.location.origin);
        url.searchParams.set('lat', String(latitude));
        url.searchParams.set('lng', String(longitude));
        return url.pathname + url.search;
    }

    function showMessage(text) {
        message.textContent = text;
        message.hidden = !text;
    }

    form.addEventListener('submit', async function (event) {
        event.preventDefault();
        var query = input.value.trim();
        if (!query) {
            showMessage('찾고 싶은 곳을 입력해 주세요.');
            return;
        }

        var currentRequest = ++requestId;
        showMessage('찾는 중…');
        results.hidden = true;
        try {
            var response = await fetch('/api/recommendations', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: query })
            });
            var data = await response.json();
            if (currentRequest !== requestId) return;
            if (!response.ok) throw new Error(data.detail || data.message || '다시 시도해 주세요.');

            showMessage('');
            var seen = new Set();
            var cards = (data.records || []).filter(function (record) {
                var key = String(record.locationName || '').trim();
                if (!key || seen.has(key) || !mapUrl(record)) return false;
                seen.add(key);
                return true;
            }).map(function (record) {
                return '<a class="recommendation-card" href="' + escapeHtml(mapUrl(record)) + '" aria-label="' + escapeHtml(record.locationName) + ' 지도에서 보기">' +
                    '<span class="recommendation-card-copy"><strong>' + escapeHtml(record.locationName) + '</strong><small>' + escapeHtml(record.title) + '</small></span>' +
                    '<span class="recommendation-card-arrow" aria-hidden="true">↗</span></a>';
            });

            results.innerHTML = cards.length ? cards.join('') : '<p class="recommendation-empty">추천할 장소가 없어요.</p>';
            results.hidden = false;
        } catch (error) {
            if (currentRequest === requestId) showMessage(error.message || '다시 시도해 주세요.');
        }
    });
})();
