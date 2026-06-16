(function () {
    function parseCatalog() {
        if (Array.isArray(window.PLIA_EMOTIONS)) {
            return window.PLIA_EMOTIONS;
        }

        var catalogElement = document.getElementById('plia-emotion-catalog');
        var catalogText = catalogElement ? catalogElement.textContent.trim() : '';

        if (!catalogText) {
            return [];
        }

        try {
            var parsedCatalog = JSON.parse(catalogText);
            return Array.isArray(parsedCatalog) ? parsedCatalog : [];
        } catch (error) {
            return [];
        }
    }

    function normalizeEmotion(emotion) {
        return {
            id: String(emotion.id || '').trim(),
            code: String(emotion.code || '').trim(),
            value: String(emotion.value || '').trim(),
            label: String(emotion.label || '').trim(),
            icon: String(emotion.icon || '').trim(),
            color: String(emotion.color || '').trim(),
            background: String(emotion.background || '').trim(),
            border: String(emotion.border || '').trim(),
            positive: Boolean(emotion.positive)
        };
    }

    function byKey(key, value) {
        var normalizedValue = String(value || '').trim();

        if (!normalizedValue) {
            return null;
        }

        return emotions.find(function (emotion) {
            if (key === 'code') {
                return emotion.code.toUpperCase() === normalizedValue.toUpperCase();
            }

            if (key === 'value') {
                return emotion.value.toLowerCase() === normalizedValue.toLowerCase();
            }

            return emotion[key] === normalizedValue;
        }) || null;
    }

    function byLabel(label) {
        return byKey('label', label);
    }

    function byCode(code) {
        return byKey('code', code);
    }

    function byValue(value) {
        return byKey('value', value);
    }

    function byId(id) {
        return byKey('id', id);
    }

    function colorFor(label, fallbackColor) {
        var emotion = byLabel(label);
        return emotion && emotion.color ? emotion.color : fallbackColor;
    }

    function metaForLabel(label, fallbackColor) {
        var emotion = byLabel(label);

        if (emotion) {
            return emotion;
        }

        return {
            id: '',
            code: '',
            value: '',
            label: String(label || '').trim(),
            icon: '',
            color: fallbackColor || '',
            background: '',
            border: '',
            positive: false
        };
    }

    var emotions = parseCatalog()
        .map(normalizeEmotion)
        .filter(function (emotion) {
            return emotion.id && emotion.code && emotion.value && emotion.label && emotion.color;
        });
    var emotionColors = emotions.reduce(function (result, emotion) {
        result[emotion.code] = emotion.color;
        return result;
    }, {});

    window.PLIA_EMOTIONS = emotions;
    window.EMOTION_COLORS = emotionColors;
    window.PLIA_EMOTION = {
        all: emotions,
        colors: emotionColors,
        byLabel: byLabel,
        byCode: byCode,
        byValue: byValue,
        byId: byId,
        colorFor: colorFor,
        metaForLabel: metaForLabel
    };
}());
