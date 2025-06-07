
---

# 🐴 黑马点评 · Java 后端程序员的成人礼

> “如果说黑马点评是一场修行，那它的终点，就是你梦想开始的地方。”✨

---

## 📌 项目简介

黑马点评，一个 Java 后端开发者几乎都听过甚至“跪过”的实战项目。  
从 Redis 缓存，到分布式锁，从登录认证到高并发秒杀。  
从 CRUD 小能手到高并发老狠人，就靠这一次蜕变！

> 本项目为黑马程序员出品的高并发点评系统实战练习，**完全遵循教学逻辑，还原真实开发**，是通往 Java 高级进阶的必经之路。

---

## 🚀 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot | 容器 + 框架核心 |
| MyBatis-Plus | ORM 框架 |
| Redis | 缓存、分布式锁、Geo定位等多种玩法 |
| MySQL | 核心数据存储 |
| Nginx | 静态资源 + 反向代理 |
| RabbitMQ（可扩展） | 秒杀异步处理（自拓展部分） |
| JMeter | 性能压测 |
| Docker（可拓展） | 微服务部署 |

---

## 🌟 项目亮点

### ✅ Redis 七连击：

- 缓存穿透：布隆过滤器、缓存空对象
- 缓存击穿：互斥锁、逻辑过期
- 缓存雪崩：设置过期时间 + 预热机制
- 热点数据预加载 + 延迟双删策略
- 分布式锁：`setIfAbsent` + Lua 脚本实现原子释放
- GEO 定位查询：附近店铺排序
- Redis 实现点赞、签到、关注功能

### ✅ 秒杀模块：

- 高并发限流处理
- 令牌桶/滑动窗口限流（可拓展）
- 异步处理秒杀订单
- Redis Stream 消息队列 + 消费确认机制

### ✅ 登录认证：

- 基于手机号验证码登录
- Token 实现登录态存储（Redis 实现分布式 Session）
- 拦截器统一身份验证

---

## 💻 环境要求

- JDK 1.8+
- Redis 6.0+
- MySQL 5.7+
- Maven 3.6+
- IDE：IntelliJ IDEA（推荐）
- Postman / Apifox / Swagger（接口调试工具）
- Linux / Windows 通用

---

## 🧭 项目启动指南

1. 克隆项目
   ```bash
   git clone https://github.com/你的用户名/Dark-Horse-Reviews.git

2. 导入数据库

数据库脚本在 docs/sql 目录下

创建数据库并执行 SQL 初始化脚本



3. 修改配置

配置文件：application.yml

设置 MySQL、Redis、端口等



4. 启动服务

mvn spring-boot:run


5. 访问首页

Vue 静态页面放置于 Nginx 或本地资源目录

前后端接口联调即可访问完整功能





---

## 📷 界面预览

> UI 基于黑马官方页面资源（Vue 前端），可通过资源目录访问。
<p align="center">
  <img src="https://github.com/user-attachments/assets/65f2ef80-dea4-46f3-bf12-ad9b281630fa?raw=true" width="300"/>
  <img src="https://github.com/user-attachments/assets/c488fcdd-355e-431b-98a4-3d0fa587629d?raw=true" width="300"/>
  <img src="https://github.com/user-attachments/assets/fa85ae1d-cf30-4a1c-94a5-e0cc153bb9d6?raw=true" width="300"/>
</p>








---

## 📚 教学来源

🧠 本项目为黑马程序员《Redis 高级特性与实战》课程配套练习项目。
如未学习推荐：去看！别犹豫！这是转型 Java 高级工程师的跳板！
📺[视频链接](https://www.bilibili.com/video/BV1cr4y1671t?spm_id_from=333.788.videopod.episodes&vd_source=d26867725b483fd1cfed871dcc83725c "快去学习吧")

---

## 😭 写在最后的鸡汤

> 那些在深夜里调试缓存更新策略、在 JMeter 压力测试中抓耳挠腮的日子，
终有一天会变成你面试时胸有成竹的资本。
所有你流过的汗，都会在 offer 上闪光！🌟



> 别怕难，挺过黑马点评，你就真的不再是 CRUD Boy 了！




---

📁 项目目录结构
blackhorse-review  
├── src  
│   ├── main  
│   │   ├── java  
│   │   │   └── com.hmdp  
│   │   │       ├── controller  # 控制器层（处理请求）  
│   │   │       ├── service     # 服务层（业务逻辑）  
│   │   │       ├── mapper      # 数据层（数据库操作）  
│   │   │       └── utils       # 工具类（自定义工具）  
│   │   └── resources  
│   │       ├── application.yml # 核心配置文件  
│   │       └── mapper          # MyBatis 映射文件  
├── docs  
│   └── sql                   # 数据库脚本（初始化、分库分表等）  
├── pom.xml                   # Maven 依赖配置  
└── README.md                 # 项目说明文档  


---

## 🙌 Star 一下，留下你的足迹！  
如果你也在黑马点评这条路上摔过跤、爬起来继续敲代码，  
那就点个 ⭐ **Star** ⭐ 吧，为曾经的努力按下一个保存键💾！


---

> 🔥“你可以哭，但不能退；你可以累，但不能怂。”


