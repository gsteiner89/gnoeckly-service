create table gnoeckly_joke_vote
(
    id         uuid                        not null,
    created_at timestamp(6) with time zone not null,
    created_by uuid,
    updated_at timestamp(6) with time zone not null,
    updated_by uuid,
    version    bigint,
    deleted_at timestamp(6) with time zone,
    tenant_id  uuid                        not null,
    joke_id    uuid                        not null,
    user_id    uuid                        not null,
    value      smallint                    not null,
    primary key (id)
);

alter table gnoeckly_joke_vote
    add constraint fk_gnoeckly_joke_vote_tenant foreign key (tenant_id) references midgard_tenant;
alter table gnoeckly_joke_vote
    add constraint fk_gnoeckly_joke_vote_joke foreign key (joke_id) references gnoeckly_joke;
alter table gnoeckly_joke_vote
    add constraint fk_gnoeckly_joke_vote_user foreign key (user_id) references midgard_user;
alter table gnoeckly_joke_vote
    add constraint chk_gnoeckly_joke_vote_value check (value in (-1, 1));

create unique index uq_gnoeckly_joke_vote_joke_user on gnoeckly_joke_vote (joke_id, user_id);
create index ix_gnoeckly_joke_vote_user on gnoeckly_joke_vote (user_id);

create table gnoeckly_joke_vote_aud
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
    joke_id    uuid,
    user_id    uuid,
    value      smallint,
    primary key (id, rev)
);
alter table gnoeckly_joke_vote_aud
    add constraint fk_gnoeckly_joke_vote_aud_rev foreign key (rev) references revinfo;
