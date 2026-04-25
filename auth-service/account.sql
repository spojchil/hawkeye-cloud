use hawkeye;

drop table if exists account;
create table if not exists account
(
    account_id  bigint primary key auto_increment comment '主键ID',
    username    varchar(64) unique not null comment '账号名（唯一）',
    password    varchar(64)        not null comment '密码',
    tenant_id   bigint             not null default 1 comment '租户ID',
    deleted     tinyint            not null default 0 comment '逻辑删除: 0-未删除, 1-已删除',
    create_time datetime           not null default CURRENT_TIMESTAMP comment '创建时间',
    update_time datetime           not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    create_by   bigint comment '创建人ID',
    update_by   bigint comment '更新人ID',
    index idx_tenant (tenant_id),
    index idx_username_tenant_del (username, tenant_id, deleted)
) default charset = utf8mb4
  collate = utf8mb4_unicode_ci
    comment ='账号表';

# 密码是password
insert into account(username, password, tenant_id, create_time, update_time)
values ('admin', '$2a$10$gp1t.ArJv56l/aTNemMube9qdo1wHUhi5Fj2StYYhoPWK3QLg7jFS',
        1, now(), now());