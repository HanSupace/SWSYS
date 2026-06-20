const assert = require('node:assert/strict');
const fs = require('node:fs');

const mainPageScript = fs.readFileSync('src/main/resources/static/js/mainpage.js', 'utf8');
const mapTemplate = fs.readFileSync('src/main/resources/templates/map.html', 'utf8');

assert.match(mainPageScript, /searchParams\.set\('locate', 'current'\)/,
    '메인 감정 버튼은 지도에 최초 현재 위치 조회 의도를 전달해야 합니다.');
assert.match(mapTemplate, /currentLocationRequest = getCurrentKakaoPosition\(\)[\s\S]*?\.finally\(function \(\) \{/,
    '현재 위치 조회는 성공과 실패 모두에서 종료 처리를 해야 합니다.');
assert.match(mapTemplate, /if \(currentLocationRequest\) \{\s*return currentLocationRequest;/,
    '중복 현재 위치 조회는 진행 중인 요청을 재사용해야 합니다.');
assert.doesNotMatch(mapTemplate, /map\.setCenter\(currentPosition\)/,
    '화면 전환이나 리사이즈가 사용자의 선택 위치를 최초 GPS 위치로 되돌리면 안 됩니다.');
assert.match(mapTemplate, /var markerPosition = selectedPosition;/,
    '저장 좌표는 최종 지도 중앙 선택 위치여야 합니다.');

console.log('map entry location flow tests passed');
