use hawkeye;

drop table if exists asset;
create table if not exists asset
(
    asset_id         bigint primary key auto_increment comment '主键ID',
    name             varchar(128)  not null comment '资产名称',
    request_method   tinyint       not null comment '请求方法: 0-GET,1-HEAD,2-POST,3-PUT,4-PATCH,5-DELETE,6-OPTIONS,7-TRACE',
    request_protocol varchar(10)   not null default 'https' comment '协议: http/https',
    request_host     varchar(255)  not null comment '请求主机(域名或IP)',
    request_port     smallint      not null comment '端口号, -1表示使用协议默认端口（应用层处理）',
    request_path     varchar(1024) not null default '/' comment '请求路径',
    request_header   json comment '请求头(JSON格式)',
    description      varchar(500) comment '资产描述',

    status           tinyint       not null default 1 comment '状态: 0-禁用, 1-启用, 2-弃用',
    risk_level       tinyint                default 0 comment '风险等级: 0-未知, 1-低, 2-中, 3-高',
    last_scan_time   datetime comment '最近扫描时间',

    tenant_id        bigint        not null default 1 comment '租户ID',
    deleted          tinyint       not null default 0 comment '逻辑删除: 0-未删除, 1-已删除',
    create_time      datetime      not null default CURRENT_TIMESTAMP comment '创建时间',
    update_time      datetime      not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    create_by        bigint comment '创建人ID',
    update_by        bigint comment '更新人ID',

    UNIQUE INDEX uk_tenant_asset (tenant_id, request_protocol, request_host, request_port, request_path(200)),
    INDEX idx_tenant_host (tenant_id, request_host(100)),
    INDEX idx_tenant_del_status (tenant_id, deleted, status),
    INDEX idx_tenant_scan_time (tenant_id, last_scan_time)
) default charset = utf8mb4
  collate = utf8mb4_unicode_ci
    comment ='资产表';
