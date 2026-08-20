-- Initial avatar schema (SPEC.md section 4).

create table avatars (
    id             uuid        not null,
    owner_id       uuid        not null,
    name           varchar(64),
    body_type      varchar(16) not null,

    -- Relative multipliers around 1.0 applied to the base silhouette.
    height         real,
    shoulder_width real,
    hip_width      real,

    constraint pk_avatars primary key (id),
    constraint ck_avatar_body_type check (body_type in ('SLIM', 'AVERAGE', 'CURVY', 'CUSTOM'))
);

create index idx_avatar_owner on avatars (owner_id);
