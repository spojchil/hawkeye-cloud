use hawkeye;

drop table if exists task_item;
drop table if exists task;

create table if not exists task
(
    task_id         bigint primary key auto_increment comment '主键ID',
    task_name       varchar(200)   not null comment '任务名称',
    target_ids      varchar(2000)  not null comment '目标资产ID列表（逗号分隔）',
    vul_ids         varchar(2000)  not null comment '漏洞模板ID列表（逗号分隔）',
    status          tinyint        not null default 0 comment '状态: 0-待执行,1-分发中,2-执行中,3-已完成,4-已取消,5-异常终止',
    total_items     int            not null default 0 comment '检测项总数（拆分后回填）',
    completed_items int            not null default 0 comment '已完成数（轮询Redis回填）',
    failed_items    int            not null default 0 comment '失败数',
    priority        tinyint        not null default 1 comment '优先级: 0-高, 1-中, 2-低',
    start_time      datetime comment '拆分开始时间',
    end_time        datetime comment '最后一个item完成时间',
    result_summary  json comment '结果摘要（示例: {"matched":3,"failed":1,"unmatched":46}）',

    tenant_id       bigint         not null default 1 comment '租户ID',
    deleted         tinyint        not null default 0 comment '逻辑删除: 0-未删除, 1-已删除',
    create_time     datetime       not null default CURRENT_TIMESTAMP comment '创建时间',
    update_time     datetime       not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    create_by   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人用户名',
    update_by   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人用户名',

    index idx_tenant_status (tenant_id, status),
    index idx_tenant_name (tenant_id, task_name(100)),
    index idx_tenant_priority (tenant_id, priority, status)
) default charset = utf8mb4
  collate = utf8mb4_unicode_ci
    comment ='任务表';


drop table if exists task_item;
create table if not exists task_item
(
    item_id    bigint primary key auto_increment comment '主键ID',
    task_id    bigint  not null comment '所属任务ID',
    asset_id   bigint  not null comment '资产ID',
    vul_id     bigint  not null comment '漏洞模板ID',
    status     tinyint not null default 0 comment '状态: 0-待执行,1-成功,2-未匹配,3-失败',
    result     json comment '检测结果（detection-service通过Feign回写）',

    tenant_id  bigint  not null default 1 comment '租户ID',
    deleted    tinyint not null default 0 comment '逻辑删除: 0-未删除, 1-已删除',
    create_time datetime not null default CURRENT_TIMESTAMP comment '创建时间',
    update_time datetime not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    create_by  VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人用户名',
    update_by  VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人用户名',

    unique index uk_task_asset_vul (task_id, asset_id, vul_id),
    index idx_task (task_id),
    index idx_asset (asset_id),
    index idx_vul (vul_id),
    index idx_task_status (task_id, status),
    index idx_tenant_deleted (tenant_id, deleted)
) default charset = utf8mb4
  collate = utf8mb4_unicode_ci
    comment ='检测项表';
