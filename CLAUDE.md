# 汽车投诉数据可视化分析 — 项目指导手册

## 协作方式

本文件是项目全流程指导手册，Claude（我）将按以下方式引导你逐步完成实训：

1. **我发命令** — 每步我会给出需要在虚拟机上执行的命令
2. **你执行并反馈** — 你在虚拟机运行命令，把输出结果/截图发给我
3. **我确认并继续** — 我确认无误后，更新进度表打勾，进入下一步
4. **遇到问题我调试** — 如果运行出错，你把错误信息发我，我来排查修改
5. **报告最后一起写** — 所有技术步骤完成后，我帮你按模板撰写实训报告

> 你不需要自己写代码，所有代码和命令由我提供，你负责在虚拟机执行并反馈结果。

---

## 项目概述

- **课程**: 大数据计算集群技术综合实践
- **教师**: 郑成
- **主题**: 基于大数据技术的汽车投诉数据可视化分析
- **目标**: 使用大数据技术（HDFS + Spark + Sqoop + MariaDB + ECharts）对汽车投诉数据进行采集、清洗、多维分析和可视化展示

## 数据说明

**文件**: `汽车投诉数据(20240429).csv`（约 2.3MB）

| 字段 | 类型 | 说明 |
|------|------|------|
| 投诉编号 | String | 唯一标识 |
| 投诉品牌 | String | 汽车品牌（如深蓝、领克、大众、哈弗等）|
| 投诉车系 | String | 车系名称 |
| 投诉车型 | String | 具体车型（含年款）|
| 投诉简述 | String | 问题详细描述（长文本）|
| 投诉问题 | String | 问题大类（如车身、电气设备、传动系等）|
| 问题类型 | String | 问题小类（如异响、部件开裂、中控黑屏等）|
| 投诉日期 | String | 投诉日期（2024-04-29）|

## 技术架构

```
CSV ──→ HDFS ──→ Spark SQL ──→ HDFS(结果) ──→ Sqoop ──→ MariaDB ──→ Servlet API ──→ ECharts 可视化
       存储      清洗+多维分析    ORC/CSV输出    数据导入    关系存储      JSON接口      前端图表
```

| 组件 | 作用 | 运行位置 |
|------|------|----------|
| Hadoop HDFS (HA) | 存储原始数据和分析结果 | 虚拟机 |
| Spark (Scala) | 数据清洗 + 6维分析 | 虚拟机 |
| Sqoop | HDFS → MariaDB 数据导入 | 虚拟机 |
| MariaDB | 存储分析结果表 | 虚拟机 |
| Java Web (Servlet) | 查询 MariaDB 返回 JSON API | 本机开发 |
| ECharts + HTML | 前端可视化仪表盘 | 浏览器 |

## 环境信息

### 集群拓扑

| 节点 | 角色 | 说明 |
|------|------|------|
| node1 | Master / NameNode | 主节点，HDFS NameNode、YARN ResourceManager |
| node2 | Slave / DataNode | 从节点，HDFS DataNode、YARN NodeManager |
| node3 | Slave / DataNode | 从节点，HDFS DataNode、YARN NodeManager |

- **工作目录**: `/export`（所有操作在虚拟机 `/export` 下进行）
- **HDFS 地址**: `hdfs://node1:9000`（或 `hdfs://master:9000`，以实际配置为准）
- **MariaDB**: 安装在虚拟机节点上

### 集群启动（每次开机后执行）

```bash
# ===== 1. 启动 Zookeeper =====
# 三台节点都执行
zkServer.sh start

# ===== 2. 启动 JournalNode（HA 需要）=====
# 三台节点都执行
hdfs --daemon start journalnode

# ===== 3. 启动 HDFS =====
# 在 node1（master）上执行
start-dfs.sh

# ===== 4. 启动 YARN =====
# 在 node1（master）上执行
start-yarn.sh

# ===== 5. 启动 MariaDB =====
systemctl start mariadb
# 或者: service mysql start

# ===== 6. 验证集群状态 =====
hdfs dfsadmin -report          # 确认 HDFS 节点正常
yarn node -list                 # 确认 YARN 节点正常
mysql -u root -p -e "SELECT VERSION();"  # 确认 MariaDB 运行
```

> **注意**: Spark on YARN 模式不需要单独启动 Spark 集群，提交任务时 `--master yarn` 会自动调度。

### 环境确认

在虚拟机上（node1）执行以下命令，把输出发给我：

```bash
# 确认 Hadoop
hadoop version

# 确认 HDFS
hdfs dfs -ls /

# 确认 Spark
spark-submit --version

# 确认 Sqoop
sqoop version

# 确认 MariaDB
mysql -u root -p -e "SELECT VERSION();"

# 确认 HBase（备用）
hbase version

# 确认工作目录
ls /export/
```

---

## 进度追踪

| 步骤 | 内容 | 状态 | 完成时间 |
|------|------|------|----------|
| Step 1 | 数据上传到 HDFS | [x] 已完成 | 2026-06-22 |
| Step 2 | Spark 数据清洗 | [x] 已完成 | 2026-06-22 |
| Step 3 | Spark 多维分析（6个维度）| [x] 已完成 | 2026-06-22 |
| Step 4 | MariaDB 建表 | [x] 已完成 | 2026-06-22 |
| Step 5 | Sqoop 数据导出到 MariaDB | [x] 已完成 | 2026-06-22 |
| Step 6 | Java Web 后端开发 | [x] 已完成 | 2026-06-22 |
| Step 7 | 前端可视化页面 | [x] 已完成 | 2026-06-22 |
| Step 8 | 撰写实训报告 | [x] 已完成 | 2026-06-22 |

---

## Step 1: 数据上传到 HDFS

### 1.1 确认数据文件编码

先将 CSV 文件放到虚拟机 `/export/` 下（如果还没放过去的话）。

```bash
cd /export

# 检查文件编码
file 汽车投诉数据\(20240429\).csv
head -3 汽车投诉数据\(20240429\).csv
```

### 1.2 上传到 HDFS

```bash
cd /export

# 创建项目目录
hdfs dfs -mkdir -p /car_complaint/input
hdfs dfs -mkdir -p /car_complaint/output

# 上传 CSV 文件
hdfs dfs -put 汽车投诉数据\(20240429\).csv /car_complaint/input/

# 验证
hdfs dfs -ls /car_complaint/input/
hdfs dfs -head /car_complaint/input/汽车投诉数据\(20240429\).csv
```

### 1.3 确认列数和行数

```bash
# 统计行数（含表头）
hdfs dfs -cat /car_complaint/input/汽车投诉数据\(20240429\).csv | wc -l
```

> **完成后**: 截图 HDFS 文件列表，告诉我行数统计结果。

---

## Step 2: Spark 数据清洗

### 2.1 创建 Maven 项目

在虚拟机上创建 Scala Maven 项目 `car_complaint_analysis`，目录结构参考 `2_课程资料/0_代码示例/stu_job_analysis/`。

**pom.xml 关键依赖**（参考课程 pom.xml）:
- Scala 2.12.18
- Spark Core 3.4.2
- Spark SQL 3.4.2
- Spark Hive 3.4.2（如需要）

### 2.2 数据清洗脚本 — ComplaintDataClean.scala

```scala
package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}
import org.apache.spark.sql.types._

object ComplaintDataClean {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Car Complaint Data Clean")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._

    // 定义 Schema
    val schema = StructType(Array(
      StructField("complaint_id", StringType),
      StructField("brand", StringType),
      StructField("series", StringType),
      StructField("model", StringType),
      StructField("description", StringType),
      StructField("problem_category", StringType),
      StructField("problem_type", StringType),
      StructField("complaint_date", StringType)
    ))

    // 读取 CSV
    val rawDF = spark.read
      .option("header", "true")
      .schema(schema)
      .csv("hdfs://master:9000/car_complaint/input/汽车投诉数据(20240429).csv")

    // 清洗：过滤空品牌、空问题分类
    val cleanDF = rawDF
      .filter($"brand".isNotNull && $"brand" =!= "")
      .filter($"problem_category".isNotNull && $"problem_category" =!= "")
      .filter($"complaint_date".isNotNull && $"complaint_date" =!= "")
      .dropDuplicates("complaint_id")

    // 写入清洗后的数据到 HDFS（ORC 格式）
    cleanDF.write
      .mode(SaveMode.Overwrite)
      .orc("hdfs://master:9000/car_complaint/output/clean")

    println(s"清洗前: ${rawDF.count()} 条")
    println(s"清洗后: ${cleanDF.count()} 条")

    spark.stop()
  }
}
```

### 2.3 编译与提交

```bash
# 编译打包
mvn clean package -DskipTests

# 提交到 Spark
spark-submit \
  --class car.complaint.analysis.ComplaintDataClean \
  --master yarn \
  --deploy-mode cluster \
  target/car_complaint_analysis-1.0-SNAPSHOT.jar
```

> **完成后**: 截图 Spark 任务运行结果（清洗前后的行数），确认 `/car_complaint/output/clean` 目录有 ORC 文件。

---

## Step 3: Spark 多维分析（6个维度）

### 3.1 BrandAnalysis.scala — 品牌投诉分析

按品牌分组，统计投诉量和涉及的问题类型数。

```scala
package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object BrandAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Brand Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://master:9000/car_complaint/output/clean")
    cleanDF.createOrReplaceTempView("complaints")

    val result = spark.sql(
      """SELECT brand AS name,
         |       COUNT(*) AS cnt,
         |       COUNT(DISTINCT problem_category) AS problem_category_count,
         |       COUNT(DISTINCT problem_type) AS problem_type_count
         |FROM complaints
         |GROUP BY brand
         |ORDER BY cnt DESC""".stripMargin)

    result.write.mode(SaveMode.Overwrite)
      .orc("hdfs://master:9000/car_complaint/output/BrandAnalysis")

    result.show(20, false)
    spark.stop()
  }
}
```

### 3.2 SeriesAnalysis.scala — 车系投诉分析

```scala
// 按车系分组，统计投诉量
val result = spark.sql(
  """SELECT series AS name, COUNT(*) AS cnt
     |FROM complaints
     |GROUP BY series
     |ORDER BY cnt DESC""".stripMargin)

result.write.mode(SaveMode.Overwrite)
  .orc("hdfs://master:9000/car_complaint/output/SeriesAnalysis")
```

### 3.3 ProblemCategoryAnalysis.scala — 投诉问题大类分析

```scala
// 按问题大类分组
val result = spark.sql(
  """SELECT problem_category AS name, COUNT(*) AS cnt
     |FROM complaints
     |GROUP BY problem_category
     |ORDER BY cnt DESC""".stripMargin)

result.write.mode(SaveMode.Overwrite)
  .orc("hdfs://master:9000/car_complaint/output/ProblemCategoryAnalysis")
```

### 3.4 ProblemTypeAnalysis.scala — 问题类型分析

```scala
// 按问题小类分组
val result = spark.sql(
  """SELECT problem_type AS name, COUNT(*) AS cnt
     |FROM complaints
     |GROUP BY problem_type
     |ORDER BY cnt DESC""".stripMargin)

result.write.mode(SaveMode.Overwrite)
  .orc("hdfs://master:9000/car_complaint/output/ProblemTypeAnalysis")
```

### 3.5 BrandProblemCross.scala — 品牌×问题交叉分析

```scala
// 品牌与问题大类的交叉统计
val result = spark.sql(
  """SELECT brand, problem_category, COUNT(*) AS cnt
     |FROM complaints
     |GROUP BY brand, problem_category
     |ORDER BY cnt DESC""".stripMargin)

result.write.mode(SaveMode.Overwrite)
  .orc("hdfs://master:9000/car_complaint/output/BrandProblemCross")
```

### 3.6 DailyTrendAnalysis.scala — 日投诉趋势分析

```scala
// 按日期统计投诉量
val result = spark.sql(
  """SELECT complaint_date AS day, COUNT(*) AS cnt
     |FROM complaints
     |GROUP BY complaint_date
     |ORDER BY day""".stripMargin)

result.write.mode(SaveMode.Overwrite)
  .orc("hdfs://master:9000/car_complaint/output/DailyTrend")
```

### 3.7 批量编译与提交

将所有 Analysis 类写好，分别提交：

```bash
mvn clean package -DskipTests

# 逐个提交（或写脚本批量）
spark-submit --class car.complaint.analysis.BrandAnalysis --master yarn --deploy-mode cluster target/car_complaint_analysis-1.0-SNAPSHOT.jar
spark-submit --class car.complaint.analysis.SeriesAnalysis --master yarn --deploy-mode cluster target/car_complaint_analysis-1.0-SNAPSHOT.jar
spark-submit --class car.complaint.analysis.ProblemCategoryAnalysis --master yarn --deploy-mode cluster target/car_complaint_analysis-1.0-SNAPSHOT.jar
spark-submit --class car.complaint.analysis.ProblemTypeAnalysis --master yarn --deploy-mode cluster target/car_complaint_analysis-1.0-SNAPSHOT.jar
spark-submit --class car.complaint.analysis.BrandProblemCross --master yarn --deploy-mode cluster target/car_complaint_analysis-1.0-SNAPSHOT.jar
spark-submit --class car.complaint.analysis.DailyTrendAnalysis --master yarn --deploy-mode cluster target/car_complaint_analysis-1.0-SNAPSHOT.jar
```

> **完成后**: 截图每个分析任务的成功日志，确认 6 个输出目录都存在。

---

## Step 4: MariaDB 建表

### 4.1 登录 MariaDB

```bash
mysql -u root -p
```

### 4.2 创建数据库和表

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS car_complaint DEFAULT CHARSET utf8mb4;
USE car_complaint;

-- 品牌投诉分析表
CREATE TABLE brand_analysis (
    name VARCHAR(100) PRIMARY KEY COMMENT '品牌名称',
    cnt INT COMMENT '投诉量',
    problem_category_count INT COMMENT '涉及问题大类数',
    problem_type_count INT COMMENT '涉及问题类型数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车系投诉分析表
CREATE TABLE series_analysis (
    name VARCHAR(200) PRIMARY KEY COMMENT '车系名称',
    cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题大类分析表
CREATE TABLE problem_category_analysis (
    name VARCHAR(100) PRIMARY KEY COMMENT '问题大类',
    cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题类型分析表
CREATE TABLE problem_type_analysis (
    name VARCHAR(200) PRIMARY KEY COMMENT '问题类型',
    cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 品牌×问题交叉表
CREATE TABLE brand_problem_cross (
    id INT AUTO_INCREMENT PRIMARY KEY,
    brand VARCHAR(100) COMMENT '品牌',
    problem_category VARCHAR(100) COMMENT '问题大类',
    cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 日投诉趋势表
CREATE TABLE daily_trend (
    day DATE PRIMARY KEY COMMENT '投诉日期',
    cnt INT COMMENT '投诉量'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.3 验证表结构

```sql
USE car_complaint;
SHOW TABLES;
DESC brand_analysis;
DESC series_analysis;
DESC problem_category_analysis;
DESC problem_type_analysis;
DESC brand_problem_cross;
DESC daily_trend;
```

> **完成后**: 截图 `SHOW TABLES;` 结果。

---

## Step 5: Sqoop 导出数据到 MariaDB

### 5.1 确认 HDFS 结果文件

```bash
hdfs dfs -ls /car_complaint/output/
hdfs dfs -ls /car_complaint/output/BrandAnalysis/
```

### 5.2 Sqoop 导出

> **注意**: ORC 格式导出需先确认 Sqoop 版本是否支持。如不支持，可先在 Step 3 中改为输出 CSV，或使用 `--hcatalog-table` 方式。

**方案 A — ORC 直接导出（需 Sqoop 1.4.7+ 且配置 HCatalog）**:

```bash
# 品牌分析
sqoop export \
  --connect jdbc:mysql://localhost:3306/car_complaint \
  --username root --password 你的密码 \
  --table brand_analysis \
  --export-dir /car_complaint/output/BrandAnalysis \
  --input-fields-terminated-by '\t'

# 车系分析
sqoop export \
  --connect jdbc:mysql://localhost:3306/car_complaint \
  --username root --password 你的密码 \
  --table series_analysis \
  --export-dir /car_complaint/output/SeriesAnalysis

# ... 依次导出其余 4 张表
```

**方案 B — 改用 CSV 格式输出（推荐，兼容性最好）**:

在 Step 3 的各个 Analysis 类中，将 `.orc()` 改为 `.csv()`：

```scala
result.write
  .mode(SaveMode.Overwrite)
  .option("header", "true")
  .csv("hdfs://master:9000/car_complaint/output/BrandAnalysis")
```

然后用 Sqoop 导出 CSV：

```bash
sqoop export \
  --connect jdbc:mysql://localhost:3306/car_complaint \
  --username root --password 你的密码 \
  --table brand_analysis \
  --export-dir /car_complaint/output/BrandAnalysis \
  --input-fields-terminated-by ',' \
  --input-lines-terminated-by '\n' \
  --input-optionally-enclosed-by '\"'
```

### 5.3 验证数据

```sql
USE car_complaint;
SELECT * FROM brand_analysis ORDER BY cnt DESC LIMIT 10;
SELECT COUNT(*) FROM series_analysis;
SELECT COUNT(*) FROM problem_category_analysis;
SELECT COUNT(*) FROM problem_type_analysis;
SELECT COUNT(*) FROM brand_problem_cross;
SELECT COUNT(*) FROM daily_trend;
```

> **完成后**: 截图各表的 SELECT 结果（前10行）。

---

## Step 6: Java Web 后端开发

### 6.1 项目说明

后端在本机开发（Windows），参考 `2_课程资料/0_代码示例/stu_job_web/` 的项目结构。

用 **Spring Boot** 替代原课程的纯 Servlet（更简洁，适合本机快速开发），或者沿用原课程的 Servlet + Maven WAR 模式。

### 6.2 Spring Boot 方案（推荐）

**pom.xml 核心依赖**:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
    </dependency>
</dependencies>
```

**application.properties**:
```properties
server.port=8080
spring.datasource.url=jdbc:mariadb://虚拟机IP:3306/car_complaint?useSSL=false&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=你的密码
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

**API 接口设计**:

| 接口 | 方法 | 说明 | 返回数据 |
|------|------|------|----------|
| `/api/brand/top20` | GET | 投诉最多的20个品牌 | `{brand:[], cnt:[]}` |
| `/api/series/top15` | GET | 投诉最多的15个车系 | `{series:[], cnt:[]}` |
| `/api/problem/category` | GET | 问题大类分布 | `{category:[], cnt:[]}` |
| `/api/problem/type` | GET | 问题类型 TOP20 | `{type:[], cnt:[]}` |
| `/api/brand/problem/cross` | GET | 品牌×问题交叉数据 | `{brand:[], category:[], data:[[cnt,...]]}` |
| `/api/trend/daily` | GET | 日投诉趋势 | `{date:[], cnt:[]}` |

**Controller 示例** (`BrandController.java`):

```java
@RestController
@RequestMapping("/api/brand")
@CrossOrigin(origins = "*")
public class BrandController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/top20")
    public Map<String, Object> getBrandTop20() {
        String sql = "SELECT name, cnt FROM brand_analysis ORDER BY cnt DESC LIMIT 20";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        List<String> brands = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            brands.add((String) row.get("name"));
            counts.add((Integer) row.get("cnt"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("brand", brands);
        result.put("cnt", counts);
        return result;
    }
}
```

### 6.3 启动与测试

```bash
mvn spring-boot:run
```

在浏览器访问 `http://localhost:8080/api/brand/top20`，确认返回 JSON 数据。

> **完成后**: 截图浏览器中各个 API 返回的 JSON 数据。

---

## Step 7: 前端可视化页面

### 7.1 页面设计

创建 `index.html`，包含6个图表，采用深色仪表盘主题（参考课程 `comon0.css` 样式）。

**布局设计**:
```
┌──────────────────────────────────────────────┐
│         汽车投诉数据可视化分析大屏              │
├────────────────────┬─────────────────────────┤
│  📊 品牌投诉TOP20  │  📈 日投诉量趋势折线图    │
│   (横向柱状图)     │   (折线图)               │
├────────────────────┼─────────────────────────┤
│  🍩 问题大类分布   │  📊 问题类型TOP20        │
│   (玫瑰图/饼图)    │   (柱状图)               │
├────────────────────┴─────────────────────────┤
│  🔥 品牌-问题关联热力图                        │
├──────────────────────────────────────────────┤
│  📋 车系投诉量 TOP15 (横向柱状图)              │
└──────────────────────────────────────────────┘
```

### 7.2 技术选型

- ECharts 5.x（CDN 引入）
- 纯 HTML + CSS + JavaScript
- Fetch API 调用后端接口
- 自动刷新（每30秒）

### 7.3 关键代码片段

**引入 ECharts**:
```html
<script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
```

**品牌柱状图示例**:
```javascript
fetch('http://localhost:8080/api/brand/top20')
  .then(res => res.json())
  .then(data => {
    const chart = echarts.init(document.getElementById('brand-chart'));
    chart.setOption({
      title: { text: '投诉品牌 TOP20', textStyle: { color: '#fff' } },
      xAxis: { type: 'value', axisLabel: { color: '#fff' } },
      yAxis: { type: 'category', data: data.brand.reverse(), axisLabel: { color: '#fff' } },
      series: [{ type: 'bar', data: data.cnt.reverse(), itemStyle: { color: '#e74c3c' } }]
    });
  });
```

> 完整 HTML 文件在实施此步骤时由我提供。

> **完成后**: 截图完整的可视化大屏页面。

---

## Step 8: 撰写实训报告

### 8.1 报告模板

参考 `2_课程资料/3_课设模板/大数据计算集群技术综合实践-课设模板-2406-郑成.doc` 的结构。

### 8.2 建议章节

1. **封面** — 课程名、题目、姓名、学号、日期
2. **项目背景与目标** — 汽车投诉数据分析的意义
3. **数据说明** — 数据来源、字段描述、数据量
4. **技术方案** — 架构图、各组件作用
5. **实施过程**（核心章节）
   - 5.1 数据采集与上传（Step 1）
   - 5.2 数据清洗（Step 2）
   - 5.3 多维分析（Step 3，含6个维度的分析SQL和结果解读）
   - 5.4 数据存储（Step 4-5）
   - 5.5 后端开发（Step 6）
   - 5.6 前端可视化（Step 7，含截图）
6. **分析结果与发现** — 从数据中发现了什么规律
7. **总结与收获** — 学到了什么，遇到什么问题如何解决
8. **参考文献**

### 8.3 报告要点

- 每个分析维度配一张图表截图
- 描述数据中发现的有意义结论（如：哪个品牌投诉最多、什么类型问题最普遍）
- 技术细节写清楚：关键代码、配置文件、命令

> **完成后**: 提交报告文件。

---

## 参考文件索引

| 文件 | 路径 | 用途 |
|------|------|------|
| 原始数据 | `汽车投诉数据(20240429).csv` | 分析数据源 |
| 数据说明 | `汽车投诉数据_readme.md` | 数据集元信息 |
| Spark 分析参考 | `2_课程资料/0_代码示例/stu_job_analysis/` | Scala 代码模板 |
| Web 项目参考 | `2_课程资料/0_代码示例/stu_job_web/` | Java Web + ECharts 模板 |
| PPT 课件 | `2_课程资料/1_课件/PPT/` | 3个PPT（开课、前后端、ECharts）|
| 实验手册 | `2_课程资料/1_课件/实验手册/` | 详细步骤 PDF |
| 项目任务书 | `2_课程资料/2_任务书/` | 7个项目选项及要求 |
| 课设模板 | `2_课程资料/3_课设模板/` | 报告格式模板 |
| 项目报告参考 | `2_课程资料/0_代码示例/基于大数据技术的高校毕业生就业数据可视化分析.pdf` | 完整报告范文 |

---

## 工作日志

> 此区域记录每步的实际操作情况，逐项填写。

### Step 1 — 数据上传到 HDFS

- 执行日期: 2026-06-22
- 命令: hdfs dfs -mkdir /car_complaint/input & /output; hdfs dfs -put csv
- 结果: 上传成功，文件 2.3MB
- 截图:

### Step 2 — Spark 数据清洗

- 执行日期: 2026-06-22
- 清洗前: ~4700 条
- 清洗后: 4670 条
- 截图:

### Step 3 — Spark 多维分析

- 执行日期: 2026-06-22
- 各维度结果: 全部完成，6 个输出目录 + clean 共 7 个
- 截图:

### Step 4 — MariaDB 建表

- 执行日期:
- 截图:

### Step 5 — Sqoop 导出

- 执行日期:
- 截图:

### Step 6 — Java Web 后端

- 执行日期:
- 截图:

### Step 7 — 前端可视化

- 执行日期:
- 截图:

### Step 8 — 实训报告

- 完成日期:
