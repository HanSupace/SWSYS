const assert = require('node:assert/strict');
const policy = require('../../main/resources/static/js/map-search-policy.js');

const seoulCenter = { latitude: 37.566826, longitude: 126.9786567 };
const nearbyPlace = { latitude: 37.575, longitude: 126.9786567 };
const distantPlace = { latitude: 35.1796, longitude: 129.0756 };

assert.equal(policy.canSelect(policy.MODES.RECORD, seoulCenter, nearbyPlace), true,
    '기록 검색은 중앙 핀에서 2km 이내 장소를 허용해야 합니다.');
assert.equal(policy.canSelect(policy.MODES.RECORD, seoulCenter, distantPlace), false,
    '기록 검색은 중앙 핀에서 2km 밖 장소를 막아야 합니다.');
assert.equal(policy.canSelect(policy.MODES.BROWSE, seoulCenter, distantPlace), true,
    '일반 지도 검색은 거리에 관계없이 장소를 허용해야 합니다.');

console.log('map-search-policy tests passed');
