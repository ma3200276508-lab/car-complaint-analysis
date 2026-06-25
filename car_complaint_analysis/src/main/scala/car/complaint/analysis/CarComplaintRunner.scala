package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}
import org.apache.spark.sql.types._

/**
 * 汽车投诉数据全流程分析 — 统一入口
 *
 * 用法:
 *   spark-submit --class car.complaint.analysis.CarComplaintRunner \
 *     --master local[2] car_complaint_analysis-1.0-SNAPSHOT.jar [hdfs_prefix]
 *
 * 参数:
 *   hdfs_prefix - HDFS前缀，默认 hdfs://node1:9000
 *
 * 流程:
 *   1. 数据清洗 → clean/
 *   2. 品牌分析 → BrandAnalysis/
 *   3. 车系分析 → SeriesAnalysis/
 *   4. 问题大类 → ProblemCategoryAnalysis/
 *   5. 问题类型 → ProblemTypeAnalysis/
 *   6. 交叉分析 → BrandProblemCross/
 *   7. 日趋势   → DailyTrend/
 */
object CarComplaintRunner {

  def main(args: Array[String]): Unit = {
    // 从命令行参数获取HDFS前缀，默认node1
    val hdfsPrefix = if (args.length > 0) args(0) else "hdfs://node1:9000"

    val spark = SparkSession.builder()
      .appName("Car Complaint Full Pipeline")
      .enableHiveSupport()
      .getOrCreate()

    import spark.implicits._

    println("=" * 60)
    println("  汽车投诉数据全流程分析")
    println("=" * 60)

    // ========================================
    // 1. 数据清洗
    // ========================================
    println("\n[1/7] 数据清洗...")

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

    val csvPath = hdfsPrefix + "/car_complaint/input/汽车投诉数据(20240429).csv"
    val rawDF = spark.read.option("header", "true").schema(schema).csv(csvPath)

    val cleanDF = rawDF
      .filter($"brand".isNotNull && $"brand" =!= "")
      .filter($"problem_category".isNotNull && $"problem_category" =!= "")
      .filter($"complaint_date".isNotNull && $"complaint_date" =!= "")
      .dropDuplicates("complaint_id")

    val cleanPath = hdfsPrefix + "/car_complaint/output/clean"
    cleanDF.write.mode(SaveMode.Overwrite).orc(cleanPath)

    val rawCount = rawDF.count()
    val cleanCount = cleanDF.count()
    println(s"   清洗前: $rawCount → 清洗后: $cleanCount (过滤 ${rawCount - cleanCount} 条)")

    val cleanOrcPath = hdfsPrefix + "/car_complaint/output/clean"
    spark.read.orc(cleanOrcPath).createOrReplaceTempView("complaints")

    // ========================================
    // 2. 品牌投诉分析
    // ========================================
    println("\n[2/7] 品牌投诉分析...")
    val brandResult = spark.sql(
      """SELECT brand AS name, COUNT(*) AS cnt,
        |       COUNT(DISTINCT problem_category) AS cat_cnt,
        |       COUNT(DISTINCT problem_type) AS type_cnt
        |FROM complaints
        |GROUP BY brand ORDER BY cnt DESC""".stripMargin)
    brandResult.write.mode(SaveMode.Overwrite)
      .csv(hdfsPrefix + "/car_complaint/output/BrandAnalysis")
    println("   TOP5 品牌:")
    brandResult.show(5, false)

    // ========================================
    // 3. 车系投诉分析
    // ========================================
    println("\n[3/7] 车系投诉分析...")
    val seriesResult = spark.sql(
      """SELECT series AS name, COUNT(*) AS cnt
        |FROM complaints GROUP BY series ORDER BY cnt DESC""".stripMargin)
    seriesResult.write.mode(SaveMode.Overwrite)
      .csv(hdfsPrefix + "/car_complaint/output/SeriesAnalysis")
    println("   TOP5 车系:")
    seriesResult.show(5, false)

    // ========================================
    // 4. 问题大类分析
    // ========================================
    println("\n[4/7] 问题大类分析...")
    val catResult = spark.sql(
      """SELECT problem_category AS name, COUNT(*) AS cnt
        |FROM complaints GROUP BY problem_category
        |ORDER BY cnt DESC""".stripMargin)
    catResult.write.mode(SaveMode.Overwrite)
      .csv(hdfsPrefix + "/car_complaint/output/ProblemCategoryAnalysis")
    println("   问题大类分布:")
    catResult.show(19, false)

    // ========================================
    // 5. 问题类型分析
    // ========================================
    println("\n[5/7] 问题类型分析...")
    val typeResult = spark.sql(
      """SELECT problem_type AS name, COUNT(*) AS cnt
        |FROM complaints GROUP BY problem_type
        |ORDER BY cnt DESC""".stripMargin)
    typeResult.write.mode(SaveMode.Overwrite)
      .csv(hdfsPrefix + "/car_complaint/output/ProblemTypeAnalysis")
    println("   TOP10 问题类型:")
    typeResult.show(10, false)

    // ========================================
    // 6. 品牌×问题交叉分析
    // ========================================
    println("\n[6/7] 品牌×问题交叉分析...")
    val crossResult = spark.sql(
      """SELECT brand, problem_category, COUNT(*) AS cnt
        |FROM complaints GROUP BY brand, problem_category
        |ORDER BY cnt DESC""".stripMargin)
    crossResult.write.mode(SaveMode.Overwrite)
      .csv(hdfsPrefix + "/car_complaint/output/BrandProblemCross")
    val crossCount = crossResult.count()
    println(s"   交叉记录数: $crossCount")

    // ========================================
    // 7. 日投诉趋势分析
    // ========================================
    println("\n[7/7] 日投诉趋势分析...")
    val trendResult = spark.sql(
      """SELECT complaint_date AS day, COUNT(*) AS cnt
        |FROM complaints GROUP BY complaint_date
        |ORDER BY day""".stripMargin)
    trendResult.write.mode(SaveMode.Overwrite)
      .csv(hdfsPrefix + "/car_complaint/output/DailyTrend")
    println(s"   日期数: ${trendResult.count()}")

    // ========================================
    // 汇总
    // ========================================
    println("\n" + "=" * 60)
    println("  全部分析完成！")
    println(s"  品牌数: ${brandResult.count()}  |  车系数: ${seriesResult.count()}")
    println(s"  问题大类: ${catResult.count()}  |  问题类型: ${typeResult.count()}")
    println(s"  交叉关系: $crossCount  |  日期范围: ${trendResult.count()} 天")
    println("=" * 60)

    spark.stop()
  }
}
