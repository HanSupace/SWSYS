# 승학캠퍼스 감정기록 100개

## PostgreSQL 실행 파일 (권장)

`seunghak-100.sql`은 psql과 PostgreSQL SQL 편집기에서 직접 실행할 수 있는 단일 시드입니다. `generate_sql.py`가 검토된 PSV에서 생성합니다. 별도 Python·Java 설치 없이 SQL 파일만 학교 클라우드에 복사해 실행할 수 있습니다.

현재 `.env`는 Supabase를 가리킵니다. 학교 클라우드 DB는 별개이며 **이번 작업에서 학교 DB에는 접속하거나 적용하지 않았습니다.**

학교 DB에 앱의 `schema.sql`이 이미 적용된 것을 확인하고, 프로젝트 루트에서 실행하세요. `SCHOOL_*` 부분을 실제 학교 PostgreSQL 접속 정보로 바꿉니다. 비밀번호는 프롬프트에 입력합니다. `psql`은 프로젝트 `.env`를 자동으로 읽지 않습니다.

```powershell
$env:PGSSLMODE = 'require'
psql -X -W -h SCHOOL_DB_HOST -p 5432 -U SCHOOL_DB_USER -d SCHOOL_DB_NAME -v ON_ERROR_STOP=1 -f scripts/seed/inspect.sql
psql -X -W -h SCHOOL_DB_HOST -p 5432 -U SCHOOL_DB_USER -d SCHOOL_DB_NAME -v ON_ERROR_STOP=1 -f scripts/seed/seunghak-100.sql
```

학교 DB의 SSL 정책에 따라 `PGSSLMODE`를 설정합니다. 인증서 검증이 요구되면 학교가 제공한 CA와 `verify-full`을 사용하세요. 아직 프로젝트 테이블이 없는 새 DB라면 프로젝트 초기화 작업을 먼저 수행해야 합니다. 이 시드는 스키마나 감정 카탈로그를 만들거나 변경하지 않습니다.

스크립트는 `BEGIN`/`COMMIT`, 잠금, 데이터 검증을 포함합니다. 최초 적용은 사용자 10명·기록 100개 생성, 재실행은 각각 0개 추가입니다. 이전 Java 시드와 같은 사용자명·제목·본문·좌표·태그를 사용하여 이미 삽입된 100개를 재사용합니다. 기존 데이터와 다른 내용이 발견되거나 같은 테스트 사용자명이 일반 계정과 충돌하면 수정하는 대신 전체를 롤백합니다. 실제 사용자에게 테스트 글을 귀속시키지 않습니다.

모든 기존 사용자·기록의 전체 행과 기존 태그 링크를 실행 전후 비교합니다. `UPDATE`, `DELETE`, `TRUNCATE`는 사용하지 않습니다. 사용자 ID와 기록 ID를 하드코딩하지 않으며, DB가 생성한 사용자 ID를 조인해 외래키를 만족시킵니다. 스크립트가 끝나면 삽입 수, 감정별 수, 장소·좌표별 수를 출력합니다.

감정명은 DB enum이나 감정 FK 테이블로 제한되는 구조가 아니라 `EmotionCatalog.java`에 정의되어 있습니다. 기존 DB에 저장된 감정명·색상과도 대조했습니다. 중립 감정은 기존 8종에 없어 새로 만들지 않았습니다. 추천 태그는 별도 `recommendation_tag_catalog`의 기존 코드만 참조하며, 없는 코드는 새로 생성하지 않고 오류를 냅니다.

**2026-11-03 검증:** 격리된 PostgreSQL 스키마에서 첫 실행 100개·재실행 0개와 기존 샘플 사용자·기록 보존을 검증하고 전체 롤백했습니다. 실제 Supabase 실행 결과는 `inserted_users=0`, `inserted_records=0`, `verified_seed_records=100`, `existing_rows_preserved=true`였습니다. 이전에 저장한 100개가 있어 추가로 누적하지 않았습니다. 자세한 스키마와 좌표별 결과는 [검증 보고서](verification.md)를 참고하세요.

## 기존 Java 시드 및 원본 데이터

`seunghak-records.psv`는 직접 작성한 가상 기록 100개입니다. 제목과 본문에 가상 접두어를 붙이지 않습니다. 2026-11-03 사용자 승인 후 Supabase에 저장했고, 100개 기록의 감정·색상·장소·본문·추천 태그 일치와 커밋 성공을 확인했습니다.

- 기존 EmotionCatalog의 감정명과 색상을 사용합니다. 기쁨·기대·슬픔·분노 각 13개, 불안·당황·놀람·짜증 각 12개입니다.
- 10개 장소에 각각 10개 기록을 배치하며, 같은 장소의 좌표는 동일합니다.
- 좌표는 지도 데모를 위한 **주변 근사 좌표**입니다. 건물 출입구, 상호 또는 등산로의 실측 좌표가 아닙니다. 정문 근처 카페는 특정 업소를 가리키지 않습니다.
- 장소 이름은 [동아대 공식 캠퍼스맵](https://rscm.donga.ac.kr/global/CMS/Contents/Contents.do?mCode=MN069)과 [찾아오시는 길](https://www.donga.ac.kr/about/campus/directions/seunghak)을 참고했습니다.
- 저장 스크립트는 실제 사용자에게 기록을 귀속시키지 않고 전용 작성자 10명(`seed_seunghak_v1_01`~`10`)을 만듭니다. 해당 계정은 로그인할 수 없는 비밀번호 값을 사용합니다.
- 기존 추천 태그도 저장하고 `tags_classified=true`로 지정하여 AI 분류 호출 없이 사용할 수 있습니다.
- 시각은 실행 시점의 한국 시간 기준, 최근 약 5일에 걸쳐 분산합니다.

프로젝트 최상위에서 Java 21 이상과 PostgreSQL JDBC 드라이버로 실행합니다. `.env`의 DB 설정을 읽으며 비밀값을 출력하지 않습니다.

```powershell
$jdbcJar = Get-ChildItem "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql" -Recurse -Filter 'postgresql-*.jar' |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } |
    Select-Object -First 1 -ExpandProperty FullName
java '-Dfile.encoding=UTF-8' --class-path $jdbcJar scripts/seed/SeedSeunghak.java validate
java '-Dfile.encoding=UTF-8' --class-path $jdbcJar scripts/seed/SeedSeunghak.java inspect
```

`validate`는 DB에 접근하지 않습니다. `inspect`는 읽기 전용입니다. 별도로 DB 저장을 승인한 경우에만 마지막 인수를 `apply`로 지정합니다. `apply`는 트랜잭션으로 저장하며, 프로젝트 테이블이 없는 경우 기존 `schema.sql`을 먼저 적용합니다. 오류가 나면 전체 변경을 롤백합니다. 이미 생성된 100개가 있으면 내용을 검증한 뒤 변경 없이 종료하며, 일부만 남아 있으면 중복 삽입을 거부합니다. 앱 시작 시 자동 실행되지 않습니다.
