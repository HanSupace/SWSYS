(function (root, factory) {
    var policy = factory();

    if (typeof module === 'object' && module.exports) {
        module.exports = policy;
    }

    root.PliaMapSearchPolicy = policy;
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
    var MODES = Object.freeze({
        BROWSE: 'browse',
        RECORD: 'record'
    });
    var EARTH_RADIUS_METERS = 6371000;
    var RECORD_RADIUS_METERS = 250;

    function coordinate(position, getterName, propertyName) {
        if (position && typeof position[getterName] === 'function') {
            return Number(position[getterName]());
        }

        return Number(position && position[propertyName]);
    }

    function distanceInMeters(firstPosition, secondPosition) {
        var firstLatitude = coordinate(firstPosition, 'getLat', 'latitude') * Math.PI / 180;
        var secondLatitude = coordinate(secondPosition, 'getLat', 'latitude') * Math.PI / 180;
        var firstLongitude = coordinate(firstPosition, 'getLng', 'longitude');
        var secondLongitude = coordinate(secondPosition, 'getLng', 'longitude');
        var latitudeDelta = secondLatitude - firstLatitude;
        var longitudeDelta = (secondLongitude - firstLongitude) * Math.PI / 180;
        var haversineValue = Math.sin(latitudeDelta / 2) ** 2
            + Math.cos(firstLatitude) * Math.cos(secondLatitude) * Math.sin(longitudeDelta / 2) ** 2;
        var normalizedHaversineValue = Math.min(1, Math.max(0, haversineValue));

        return EARTH_RADIUS_METERS * 2 * Math.atan2(
            Math.sqrt(normalizedHaversineValue),
            Math.sqrt(1 - normalizedHaversineValue)
        );
    }

    function canSelect(mode, centerPosition, candidatePosition) {
        if (mode !== MODES.RECORD) {
            return true;
        }

        return distanceInMeters(centerPosition, candidatePosition) <= RECORD_RADIUS_METERS;
    }

    return Object.freeze({
        MODES: MODES,
        RECORD_RADIUS_METERS: RECORD_RADIUS_METERS,
        distanceInMeters: distanceInMeters,
        canSelect: canSelect
    });
}));
