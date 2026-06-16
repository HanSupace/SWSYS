create table if not exists users (
    id bigserial primary key,
    username varchar(30) not null unique,
    password_hash varchar(100) not null,
    nickname varchar(20) not null unique,
    created_at timestamptz not null default now()
);

create table if not exists daily_mission_days (
    user_id bigint not null references users (id) on delete cascade,
    mission_date date not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    success_count int not null default 0,
    primary key (user_id, mission_date)
);

create table if not exists daily_mission_completions (
    user_id bigint not null references users (id) on delete cascade,
    mission_date date not null,
    mission_key varchar(60) not null,
    completed_at timestamptz not null default now(),
    primary key (user_id, mission_date, mission_key)
);

create table if not exists daily_mission_slot_rerolls (
    user_id bigint not null references users (id) on delete cascade,
    mission_date date not null,
    slot_index int not null,
    reroll_count int not null default 0,
    updated_at timestamptz not null default now(),
    primary key (user_id, mission_date, slot_index)
);

create table if not exists user_mission_settings (
    user_id bigint primary key references users (id) on delete cascade,
    mission_mode varchar(20) not null default 'PLAIN',
    life_stage varchar(30) not null default 'ANY',
    environment_type varchar(30) not null default 'ANY',
    condition_type varchar(30) not null default 'NORMAL',
    updated_at timestamptz not null default now()
);

create table if not exists user_progress (
    user_id bigint primary key references users (id) on delete cascade,
    total_xp int not null default 0,
    updated_at timestamptz not null default now()
);

create table if not exists emotion_map_markers (
    id bigserial primary key,
    user_id bigint not null references users (id) on delete cascade,
    latitude numeric(10, 7) not null,
    longitude numeric(10, 7) not null,
    emotion_label varchar(20) not null,
    emotion_color varchar(20) not null,
    title varchar(24) not null,
    location_name varchar(80) not null,
    description text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_emotion_map_markers_user_created
    on emotion_map_markers (user_id, created_at);

create table if not exists likes (
    id bigserial primary key,
    record_id bigint not null references emotion_map_markers (id) on delete cascade,
    user_id bigint not null references users (id) on delete cascade,
    created_at timestamptz not null default now(),
    unique (record_id, user_id)
);

create index if not exists idx_likes_record on likes (record_id);

create table if not exists comments (
    id bigserial primary key,
    record_id bigint not null references emotion_map_markers (id) on delete cascade,
    user_id bigint not null references users (id) on delete cascade,
    content text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_comments_record_created
    on comments (record_id, created_at);

create table if not exists user_reports (
    id bigserial primary key,
    user_id bigint not null references users (id) on delete cascade,
    location_name varchar(80) not null,
    title varchar(60) not null,
    content text not null,
    category varchar(30) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_user_reports_user_created
    on user_reports (user_id, created_at);
