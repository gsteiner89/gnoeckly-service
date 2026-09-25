create table gnoeckly_push_token
(
    id           uuid                        not null,
    created_at   timestamp(6) with time zone not null,
    created_by   uuid,
    updated_at   timestamp(6) with time zone not null,
    updated_by   uuid,
    version      bigint,
    deleted_at   timestamp(6) with time zone,
    tenant_id    uuid                        not null,
    user_id      uuid                        not null,
    token        varchar(512)                not null,
    platform     varchar(10)                 not null,
    last_seen_at timestamp(6) with time zone not null,
    primary key (id)
);

alter table gnoeckly_push_token
    add constraint fk_gnoeckly_push_token_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_push_token
    add constraint fk_gnoeckly_push_token_user foreign key (user_id) references midgard_user;
alter table gnoeckly_push_token
    add constraint chk_gnoeckly_push_token_platform check (platform in ('ANDROID', 'IOS'));

-- Ein Geraete-Token gehoert genau einem User; bei Konto-Wechsel am selben Geraet wird er umgehaengt.
create unique index uq_gnoeckly_push_token_token on gnoeckly_push_token (token);
create index ix_gnoeckly_push_token_user on gnoeckly_push_token (user_id);

create table gnoeckly_push_token_aud
(
    id           uuid    not null,
    rev          integer not null,
    revtype      smallint,
    created_at   timestamp(6) with time zone,
    created_by   uuid,
    updated_at   timestamp(6) with time zone,
    updated_by   uuid,
    version      bigint,
    deleted_at   timestamp(6) with time zone,
    tenant_id    uuid,
    user_id      uuid,
    token        varchar(512),
    platform     varchar(10),
    last_seen_at timestamp(6) with time zone,
    primary key (id, rev)
);
alter table gnoeckly_push_token_aud
    add constraint fk_gnoeckly_push_token_aud_rev foreign key (rev) references revinfo;
