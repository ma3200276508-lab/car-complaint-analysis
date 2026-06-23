package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object ProblemCategoryAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Problem Category Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
    cleanDF.createOrReplaceTempView("complaints")

    val result = spark.sql(
      """SELECT problem_category AS name, COUNT(*) AS cnt
         |FROM complaints
         |GROUP BY problem_category
         |ORDER BY cnt DESC""".stripMargin)

    result.write.mode(SaveMode.Overwrite)
      .csv("hdfs://node1:9000/car_complaint/output/ProblemCategoryAnalysis")

    println("=== 投诉问题大类分布 ===")
    result.show(20, false)

    spark.stop()
  }
}
