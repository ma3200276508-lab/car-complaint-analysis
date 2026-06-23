package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object DailyTrendAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Daily Trend Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
    cleanDF.createOrReplaceTempView("complaints")

    val result = spark.sql(
      """SELECT complaint_date AS day, COUNT(*) AS cnt
         |FROM complaints
         |GROUP BY complaint_date
         |ORDER BY day""".stripMargin)

    result.write.mode(SaveMode.Overwrite)
      .csv("hdfs://node1:9000/car_complaint/output/DailyTrend")

    println("=== 日投诉量趋势 ===")
    result.show(50, false)

    spark.stop()
  }
}
