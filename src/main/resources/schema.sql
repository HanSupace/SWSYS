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
