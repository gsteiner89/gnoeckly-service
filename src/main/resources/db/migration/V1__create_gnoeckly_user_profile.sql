-- Oeffentliches Profil (Nickname/Bio) je midgard_user, Komposition per FK (midgard CLAUDE.md Regel 2).
create table gnoeckly_user_profile
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
    nickname   varchar(20)                 not null,
    bio        varchar(280),
    primary key (id)
);

alter table gnoeckly_user_profile
    add constraint fk_gnoeckly_user_profile_user foreign key (user_id) references midgard_user;
alter table gnoeckly_user_profile
    add constraint fk_gnoeckly_user_profile_tenant foreign key (tenant_id) references midgard_tenant;

create unique index uq_gnoeckly_user_profile_user on gnoeckly_user_profile (tenant_id, user_id) where deleted_at is null;
create unique index uq_gnoeckly_user_profile_nickname on gnoeckly_user_profile (tenant_id, lower(nickname)) where deleted_at is null;

-- Envers-Kopie: alle Nicht-PK-Spalten nullable (DEL-Revision traegt nur PK + rev + revtype),
-- FK auf midgards globale revinfo (V9 in midgard/db/migration, laeuft vor diesem Skript).
create table gnoeckly_user_profile_aud
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
    nickname   varchar(20),
    bio        varchar(280),
    primary key (id, rev)
);
alter table gnoeckly_user_profile_aud
    add constraint fk_gnoeckly_user_profile_aud_rev foreign key (rev) references revinfo;
