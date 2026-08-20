-- Initial outfit schema (SPEC.md section 4). Outfits store positions only; the
-- client composites the actual image.

create table outfits (
    id         uuid not null,
    owner_id   uuid not null,
    avatar_id  uuid not null,
    name       varchar(120),
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,

    constraint pk_outfits primary key (id)
);

create index idx_outfit_owner on outfits (owner_id);

create table outfit_layers (
    outfit_id  uuid        not null,
    garment_id uuid        not null,
    category   varchar(16) not null,
    z_index    integer     not null,
    offset_x   real,
    offset_y   real,
    scale      real,

    constraint fk_outfit_layers_outfit foreign key (outfit_id) references outfits (id) on delete cascade,
    constraint ck_outfit_layer_category check (category in ('TOP', 'BOTTOM', 'SOCKS', 'LAYER', 'SHOES', 'ACCESSORY'))
);

create index idx_outfit_layers_outfit on outfit_layers (outfit_id);

-- garment_id deliberately carries no foreign key: garments live in another
-- service's schema. See docs/ADR.md (ADR-008) for how that boundary is handled.
