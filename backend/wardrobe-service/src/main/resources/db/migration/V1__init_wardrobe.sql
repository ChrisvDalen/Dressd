-- Initial wardrobe schema (SPEC.md section 4).
-- Portable across Postgres (prod) and H2 in PostgreSQL mode (dev/test): enums are
-- stored as varchar with a check constraint rather than a native enum type.

create table garments (
    id          uuid          not null,
    owner_id    uuid          not null,
    category    varchar(16)   not null,
    image_url   varchar(1024) not null,
    color_tag   varchar(32),
    pattern     varchar(32),
    season      varchar(16),

    -- Normalised (0..1) anchor points used by the client canvas.
    shoulder_y  real,
    waist_y     real,
    hem_y       real,
    width_scale real,

    created_at  timestamp(6) with time zone not null,
    updated_at  timestamp(6) with time zone not null,

    constraint pk_garments primary key (id),
    constraint ck_garment_category check (category in ('TOP', 'BOTTOM', 'SOCKS', 'LAYER', 'SHOES', 'ACCESSORY')),
    constraint ck_garment_season check (season in ('SUMMER', 'WINTER', 'ALL'))
);

create index idx_garment_owner on garments (owner_id);
create index idx_garment_owner_category on garments (owner_id, category);
