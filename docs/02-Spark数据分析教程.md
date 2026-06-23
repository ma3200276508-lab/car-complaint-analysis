# 02 — Spark 数据分析教程

> 使用 Scala + Spark SQL 完成汽车投诉数据的清洗与6维OLAP分析

## 项目结构

```
car_complaint_analysis/
├── pom.xml
└── src/main/scala/car/complaint/analysis/
    ├── ComplaintDataClean.scala    # ⭐ 数据清洗
    ├── BrandAnalysis.scala         # 品牌投诉统计
    ├── SeriesAnalysis.scala        # 车系投诉统计
    ├── ProblemCategoryAnalysis.scala  # 问题大类统计
    ├── ProblemTypeAnalysis.scala   # 问题类型统计
    ├── BrandProblemCross.scala     # 品牌×问题交叉
    └── DailyTrendAnalysis.scala    # 日投诉趋势
```

---

## 1. Maven 配置要点

```xml
<properties>
    <scala.version>2.12.17</scala.version>
    <spark.version>3.4.2</spark.version>
</properties>

<!-- 三大核心依赖，scope=provided（集群已预装） -->
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.12</artifactId>
    <version>${spark.version}</version>
    <scope>provided</scope>
</dependency>
```

> **注意**：scope 必须设为 `provided`，否则打出的 Fat JAR 会包含 Spark 核心库，与集群产生冲突。

---

## 2. 数据清洗（ComplaintDataClean.scala）

### 核心步骤

1. **创建 SparkSession** — Spark 2.x+ 的统一入口
2. **定义 Schema** — 显式声明8列的数据类型（全为 StringType）
3. **读取 CSV** — `.option("header","true")` 跳过首行
4. **清洗管道** — 空值过滤 → 主键去重
5. **输出 ORC** — 列式存储，后续分析高效读取

### 完整代码

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

    // 定义Schema：显式声明每列类型
    val schema = StructType(Array(
      StructField("complaint_id",     StringType),
      StructField("brand",            StringType),
      StructField("series",           StringType),
      StructField("model",            StringType),
      StructField("description",      StringType),
      StructField("problem_category", StringType),
      StructField("problem_type",     StringType),
      StructField("complaint_date",   StringType)
    ))

    // 读取HDFS上的CSV文件
    val rawDF = spark.read
      .option("header", "true")
      .schema(schema)
      .csv("hdfs://node1:9000/car_complaint/input/汽车投诉数据(20240429).csv")

    // 数据清洗管道
    val cleanDF = rawDF
      .filter($"brand".isNotNull && $"brand" =!= "")          // 品牌不为空
      .filter($"problem_category".isNotNull && $"problem_category" =!= "")  // 问题分类不为空
      .filter($"complaint_date".isNotNull && $"complaint_date" =!= "")      // 日期不为空
      .dropDuplicates("complaint_id")                         // 按ID去重

    // 写入HDFS（ORC格式）
    cleanDF.write.mode(SaveMode.Overwrite)
      .orc("hdfs://node1:9000/car_complaint/output/clean")

    println(s"清洗前: ${rawDF.count()} 条")
    println(s"清洗后: ${cleanDF.count()} 条")

    spark.stop()
  }
}
```

### 清洗结果

| 阶段 | 记录数 | 占比 |
|------|--------|------|
| 原始数据 | ~4,700条 | 100% |
| 清洗后 | 4,670条 | 99.4% |
| 过滤 | ~30条 | 0.6% |

---

## 3. 六维分析（6个分析类）

### 通用模式

每个分析类遵循相同流程：

```
1. spark.read.orc() 读取清洗后数据
2. createOrReplaceTempView("complaints") 注册临时视图
3. spark.sql() 执行 GROUP BY 聚合查询
4. result.write.csv() 输出CSV结果
```

### BrandAnalysis.scala — 品牌投诉统计

```scala
val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
cleanDF.createOrReplaceTempView("complaints")

val result = spark.sql(
  """SELECT brand AS name, COUNT(*) AS cnt,
    |       COUNT(DISTINCT problem_category) AS problem_category_count,
    |       COUNT(DISTINCT problem_type)     AS problem_type_count
    |FROM complaints
    |GROUP BY brand
    |ORDER BY cnt DESC""".stripMargin)

result.write.mode(SaveMode.Overwrite)
  .csv("hdfs://node1:9000/car_complaint/output/BrandAnalysis")

println("=== 品牌投诉分析 TOP20 ===")
result.show(20, false)   // 打印前20行结果
```

> **分析结果**：118个品牌，比亚迪613条第一，大众478条第二

### SeriesAnalysis.scala — 车系投诉统计

```scala
val result = spark.sql(
  """SELECT series AS name, COUNT(*) AS cnt
    |FROM complaints
    |GROUP BY series
    |ORDER BY cnt DESC""".stripMargin)
```
> 670个车系统计

### ProblemCategoryAnalysis.scala — 问题大类

```scala
val result = spark.sql(
  """SELECT problem_category AS name, COUNT(*) AS cnt
    |FROM complaints
    |GROUP BY problem_category
    |ORDER BY cnt DESC""".stripMargin)
```
> 19个问题大类

### ProblemTypeAnalysis.scala — 问题类型

```scala
val result = spark.sql(
  """SELECT problem_type AS name, COUNT(*) AS cnt
    |FROM complaints
    |GROUP BY problem_type
    |ORDER BY cnt DESC""".stripMargin)
```
> 110种问题类型

### BrandProblemCross.scala — 品牌×问题交叉

```scala
val result = spark.sql(
  """SELECT brand, problem_category, COUNT(*) AS cnt
    |FROM complaints
    |GROUP BY brand, problem_category
    |ORDER BY cnt DESC""".stripMargin)
```
> 733条交叉关联数据，用于热力图

### DailyTrendAnalysis.scala — 日投诉趋势

```scala
val result = spark.sql(
  """SELECT complaint_date AS day, COUNT(*) AS cnt
    |FROM complaints
    |GROUP BY complaint_date
    |ORDER BY day""".stripMargin)
```
> 92天时序数据

---

## 4. 编译与提交

```bash
# 在IDEA或命令行编译
cd car_complaint_analysis
mvn clean package -DskipTests

# 传输JAR到集群
scp target/car_complaint_analysis-1.0-SNAPSHOT.jar hadoop@node1:/export/

# 在node1上提交任务（local模式，适合小数据量）
cd /export
spark-submit \
  --class car.complaint.analysis.ComplaintDataClean \
  --master local[2] \
  car_complaint_analysis-1.0-SNAPSHOT.jar

# 依次运行6个分析任务
for cls in BrandAnalysis SeriesAnalysis ProblemCategoryAnalysis \
           ProblemTypeAnalysis BrandProblemCross DailyTrendAnalysis; do
  spark-submit --class car.complaint.analysis.$cls \
    --master local[2] car_complaint_analysis-1.0-SNAPSHOT.jar
done
```

---

## 5. 关键技术点

| 技术点 | 说明 |
|--------|------|
| `StructType` | 显式定义Schema，避免Spark自动推断错误 |
| `SaveMode.Overwrite` | 重复运行时覆盖旧数据 |
| ORC格式 | 列式存储，压缩率高，支持列裁剪 |
| CSV无header | `LOAD DATA`导入MariaDB不需要header行 |
| `local[2]` | 本地2线程模式，绕过YARN调度延迟 |
| `stripMargin` | Scala多行字符串，用`|`对齐 |

---

## 下一步

分析结果进入 MariaDB，见 [03-Spring-Boot-ECharts教程.md](03-Spring-Boot-ECharts教程.md)。
