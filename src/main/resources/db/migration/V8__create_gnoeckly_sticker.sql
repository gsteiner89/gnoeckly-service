create table gnoeckly_sticker
(
    id                 uuid                        not null,
    created_at         timestamp(6) with time zone not null,
    created_by         uuid,
    updated_at         timestamp(6) with time zone not null,
    updated_by         uuid,
    version            bigint,
    deleted_at         timestamp(6) with time zone,
    tenant_id          uuid                        not null,
    slug               varchar(40)                 not null,
    name               varchar(80)                 not null,
    description        varchar(280),
    image_key          varchar(255),
    image_content_type varchar(100),
    price              bigint                      not null,
    active             boolean                     not null default true,
    stock_total        integer,
    stock_sold         integer                     not null default 0,
    available_from     timestamp(6) with time zone,
    available_until    timestamp(6) with time zone,
    sort_order         integer                     not null default 0,
    primary key (id)
);

alter table gnoeckly_sticker
    add constraint fk_gnoeckly_sticker_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_sticker
    add constraint chk_gnoeckly_sticker_price check (price >= 0);
alter table gnoeckly_sticker
    add constraint chk_gnoeckly_sticker_stock check (stock_sold >= 0 and (stock_total is null or stock_sold <= stock_total));

create unique index uq_gnoeckly_sticker_slug on gnoeckly_sticker (tenant_id, slug) where deleted_at is null;

create table gnoeckly_sticker_aud
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
    slug               varchar(40),
    name               varchar(80),
    description        varchar(280),
    image_key          varchar(255),
    image_content_type varchar(100),
    price              bigint,
    active             boolean,
    stock_total        integer,
    stock_sold         integer,
    available_from     timestamp(6) with time zone,
    available_until    timestamp(6) with time zone,
    sort_order         integer,
    primary key (id, rev)
);
alter table gnoeckly_sticker_aud
    add constraint fk_gnoeckly_sticker_aud_rev foreign key (rev) references revinfo;
