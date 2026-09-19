create table gnoeckly_user_sticker
(
    id              uuid                        not null,
    created_at      timestamp(6) with time zone not null,
    created_by      uuid,
    updated_at      timestamp(6) with time zone not null,
    updated_by      uuid,
    version         bigint,
    deleted_at      timestamp(6) with time zone,
    tenant_id       uuid                        not null,
    user_id         uuid                        not null,
    sticker_id      uuid                        not null,
    quantity        integer                     not null default 0,
    purchased_total integer                     not null default 0,
    primary key (id)
);

alter table gnoeckly_user_sticker
    add constraint fk_gnoeckly_user_sticker_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_user_sticker
    add constraint fk_gnoeckly_user_sticker_user foreign key (user_id) references midgard_user;
alter table gnoeckly_user_sticker
    add constraint fk_gnoeckly_user_sticker_sticker foreign key (sticker_id) references gnoeckly_sticker;
alter table gnoeckly_user_sticker
    add constraint chk_gnoeckly_user_sticker_quantity check (quantity >= 0 and purchased_total >= quantity);

create unique index uq_gnoeckly_user_sticker_user_sticker on gnoeckly_user_sticker (user_id, sticker_id);

create table gnoeckly_user_sticker_aud
(
    id              uuid    not null,
    rev             integer not null,
    revtype         smallint,
    created_at      timestamp(6) with time zone,
    created_by      uuid,
    updated_at      timestamp(6) with time zone,
    updated_by      uuid,
    version         bigint,
    deleted_at      timestamp(6) with time zone,
    tenant_id       uuid,
    user_id         uuid,
    sticker_id      uuid,
    quantity        integer,
    purchased_total integer,
    primary key (id, rev)
);
alter table gnoeckly_user_sticker_aud
    add constraint fk_gnoeckly_user_sticker_aud_rev foreign key (rev) references revinfo;
