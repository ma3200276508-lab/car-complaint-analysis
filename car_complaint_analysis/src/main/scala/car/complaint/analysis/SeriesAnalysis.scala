package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object SeriesAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Series Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
    cleanDF.createOrReplaceTempView("complaints")

    val result = spark.sql(
      """SELECT series AS name, COUNT(*) AS cnt
         |FROM complaints
         |GROUP BY series
         |ORDER BY cnt DESC""".stripMargin)

    result.write.mode(SaveMode.Overwrite)
      .csv("hdfs://node1:9000/car_complaint/output/SeriesAnalysis")

    println("=== 车系投诉分析 TOP15 ===")
    result.show(15, false)

    spark.stop()
  }
}
