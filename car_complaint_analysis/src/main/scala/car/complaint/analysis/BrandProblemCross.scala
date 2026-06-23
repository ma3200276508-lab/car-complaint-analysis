package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object BrandProblemCross {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Brand Problem Cross Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
    cleanDF.createOrReplaceTempView("complaints")

    val result = spark.sql(
      """SELECT brand, problem_category, COUNT(*) AS cnt
         |FROM complaints
         |GROUP BY brand, problem_category
         |ORDER BY cnt DESC""".stripMargin)

    result.write.mode(SaveMode.Overwrite)
      .csv("hdfs://node1:9000/car_complaint/output/BrandProblemCross")

    println("=== 品牌×问题交叉分析 ===")
    result.show(30, false)

    spark.stop()
  }
}
