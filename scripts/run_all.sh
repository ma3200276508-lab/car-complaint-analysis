#!/bin/bash
# =====================================================
# 汽车投诉数据分析 — 一键全流程脚本
# 用法: bash run_all.sh [HDFS_PREFIX]
# 示例: bash run_all.sh hdfs://node1:9000
# =====================================================

set -e

HDFS_PREFIX="${1:-hdfs://node1:9000}"
JAR="car_complaint_analysis-1.0-SNAPSHOT.jar"
CLASS="car.complaint.analysis.CarComplaintRunner"

echo "=========================================="
echo "  汽车投诉数据分析 — 全流程"
echo "  HDFS: $HDFS_PREFIX"
echo "=========================================="

# 1. 检查JAR
if [ ! -f "$JAR" ]; then
    echo "❌ JAR文件不存在: $JAR"
    echo "   请先运行: cd car_complaint_analysis && mvn clean package -DskipTests"
    exit 1
fi

# 2. 检查HDFS
echo ""
echo "📂 检查HDFS连接..."
if ! hdfs dfs -ls / > /dev/null 2>&1; then
    echo "❌ HDFS不可用，请确认集群已启动"
    exit 1
fi

# 3. 创建目录
echo ""
echo "📁 创建HDFS目录..."
hdfs dfs -mkdir -p /car_complaint/input /car_complaint/output

# 4. 检查数据文件
if ! hdfs dfs -test -f /car_complaint/input/*.csv 2>/dev/null; then
    echo "⚠️  HDFS中未找到CSV文件，请先上传："
    echo "   hdfs dfs -put 汽车投诉数据\(20240429\).csv /car_complaint/input/"
    exit 1
fi

# 5. 运行分析
echo ""
echo "🚀 开始Spark分析..."
spark-submit \
    --class "$CLASS" \
    --master local[2] \
    --name "Car Complaint Analysis" \
    "$JAR" \
    "$HDFS_PREFIX"

# 6. 显示结果
echo ""
echo "📊 HDFS输出目录："
hdfs dfs -ls /car_complaint/output/

echo ""
echo "✅ 全部完成！"
