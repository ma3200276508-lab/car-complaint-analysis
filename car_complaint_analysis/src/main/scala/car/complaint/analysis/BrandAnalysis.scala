package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object BrandAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Brand Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
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
      .csv("hdfs://node1:9000/car_complaint/output/BrandAnalysis")

    println("=== 品牌投诉分析 TOP20 ===")
    result.show(20, false)

    spark.stop()
  }
}
