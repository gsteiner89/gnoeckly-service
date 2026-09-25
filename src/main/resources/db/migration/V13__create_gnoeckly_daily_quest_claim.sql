create table gnoeckly_daily_quest_claim
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
    quest_key  varchar(20)                 not null,
    quest_date date                        not null,
    reward     bigint                      not null,
    primary key (id)
);

alter table gnoeckly_daily_quest_claim
    add constraint fk_gnoeckly_daily_quest_claim_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_daily_quest_claim
    add constraint fk_gnoeckly_daily_quest_claim_user foreign key (user_id) references midgard_user;

-- Eine Einloesung je User, Quest und Tag: schuetzt auch bei parallelen Requests vor Doppelbuchung.
create unique index uq_gnoeckly_daily_quest_claim_user_key_date
    on gnoeckly_daily_quest_claim (user_id, quest_key, quest_date);

create table gnoeckly_daily_quest_claim_aud
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
    quest_key  varchar(20),
    quest_date date,
    reward     bigint,
    primary key (id, rev)
);
alter table gnoeckly_daily_quest_claim_aud
    add constraint fk_gnoeckly_daily_quest_claim_aud_rev foreign key (rev) references revinfo;

alter table gnoeckly_coin_transaction
    drop constraint chk_gnoeckly_coin_transaction_type;
alter table gnoeckly_coin_transaction
    add constraint chk_gnoeckly_coin_transaction_type check (type in
        ('AD_REWARD', 'SUBMIT_FEE', 'BOOST', 'STICKER_PURCHASE', 'JOKE_APPROVED', 'WELCOME', 'ADMIN_ADJUSTMENT',
         'STREAK_REWARD', 'STREAK_FREEZE', 'DAILY_QUEST'));
