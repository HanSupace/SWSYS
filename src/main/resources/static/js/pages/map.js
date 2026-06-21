(function () {
        var container = document.getElementById('map');
        var message = document.getElementById('map-message');
        var searchForm = document.getElementById('map-search-form');
        var searchInput = document.getElementById('map-search-input');
        var searchResults = document.getElementById('map-search-results');
        var mapViewButtons = document.querySelectorAll('[data-map-view]');
        var feedPanel = document.getElementById('map-feed-panel');
        var feedSummary = document.getElementById('map-feed-summary');
        var feedList = document.getElementById('map-feed-list');
        var mapCrosshair = document.querySelector('.map-crosshair');
        var markerModal = document.getElementById('marker-modal');
        var openMarkerModalButton = document.getElementById('open-marker-modal');
        var moveCurrentLocationButton = document.getElementById('move-current-location');
        var closeMarkerModalButton = document.getElementById('close-marker-modal');
        var markerModalBackdrop = document.getElementById('marker-modal-backdrop');
        var filterToggleButton = document.getElementById('map-filter-toggle');
        var filterPanel = document.getElementById('map-filter-panel');
        var filterBackdrop = document.getElementById('map-filter-backdrop');
        var filterSummary = document.getElementById('map-filter-summary');
        var emotionFilters = document.getElementById('map-emotion-filters');
        var filterAllCount = document.getElementById('map-filter-all-count');
        var filterMineCount = document.getElementById('map-filter-mine-count');
        var emotionToggleMark = document.getElementById('map-emotion-toggle-mark');
        var clusterPanel = document.getElementById('map-cluster-panel');
        var clusterPanelTitle = document.getElementById('map-cluster-panel-title');
        var clusterPanelSummary = document.getElementById('map-cluster-panel-summary');
        var clusterRecords = document.getElementById('map-cluster-records');
        var clusterPanelCloseButton = document.getElementById('map-cluster-panel-close');
        var recordPanel = document.getElementById('map-record-panel');
        var recordPanelTitle = document.getElementById('map-record-title');
        var recordPanelMeta = document.getElementById('map-record-meta');
        var recordPanelCloseButton = document.getElementById('map-record-panel-close');
        var recordContent = document.getElementById('map-record-content');
        var recordCommentsList = document.getElementById('map-record-comments-list');
        var recordCommentForm = document.getElementById('map-record-comment-form');
        var recordCommentInput = document.getElementById('map-record-comment-input');
        var recordDeleteModal = document.getElementById('record-delete-modal');
        var recordDeleteBackdrop = document.getElementById('record-delete-backdrop');
        var recordDeleteCancelButton = document.getElementById('record-delete-cancel');
        var recordDeleteConfirmButton = document.getElementById('record-delete-confirm');
        var mapViewStorageKey = 'plia.mapViewMode';
        var currentViewMode = loadMapViewMode();
        var isInitialFeedView = currentViewMode === 'feed';
        document.body.classList.toggle('is-feed-view', isInitialFeedView);
        container.classList.toggle('is-feed-hidden', isInitialFeedView);
        container.setAttribute('aria-hidden', String(isInitialFeedView));
        feedPanel.hidden = !isInitialFeedView;
        searchForm.hidden = false;
        openMarkerModalButton.hidden = isInitialFeedView;
        moveCurrentLocationButton.hidden = isInitialFeedView;
        if (mapCrosshair) {
            mapCrosshair.hidden = isInitialFeedView;
        }

        if (!window.kakao || !window.kakao.maps) {
            message.textContent = '지도를 불러오지 못했습니다. Kakao JavaScript 키와 도메인 설정을 확인해주세요.';
            message.classList.add('is-error');
            return;
        }

        var form = document.getElementById('emotion-marker-form');
        var markerLocationInput = document.getElementById('marker-location');
        var markerLocationSelectedInput = document.getElementById('marker-location-selected');
        var markerLocationResults = document.getElementById('marker-location-results');
        var markerFormMessage = document.getElementById('marker-form-message');
        var titleInput = document.getElementById('marker-title');
        var descriptionInput = document.getElementById('marker-description');
        var defaultPosition = new kakao.maps.LatLng(37.566826, 126.9786567);
        var urlParams = new URLSearchParams(window.location.search);
        var requestedLatParam = urlParams.get('lat');
        var requestedLngParam = urlParams.get('lng');
        var requestedLat = Number(requestedLatParam);
        var requestedLng = Number(requestedLngParam);
        var hasRequestedPosition = requestedLatParam !== null
            && requestedLngParam !== null
            && requestedLatParam.trim() !== ''
            && requestedLngParam.trim() !== ''
            && Number.isFinite(requestedLat)
            && Number.isFinite(requestedLng)
            && requestedLat >= -90
            && requestedLat <= 90
            && requestedLng >= -180
            && requestedLng <= 180;
        var requestedPosition = hasRequestedPosition ? new kakao.maps.LatLng(requestedLat, requestedLng) : null;
        var currentPosition = requestedPosition || defaultPosition;
        var selectedPosition = currentPosition;
        var searchedPlaces = [];
        var markerPlaces = [];
        var selectedMarkerLocationPosition = null;
        var emotionMarkerItems = [];
        var activeEmotionFilter = 'ALL';
        var showOnlyMine = false;
        var activeInfoOverlay = null;
        var activeMarkerItem = null;
        var emotionFiltersOpen = false;
        var filterPanelCloseTimer = null;
        var searchTimer = null;
        var markerLocationTimer = null;
        var markerLayerRefreshFrame = null;
        var markerVisibilityFrame = null;
        var emotionFilterRenderFrame = null;
        var clusteredMarkerSet = new Set();
        var clusterOverlays = [];
        var selectedClusterItems = [];
        var activeRecordDetail = null;
        var activeRecordComments = [];
        var recordDetailLoading = false;
        var pendingDeleteRecordId = null;
        var requestedEmotionId = urlParams.get('emotionId') || '';
        var shouldLocateOnEntry = urlParams.get('locate') === 'current';
        var currentLocationRequest = null;
        var markerVisualSize = 34;
        var searchPolicy = window.PliaMapSearchPolicy;
        var SEARCH_MODES = searchPolicy.MODES;
        var markerOverlayZIndex = 6;
        var selectedMarkerOverlayZIndex = 24;
        var clusterOverlayZIndex = 12;
        var infoOverlayZIndex = 48;
        var options = {
            center: currentPosition,
            level: 5,
            draggable: true
        };
        var emotionFilterOptions = Array.isArray(window.PLIA_EMOTIONS) ? window.PLIA_EMOTIONS : [];

        var map = new kakao.maps.Map(container, options);
        map.setDraggable(true);
        var places = kakao.maps.services ? new kakao.maps.services.Places() : null;
        var geocoder = kakao.maps.services ? new kakao.maps.services.Geocoder() : null;
        var markerLocationDefaultPlaceholder = markerLocationInput.getAttribute('placeholder') || '';
        var markerLocationRequestId = 0;
        var isResolvingMarkerLocation = false;
        var markerLocationEditedDuringResolve = false;
        message.hidden = true;

        function openMarkerModal() {
            selectedPosition = map.getCenter();
            markerModal.hidden = false;
            document.body.classList.add('is-marker-modal-open');
            fillMarkerLocationFromMapCenter();
            window.setTimeout(function () {
                markerLocationInput.focus();
            }, 120);
        }

        function selectMarkerEmotion(emotionId) {
            var emotionOptions = form.querySelectorAll('.emotion-option');

            for (var index = 0; index < emotionOptions.length; index += 1) {
                if (emotionOptions[index].dataset.emotionId !== emotionId) {
                    continue;
                }

                var emotionInput = emotionOptions[index].querySelector('input[name="emotion"]');

                if (!emotionInput) {
                    return false;
                }

                emotionInput.checked = true;
                return true;
            }

            return false;
        }

        function clearRequestedEmotionIntent() {
            if (!requestedEmotionId || !window.history || !window.history.replaceState) {
                requestedEmotionId = '';
                return;
            }

            try {
                var nextUrl = new URL(window.location.href);
                nextUrl.searchParams.delete('emotionId');
                nextUrl.searchParams.delete('locate');
                window.history.replaceState(window.history.state, '', nextUrl.pathname + nextUrl.search + nextUrl.hash);
            } catch {
            }

            requestedEmotionId = '';
        }

        function closeMarkerModal() {
            markerLocationRequestId += 1;
            isResolvingMarkerLocation = false;
            markerLocationEditedDuringResolve = false;
            markerLocationInput.placeholder = markerLocationDefaultPlaceholder;
            markerModal.hidden = true;
            document.body.classList.remove('is-marker-modal-open');
            openMarkerModalButton.focus();
        }

        function moveTo(position, level) {
            selectedPosition = position;
            map.setLevel(level);
            map.setCenter(position);
        }

        function getCurrentKakaoPosition() {
            return new Promise(function (resolve, reject) {
                if (!navigator.geolocation) {
                    reject(new Error('geolocation-unsupported'));
                    return;
                }

                navigator.geolocation.getCurrentPosition(
                    function (position) {
                        resolve(new kakao.maps.LatLng(
                            position.coords.latitude,
                            position.coords.longitude
                        ));
                    },
                    function (error) {
                        reject(error || new Error('geolocation-failed'));
                    },
                    {
                        enableHighAccuracy: true,
                        timeout: 8000,
                        maximumAge: 0
                    }
                );
            });
        }

        function moveToCurrentLocation() {
            if (currentLocationRequest) {
                return currentLocationRequest;
            }

            message.textContent = '현재 위치를 확인하는 중입니다.';
            message.classList.remove('is-error');
            message.hidden = false;

            var locationSucceeded = false;
            currentLocationRequest = getCurrentKakaoPosition()
                .then(function (userPosition) {
                    locationSucceeded = true;
                    moveTo(userPosition, 4);
                })
                .catch(function (error) {
                    selectedPosition = map.getCenter();

                    if (error && error.code === 1) {
                        showError('위치 권한이 거부되어 현재 지도 위치를 사용합니다. 브라우저에서 위치 권한을 허용할 수 있습니다.');
                    } else if (error && error.code === 3) {
                        showError('현재 위치 확인 시간이 초과되어 현재 지도 위치를 사용합니다.');
                    } else {
                        showError('현재 위치를 가져오지 못해 현재 지도 위치를 사용합니다.');
                    }
                })
                .finally(function () {
                    currentLocationRequest = null;

                    if (locationSucceeded) {
                        message.hidden = true;
                    }
                });

            return currentLocationRequest;
        }

        function formatPosition(position) {
            return position.getLat().toFixed(6) + ', ' + position.getLng().toFixed(6);
        }

        function trimmedLocationName(value) {
            var maxLength = Number(markerLocationInput.getAttribute('maxlength')) || 80;
            return String(value || '').trim().slice(0, maxLength);
        }

        function addressNameFromResult(results) {
            var firstResult = results && results[0];

            if (!firstResult) {
                return '';
            }

            if (firstResult.road_address && firstResult.road_address.address_name) {
                return firstResult.road_address.address_name;
            }

            if (firstResult.address && firstResult.address.address_name) {
                return firstResult.address.address_name;
            }

            return '';
        }

        function addressFromPosition(position) {
            return new Promise(function (resolve, reject) {
                if (!geocoder) {
                    reject(new Error('geocoder-unavailable'));
                    return;
                }

                geocoder.coord2Address(position.getLng(), position.getLat(), function (results, status) {
                    var addressName = addressNameFromResult(results);

                    if (status === kakao.maps.services.Status.OK && addressName) {
                        resolve(addressName);
                        return;
                    }

                    reject(new Error('address-not-found'));
                });
            });
        }

        function setMarkerFormMessage(text, variant) {
            markerFormMessage.textContent = text || '';
            markerFormMessage.hidden = !text;
            markerFormMessage.classList.toggle('is-info', variant === 'info');
        }

        function showMarkerFormInfo(text) {
            setMarkerFormMessage(text, 'info');
        }

        function failMarkerLocationAutofill() {
            if (markerLocationEditedDuringResolve) {
                hideMarkerFormMessage();
                return;
            }

            markerLocationInput.value = '';
            markerLocationSelectedInput.value = '';
            selectedMarkerLocationPosition = null;
            showMarkerFormInfo('선택한 지도 위치의 주소를 찾지 못했습니다. 위치를 직접 입력해주세요.');
        }

        function fillMarkerLocationFromMapCenter() {
            var requestId = markerLocationRequestId + 1;
            var centerPosition = map.getCenter();
            markerLocationRequestId = requestId;
            selectedPosition = centerPosition;
            isResolvingMarkerLocation = true;
            markerLocationEditedDuringResolve = false;
            markerPlaces = [];
            markerLocationResults.hidden = true;
            markerLocationInput.value = '';
            markerLocationSelectedInput.value = '';
            selectedMarkerLocationPosition = null;
            markerLocationInput.placeholder = '선택 위치의 주소를 확인하는 중...';
            showMarkerFormInfo('지도 중앙의 선택 위치를 확인하는 중...');

            addressFromPosition(centerPosition)
                .then(function (locationName) {
                    return {
                        position: centerPosition,
                        locationName: locationName
                    };
                })
                .then(function (result) {
                    if (!result || requestId !== markerLocationRequestId) {
                        return;
                    }

                    isResolvingMarkerLocation = false;
                    markerLocationInput.placeholder = markerLocationDefaultPlaceholder;

                    if (markerLocationEditedDuringResolve) {
                        hideMarkerFormMessage();
                        return;
                    }

                    var locationName = trimmedLocationName(result.locationName);

                    if (!locationName) {
                        failMarkerLocationAutofill();
                        return;
                    }

                    markerLocationInput.value = locationName;
                    markerLocationSelectedInput.value = locationName;
                    selectedMarkerLocationPosition = result.position;
                    hideMarkerFormMessage();
                })
                .catch(function () {
                    if (requestId !== markerLocationRequestId) {
                        return;
                    }

                    isResolvingMarkerLocation = false;
                    markerLocationInput.placeholder = markerLocationDefaultPlaceholder;
                    failMarkerLocationAutofill();
                });
        }

        function placeSearchOptions(mode) {
            var baseLocation = mode === SEARCH_MODES.RECORD ? selectedPosition : map.getCenter();
            return {
                location: baseLocation
            };
        }

        function markerPlaceSearchOptions() {
            return placeSearchOptions(SEARCH_MODES.RECORD);
        }

        function formatDistance(distance) {
            var meters = Number(distance);

            if (!Number.isFinite(meters)) {
                return '';
            }

            if (meters >= 1000) {
                return (meters / 1000).toFixed(meters >= 10000 ? 0 : 1) + 'km';
            }

            return Math.round(meters) + 'm';
        }

        function placeAddressText(place) {
            var address = place.road_address_name || place.address_name || '주소 정보 없음';
            var distance = formatDistance(place.distance);
            return distance ? distance + ' · ' + address : address;
        }

        function normalizeSearchText(value) {
            return String(value || '').trim().toLowerCase();
        }

        function relevanceTokens(value) {
            return normalizeSearchText(value)
                .split(/[\s,./|·:;!?()[\]{}"'`~_-]+/)
                .filter(function (token) {
                    return token.length >= 2;
                });
        }

        function selectedEmotionLabel() {
            var checkedEmotion = form.querySelector('input[name="emotion"]:checked');
            return checkedEmotion ? checkedEmotion.dataset.label : '';
        }

        function emotionMetaForLabel(label, fallbackColor) {
            if (window.PLIA_EMOTION && typeof window.PLIA_EMOTION.metaForLabel === 'function') {
                var emotion = window.PLIA_EMOTION.metaForLabel(label, fallbackColor || '');
                return normalizeEmotionMeta(emotion.label || label, emotion.color || fallbackColor, emotion.icon || '');
            }

            return normalizeEmotionMeta(label, fallbackColor, '');
        }

        function normalizeEmotionMeta(label, fallbackColor, fallbackIcon) {
            var normalizedLabel = String(label || '').trim();
            var normalizedKey = normalizedLabel.toLowerCase();
            var emotionColors = {
                '\uae30\uc068': '#765A08',
                happy: '#765A08',
                happiness: '#765A08',
                '\uae30\ub300': '#4F711F',
                anticipation: '#4F711F',
                '\uc2ac\ud514': '#315C86',
                sadness: '#315C86',
                sad: '#315C86',
                '\ubd84\ub178': '#9D312B',
                anger: '#9D312B',
                angry: '#9D312B',
                '\ubd88\uc548': '#5C477D',
                anxious: '#5C477D',
                anxiety: '#5C477D',
                '\ub2f9\ud669': '#8B3D66',
                embarrassment: '#8B3D66',
                '\ub180\ub78c': '#28746F',
                surprised: '#28746F',
                surprise: '#28746F',
                '\uc9dc\uc99d': '#925021',
                irritation: '#925021'
            };
            var emotionIcons = {
                '\uae30\uc068': '😊',
                happy: '😊',
                happiness: '😊',
                '\uae30\ub300': '🙂',
                anticipation: '🙂',
                '\uc2ac\ud514': '😢',
                sadness: '😢',
                sad: '😢',
                '\ubd84\ub178': '😠',
                anger: '😠',
                angry: '😠',
                '\ubd88\uc548': '😟',
                anxious: '😟',
                anxiety: '😟',
                '\ub2f9\ud669': '😳',
                embarrassment: '😳',
                '\ub180\ub78c': '😮',
                surprised: '😮',
                surprise: '😮',
                '\uc9dc\uc99d': '😣',
                irritation: '😣'
            };

            return {
                label: normalizedLabel,
                color: emotionColors[normalizedLabel] || emotionColors[normalizedKey] || fallbackColor || '#2F7650',
                icon: emotionIcons[normalizedLabel] || emotionIcons[normalizedKey] || fallbackIcon || '🙂'
            };
        }

        function normalizeRecordDetail(recordDetail) {
            var emotion = emotionMetaForLabel(recordDetail.emotionLabel, recordDetail.emotionColor);
            recordDetail.emotionLabel = emotion.label || recordDetail.emotionLabel;
            recordDetail.emotionColor = emotion.color || recordDetail.emotionColor;
            recordDetail.emotionIcon = emotion.icon || recordDetail.emotionIcon || '🙂';
            return recordDetail;
        }

        function placeRelevanceScore(place, keyword, contextValue) {
            var normalizedKeyword = normalizeSearchText(keyword);
            var placeName = normalizeSearchText(place.place_name);
            var categoryName = normalizeSearchText(place.category_name);
            var address = normalizeSearchText((place.road_address_name || '') + ' ' + (place.address_name || ''));
            var searchContext = relevanceTokens([keyword, contextValue].join(' '));
            var score = 0;

            if (normalizedKeyword && placeName === normalizedKeyword) {
                score += 120;
            }

            if (normalizedKeyword && placeName.startsWith(normalizedKeyword)) {
                score += 80;
            }

            if (normalizedKeyword && placeName.includes(normalizedKeyword)) {
                score += 60;
            }

            searchContext.forEach(function (token) {
                if (placeName.includes(token)) {
                    score += 30;
                }

                if (categoryName.includes(token)) {
                    score += 18;
                }

                if (address.includes(token)) {
                    score += 8;
                }
            });

            return score;
        }

        function markerLocationRelevanceScore(place, keyword) {
            return placeRelevanceScore(place, keyword, [
                titleInput.value,
                descriptionInput.value,
                selectedEmotionLabel()
            ].join(' '));
        }

        function comparePlaceRelevance(firstEntry, secondEntry) {
            if (firstEntry.relevanceScore !== secondEntry.relevanceScore) {
                return secondEntry.relevanceScore - firstEntry.relevanceScore;
            }

            var firstDistance = Number(firstEntry.place.distance);
            var secondDistance = Number(secondEntry.place.distance);
            var hasFirstDistance = Number.isFinite(firstDistance);
            var hasSecondDistance = Number.isFinite(secondDistance);

            if (hasFirstDistance && hasSecondDistance && firstDistance !== secondDistance) {
                return firstDistance - secondDistance;
            }

            if (hasFirstDistance !== hasSecondDistance) {
                return hasFirstDistance ? -1 : 1;
            }

            return firstEntry.index - secondEntry.index;
        }

        function sortMarkerLocationResultsByRelevance(results, keyword) {
            return results.map(function (place, index) {
                return {
                    place: place,
                    relevanceScore: markerLocationRelevanceScore(place, keyword) + Math.max(0, results.length - index),
                    index: index
                };
            }).sort(comparePlaceRelevance).map(function (entry) {
                return entry.place;
            });
        }

        function sortMapSearchResultsByRelevance(results, keyword) {
            return results.map(function (place, index) {
                return {
                    place: place,
                    relevanceScore: placeRelevanceScore(place, keyword, '') + Math.max(0, results.length - index),
                    index: index
                };
            }).sort(comparePlaceRelevance).map(function (entry) {
                return entry.place;
            });
        }

        function canSelectSearchResult(mode, position) {
            return searchPolicy.canSelect(mode, selectedPosition, position);
        }

        function showMarkerDistanceError() {
            setMarkerFormMessage('핀 위치에서 250m 이내의 위치만 선택할 수 있습니다.');
        }

        function hideMarkerFormMessage() {
            setMarkerFormMessage('');
        }

        function chooseMapPlace(index) {
            var place = searchedPlaces[index];

            if (!place) {
                return;
            }

            var searchedPosition = new kakao.maps.LatLng(place.y, place.x);

            map.setLevel(4);
            map.setCenter(searchedPosition);
            searchInput.value = place.place_name;
            searchInput.blur();
            searchResults.hidden = true;
            showNotice((index + 1) + '번째 검색 위치를 선택했습니다.');
        }

        function showError(text) {
            message.textContent = text;
            message.classList.add('is-error');
            message.hidden = false;
        }

        function showNotice(text) {
            message.textContent = text;
            message.classList.remove('is-error');
            message.hidden = false;

            window.setTimeout(function () {
                message.hidden = true;
            }, 1800);
        }

        function loadMapViewMode() {
            try {
                var storedViewMode = localStorage.getItem(mapViewStorageKey);
                return storedViewMode === 'feed' ? 'feed' : 'map';
            } catch {
                return 'map';
            }
        }

        function persistMapViewMode(viewMode) {
            try {
                localStorage.setItem(mapViewStorageKey, viewMode);
            } catch {
            }
        }

        function setMapViewMode(viewMode, shouldPersist) {
            currentViewMode = viewMode === 'feed' ? 'feed' : 'map';

            if (shouldPersist !== false) {
                persistMapViewMode(currentViewMode);
            }

            mapViewButtons.forEach(function (button) {
                var isActive = button.dataset.mapView === currentViewMode;
                button.classList.toggle('is-active', isActive);
                button.setAttribute('aria-pressed', String(isActive));
            });

            var isFeedView = currentViewMode === 'feed';
            document.body.classList.toggle('is-feed-view', isFeedView);
            container.classList.toggle('is-feed-hidden', isFeedView);
            container.setAttribute('aria-hidden', String(isFeedView));
            feedPanel.hidden = !isFeedView;
            searchForm.hidden = false;
            searchResults.hidden = true;
            if (mapCrosshair) {
                mapCrosshair.hidden = isFeedView;
            }
            openMarkerModalButton.hidden = isFeedView;
            moveCurrentLocationButton.hidden = isFeedView;

            if (isFeedView) {
                closeClusterPanel();
                if (activeMarkerItem) {
                    closeInfoOverlay(activeMarkerItem);
                }
                removeClusterOverlays();
                emotionMarkerItems.forEach(function (item) {
                    item.markerOverlay.setMap(null);
                });
                renderFeedList();
                updateFilterMenu(visibleMarkerItems().length);
                return;
            }

            feedPanel.hidden = true;
            kakao.maps.event.trigger(map, 'resize');
            map.setCenter(selectedPosition);
            refreshMarkerLayers();
        }

        function renderFeedList() {
            if (!feedList || !feedSummary) {
                return;
            }

            var keyword = normalizeSearchText(searchInput ? searchInput.value : '');
            var items = visibleMarkerItems().filter(function (item) {
                return !keyword || normalizeSearchText(item.locationName).includes(keyword);
            });
            var summaryText = activeEmotionFilter === 'ALL' ? '전체 기록' : activeEmotionFilter + ' 기록';
            feedSummary.textContent = summaryText + ' ' + items.length + '개';

            if (!items.length) {
                feedList.innerHTML = '<p class="map-feed-empty">조건에 맞는 감정 기록이 없습니다.</p>';
                return;
            }

            feedList.innerHTML = items.map(function (item) {
                var summary = summarizeText(item.description || item.title || '');
                return '<article class="map-feed-item" tabindex="0" role="button" data-record-id="' + escapeHtml(item.id) + '" style="--feed-color: ' + escapeHtml(item.emotionColor) + '">' +
                    '<div class="map-feed-item-top">' +
                    '<span>' + escapeHtml(item.emotionLabel) + '</span>' +
                    '<time>' + escapeHtml(formatMarkerDate(item.createdAt)) + '</time>' +
                    '</div>' +
                    '<h3>' + escapeHtml(item.title) + '</h3>' +
                    '<p>' + escapeHtml(summary) + '</p>' +
                    '<div class="map-feed-meta">' +
                    '<strong>' + escapeHtml(item.authorNickname || '사용자') + '</strong>' +
                    '</div>' +
                    '<div class="map-feed-bottom">' +
                    '<small class="map-feed-location">' + escapeHtml(item.locationName || formatPosition(item.position)) + '</small>' +
                    (item.own ? '<button class="map-feed-delete" type="button" data-feed-delete-id="' + escapeHtml(item.id) + '" aria-label="기록 삭제">삭제</button>' : '') +
                    '</div>' +
                    '</article>';
            }).join('');
        }

        function summarizeText(value) {
            var text = String(value || '').replace(/\s+/g, ' ').trim();

            if (text.length <= 90) {
                return text;
            }

            return text.slice(0, 90) + '...';
        }

        function setFilterPanelOpen(isOpen) {
            if (!filterToggleButton || !filterPanel) {
                return;
            }

            window.clearTimeout(filterPanelCloseTimer);

            if (isOpen) {
                emotionFiltersOpen = false;
                renderEmotionFilters();
                filterPanel.hidden = false;
                if (filterBackdrop) {
                    filterBackdrop.hidden = false;
                }
                window.requestAnimationFrame(function () {
                    filterPanel.classList.add('is-open');
                    if (filterBackdrop) {
                        filterBackdrop.classList.add('is-open');
                    }
                });
            } else {
                filterPanel.classList.remove('is-open');
                if (filterBackdrop) {
                    filterBackdrop.classList.remove('is-open');
                }
                filterPanelCloseTimer = window.setTimeout(function () {
                    filterPanel.hidden = true;
                    if (filterBackdrop) {
                        filterBackdrop.hidden = true;
                    }
                }, 220);
            }

            filterToggleButton.setAttribute('aria-expanded', String(isOpen));
        }

        function markerMatchesFilter(item) {
            if (showOnlyMine && !item.own) {
                return false;
            }

            return activeEmotionFilter === 'ALL' || item.emotionLabel === activeEmotionFilter;
        }

        function visibleMarkerItems() {
            return emotionMarkerItems.filter(markerMatchesFilter);
        }

        function hideInfoOverlay(item) {
            if (!item || activeInfoOverlay !== item.infoOverlay) {
                return;
            }

            item.infoOverlay.setMap(null);
            item.markerOverlay.setZIndex(markerOverlayZIndex);
            activeInfoOverlay = null;
            activeMarkerItem = null;
        }

        function openInfoOverlay(markerItem) {
            if (activeInfoOverlay && activeInfoOverlay !== markerItem.infoOverlay) {
                activeInfoOverlay.setMap(null);
            }

            if (activeMarkerItem && activeMarkerItem !== markerItem) {
                activeMarkerItem.markerOverlay.setZIndex(markerOverlayZIndex);
            }

            markerItem.markerOverlay.setZIndex(selectedMarkerOverlayZIndex);
            markerItem.infoOverlay.setZIndex(infoOverlayZIndex);
            markerItem.infoOverlay.setMap(map);
            activeInfoOverlay = markerItem.infoOverlay;
            activeMarkerItem = markerItem;
            scheduleCustomMarkerVisibilitySync();
        }

        function closeInfoOverlay(markerItem) {
            markerItem.infoOverlay.setMap(null);
            markerItem.markerOverlay.setZIndex(markerOverlayZIndex);

            if (activeInfoOverlay === markerItem.infoOverlay) {
                activeInfoOverlay = null;
            }

            if (activeMarkerItem === markerItem) {
                activeMarkerItem = null;
            }

            scheduleCustomMarkerVisibilitySync();
        }

        function closeClusterPanel() {
            selectedClusterItems = [];

            if (clusterPanel) {
                clusterPanel.hidden = true;
            }
        }

        function closeRecordPanel() {
            activeRecordDetail = null;
            activeRecordComments = [];

            if (recordPanel) {
                recordPanel.hidden = true;
            }
        }

        function openDeleteConfirmModal() {
            if (!activeRecordDetail || !activeRecordDetail.own) {
                return;
            }

            openDeleteConfirmForRecord(activeRecordDetail.id);
        }

        function openDeleteConfirmForRecord(recordId) {
            var markerItem = findMarkerItem(recordId);

            if (!markerItem || !markerItem.own) {
                return;
            }

            pendingDeleteRecordId = markerItem.id;
            recordDeleteModal.hidden = false;
            document.body.classList.add('is-record-delete-modal-open');
            window.setTimeout(function () {
                recordDeleteConfirmButton.focus();
            }, 80);
        }

        function closeDeleteConfirmModal() {
            pendingDeleteRecordId = null;
            recordDeleteModal.hidden = true;
            document.body.classList.remove('is-record-delete-modal-open');
        }

        function findMarkerItem(recordId) {
            var normalizedId = String(recordId);
            return emotionMarkerItems.find(function (item) {
                return String(item.id) === normalizedId;
            });
        }

        function markerHiddenByCluster(item) {
            return clusteredMarkerSet.has(item);
        }

        function syncCustomMarkerVisibility() {
            var visibleCount = 0;

            emotionMarkerItems.forEach(function (item) {
                var matchesFilter = markerMatchesFilter(item);
                var shouldShow = currentViewMode === 'map'
                    && matchesFilter
                    && (!markerHiddenByCluster(item) || item === activeMarkerItem);

                item.markerOverlay.setMap(shouldShow ? map : null);

                if (!shouldShow) {
                    hideInfoOverlay(item);
                }

                if (matchesFilter) {
                    visibleCount += 1;
                }
            });

            updateFilterMenu(visibleCount);
            renderFeedList();
        }

        function removeClusterOverlays() {
            clusterOverlays.forEach(function (overlay) {
                overlay.setMap(null);
            });
            clusterOverlays = [];
            clusteredMarkerSet = new Set();
        }

        function isPointInViewport(point) {
            var width = container.clientWidth;
            var height = container.clientHeight;
            return point.x >= 0 && point.y >= 0 && point.x <= width && point.y <= height;
        }

        function markerScreenPoint(item) {
            var projection = map.getProjection();
            var point = projection.containerPointFromCoords(item.position);

            if (!point) {
                return null;
            }

            return {
                x: point.x,
                y: point.y
            };
        }

        function measureMarkerVisualSize() {
            var marker = document.querySelector('.emotion-map-marker');

            if (!marker) {
                return markerVisualSize;
            }

            var rect = marker.getBoundingClientRect();
            if (rect.width > 0) {
                markerVisualSize = rect.width;
            }

            return markerVisualSize;
        }

        function markersOverlapByHalf(first, second, threshold) {
            return Math.abs(first.screenPoint.x - second.screenPoint.x) <= threshold
                && Math.abs(first.screenPoint.y - second.screenPoint.y) <= threshold;
        }

        function buildViewportClusters(items) {
            var markerSize = measureMarkerVisualSize();
            var overlapThreshold = markerSize * 0.5;
            var candidates = items.filter(function (item) {
                return item !== activeMarkerItem;
            }).map(function (item) {
                return {
                    item: item,
                    screenPoint: markerScreenPoint(item)
                };
            }).filter(function (entry) {
                return entry.screenPoint;
            }).filter(function (entry) {
                return isPointInViewport(entry.screenPoint);
            });
            var visited = new Set();
            var clusters = [];

            candidates.forEach(function (entry) {
                if (visited.has(entry.item)) {
                    return;
                }

                var queue = [entry];
                var cluster = [];
                visited.add(entry.item);

                while (queue.length) {
                    var current = queue.shift();
                    cluster.push(current.item);

                    candidates.forEach(function (candidate) {
                        if (visited.has(candidate.item)) {
                            return;
                        }

                        if (!markersOverlapByHalf(current, candidate, overlapThreshold)) {
                            return;
                        }

                        visited.add(candidate.item);
                        queue.push(candidate);
                    });
                }

                if (cluster.length > 1) {
                    clusters.push(cluster);
                }
            });

            return clusters;
        }

        function dominantEmotion(clusterItems) {
            var counts = {};
            var firstByEmotion = {};
            var firstEmotion = emotionMetaForLabel(clusterItems[0].emotionLabel, clusterItems[0].emotionColor);
            var bestEmotion = {
                label: firstEmotion.label || clusterItems[0].emotionLabel,
                color: firstEmotion.color || clusterItems[0].emotionColor
            };

            clusterItems.forEach(function (item, index) {
                counts[item.emotionLabel] = (counts[item.emotionLabel] || 0) + 1;
                if (firstByEmotion[item.emotionLabel] === undefined) {
                    firstByEmotion[item.emotionLabel] = index;
                }

                if (
                    counts[item.emotionLabel] > (counts[bestEmotion.label] || 0)
                    || counts[item.emotionLabel] === counts[bestEmotion.label]
                    && firstByEmotion[item.emotionLabel] < firstByEmotion[bestEmotion.label]
                ) {
                    var emotion = emotionMetaForLabel(item.emotionLabel, item.emotionColor);
                    bestEmotion = {
                        label: emotion.label || item.emotionLabel,
                        color: emotion.color || item.emotionColor
                    };
                }
            });

            return bestEmotion;
        }

        function clusterCenter(clusterItems) {
            var sumLat = 0;
            var sumLng = 0;

            clusterItems.forEach(function (item) {
                sumLat += item.position.getLat();
                sumLng += item.position.getLng();
            });

            return new kakao.maps.LatLng(sumLat / clusterItems.length, sumLng / clusterItems.length);
        }

        function formatMarkerDate(value) {
            if (!value) {
                return '';
            }

            var date = new Date(value);
            if (Number.isNaN(date.getTime())) {
                return String(value).replace('T', ' ').slice(0, 16);
            }

            return date.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            });
        }

        function renderRecordComments() {
            if (!activeRecordComments.length) {
                recordCommentsList.innerHTML = '<p class="map-record-empty">아직 댓글이 없습니다.</p>';
                return;
            }

            recordCommentsList.innerHTML = activeRecordComments.map(function (comment) {
                return '<article class="map-record-comment">' +
                    '<div><strong>' + escapeHtml(comment.authorNickname || '사용자') + '</strong><span>' + escapeHtml(formatMarkerDate(comment.createdAt)) + '</span></div>' +
                    '<p>' + escapeHtml(comment.content) + '</p>' +
                    '</article>';
            }).join('');
        }

        function renderRecordDetail() {
            if (!activeRecordDetail) {
                return;
            }

            recordPanelTitle.textContent = activeRecordDetail.title;
            recordPanelMeta.textContent = (activeRecordDetail.authorNickname || '사용자') + ' · ' + formatMarkerDate(activeRecordDetail.createdAt);
            recordContent.innerHTML =
                '<div class="map-record-emotion" style="--record-color: ' + escapeHtml(activeRecordDetail.emotionColor) + '">' +
                '<span>' + escapeHtml(activeRecordDetail.emotionLabel) + '</span>' +
                '<small>' + escapeHtml(activeRecordDetail.locationName) + '</small>' +
                '</div>' +
                '<p class="map-record-body">' + escapeHtml(activeRecordDetail.content || '') + '</p>' +
                '<div class="map-record-actions">' +
                '<button id="map-record-like" type="button" class="' + (activeRecordDetail.likedByMe ? 'is-liked' : '') + '">' +
                (activeRecordDetail.likedByMe ? '좋아요 취소' : '좋아요') +
                '</button>' +
                '<span>좋아요 <b id="map-record-like-count">' + activeRecordDetail.likeCount + '</b></span>' +
                '<span>댓글 <b id="map-record-comment-count">' + activeRecordDetail.commentCount + '</b></span>' +
                (activeRecordDetail.own ? '<button id="map-record-delete" class="is-danger" type="button">삭제</button>' : '') +
                '</div>';

            var likeButton = document.getElementById('map-record-like');
            var deleteButton = document.getElementById('map-record-delete');
            likeButton?.addEventListener('click', toggleRecordLike);
            deleteButton?.addEventListener('click', openDeleteConfirmModal);
            renderRecordComments();
        }

        async function loadRecordComments(recordId) {
            var response = await fetch('/api/emotion-map-markers/' + encodeURIComponent(recordId) + '/comments');

            if (!response.ok) {
                throw new Error(String(response.status));
            }

            activeRecordComments = await response.json();
            renderRecordComments();
        }

        async function openRecordDetail(recordId) {
            if (!recordId || recordDetailLoading) {
                return;
            }

            recordDetailLoading = true;
            recordPanel.hidden = false;
            recordPanelTitle.textContent = '불러오는 중';
            recordPanelMeta.textContent = '기록 상세';
            recordContent.innerHTML = '<p class="map-record-empty">기록을 불러오는 중입니다.</p>';
            recordCommentsList.innerHTML = '';

            if (activeMarkerItem) {
                closeInfoOverlay(activeMarkerItem);
            }

            try {
                var response = await fetch('/api/emotion-map-markers/' + encodeURIComponent(recordId));

                if (!response.ok) {
                    throw new Error(String(response.status));
                }

                activeRecordDetail = normalizeRecordDetail(await response.json());
                activeRecordComments = [];
                renderRecordDetail();
                await loadRecordComments(recordId);
            } catch {
                showError('기록 상세를 불러오지 못했습니다.');
                closeRecordPanel();
            } finally {
                recordDetailLoading = false;
            }
        }

        async function toggleRecordLike() {
            if (!activeRecordDetail) {
                return;
            }

            var previousLiked = activeRecordDetail.likedByMe;
            var previousCount = activeRecordDetail.likeCount;
            activeRecordDetail.likedByMe = !previousLiked;
            activeRecordDetail.likeCount = Math.max(0, previousCount + (activeRecordDetail.likedByMe ? 1 : -1));
            renderRecordDetail();

            try {
                var response = await fetch('/api/emotion-map-markers/' + encodeURIComponent(activeRecordDetail.id) + '/likes', {
                    method: 'POST'
                });

                if (!response.ok) {
                    throw new Error(String(response.status));
                }

                var result = await response.json();
                activeRecordDetail.likedByMe = result.likedByMe;
                activeRecordDetail.likeCount = result.likeCount;
                renderRecordDetail();
            } catch {
                activeRecordDetail.likedByMe = previousLiked;
                activeRecordDetail.likeCount = previousCount;
                renderRecordDetail();
                showError('좋아요를 반영하지 못했습니다.');
            }
        }

        async function deleteActiveRecord() {
            var markerItem = findMarkerItem(pendingDeleteRecordId);

            if (!pendingDeleteRecordId || !markerItem || !markerItem.own) {
                return;
            }

            var recordId = pendingDeleteRecordId;

            try {
                await deleteEmotionMarker(recordId);
                closeDeleteConfirmModal();
                showNotice('기록을 삭제했습니다.');
            } catch (error) {
                if (error.message === '401') {
                    showError('로그인 후 기록을 삭제할 수 있습니다.');
                } else {
                    showError('기록을 삭제하지 못했습니다.');
                }
            }
        }

        function openClusterPanel(clusterItems) {
            selectedClusterItems = clusterItems.slice();

            if (activeMarkerItem) {
                closeInfoOverlay(activeMarkerItem);
            }

            var emotion = dominantEmotion(clusterItems);
            clusterPanelTitle.textContent = '기록 ' + clusterItems.length + '개';
            clusterPanelSummary.textContent = emotion.label + ' 중심 클러스터';
            clusterRecords.innerHTML = clusterItems.map(function (item) {
                return '<button class="map-cluster-record" type="button" data-record-id="' + escapeHtml(item.id) + '" style="--record-color: ' + escapeHtml(item.emotionColor) + '">' +
                    '<div><strong>' + escapeHtml(item.title) + '</strong><span>' + escapeHtml(formatMarkerDate(item.createdAt)) + '</span></div>' +
                    '<em>' + escapeHtml(item.emotionLabel) + '</em>' +
                    '<small>' + escapeHtml(item.locationName || formatPosition(item.position)) + '</small>' +
                    '</button>';
            }).join('');
            clusterPanel.hidden = false;
        }

        function createClusterOverlay(clusterItems) {
            var emotion = dominantEmotion(clusterItems);
            var clusterElement = document.createElement('button');
            clusterElement.type = 'button';
            clusterElement.className = 'emotion-map-cluster';
            clusterElement.style.setProperty('--cluster-color', emotion.color);
            clusterElement.setAttribute('aria-label', emotion.label + ' 중심 기록 ' + clusterItems.length + '개 보기');
            clusterElement.textContent = '+' + clusterItems.length;

            clusterElement.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                openClusterPanel(clusterItems);
            });

            return new kakao.maps.CustomOverlay({
                position: clusterCenter(clusterItems),
                content: clusterElement,
                yAnchor: 0.5,
                xAnchor: 0.5,
                zIndex: clusterOverlayZIndex
            });
        }

        function renderViewportClusters() {
            removeClusterOverlays();

            if (currentViewMode !== 'map') {
                return;
            }

            var clusters = buildViewportClusters(visibleMarkerItems());
            clusters.forEach(function (clusterItems) {
                clusterItems.forEach(function (item) {
                    clusteredMarkerSet.add(item);
                    hideInfoOverlay(item);
                });

                var overlay = createClusterOverlay(clusterItems);
                overlay.setMap(map);
                clusterOverlays.push(overlay);
            });

            if (selectedClusterItems.length && !selectedClusterItems.some(function (item) {
                return clusteredMarkerSet.has(item);
            })) {
                closeClusterPanel();
            }
        }

        function renderViewportClustersSafely() {
            try {
                renderViewportClusters();
            } catch {
                removeClusterOverlays();
            }
        }

        function scheduleCustomMarkerVisibilitySync() {
            if (markerVisibilityFrame) {
                return;
            }

            markerVisibilityFrame = window.requestAnimationFrame(function () {
                markerVisibilityFrame = null;
                if (currentViewMode !== 'map') {
                    syncCustomMarkerVisibility();
                    return;
                }
                renderViewportClustersSafely();
                syncCustomMarkerVisibility();
            });
        }

        function refreshMarkerLayers() {
            var visibleItems = visibleMarkerItems();

            removeClusterOverlays();

            emotionMarkerItems.forEach(function (item) {
                item.markerOverlay.setMap(null);

                if (!markerMatchesFilter(item)) {
                    hideInfoOverlay(item);
                }
            });

            updateFilterMenu(visibleItems.length);
            renderFeedList();

            if (currentViewMode !== 'map') {
                return;
            }

            renderViewportClustersSafely();
            syncCustomMarkerVisibility();
        }

        function applyMarkerFilters() {
            if (markerLayerRefreshFrame) {
                return;
            }

            markerLayerRefreshFrame = window.requestAnimationFrame(function () {
                markerLayerRefreshFrame = null;
                refreshMarkerLayers();
            });
        }

        function updateFilterMenu(visibleCount) {
            if (filterSummary) {
                var summaryText = activeEmotionFilter === 'ALL' ? '전체' : activeEmotionFilter;
                filterSummary.textContent = summaryText + ' ' + visibleCount + '개';
            }

            if (filterAllCount) {
                filterAllCount.textContent = emotionMarkerItems.length;
            }

            if (filterMineCount) {
                filterMineCount.textContent = emotionMarkerItems.filter(function (item) {
                    return item.own;
                }).length;
            }

            document.querySelectorAll('.map-filter-chip').forEach(function (chip) {
                var filterType = chip.dataset.filterType;
                var emotionLabel = chip.dataset.emotionLabel;
                var isActive = filterType === 'all' && !showOnlyMine && activeEmotionFilter === 'ALL'
                    || filterType === 'mine' && showOnlyMine
                    || filterType === 'emotions' && emotionFiltersOpen
                    || emotionLabel && emotionLabel === activeEmotionFilter;

                chip.classList.toggle('is-active', Boolean(isActive));
            });
        }

        function scheduleEmotionFiltersRender() {
            if (emotionFilterRenderFrame) {
                return;
            }

            emotionFilterRenderFrame = window.requestAnimationFrame(function () {
                emotionFilterRenderFrame = null;
                renderEmotionFilters();
            });
        }

        function renderEmotionFilters() {
            if (!emotionFilters) {
                return;
            }

            filterPanel?.classList.toggle('is-emotion-expanded', emotionFiltersOpen);
            if (emotionToggleMark) {
                emotionToggleMark.textContent = emotionFiltersOpen ? '-' : '+';
            }

            if (!emotionFiltersOpen) {
                emotionFilters.hidden = true;
                emotionFilters.innerHTML = '';
                updateFilterMenu(emotionMarkerItems.filter(markerMatchesFilter).length);
                return;
            }

            var counts = emotionMarkerItems.reduce(function (result, item) {
                result[item.emotionLabel] = (result[item.emotionLabel] || 0) + 1;
                return result;
            }, {});

            emotionFilters.hidden = false;
            emotionFilters.innerHTML = emotionFilterOptions.map(function (emotion) {
                var count = counts[emotion.label] || 0;
                return '<button class="map-filter-chip map-filter-emotion-chip" type="button" data-emotion-label="' + escapeHtml(emotion.label) + '" style="--chip-color: ' + emotion.color + '">' +
                    '<span>' + escapeHtml(emotion.label) + '</span>' +
                    '<em>' + count + '</em>' +
                    '</button>';
            }).join('');

            updateFilterMenu(emotionMarkerItems.filter(markerMatchesFilter).length);
        }

        filterToggleButton?.addEventListener('click', function () {
            setFilterPanelOpen(filterPanel.hidden);
        });

        filterBackdrop?.addEventListener('click', function () {
            setFilterPanelOpen(false);
        });

        filterPanel?.addEventListener('click', function (event) {
            var filterButton = event.target.closest('.map-filter-chip');

            if (!filterButton || !filterPanel.contains(filterButton)) {
                return;
            }

            if (filterButton.dataset.filterType === 'all') {
                activeEmotionFilter = 'ALL';
                showOnlyMine = false;
            } else if (filterButton.dataset.filterType === 'mine') {
                activeEmotionFilter = 'ALL';
                showOnlyMine = true;
            } else if (filterButton.dataset.filterType === 'emotions') {
                emotionFiltersOpen = !emotionFiltersOpen;
                renderEmotionFilters();
                return;
            } else if (filterButton.dataset.emotionLabel) {
                activeEmotionFilter = filterButton.dataset.emotionLabel;
                showOnlyMine = false;
            }

            applyMarkerFilters();
            setFilterPanelOpen(false);
        });

        mapViewButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                setMapViewMode(button.dataset.mapView);
            });
        });

        feedList?.addEventListener('click', function (event) {
            var deleteButton = event.target.closest('[data-feed-delete-id]');

            if (deleteButton && feedList.contains(deleteButton)) {
                event.preventDefault();
                event.stopPropagation();
                openDeleteConfirmForRecord(deleteButton.dataset.feedDeleteId);
                return;
            }

            var feedItem = event.target.closest('.map-feed-item');

            if (!feedItem || !feedList.contains(feedItem)) {
                return;
            }

            openRecordDetail(feedItem.dataset.recordId);
        });

        feedList?.addEventListener('keydown', function (event) {
            if (event.key !== 'Enter' && event.key !== ' ') {
                return;
            }

            var feedItem = event.target.closest('.map-feed-item');

            if (!feedItem || !feedList.contains(feedItem) || event.target.closest('[data-feed-delete-id]')) {
                return;
            }

            event.preventDefault();
            openRecordDetail(feedItem.dataset.recordId);
        });

        clusterRecords?.addEventListener('click', function (event) {
            var recordButton = event.target.closest('.map-cluster-record');

            if (!recordButton || !clusterRecords.contains(recordButton)) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            openRecordDetail(recordButton.dataset.recordId);
        });

        var initialLocationRequest = Promise.resolve();

        if (hasRequestedPosition) {
            map.setCenter(requestedPosition);
            selectedPosition = requestedPosition;
        } else if (shouldLocateOnEntry || !requestedEmotionId) {
            initialLocationRequest = moveToCurrentLocation();
        }

        kakao.maps.event.addListener(map, 'center_changed', function () {
            selectedPosition = map.getCenter();
        });

        kakao.maps.event.addListener(map, 'zoom_changed', function () {
            window.setTimeout(scheduleCustomMarkerVisibilitySync, 0);
        });

        kakao.maps.event.addListener(map, 'idle', function () {
            scheduleCustomMarkerVisibilitySync();
        });

        kakao.maps.event.addListener(map, 'click', function (mouseEvent) {
            map.panTo(mouseEvent.latLng);
            searchResults.hidden = true;
            closeClusterPanel();
            closeRecordPanel();
        });

        function renderSearchResults(results, keyword) {
            searchResults.innerHTML = '';
            searchedPlaces = sortMapSearchResultsByRelevance(results, keyword).slice(0, 5);

            searchedPlaces.forEach(function (place, index) {
                var resultButton = document.createElement('button');
                resultButton.type = 'button';
                resultButton.className = 'map-search-result';
                resultButton.innerHTML =
                    '<strong>' + escapeHtml(place.place_name) + '</strong>' +
                    '<span>' + escapeHtml(placeAddressText(place)) + '</span>';

                resultButton.addEventListener('click', function () {
                    chooseMapPlace(index);
                });

                searchResults.appendChild(resultButton);
            });

            searchResults.hidden = false;
        }

        function searchPlaces() {
            var keyword = searchInput.value.trim();

            if (currentViewMode === 'feed') {
                searchResults.hidden = true;
                renderFeedList();
                return;
            }

            if (keyword.length < 2) {
                searchResults.hidden = true;
                return;
            }

            if (!places) {
                showError('위치 검색을 사용할 수 없습니다. Kakao 지도 services 라이브러리를 확인해주세요.');
                return;
            }

            places.keywordSearch(keyword, function (results, status) {
                if (status !== kakao.maps.services.Status.OK || !results.length) {
                    searchResults.hidden = true;
                    return;
                }

                renderSearchResults(results, keyword);
            }, placeSearchOptions(SEARCH_MODES.BROWSE));
        }

        searchForm.addEventListener('submit', function (event) {
            event.preventDefault();
            searchPlaces();
        });

        searchInput.addEventListener('input', function () {
            window.clearTimeout(searchTimer);

            if (currentViewMode === 'feed') {
                searchResults.hidden = true;
                renderFeedList();
                return;
            }

            searchTimer = window.setTimeout(function () {
                searchPlaces();
            }, 220);
        });

        function renderMarkerLocationResults(results, keyword) {
            markerLocationResults.innerHTML = '';
            markerPlaces = sortMarkerLocationResultsByRelevance(results, keyword).slice(0, 5);

            markerPlaces.forEach(function (place) {
                var resultButton = document.createElement('button');
                resultButton.type = 'button';
                resultButton.className = 'marker-location-result';
                resultButton.innerHTML =
                    '<strong>' + escapeHtml(place.place_name) + '</strong>' +
                    '<span>' + escapeHtml(placeAddressText(place)) + '</span>';

                resultButton.addEventListener('click', function () {
                    var markerLocationPosition = new kakao.maps.LatLng(place.y, place.x);

                    if (!canSelectSearchResult(SEARCH_MODES.RECORD, markerLocationPosition)) {
                        showMarkerDistanceError();
                        return;
                    }

                    markerLocationInput.value = place.place_name;
                    markerLocationSelectedInput.value = place.place_name;
                    selectedMarkerLocationPosition = markerLocationPosition;
                    hideMarkerFormMessage();
                    markerLocationResults.hidden = true;
                    titleInput.focus();
                });

                markerLocationResults.appendChild(resultButton);
            });

            markerLocationResults.hidden = !markerPlaces.length;
        }

        function searchMarkerLocations() {
            var keyword = markerLocationInput.value.trim();

            if (keyword.length < 2) {
                markerPlaces = [];
                markerLocationResults.hidden = true;
                return;
            }

            if (!places) {
                showError('위치 검색을 사용할 수 없습니다. Kakao 지도 services 라이브러리를 확인해주세요.');
                return;
            }

            places.keywordSearch(keyword, function (results, status) {
                if (status !== kakao.maps.services.Status.OK || !results.length) {
                    markerPlaces = [];
                    markerLocationResults.hidden = true;
                    return;
                }

                renderMarkerLocationResults(results, keyword);
            }, markerPlaceSearchOptions());
        }

        markerLocationInput.addEventListener('input', function () {
            window.clearTimeout(markerLocationTimer);

            if (isResolvingMarkerLocation) {
                markerLocationEditedDuringResolve = true;
                markerLocationInput.placeholder = markerLocationDefaultPlaceholder;
            }

            markerLocationSelectedInput.value = '';
            selectedMarkerLocationPosition = null;
            hideMarkerFormMessage();

            markerLocationTimer = window.setTimeout(function () {
                searchMarkerLocations();
            }, 220);
        });

        function escapeHtml(value) {
            return String(value || '').replace(/[&<>"']/g, function (match) {
                return {
                    '&': '&amp;',
                    '<': '&lt;',
                    '>': '&gt;',
                    '"': '&quot;',
                    "'": '&#39;'
                }[match];
            });
        }

        async function deleteEmotionMarker(markerItemOrId) {
            var markerId = typeof markerItemOrId === 'object' && markerItemOrId !== null ? markerItemOrId.id : markerItemOrId;
            var markerItem = typeof markerItemOrId === 'object' && markerItemOrId !== null ? markerItemOrId : findMarkerItem(markerId);

            if (!markerId) {
                return;
            }

            var response = await fetch('/api/emotion-map-markers/' + encodeURIComponent(markerId), {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error(String(response.status));
            }

            if (markerItem) {
                markerItem.markerOverlay.setMap(null);
                markerItem.infoOverlay.setMap(null);
            }
            emotionMarkerItems = emotionMarkerItems.filter(function (item) {
                return String(item.id) !== String(markerId);
            });
            selectedClusterItems = selectedClusterItems.filter(function (item) {
                return String(item.id) !== String(markerId);
            });

            if (markerItem && activeInfoOverlay === markerItem.infoOverlay) {
                activeInfoOverlay = null;
            }

            if (markerItem && activeMarkerItem === markerItem) {
                activeMarkerItem = null;
            }

            if (activeRecordDetail && String(activeRecordDetail.id) === String(markerId)) {
                closeRecordPanel();
            }

            if (selectedClusterItems.length > 1) {
                openClusterPanel(selectedClusterItems);
            } else {
                closeClusterPanel();
            }

            renderFeedList();
            scheduleEmotionFiltersRender();
            applyMarkerFilters();
        }

        function createEmotionMarker(markerId, position, emotion, title, description, locationName, authorNickname, createdAt, own, shouldOpen) {
            var markerElement = document.createElement('button');
            markerElement.type = 'button';
            markerElement.className = 'emotion-map-marker';
            markerElement.style.setProperty('--marker-color', emotion.color);
            markerElement.setAttribute('aria-label', emotion.label + ' 감정 마커: ' + title);
            markerElement.innerHTML = '<span class="emotion-map-marker-icon" aria-hidden="true">' + escapeHtml(emotion.icon || '🙂') + '</span>';

            var markerOverlay = new kakao.maps.CustomOverlay({
                position: position,
                content: markerElement,
                yAnchor: 1,
                xAnchor: 0.5,
                zIndex: markerOverlayZIndex
            });

            var infoElement = document.createElement('article');
            infoElement.className = 'emotion-marker-popup';
            infoElement.style.setProperty('--marker-color', emotion.color);
            infoElement.setAttribute('role', 'button');
            infoElement.setAttribute('tabindex', '0');
            infoElement.setAttribute('aria-label', title + ' 기록 상세 보기');
            infoElement.innerHTML =
                '<strong>' + escapeHtml(title) + '</strong>' +
                '<span>' + escapeHtml(emotion.label) + '</span>' +
                '<small>' + escapeHtml(locationName || formatPosition(position)) + '</small>';

            var infoOverlay = new kakao.maps.CustomOverlay({
                position: position,
                content: infoElement,
                yAnchor: 1.28,
                xAnchor: 0.5,
                zIndex: infoOverlayZIndex
            });

            markerElement.addEventListener('click', function (event) {
                event.stopPropagation();
                closeClusterPanel();

                var isOpen = activeInfoOverlay === infoOverlay;
                if (isOpen) {
                    closeInfoOverlay(markerItem);
                } else {
                    openInfoOverlay(markerItem);
                }
            });

            var markerItem = {
                id: markerId,
                position: position,
                markerOverlay: markerOverlay,
                infoOverlay: infoOverlay,
                emotionLabel: emotion.label,
                emotionColor: emotion.color,
                emotionIcon: emotion.icon || '🙂',
                title: title,
                description: description,
                locationName: locationName,
                authorNickname: authorNickname,
                createdAt: createdAt,
                own: Boolean(own)
            };

            infoElement.addEventListener('click', function (event) {
                event.stopPropagation();
                openRecordDetail(markerItem.id);
            });

            infoElement.addEventListener('keydown', function (event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    openRecordDetail(markerItem.id);
                }
            });

            emotionMarkerItems.push(markerItem);
            if (currentViewMode === 'map' && markerMatchesFilter(markerItem)) {
                markerOverlay.setMap(map);
            }
            renderFeedList();
            scheduleEmotionFiltersRender();
            applyMarkerFilters();

            if (shouldOpen) {
                if (currentViewMode === 'feed') {
                    openRecordDetail(markerItem.id);
                    return markerItem;
                }

                if (activeInfoOverlay) {
                    activeInfoOverlay.setMap(null);
                }

                if (markerMatchesFilter(markerItem)) {
                    markerOverlay.setMap(map);
                    openInfoOverlay(markerItem);
                }
            }

            return markerItem;
        }

        function markerFromResponse(marker, shouldOpen) {
            if (marker.latitude == null || marker.longitude == null || !marker.emotionLabel || !marker.title) {
                return null;
            }

            var emotion = emotionMetaForLabel(marker.emotionLabel, marker.emotionColor);

            return createEmotionMarker(
                marker.id,
                new kakao.maps.LatLng(marker.latitude, marker.longitude),
                {
                    label: emotion.label || marker.emotionLabel,
                    color: emotion.color || marker.emotionColor,
                    icon: emotion.icon || '🙂'
                },
                marker.title,
                marker.description || '',
                marker.locationName || '',
                marker.authorNickname || '',
                marker.createdAt || '',
                marker.own,
                shouldOpen
            );
        }

        async function loadEmotionMarkers() {
            try {
                var response = await fetch('/api/emotion-map-markers');

                if (!response.ok) {
                    throw new Error(String(response.status));
                }

                var markers = await response.json();
                markers.forEach(function (marker) {
                    markerFromResponse(marker, false);
                });
                if (currentViewMode === 'map') {
                    refreshMarkerLayers();
                }
                renderFeedList();
                updateFilterMenu(visibleMarkerItems().length);
            } catch {
                showError('저장된 감정 마커를 불러오지 못했습니다.');
            }
        }

        async function saveEmotionMarker(marker) {
            var response = await fetch('/api/emotion-map-markers', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(marker)
            });

            if (!response.ok) {
                throw new Error(String(response.status));
            }

            return response.json();
        }

        setMapViewMode(currentViewMode, false);
        renderEmotionFilters();
        loadEmotionMarkers();

        form.addEventListener('submit', async function (event) {
            event.preventDefault();

            var checkedEmotion = form.querySelector('input[name="emotion"]:checked');
            var locationName = markerLocationInput.value.trim();
            var selectedLocationName = markerLocationSelectedInput.value.trim();
            var title = titleInput.value.trim();
            var description = descriptionInput.value.trim();

            if (!locationName) {
                markerLocationInput.focus();
                showError('위치를 입력해주세요.');
                return;
            }

            if (selectedLocationName && locationName !== selectedLocationName) {
                markerLocationSelectedInput.value = '';
                selectedMarkerLocationPosition = null;
            }

            if (selectedMarkerLocationPosition && !canSelectSearchResult(SEARCH_MODES.RECORD, selectedMarkerLocationPosition)) {
                markerLocationInput.focus();
                showMarkerDistanceError();
                return;
            }

            if (!title) {
                titleInput.focus();
                return;
            }

            if (!description) {
                descriptionInput.focus();
                showError('설명을 입력해주세요.');
                return;
            }

            try {
                var markerPosition = selectedPosition;
                var selectedEmotion = emotionMetaForLabel(checkedEmotion.dataset.label, checkedEmotion.dataset.color);
                var savedMarker = await saveEmotionMarker({
                    latitude: markerPosition.getLat(),
                    longitude: markerPosition.getLng(),
                    emotionLabel: selectedEmotion.label || checkedEmotion.dataset.label,
                    emotionColor: selectedEmotion.color || checkedEmotion.dataset.color,
                    title: title,
                    locationName: locationName,
                    description: description
                });

                markerFromResponse(savedMarker, true);
            } catch (error) {
                if (error.message === '401') {
                    showError('로그인 후 감정 마커를 저장할 수 있습니다.');
                } else if (error.message === '400') {
                    showError('입력한 내용을 확인해주세요.');
                } else {
                    showError('감정 마커를 저장하지 못했습니다. 서버 상태를 확인해주세요.');
                }
                return;
            }

            titleInput.value = '';
            descriptionInput.value = '';
            markerLocationInput.value = '';
            markerLocationSelectedInput.value = '';
            selectedMarkerLocationPosition = null;
            hideMarkerFormMessage();
            markerLocationResults.hidden = true;
            markerPlaces = [];
            closeMarkerModal();
            message.classList.remove('is-error');
            message.textContent = '감정 마커를 추가했습니다.';
            message.hidden = false;

            window.setTimeout(function () {
                message.hidden = true;
            }, 1800);
        });

        openMarkerModalButton.addEventListener('click', openMarkerModal);
        moveCurrentLocationButton.addEventListener('click', moveToCurrentLocation);
        closeMarkerModalButton.addEventListener('click', closeMarkerModal);
        markerModalBackdrop.addEventListener('click', closeMarkerModal);
        clusterPanelCloseButton?.addEventListener('click', closeClusterPanel);
        recordPanelCloseButton?.addEventListener('click', closeRecordPanel);
        recordDeleteBackdrop?.addEventListener('click', closeDeleteConfirmModal);
        recordDeleteCancelButton?.addEventListener('click', closeDeleteConfirmModal);
        recordDeleteConfirmButton?.addEventListener('click', deleteActiveRecord);
        recordCommentForm?.addEventListener('submit', async function (event) {
            event.preventDefault();

            if (!activeRecordDetail) {
                return;
            }

            var content = recordCommentInput.value.trim();
            if (!content) {
                recordCommentInput.focus();
                return;
            }

            try {
                var response = await fetch('/api/emotion-map-markers/' + encodeURIComponent(activeRecordDetail.id) + '/comments', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        content: content
                    })
                });

                if (!response.ok) {
                    throw new Error(String(response.status));
                }

                var comment = await response.json();
                activeRecordComments.push(comment);
                activeRecordDetail.commentCount += 1;
                recordCommentInput.value = '';
                renderRecordDetail();
            } catch {
                showError('댓글을 등록하지 못했습니다.');
            }
        });

        if (requestedEmotionId && selectMarkerEmotion(requestedEmotionId)) {
            var waitForInitialLocation = shouldLocateOnEntry && !hasRequestedPosition;
            clearRequestedEmotionIntent();

            if (waitForInitialLocation) {
                initialLocationRequest.finally(openMarkerModal);
            } else {
                openMarkerModal();
            }
        } else if (requestedEmotionId) {
            clearRequestedEmotionIntent();
        }

        window.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !recordDeleteModal.hidden) {
                closeDeleteConfirmModal();
            } else if (event.key === 'Escape' && !markerModal.hidden) {
                closeMarkerModal();
            }
        });

        window.addEventListener('resize', function () {
            if (currentViewMode !== 'map') {
                return;
            }

            var centerBeforeResize = map.getCenter();
            kakao.maps.event.trigger(map, 'resize');
            map.setCenter(centerBeforeResize);
            selectedPosition = centerBeforeResize;
            scheduleCustomMarkerVisibilitySync();
        });
    })();
