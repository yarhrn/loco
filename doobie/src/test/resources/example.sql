create table increment_events
(
    id         varchar(36) not null,
    version    int         not null,
    created_at timestamp   not null,
    event      bytea       not null,
    primary key (id, version)
)