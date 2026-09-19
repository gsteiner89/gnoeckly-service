create table gnoeckly_joke_sticker
(
    id         uuid                        not null,
    created_at timestamp(6) with time zone not null,
    created_by uuid,
    updated_at timestamp(6) with time zone not null,
    updated_by uuid,
    version    bigint,
    deleted_at timestamp(6) with time zone,
    tenant_id  uuid                        not null,
    joke_id    uuid                        not null,
    sticker_id uuid                        not null,
    giver_id   uuid                        not null,
    message    varchar(140),
    primary key (id)
);

alter table gnoeckly_joke_sticker
    add constraint fk_gnoeckly_joke_sticker_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_joke_sticker
    add constraint fk_gnoeckly_joke_sticker_joke foreign key (joke_id) references gnoeckly_joke;
alter table gnoeckly_joke_sticker
    add constraint fk_gnoeckly_joke_sticker_sticker foreign key (sticker_id) references gnoeckly_sticker;
alter table gnoeckly_joke_sticker
    add constraint fk_gnoeckly_joke_sticker_giver foreign key (giver_id) references midgard_user;

create index ix_gnoeckly_joke_sticker_joke on gnoeckly_joke_sticker (joke_id);
create index ix_gnoeckly_joke_sticker_giver on gnoeckly_joke_sticker (giver_id);

create table gnoeckly_joke_sticker_aud
(
    id         uuid    not null,
    rev        integer not null,
    revtype    smallint,
    created_at timestamp(6) with time zone,
    created_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    version    bigint,
    deleted_at timestamp(6) with time zone,
    tenant_id  uuid,
    joke_id    uuid,
    sticker_id uuid,
    giver_id   uuid,
    message    varchar(140),
    primary key (id, rev)
);
alter table gnoeckly_joke_sticker_aud
    add constraint fk_gnoeckly_joke_sticker_aud_rev foreign key (rev) references revinfo;
