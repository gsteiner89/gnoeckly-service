create table gnoeckly_wallet
(
    id         uuid                        not null,
    created_at timestamp(6) with time zone not null,
    created_by uuid,
    updated_at timestamp(6) with time zone not null,
    updated_by uuid,
    version    bigint,
    deleted_at timestamp(6) with time zone,
    tenant_id  uuid                        not null,
    user_id    uuid                        not null,
    balance    bigint                      not null default 0,
    primary key (id)
);

alter table gnoeckly_wallet
    add constraint fk_gnoeckly_wallet_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_wallet
    add constraint fk_gnoeckly_wallet_user foreign key (user_id) references midgard_user;
alter table gnoeckly_wallet
    add constraint chk_gnoeckly_wallet_balance check (balance >= 0);

create unique index uq_gnoeckly_wallet_user on gnoeckly_wallet (tenant_id, user_id);

create table gnoeckly_wallet_aud
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
    user_id    uuid,
    balance    bigint,
    primary key (id, rev)
);
alter table gnoeckly_wallet_aud
    add constraint fk_gnoeckly_wallet_aud_rev foreign key (rev) references revinfo;
