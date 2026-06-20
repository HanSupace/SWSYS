const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const templatesRoot = 'src/main/resources/templates';
const staticRoot = 'src/main/resources/static';

function filesBelow(directory, extension) {
    return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
        const entryPath = path.join(directory, entry.name);
        return entry.isDirectory() ? filesBelow(entryPath, extension) : (entryPath.endsWith(extension) ? [entryPath] : []);
    });
}

const templates = filesBelow(templatesRoot, '.html');

templates.forEach((templatePath) => {
    const html = fs.readFileSync(templatePath, 'utf8');
    const executableInlineScript = /<script(?![^>]*\bsrc=)(?![^>]*type=["']application\/json["'])[^>]*>/i;

    assert.doesNotMatch(html, /<style\b/i, `${templatePath}: CSS는 외부 파일로 분리해야 합니다.`);
    assert.doesNotMatch(html, /(?:\s|th:)style=/i, `${templatePath}: 인라인 style 속성을 사용하지 않아야 합니다.`);
    assert.doesNotMatch(html, executableInlineScript, `${templatePath}: 실행 JavaScript는 외부 파일로 분리해야 합니다.`);

    for (const match of html.matchAll(/@\{\/((?:css|js)\/[^?(}]+)/g)) {
        const assetPath = path.join(staticRoot, match[1]);
        assert.equal(fs.existsSync(assetPath), true, `${templatePath}: 참조 파일이 없습니다: ${assetPath}`);
    }
});

filesBelow(path.join(staticRoot, 'js'), '.js').forEach((scriptPath) => {
    const source = fs.readFileSync(scriptPath, 'utf8');
    assert.doesNotThrow(() => new Function(source), `${scriptPath}: JavaScript 구문 오류가 없어야 합니다.`);
});

console.log('static asset structure tests passed');
