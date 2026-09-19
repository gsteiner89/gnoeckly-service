create table gnoeckly_joke_report
(
    id          uuid                        not null,
    created_at  timestamp(6) with time zone not null,
    created_by  uuid,
    updated_at  timestamp(6) with time zone not null,
    updated_by  uuid,
    version     bigint,
    deleted_at  timestamp(6) with time zone,
    tenant_id   uuid                        not null,
    joke_id     uuid                        not null,
    reporter_id uuid                        not null,
    reason      varchar(500)                not null,
    resolved_at timestamp(6) with time zone,
    primary key (id)
);

alter table gnoeckly_joke_report
    add constraint fk_gnoeckly_joke_report_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_joke_report
    add constraint fk_gnoeckly_joke_report_joke foreign key (joke_id) references gnoeckly_joke;
alter table gnoeckly_joke_report
    add constraint fk_gnoeckly_joke_report_reporter foreign key (reporter_id) references midgard_user;

create unique index uq_gnoeckly_joke_report_joke_reporter on gnoeckly_joke_report (joke_id, reporter_id);
create index ix_gnoeckly_joke_report_open on gnoeckly_joke_report (created_at) where resolved_at is null;

create table gnoeckly_joke_report_aud
(
    id          uuid    not null,
    rev         integer not null,
    revtype     smallint,
    created_at  timestamp(6) with time zone,
    created_by  uuid,
    updated_at  timestamp(6) with time zone,
    updated_by  uuid,
    version     bigint,
    deleted_at  timestamp(6) with time zone,
    tenant_id   uuid,
    joke_id     uuid,
    reporter_id uuid,
    reason      varchar(500),
    resolved_at timestamp(6) with time zone,
    primary key (id, rev)
);
alter table gnoeckly_joke_report_aud
    add constraint fk_gnoeckly_joke_report_aud_rev foreign key (rev) references revinfo;
