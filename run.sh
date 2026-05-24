#!/bin/bash

echo "=========================================="
echo "日志分析平台 - 一键运行"
echo "当前节点: $(hostname)"
echo "Spark集群: fgedu91, fgedu92, fgedu93"
echo "=========================================="

cd /data/soft/bigdata-log-analyzer

# 1. 生成日志
echo "[1/5] 生成测试日志..."
python scripts/generate_log.py

# 2. 上传到HDFS
echo "[2/5] 上传到HDFS..."
hdfs dfs -mkdir -p /data/raw_logs
hdfs dfs -put -f data/access.log /data/raw_logs/

# 3. 编译打包
echo "[3/5] 编译打包..."
mvn clean package -DskipTests -q

# 4. 提交Spark作业
echo "[4/5] 提交Spark作业..."
spark-submit \
  --class com.bigdata.log.LogAnalyzer \
  --master yarn \
  --deploy-mode cluster \
  --name "LogAnalyzer" \
  --num-executors 3 \
  --executor-memory 1024m \
  --executor-cores 2 \
  --driver-memory 1024m \
  target/log-analyzer-1.0.0-jar-with-dependencies.jar \
  "hdfs://fgeduns/data/raw_logs/access.log" \
  "jdbc:mysql://fgedu81:3306/log_analyzer?useSSL=false" \
  "root" \
  "root"

# 5. 显示结果
echo "[5/5] 查看结果..."
sleep 10
mysql -h fgedu81 -uroot -proot -e "SELECT stat_date, pv, uv FROM log_analyzer.log_stats_daily;" 2>/dev/null

echo "=========================================="
echo "✅ 完成！"
echo "查看YARN: http://fgedu82:8088"
echo "=========================================="
