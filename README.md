基于 Spring Boot + MQ + AIGC 的智能数据分析平台。区别于传统 BI，用户只需要导入原始数据集、并输入分析诉求，就能自动生成可视化图表及分析结论，降低数据分析的人工成本、提高数据分析效率。

## 项目背景

1. 基于AI快速发展的时代，AI + 程序员 = 无限可能。

2. 传统数据分析流程繁琐：传统的数据分析过程需要经历繁琐的数据处理和可视化操作，耗时且复杂。

3. 技术要求高：传统数据分析需要数据分析者具备一定的技术和专业知识，限制了非专业人士的参与。

4. 人工成本高：传统数据分析需要大量的人力投入，成本昂贵。

5. AI自动生成图表和分析结论：该项目利用AI技术，只需导入原始数据和输入分析目标，即可自动生成符合要求的图表和分析结论。、

6. 提高效率降低成本：通过项目的应用，能够大幅降低人工数据分析成本，提高数据分析的效率和准确性。

### 后端

- Spring Boot 2.7.2
- Spring MVC
- MyBatis + MyBatis Plus 数据访问（开启分页）
- Spring Boot 调试工具和项目处理器
- Spring AOP 切面编程：鉴权、记录日志、使用次数限制
- Spring Scheduler 定时任务
- Spring 事务注解
- Redis：Redisson限流控制
- MyBatis-Plus 数据库访问结构
- IDEA插件 MyBatisX ： 根据数据库表自动生成
- RabbitMQ：消息队列
- 讯飞星火api：ai分析接口
- JDK 线程池及异步化
- Swagger + Knife4j 项目文档
- Guava retrying 任务重试机制

### 数据存储

- MySQL 数据库
- Redis 内存数据库

### 工具类

- Easy Excel 表格处理
- Hutool 工具库
- Apache Commons Lang3 工具类
- Lombok 注解

### 业务特性

- Spring Session Redis 分布式登录
- 全局请求响应拦截器（记录日志）
- 全局异常处理器
- 自定义错误码
- 封装通用响应类
- Swagger + Knife4j 接口文档
- 自定义权限注解 + 全局校验
- 全局跨域处理
- 长整数丢失精度解决
- 多环境配置



## 快速上手

### 安装环境

rabbitmq  redis  mysql  java8 

### 前端

打开命令行输入：

`yarn` 或 `npm install` 安装依赖包

`npm run dev`运行

### 后端

修改 `application.yml` 中的信息，如mysql、rabbitmq、redis、ai配置

刷新 `pom.xml`

修改`resources/config/mail.setting` 中的邮箱信息

运行一次'BiInitMain`

启动项目
