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

    val rawDF = spark.read
      .option("header", "true")
      .schema(schema)
      .csv("hdfs://node1:9000/car_complaint/input/汽车投诉数据(20240429).csv")

    val cleanDF = rawDF
      .filter($"brand".isNotNull && $"brand" =!= "")
      .filter($"problem_category".isNotNull && $"problem_category" =!= "")
      .filter($"complaint_date".isNotNull && $"complaint_date" =!= "")
      .dropDuplicates("complaint_id")

    cleanDF.write
      .mode(SaveMode.Overwrite)
      .orc("hdfs://node1:9000/car_complaint/output/clean")

    println(s"清洗前: ${rawDF.count()} 条")
    println(s"清洗后: ${cleanDF.count()} 条")

    spark.stop()
  }
}
