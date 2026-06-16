require('dotenv').config();

const fs = require('fs/promises');
const path = require('path');
const bcrypt = require('bcryptjs');
const express = require('express');
const session = require('express-session');
const PgSession = require('connect-pg-simple')(session);
const helmet = require('helmet');
const pool = require('./db/pool');
const { emotions, byLabel } = require('./services/emotions');

const app = express();
const viewsDir = path.join(__dirname, 'views');
const publicDir = path.join(__dirname, 'public');
const port = Number(process.env.PORT || 3000);

const missions = [
  ['thanks-message', '가족이나 친구에게 고마웠던 점을 하나 메시지로 보내기'],
  ['hold-door', '문이나 엘리베이터 앞에서 뒤 사람을 위해 잠깐 기다려주기'],
  ['pick-trash', '내 주변에 보이는 작은 쓰레기 하나 줍기'],
  ['warm-greeting', '오늘 만나는 사람 한 명에게 먼저 따뜻하게 인사하기'],
  ['self-kindness', '고생한 나 자신에게 다정한 말 한마디 남기기'],
  ['compliment', '누군가에게 진심 어린 칭찬 한마디 건네기'],
  ['listen', '상대 이야기를 끊지 않고 끝까지 들어주기'],
  ['share-snack', '간식이나 음료를 주변 사람과 나누기']
];

app.use(helmet({ contentSecurityPolicy: false }));
app.use(express.urlencoded({ extended: false }));
app.use(express.json());
app.use('/static', express.static(publicDir));
app.use(express.static(publicDir));
app.use(session({
  store: new PgSession({ pool, createTableIfMissing: true }),
  secret: process.env.SESSION_SECRET || 'dev-session-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: process.env.NODE_ENV === 'production'
  }
}));

function todayIso() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date());
}

function progress(totalXp = 0) {
  const level = Math.floor(totalXp / 100) + 1;
  const currentXp = totalXp % 100;
  return { level, currentXp, requiredXp: 100, progressPercent: currentXp };
}

function requireLogin(req, res, next) {
  if (!req.session.user) {
    if (req.path.startsWith('/api/')) {
      res.status(401).json({ message: 'login required' });
      return;
    }
    res.redirect('/login');
    return;
  }
  next();
}

async function renderPage(res, file, user = null) {
  const pagePath = path.join(viewsDir, file);
  let html = await fs.readFile(pagePath, 'utf8');
  const emotionJson = JSON.stringify(emotions);
  const emotionAssets = [
    '<link rel="stylesheet" href="/css/emotions.css?v=node">',
    `<script id="plia-emotion-catalog" type="application/json">${emotionJson}</script>`,
    '<script src="/js/emotions.js?v=node"></script>',
    '<script src="/js/node-emotions-init.js?v=node" defer></script>'
  ].join('\n    ');
  html = html.replace(/<th:block th:replace="~\{fragments\/emotions :: assets\}"><\/th:block>/g, emotionAssets);
  html = html.replace(/<span th:text="\$\{appName\}">Plia<\/span>/g, '<span>Plia</span>');
  if (user) {
    html = html.replace(/닉네임/g, user.nickname);
  }
  res.type('html').send(html);
}

async function currentUserProgress(userId) {
  await pool.query(
    'insert into user_progress (user_id, total_xp) values ($1, 0) on conflict (user_id) do nothing',
    [userId]
  );
  const result = await pool.query('select total_xp from user_progress where user_id = $1', [userId]);
  return progress(result.rows[0]?.total_xp || 0);
}

async function missionPayload(userId) {
  const date = todayIso();
  await pool.query(
    'insert into daily_mission_days (user_id, mission_date) values ($1, $2) on conflict (user_id, mission_date) do nothing',
    [userId, date]
  );
  const completed = await pool.query(
    'select mission_key from daily_mission_completions where user_id = $1 and mission_date = $2',
    [userId, date]
  );
  const completedKeys = new Set(completed.rows.map((row) => row.mission_key));
  const day = await pool.query(
    'select success_count from daily_mission_days where user_id = $1 and mission_date = $2',
    [userId, date]
  );

  return {
    missions: missions.slice(0, 5).map(([id, text], slotIndex) => ({
      id,
      slotIndex,
      text,
      completed: completedKeys.has(id),
      rerollCount: 0,
      remainingRerolls: 3,
      rerollAvailable: !completedKeys.has(id)
    })),
    todaySuccessCount: day.rows[0]?.success_count || 0,
    progress: await currentUserProgress(userId)
  };
}

app.get('/', async (req, res, next) => {
  try {
    await renderPage(res, req.session.user ? 'home/mainpage.html' : 'home/home.html', req.session.user);
  } catch (error) {
    next(error);
  }
});

app.get('/login', async (req, res, next) => {
  try {
    await renderPage(res, 'auth/login.html');
  } catch (error) {
    next(error);
  }
});

app.post('/login', async (req, res, next) => {
  try {
    const { username = '', password = '' } = req.body;
    const result = await pool.query('select id, username, password_hash, nickname from users where username = $1', [username.trim()]);
    const user = result.rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      res.redirect('/login');
      return;
    }
    req.session.user = { id: user.id, username: user.username, nickname: user.nickname };
    res.redirect('/');
  } catch (error) {
    next(error);
  }
});

app.post('/logout', (req, res) => {
  req.session.destroy(() => res.redirect('/login'));
});

app.get('/signup', async (req, res, next) => {
  try {
    await renderPage(res, 'auth/signup.html');
  } catch (error) {
    next(error);
  }
});

app.post('/signup', async (req, res, next) => {
  try {
    const { username = '', password = '', passwordConfirm = '', nickname = '' } = req.body;
    if (username.trim().length < 3 || password.length < 8 || password !== passwordConfirm || nickname.trim().length < 2) {
      res.redirect('/signup');
      return;
    }
    const passwordHash = await bcrypt.hash(password, 10);
    const result = await pool.query(
      'insert into users (username, password_hash, nickname) values ($1, $2, $3) returning id',
      [username.trim(), passwordHash, nickname.trim()]
    );
    await pool.query('insert into user_progress (user_id) values ($1)', [result.rows[0].id]);
    res.redirect('/signup/success?nickname=' + encodeURIComponent(nickname.trim()));
  } catch (error) {
    if (error.code === '23505') {
      res.redirect('/signup');
      return;
    }
    next(error);
  }
});

app.get('/signup/success', async (req, res, next) => {
  try {
    await renderPage(res, 'auth/signup-success.html', { nickname: req.query.nickname || '사용자' });
  } catch (error) {
    next(error);
  }
});

for (const [route, file] of [
  ['/dailymission', 'home/dailymission.html'],
  ['/ranking', 'home/ranking.html'],
  ['/profile', 'home/profile.html'],
  ['/profile/mission-settings', 'home/mission-settings.html'],
  ['/profile/edit', 'home/profile-edit.html'],
  ['/profile/change-password', 'home/change-password.html'],
  ['/profile/emotion-records', 'home/emotion-records.html'],
  ['/profile/report', 'home/report.html'],
  ['/map', 'map.html']
]) {
  app.get(route, requireLogin, async (req, res, next) => {
    try {
      await renderPage(res, file, req.session.user);
    } catch (error) {
      next(error);
    }
  });
}

app.post('/profile/edit', requireLogin, async (req, res, next) => {
  try {
    const nickname = String(req.body.nickname || '').trim();
    if (nickname.length >= 2) {
      await pool.query('update users set nickname = $1 where id = $2', [nickname, req.session.user.id]);
      req.session.user.nickname = nickname;
    }
    res.redirect('/profile');
  } catch (error) {
    next(error);
  }
});

app.post('/profile/change-password', requireLogin, async (req, res, next) => {
  try {
    const nextPassword = req.body.newPassword || req.body.password || '';
    if (nextPassword.length >= 8) {
      await pool.query('update users set password_hash = $1 where id = $2', [await bcrypt.hash(nextPassword, 10), req.session.user.id]);
    }
    res.redirect('/profile');
  } catch (error) {
    next(error);
  }
});

app.post('/profile/report', requireLogin, async (req, res, next) => {
  try {
    await pool.query(
      'insert into user_reports (user_id, location_name, title, content, category) values ($1, $2, $3, $4, $5)',
      [req.session.user.id, req.body.location || '', req.body.title || '', req.body.content || '', req.body.category || 'OTHER']
    );
    res.redirect('/profile');
  } catch (error) {
    next(error);
  }
});

app.post('/profile/mission-settings', requireLogin, async (req, res, next) => {
  try {
    await pool.query(
      'insert into user_mission_settings (user_id, mission_mode, life_stage, environment_type, condition_type) values ($1, $2, $3, $4, $5) on conflict (user_id) do update set mission_mode = excluded.mission_mode, life_stage = excluded.life_stage, environment_type = excluded.environment_type, condition_type = excluded.condition_type, updated_at = now()',
      [
        req.session.user.id,
        req.body.mode || 'PLAIN',
        req.body.lifeStage || 'ANY',
        req.body.environmentType || 'ANY',
        req.body.conditionType || 'NORMAL'
      ]
    );
    res.redirect('/profile/mission-settings?missionSettingsSaved=true');
  } catch (error) {
    next(error);
  }
});

app.post('/profile/delete-account', requireLogin, async (req, res, next) => {
  try {
    if (req.body.confirmation === '탈퇴') {
      await pool.query('delete from users where id = $1', [req.session.user.id]);
      req.session.destroy(() => res.redirect('/'));
      return;
    }
    res.redirect('/profile');
  } catch (error) {
    next(error);
  }
});

app.get('/api/daily-missions', requireLogin, async (req, res, next) => {
  try {
    res.json(await missionPayload(req.session.user.id));
  } catch (error) {
    next(error);
  }
});

app.post('/api/daily-missions/:missionKey/complete', requireLogin, async (req, res, next) => {
  try {
    const date = todayIso();
    const missionKey = req.params.missionKey;
    if (!missions.some(([key]) => key === missionKey)) {
      res.json(await missionPayload(req.session.user.id));
      return;
    }
    const inserted = await pool.query(
      'insert into daily_mission_completions (user_id, mission_date, mission_key) values ($1, $2, $3) on conflict do nothing returning mission_key',
      [req.session.user.id, date, missionKey]
    );
    if (inserted.rowCount > 0) {
      await pool.query(
        'insert into daily_mission_days (user_id, mission_date, success_count) values ($1, $2, 1) on conflict (user_id, mission_date) do update set success_count = least(daily_mission_days.success_count + 1, 5), updated_at = now()',
        [req.session.user.id, date]
      );
      await pool.query(
        'insert into user_progress (user_id, total_xp) values ($1, 20) on conflict (user_id) do update set total_xp = user_progress.total_xp + 20, updated_at = now()',
        [req.session.user.id]
      );
    }
    res.json(await missionPayload(req.session.user.id));
  } catch (error) {
    next(error);
  }
});

app.post('/api/daily-missions/slots/:slotIndex/reroll', requireLogin, async (req, res, next) => {
  try {
    res.json(await missionPayload(req.session.user.id));
  } catch (error) {
    next(error);
  }
});

app.get('/api/daily-missions/calendar', requireLogin, async (req, res, next) => {
  try {
    const year = Number(req.query.year);
    const month = Number(req.query.month);
    const start = `${year}-${String(month).padStart(2, '0')}-01`;
    const result = await pool.query(
      "select to_char(created_at at time zone 'Asia/Seoul', 'YYYY-MM-DD') as date, emotion_label, emotion_color, count(*) from emotion_map_markers where user_id = $1 and created_at >= $2::date and created_at < ($2::date + interval '1 month') group by date, emotion_label, emotion_color order by date, count(*) desc",
      [req.session.user.id, start]
    );
    const seen = new Set();
    res.json(result.rows.filter((row) => {
      if (seen.has(row.date)) return false;
      seen.add(row.date);
      return true;
    }).map((row) => ({ date: row.date, emotionLabel: row.emotion_label, emotionColor: row.emotion_color })));
  } catch (error) {
    next(error);
  }
});

app.get('/api/emotion-map-markers', requireLogin, async (req, res, next) => {
  try {
    const result = await pool.query(
      'select m.*, u.nickname as author_nickname, m.user_id = $1 as own from emotion_map_markers m join users u on u.id = m.user_id order by m.created_at desc, m.id desc',
      [req.session.user.id]
    );
    res.json(result.rows.map(markerSummary));
  } catch (error) {
    next(error);
  }
});

app.post('/api/emotion-map-markers', requireLogin, async (req, res, next) => {
  try {
    const emotion = byLabel(req.body.emotionLabel) || emotions[0];
    const result = await pool.query(
      'insert into emotion_map_markers (user_id, latitude, longitude, emotion_label, emotion_color, title, location_name, description) values ($1, $2, $3, $4, $5, $6, $7, $8) returning *',
      [req.session.user.id, req.body.latitude, req.body.longitude, emotion.label, emotion.color, req.body.title || '감정 기록', req.body.locationName || '선택한 위치', req.body.description || null]
    );
    const row = { ...result.rows[0], author_nickname: req.session.user.nickname, own: true };
    res.json(markerSummary(row));
  } catch (error) {
    next(error);
  }
});

app.get('/api/emotion-map-markers/:id', requireLogin, async (req, res, next) => {
  try {
    const result = await markerDetail(req.params.id, req.session.user.id);
    if (!result) {
      res.sendStatus(404);
      return;
    }
    res.json(result);
  } catch (error) {
    next(error);
  }
});

app.put('/api/emotion-map-markers/:id', requireLogin, async (req, res, next) => {
  try {
    const emotion = byLabel(req.body.emotionLabel) || emotions[0];
    await pool.query(
      'update emotion_map_markers set latitude = $1, longitude = $2, emotion_label = $3, emotion_color = $4, title = $5, location_name = $6, description = $7, updated_at = now() where id = $8 and user_id = $9',
      [req.body.latitude, req.body.longitude, emotion.label, emotion.color, req.body.title, req.body.locationName, req.body.description || null, req.params.id, req.session.user.id]
    );
    res.json(await markerDetail(req.params.id, req.session.user.id));
  } catch (error) {
    next(error);
  }
});

app.delete('/api/emotion-map-markers/:id', requireLogin, async (req, res, next) => {
  try {
    await pool.query('delete from emotion_map_markers where id = $1 and user_id = $2', [req.params.id, req.session.user.id]);
    res.sendStatus(204);
  } catch (error) {
    next(error);
  }
});

app.post('/api/emotion-map-markers/:id/likes', requireLogin, async (req, res, next) => {
  try {
    const inserted = await pool.query(
      'insert into likes (record_id, user_id) values ($1, $2) on conflict do nothing returning id',
      [req.params.id, req.session.user.id]
    );
    let liked = inserted.rowCount > 0;
    if (!liked) {
      await pool.query('delete from likes where record_id = $1 and user_id = $2', [req.params.id, req.session.user.id]);
    }
    const count = await pool.query('select count(*)::int as count from likes where record_id = $1', [req.params.id]);
    res.json({ liked, likeCount: count.rows[0].count });
  } catch (error) {
    next(error);
  }
});

app.get('/api/emotion-map-markers/:id/comments', requireLogin, async (req, res, next) => {
  try {
    const result = await pool.query(
      'select c.*, u.nickname as author_nickname, c.user_id = $1 as own from comments c join users u on u.id = c.user_id where c.record_id = $2 order by c.created_at asc, c.id asc',
      [req.session.user.id, req.params.id]
    );
    res.json(result.rows.map(commentSummary));
  } catch (error) {
    next(error);
  }
});

app.post('/api/emotion-map-markers/:id/comments', requireLogin, async (req, res, next) => {
  try {
    const result = await pool.query(
      'insert into comments (record_id, user_id, content) values ($1, $2, $3) returning *',
      [req.params.id, req.session.user.id, String(req.body.content || '').trim()]
    );
    res.json(commentSummary({ ...result.rows[0], author_nickname: req.session.user.nickname, own: true }));
  } catch (error) {
    next(error);
  }
});

app.get('/api/emotions/summary', requireLogin, async (req, res, next) => {
  try {
    const result = await pool.query(
      'select emotion_label, count(*)::int as count from emotion_map_markers where user_id = $1 group by emotion_label order by count desc',
      [req.session.user.id]
    );
    res.json(result.rows.map((row) => ({ label: row.emotion_label, count: row.count })));
  } catch (error) {
    next(error);
  }
});

app.get('/api/spots/liked', requireLogin, async (req, res, next) => {
  try {
    const result = await pool.query(
      'select m.*, count(l.id)::int as like_count from emotion_map_markers m join likes l on l.record_id = m.id group by m.id order by like_count desc, m.created_at desc limit 5'
    );
    res.json(result.rows.map((row) => ({
      name: row.location_name,
      lat: Number(row.latitude),
      lng: Number(row.longitude),
      emotion: byLabel(row.emotion_label)?.icon || '•',
      emotionLabel: row.emotion_label,
      likeCount: row.like_count,
      description: row.description
    })));
  } catch (error) {
    next(error);
  }
});

app.get('/api/spots/healing', requireLogin, async (req, res, next) => {
  try {
    const lat = Number(req.query.lat || 0);
    const lng = Number(req.query.lng || 0);
    const positiveLabels = emotions.filter((emotion) => emotion.positive).map((emotion) => emotion.label);
    const result = await pool.query('select * from emotion_map_markers where emotion_label = any($1) order by created_at desc limit 10', [positiveLabels]);
    res.json(result.rows.map((row) => ({
      name: row.location_name,
      lat: Number(row.latitude),
      lng: Number(row.longitude),
      emotion: byLabel(row.emotion_label)?.icon || '•',
      distance: distanceText(lat, lng, Number(row.latitude), Number(row.longitude))
    })));
  } catch (error) {
    next(error);
  }
});

function markerSummary(row) {
  return {
    id: row.id,
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    authorNickname: row.author_nickname,
    emotionLabel: row.emotion_label,
    emotionColor: row.emotion_color,
    title: row.title,
    locationName: row.location_name,
    description: row.description,
    createdAt: row.created_at,
    own: row.own
  };
}

async function markerDetail(id, userId) {
  const result = await pool.query(
    'select m.*, u.nickname as author_nickname, m.user_id = $1 as own, count(distinct l.id)::int as like_count, count(distinct c.id)::int as comment_count, bool_or(ml.user_id is not null) as liked_by_me from emotion_map_markers m join users u on u.id = m.user_id left join likes l on l.record_id = m.id left join comments c on c.record_id = m.id left join likes ml on ml.record_id = m.id and ml.user_id = $1 where m.id = $2 group by m.id, u.nickname',
    [userId, id]
  );
  const row = result.rows[0];
  if (!row) return null;
  return {
    ...markerSummary(row),
    userId: row.user_id,
    updatedAt: row.updated_at,
    likeCount: row.like_count,
    commentCount: row.comment_count,
    likedByMe: Boolean(row.liked_by_me)
  };
}

function commentSummary(row) {
  return {
    id: row.id,
    recordId: row.record_id,
    userId: row.user_id,
    authorNickname: row.author_nickname,
    content: row.content,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    own: row.own
  };
}

function distanceText(aLat, aLng, bLat, bLng) {
  if (![aLat, aLng, bLat, bLng].every(Number.isFinite)) return '거리 알 수 없음';
  const toRad = (value) => value * Math.PI / 180;
  const earth = 6371000;
  const dLat = toRad(bLat - aLat);
  const dLng = toRad(bLng - aLng);
  const x = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(aLat)) * Math.cos(toRad(bLat)) * Math.sin(dLng / 2) ** 2;
  const meters = earth * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${Math.round(meters)}m`;
}

app.use((error, req, res, _next) => {
  console.error(error);
  if (req.path.startsWith('/api/')) {
    res.status(500).json({ message: 'server error' });
    return;
  }
  res.status(500).send('서버 오류가 발생했습니다.');
});

app.listen(port, () => {
  console.log(`LastSys Node/PostgreSQL app listening on http://localhost:${port}`);
});
