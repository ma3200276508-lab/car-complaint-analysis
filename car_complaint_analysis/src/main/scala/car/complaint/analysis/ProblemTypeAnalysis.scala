package car.complaint.analysis

import org.apache.spark.sql.{SparkSession, SaveMode}

object ProblemTypeAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Problem Type Analysis")
      .enableHiveSupport()
      .getOrCreate()

    val cleanDF = spark.read.orc("hdfs://node1:9000/car_complaint/output/clean")
    cleanDF.createOrReplaceTempView("complaints")

    val result = spark.sql(
      """SELECT problem_type AS name, COUNT(*) AS cnt
         |FROM complaints
         |GROUP BY problem_type
         |ORDER BY cnt DESC""".stripMargin)

    result.write.mode(SaveMode.Overwrite)
      .csv("hdfs://node1:9000/car_complaint/output/ProblemTypeAnalysis")

    println("=== 问题类型 TOP20 ===")
    result.show(20, false)

    spark.stop()
  }
}
