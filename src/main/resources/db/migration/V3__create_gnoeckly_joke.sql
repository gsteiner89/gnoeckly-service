create table gnoeckly_joke
(
    id               uuid                        not null,
    created_at       timestamp(6) with time zone not null,
    created_by       uuid,
    updated_at       timestamp(6) with time zone not null,
    updated_by       uuid,
    version          bigint,
    deleted_at       timestamp(6) with time zone,
    tenant_id        uuid                        not null,
    author_id        uuid                        not null,
    category_id      uuid                        not null,
    title            varchar(120),
    text             varchar(2000)               not null,
    status           varchar(20)                 not null,
    upvotes          integer                     not null default 0,
    downvotes        integer                     not null default 0,
    score            integer                     not null default 0,
    hot_score        double precision            not null default 0,
    approved_at      timestamp(6) with time zone,
    approved_by      uuid,
    rejection_reason varchar(500),
    boosted_until    timestamp(6) with time zone,
    primary key (id)
);

alter table gnoeckly_joke
    add constraint fk_gnoeckly_joke_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_joke
    add constraint fk_gnoeckly_joke_author foreign key (author_id) references midgard_user;
alter table gnoeckly_joke
    add constraint fk_gnoeckly_joke_category foreign key (category_id) references gnoeckly_joke_category;
alter table gnoeckly_joke
    add constraint chk_gnoeckly_joke_status check (status in ('PENDING', 'APPROVED', 'REJECTED'));

-- Feed-Sortierungen (nur freigegebene, nicht geloeschte Witze).
create index ix_gnoeckly_joke_hot on gnoeckly_joke (status, hot_score desc) where deleted_at is null;
create index ix_gnoeckly_joke_top on gnoeckly_joke (status, approved_at, score desc) where deleted_at is null;
create index ix_gnoeckly_joke_new on gnoeckly_joke (status, approved_at desc) where deleted_at is null;
create index ix_gnoeckly_joke_author on gnoeckly_joke (author_id, status);
create index ix_gnoeckly_joke_category on gnoeckly_joke (category_id);

create table gnoeckly_joke_aud
(
    id               uuid    not null,
    rev              integer not null,
    revtype          smallint,
    created_at       timestamp(6) with time zone,
    created_by       uuid,
    updated_at       timestamp(6) with time zone,
    updated_by       uuid,
    version          bigint,
    deleted_at       timestamp(6) with time zone,
    tenant_id        uuid,
    author_id        uuid,
    category_id      uuid,
    title            varchar(120),
    text             varchar(2000),
    status           varchar(20),
    upvotes          integer,
    downvotes        integer,
    score            integer,
    hot_score        double precision,
    approved_at      timestamp(6) with time zone,
    approved_by      uuid,
    rejection_reason varchar(500),
    boosted_until    timestamp(6) with time zone,
    primary key (id, rev)
);
alter table gnoeckly_joke_aud
    add constraint fk_gnoeckly_joke_aud_rev foreign key (rev) references revinfo;
