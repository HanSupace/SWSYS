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
    success_count int not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    primary key (user_id, mission_date),
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

create table if not exists user_progress (
    user_id bigint not null,
    total_xp int not null default 0,
    updated_at timestamp not null default current_timestamp,
    primary key (user_id),
    constraint fk_user_progress_user foreign key (user_id) references users (id) on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
