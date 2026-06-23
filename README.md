# 汽车投诉数据可视化分析 🚗📊

> 基于大数据技术的汽车投诉数据采集、清洗、多维分析及可视化系统  
> 适合大数据入门学习，覆盖 Hadoop/Spark/Spring Boot/ECharts 全栈实践

[![Tech Stack](https://img.shields.io/badge/Hadoop-3.1.3-blue)](https://hadoop.apache.org/)
[![Spark](https://img.shields.io/badge/Spark-3.4.2-orange)](https://spark.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)](https://spring.io/projects/spring-boot)
[![ECharts](https://img.shields.io/badge/ECharts-5.4.3-red)](https://echarts.apache.org/)
[![MariaDB](https://img.shields.io/badge/MariaDB-10.11.9-brown)](https://mariadb.org/)

---

## 📖 项目简介

本项目以 **4,700 条真实汽车消费者投诉数据** 为基础，构建了一条完整的大数据处理与可视化管线：

```
CSV → HDFS → Spark SQL(清洗+6维OLAP) → HDFS → MariaDB → Spring Boot API → ECharts 大屏
```

通过 **品牌、车系、问题类型、时间趋势** 等维度，直观展示汽车质量问题的分布规律，为消费者购车决策、制造商质量改进和监管部门市场监管提供数据支撑。

---

## 🏗️ 项目架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  数据采集层   │ → │  数据处理层   │ → │  数据存储层   │
│  HDFS 上传   │    │  Spark SQL   │    │   MariaDB    │
│  CSV → HDFS  │    │  清洗+分析   │    │  6张分析表   │
└──────────────┘    └──────────────┘    └──────────────┘
                                              ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  展示层       │ ← │  服务层       │ ← │              │
│  ECharts 6图 │    │  Spring Boot │    │              │
│  深色仪表盘   │    │  RESTful API │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
```

---

## 📂 项目结构

```
G:/大二下实训/
│
├── CLAUDE.md                          # 项目指导手册（8步全流程）
├── 汽车投诉数据(20240429).csv          # 原始数据（4,700条投诉，2.3MB）
├── 汽车投诉数据_readme.md              # 数据集元信息
│
├── car_complaint_analysis/            # 🧠 子项目一：Spark数据分析
│   ├── pom.xml                        #   Maven配置
│   └── src/main/scala/car/complaint/analysis/
│       ├── ComplaintDataClean.scala   #   数据清洗
│       ├── BrandAnalysis.scala        #   品牌分析
│       ├── SeriesAnalysis.scala       #   车系分析
│       ├── ProblemCategoryAnalysis.scala  # 问题大类分析
│       ├── ProblemTypeAnalysis.scala   #   问题类型分析
│       ├── BrandProblemCross.scala    #   品牌×问题交叉
│       └── DailyTrendAnalysis.scala   #   日趋势分析
│
├── car_complaint_web/                 # 🌐 子项目二：Web可视化系统
│   ├── pom.xml                        #   Spring Boot配置
│   └── src/main/
│       ├── java/car/complaint/web/
│       │   ├── ComplaintWebApplication.java   # 启动类
│       │   └── controller/
│       │       └── AnalysisController.java    # 6个REST API
│       └── resources/
│           ├── application.properties         # 数据源配置
│           └── static/
│               └── index.html                 # ECharts大屏
│
├── 大纲_数据清洗与分析.docx            # 📝 课程设计大纲一
└── 大纲_前后端开发.docx                # 📝 课程设计大纲二
```

---

## 🔧 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **分布式存储** | Hadoop HDFS (HA) | 3.1.3 | 原始数据与分析结果存储 |
| **计算引擎** | Apache Spark SQL | 3.4.2 | 数据清洗 + 6维OLAP分析 |
| **编程语言** | Scala | 2.12.17 | Spark原生语言 |
| **关系型数据库** | MariaDB | 10.11.9 | 分析结果存储与查询 |
| **后端框架** | Spring Boot | 2.7.18 | RESTful API服务 |
| **数据库连接** | JdbcTemplate + HikariCP | — | 轻量查询 + 连接池 |
| **前端可视化** | Apache ECharts | 5.4.3 | 6种图表 + 深色仪表盘 |
| **构建工具** | Maven | 3.9.11 | 依赖管理与JAR打包 |
| **集群协调** | ZooKeeper | 3.4.x | NameNode HA选主 |

---

## 🚀 快速开始

### 环境要求

- **虚拟机集群**：3 × CentOS 7（node1/2/3），Hadoop HA
- **开发机**：Windows 11，JDK 8，IntelliJ IDEA，Maven 3.9+
- **集群软件**：Hadoop 3.1.3，Spark 3.4.2，MariaDB 10.11.9

### Step 1 — 启动集群

```bash
# 三台节点执行
zkServer.sh start
hdfs --daemon start journalnode

# node1 执行
start-dfs.sh && start-yarn.sh
systemctl start mariadb

# 验证
hdfs dfsadmin -report
yarn node -list
```

### Step 2 — 上传数据

```bash
hdfs dfs -mkdir -p /car_complaint/input /car_complaint/output
hdfs dfs -put 汽车投诉数据\(20240429\).csv /car_complaint/input/
```

### Step 3 — 数据分析

```bash
# 编译打包
cd car_complaint_analysis
mvn clean package -DskipTests

# 传输到虚拟机并执行
scp target/*.jar hadoop@node1:/export/
ssh hadoop@node1 "cd /export && spark-submit \
  --class car.complaint.analysis.ComplaintDataClean \
  --master local[2] car_complaint_analysis-1.0-SNAPSHOT.jar"
```

### Step 4 — 数据入库

```sql
-- MariaDB 建表（详见 car_complaint_web 项目）
CREATE DATABASE car_complaint DEFAULT CHARSET utf8mb4;
-- 创建6张分析表...
-- LOAD DATA LOCAL INFILE 导入CSV...
```

### Step 5 — 启动Web

```bash
cd car_complaint_web
# IDEA 中运行 ComplaintWebApplication.java
# 或: mvn spring-boot:run
```

浏览器访问 `http://localhost:8080` 查看可视化大屏。

---

## 📊 分析维度与数据

| 分析维度 | Scala类 | 数据量 | 关键发现 |
|----------|---------|--------|----------|
| 🔴 **品牌投诉** | BrandAnalysis | 118品牌 | 比亚迪613条最多 |
| 🟠 **车系投诉** | SeriesAnalysis | 670车系 | 头部车系集中度高 |
| 🟡 **问题大类** | ProblemCategoryAnalysis | 19大类 | 车身问题占比最高 |
| 🟢 **问题类型** | ProblemTypeAnalysis | 110类型 | 异响+部件开裂最常见 |
| 🔵 **品牌×问题** | BrandProblemCross | 733关联 | 各品牌问题分布差异化 |
| 🟣 **日趋势** | DailyTrendAnalysis | 92天 | 日投诉量存在波动 |

---

## 📡 API 接口

| 接口 | 返回数据 | 对应图表 |
|------|----------|----------|
| `GET /api/brand/top20` | 投诉品牌TOP20 | 横向柱状图 |
| `GET /api/series/top15` | 投诉车系TOP15 | 横向柱状图 |
| `GET /api/problem/category` | 问题大类分布 | 玫瑰饼图 |
| `GET /api/problem/type` | 问题类型TOP20 | 柱状图 |
| `GET /api/brand/problem/cross` | 品牌×问题交叉矩阵 | 热力图 |
| `GET /api/trend/daily` | 日投诉量趋势 | 折线图 |

---

## 🛠️ 问题排查记录

| # | 问题 | 原因 | 解决方案 |
|---|------|------|----------|
| 1 | HDFS 上传阻塞 | NameNode Safe Mode | `hdfs dfsadmin -safemode leave` |
| 2 | YARN ACCEPTED 卡死 | RM 调度未分配资源 | 改用 `--master local[2]` |
| 3 | JDK 21 编译报错 | Scala 2.12 不兼容 JDK 21 | 降级至 JDK 8 |
| 4 | MariaDB 连接超时 | 防火墙阻挡 3306 端口 | `firewall-cmd --add-port=3306/tcp` |
| 5 | ECharts 热力图空白 | 数据格式不匹配 | 前端转换矩阵为三元组 |
| 6 | CSV header 被当数据 | Spark 输出含表头行 | 移除 `.option("header","true")` |

---

## 📝 文档

- [CLAUDE.md](CLAUDE.md) — 项目全流程指导手册
- [大纲_数据清洗与分析.docx](大纲_数据清洗与分析.docx) — 数据分析大纲
- [大纲_前后端开发.docx](大纲_前后端开发.docx) — Web开发大纲
- [汽车投诉数据_readme.md](汽车投诉数据_readme.md) — 数据集元信息

---

## 📖 学习指南

想复现本项目或学习大数据全栈开发？查看 [docs/](docs/) 目录：

| 文档 | 内容 |
|------|------|
| [环境搭建指南](docs/01-环境搭建指南.md) | Hadoop HA集群 + Spark + MariaDB安装配置 |
| [数据分析教程](docs/02-Spark数据分析教程.md) | 从CSV到6维OLAP分析的完整Scala代码讲解 |
| [Web开发教程](docs/03-Spring-Boot-ECharts教程.md) | 后端API开发 + 前端大屏构建全流程 |
| [常见问题排查](docs/04-常见问题排查.md) | 6个经典报错及解决方案 |

## 📄 许可

MIT License — 自由使用、修改、分发
