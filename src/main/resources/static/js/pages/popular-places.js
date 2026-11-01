(function () {
    var panel = document.querySelector('.popular-places-panel');
    if (!panel) return;

    var toggle = document.getElementById('popular-place-toggle');
    var current = document.getElementById('popular-place-current');
    var list = document.getElementById('popular-place-list');
    var places = [];
    var currentIndex = 0;
    var expanded = false;
    var lastPayload = '';
    var loading = false;

    function placeRow(place, index) {
        var rank = document.createElement('span');
        rank.className = 'popular-place-rank';
        rank.textContent = String(index + 1).padStart(2, '0');

        var name = document.createElement('span');
        name.className = 'popular-place-name';
        name.textContent = place.placeName;

        return [rank, name];
    }

    function placeLink(place, index) {
        var latitude = Number(place.latitude);
        var longitude = Number(place.longitude);
        var link = document.createElement('a');
        link.className = 'popular-place-link';
        if (place.latitude != null && place.longitude != null
            && Number.isFinite(latitude) && Number.isFinite(longitude)
            && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
            var url = new URL('/map', window.location.origin);
            url.searchParams.set('lat', String(latitude));
            url.searchParams.set('lng', String(longitude));
            link.href = url.pathname + url.search;
        }
        link.append.apply(link, placeRow(place, index));
        return link;
    }

    function renderCurrent() {
        current.replaceChildren();
        if (!places.length) {
            current.textContent = '순위 없음';
            return;
        }
        current.appendChild(placeLink(places[currentIndex], currentIndex));
    }

    function renderList() {
        list.replaceChildren();
        places.forEach(function (place, index) {
            var item = document.createElement('li');
            item.className = 'popular-place-item';
            item.appendChild(placeLink(place, index));
            list.appendChild(item);
        });
    }

    function render() {
        toggle.disabled = !places.length;
        renderCurrent();
        renderList();
    }

    toggle.addEventListener('click', function () {
        expanded = !expanded;
        panel.classList.toggle('is-expanded', expanded);
        toggle.setAttribute('aria-expanded', String(expanded));
        current.hidden = expanded;
        list.hidden = !expanded;
    });

    async function refresh() {
        if (document.hidden || loading) return;
        loading = true;
        try {
            var response = await fetch('/api/popular-places', { headers: { Accept: 'application/json' }, cache: 'no-store' });
            if (!response.ok) return;
            var incoming = (await response.json()).slice(0, 10);
            var payload = JSON.stringify(incoming);
            if (payload === lastPayload) return;
            var currentName = places[currentIndex] && places[currentIndex].placeName;
            places = incoming;
            lastPayload = payload;
            currentIndex = Math.max(0, places.findIndex(function (place) { return place.placeName === currentName; }));
            render();
        } catch (error) {
            if (!lastPayload) current.textContent = '순위를 불러올 수 없어요';
        } finally {
            loading = false;
        }
    }

    refresh();
    window.setInterval(function () {
        if (expanded || document.hidden || places.length < 2) return;
        currentIndex = (currentIndex + 1) % places.length;
        renderCurrent();
    }, 3000);
    window.setInterval(refresh, 5000);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) refresh();
    });
})();
