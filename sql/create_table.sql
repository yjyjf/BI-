# 数据库初始化


create database if not exists bi;

-- 切换库
use bi;

-- 用户表
-- auto-generated definition
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    email        varchar(128)                           not null comment '用户邮箱',
    totalNum     int          default 0                 not null comment '总使用次数',
    leftNum      int          default 0                 not null comment '剩余使用次数',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    userAvatar   varchar(1024)                          null comment '用户头像'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userAccount
    on user (userAccount);




-- 图表信息表
-- auto-generated definition
create table chart
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(128)                       null comment '图表名称',
    chartData   text                               null comment '图表数据',
    chartType   varchar(128)                       null comment '图表类型',
    genChart    text                               null comment '生成的图表数据',
    genResult   text                               null comment '生成的分析结论',
    userId      bigint                             null comment '创建用户 id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    goal        text                               null comment '分析目标',
    status      tinyint  default 0                 not null comment '任务状态字段(排队中0-wait、执行中1-running、已完成2-succeed、3-失败failed)',
    execMessage text                               null comment '执行信息'
)
    comment '图表信息表' collate = utf8mb4_unicode_ci;

