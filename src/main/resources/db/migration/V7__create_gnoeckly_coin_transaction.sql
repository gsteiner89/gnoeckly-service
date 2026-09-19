create table gnoeckly_coin_transaction
(
    id                 uuid                        not null,
    created_at         timestamp(6) with time zone not null,
    created_by         uuid,
    updated_at         timestamp(6) with time zone not null,
    updated_by         uuid,
    version            bigint,
    deleted_at         timestamp(6) with time zone,
    tenant_id          uuid                        not null,
    wallet_id          uuid                        not null,
    user_id            uuid                        not null,
    type               varchar(30)                 not null,
    amount             bigint                      not null,
    balance_after      bigint                      not null,
    reference_id       uuid,
    external_reference varchar(255),
    description        varchar(255),
    primary key (id)
);

alter table gnoeckly_coin_transaction
    add constraint fk_gnoeckly_coin_transaction_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_coin_transaction
    add constraint fk_gnoeckly_coin_transaction_wallet foreign key (wallet_id) references gnoeckly_wallet;
alter table gnoeckly_coin_transaction
    add constraint fk_gnoeckly_coin_transaction_user foreign key (user_id) references midgard_user;
alter table gnoeckly_coin_transaction
    add constraint chk_gnoeckly_coin_transaction_type check (type in
        ('AD_REWARD', 'SUBMIT_FEE', 'BOOST', 'STICKER_PURCHASE', 'JOKE_APPROVED', 'WELCOME', 'ADMIN_ADJUSTMENT'));

-- Idempotenz des AdMob-SSV-Callbacks: dieselbe transaction_id darf nur einmal gebucht werden.
create unique index uq_gnoeckly_coin_transaction_external_reference
    on gnoeckly_coin_transaction (external_reference) where external_reference is not null;
create index ix_gnoeckly_coin_transaction_user on gnoeckly_coin_transaction (user_id, created_at desc);

create table gnoeckly_coin_transaction_aud
(
    id                 uuid    not null,
    rev                integer not null,
    revtype            smallint,
    created_at         timestamp(6) with time zone,
    created_by         uuid,
    updated_at         timestamp(6) with time zone,
    updated_by         uuid,
    version            bigint,
    deleted_at         timestamp(6) with time zone,
    tenant_id          uuid,
    wallet_id          uuid,
    user_id            uuid,
    type               varchar(30),
    amount             bigint,
    balance_after      bigint,
    reference_id       uuid,
    external_reference varchar(255),
    description        varchar(255),
    primary key (id, rev)
);
alter table gnoeckly_coin_transaction_aud
    add constraint fk_gnoeckly_coin_transaction_aud_rev foreign key (rev) references revinfo;
