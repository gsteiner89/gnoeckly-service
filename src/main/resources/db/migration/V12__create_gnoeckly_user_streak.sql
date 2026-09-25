create table gnoeckly_user_streak
(
    id                     uuid                        not null,
    created_at             timestamp(6) with time zone not null,
    created_by             uuid,
    updated_at             timestamp(6) with time zone not null,
    updated_by             uuid,
    version                bigint,
    deleted_at             timestamp(6) with time zone,
    tenant_id              uuid                        not null,
    user_id                uuid                        not null,
    current_streak         integer                     not null default 0,
    longest_streak         integer                     not null default 0,
    last_active_date       date,
    freezes                integer                     not null default 0,
    last_milestone_claimed integer                     not null default 0,
    primary key (id)
);

alter table gnoeckly_user_streak
    add constraint fk_gnoeckly_user_streak_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_user_streak
    add constraint fk_gnoeckly_user_streak_user foreign key (user_id) references midgard_user;
alter table gnoeckly_user_streak
    add constraint chk_gnoeckly_user_streak_counts check (current_streak >= 0 and longest_streak >= current_streak
        and freezes >= 0 and last_milestone_claimed >= 0);

create unique index uq_gnoeckly_user_streak_user on gnoeckly_user_streak (user_id);

create table gnoeckly_user_streak_aud
(
    id                     uuid    not null,
    rev                    integer not null,
    revtype                smallint,
    created_at             timestamp(6) with time zone,
    created_by             uuid,
    updated_at             timestamp(6) with time zone,
    updated_by             uuid,
    version                bigint,
    deleted_at             timestamp(6) with time zone,
    tenant_id              uuid,
    user_id                uuid,
    current_streak         integer,
    longest_streak         integer,
    last_active_date       date,
    freezes                integer,
    last_milestone_claimed integer,
    primary key (id, rev)
);
alter table gnoeckly_user_streak_aud
    add constraint fk_gnoeckly_user_streak_aud_rev foreign key (rev) references revinfo;

-- Neue Buchungstypen fuer Streak-Meilensteine und Streak-Freeze (V7 listet die erlaubten Werte hart).
alter table gnoeckly_coin_transaction
    drop constraint chk_gnoeckly_coin_transaction_type;
alter table gnoeckly_coin_transaction
    add constraint chk_gnoeckly_coin_transaction_type check (type in
        ('AD_REWARD', 'SUBMIT_FEE', 'BOOST', 'STICKER_PURCHASE', 'JOKE_APPROVED', 'WELCOME', 'ADMIN_ADJUSTMENT',
         'STREAK_REWARD', 'STREAK_FREEZE'));

-- Belohnungs-Sticker (Streak-Meilensteine) sind nicht im Marktplatz kaeuflich.
alter table gnoeckly_sticker
    add column purchasable boolean not null default true;
alter table gnoeckly_sticker_aud
    add column purchasable boolean;
