create table gnoeckly_joke_category
(
    id         uuid                        not null,
    created_at timestamp(6) with time zone not null,
    created_by uuid,
    updated_at timestamp(6) with time zone not null,
    updated_by uuid,
    version    bigint,
    deleted_at timestamp(6) with time zone,
    tenant_id  uuid                        not null,
    slug       varchar(40)                 not null,
    name       varchar(80)                 not null,
    icon       varchar(80),
    sort_order integer                     not null default 0,
    active     boolean                     not null default true,
    primary key (id)
);

alter table gnoeckly_joke_category
    add constraint fk_gnoeckly_joke_category_tenant foreign key (tenant_id) references midgard_tenant;

create unique index uq_gnoeckly_joke_category_slug on gnoeckly_joke_category (tenant_id, slug) where deleted_at is null;

create table gnoeckly_joke_category_aud
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
    slug       varchar(40),
    name       varchar(80),
    icon       varchar(80),
    sort_order integer,
    active     boolean,
    primary key (id, rev)
);
alter table gnoeckly_joke_category_aud
    add constraint fk_gnoeckly_joke_category_aud_rev foreign key (rev) references revinfo;
