create table if not exists users (
    id bigint not null auto_increment,
    username varchar(30) not null,
    password_hash varchar(100) not null,
    nickname varchar(20) not null,
    created_at timestamp not null default current_timestamp,
    primary key (id),
    unique key uk_users_username (username),
    unique key uk_users_nickname (nickname)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists daily_mission_days (
    user_id bigint not null,
    mission_date date not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    primary key (user_id, mission_date),
    success_count int not null default 0,
    constraint fk_daily_mission_days_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists daily_mission_completions (
    user_id bigint not null,
    mission_date date not null,
    mission_key varchar(60) not null,
    completed_at timestamp not null default current_timestamp,
    primary key (user_id, mission_date, mission_key),
    constraint fk_daily_mission_completions_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists user_mission_settings (
    user_id bigint not null,
    mission_mode varchar(20) not null default 'PLAIN',
    life_stage varchar(30) not null default 'ANY',
    environment_type varchar(30) not null default 'ANY',
    condition_type varchar(30) not null default 'NORMAL',
    updated_at timestamp not null default current_timestamp,
    primary key (user_id),
    constraint fk_user_mission_settings_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists daily_mission_rerolls (
    user_id bigint not null,
    mission_date date not null,
    reroll_count int not null default 0,
    updated_at timestamp not null default current_timestamp,
    primary key (user_id, mission_date),
    constraint fk_daily_mission_rerolls_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists daily_mission_slot_rerolls (
    user_id bigint not null,
    mission_date date not null,
    slot_index int not null,
    reroll_count int not null default 0,
    updated_at timestamp not null default current_timestamp,
    primary key (user_id, mission_date, slot_index),
    constraint fk_daily_mission_slot_rerolls_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists user_progress (
    user_id bigint not null,
    total_xp int not null default 0,
    updated_at timestamp not null default current_timestamp,
    primary key (user_id),
    constraint fk_user_progress_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists emotion_map_markers (
    id bigint not null auto_increment,
    user_id bigint not null,
    latitude decimal(10, 7) not null,
    longitude decimal(10, 7) not null,
    emotion_label varchar(20) not null,
    emotion_color varchar(20) not null,
    title varchar(24) not null,
    location_name varchar(80) not null,
    description text,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    primary key (id),
    key idx_emotion_map_markers_user_created (user_id, created_at),
    constraint fk_emotion_map_markers_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists likes (
    id bigint not null auto_increment,
    record_id bigint not null,
    user_id bigint not null,
    created_at timestamp not null default current_timestamp,
    primary key (id),
    unique key uk_likes_record_user (record_id, user_id),
    key idx_likes_record (record_id),
    constraint fk_likes_record foreign key (record_id) references emotion_map_markers (id) on delete cascade,
    constraint fk_likes_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists comments (
    id bigint not null auto_increment,
    record_id bigint not null,
    user_id bigint not null,
    content text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    primary key (id),
    key idx_comments_record_created (record_id, created_at),
    constraint fk_comments_record foreign key (record_id) references emotion_map_markers (id) on delete cascade,
    constraint fk_comments_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists user_reports (
    id bigint not null auto_increment,
    user_id bigint not null,
    location_name varchar(80) not null,
    title varchar(60) not null,
    content text not null,
    category varchar(30) not null,
    created_at timestamp not null default current_timestamp,
    primary key (id),
    key idx_user_reports_user_created (user_id, created_at),
    constraint fk_user_reports_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

set @emotion_map_markers_updated_at_exists := (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'emotion_map_markers'
      and column_name = 'updated_at'
);
set @emotion_map_markers_updated_at_sql := if(
    @emotion_map_markers_updated_at_exists = 0,
    'alter table emotion_map_markers add column updated_at timestamp not null default current_timestamp',
    'select 1'
);
prepare emotion_map_markers_updated_at_statement from @emotion_map_markers_updated_at_sql;
execute emotion_map_markers_updated_at_statement;
deallocate prepare emotion_map_markers_updated_at_statement;

alter table emotion_map_markers modify description text;
alter table comments modify content text not null;
alter table user_reports modify content text not null;
