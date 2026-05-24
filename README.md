# 🚀 大数据日志分析平台

[![Spark Version](https://img.shields.io/badge/Spark-2.4.5-orange)](https://spark.apache.org/)
[![Scala Version](https://img.shields.io/badge/Scala-2.12-blue)](https://www.scala-lang.org/)
[![Hadoop Version](https://img.shields.io/badge/Hadoop-3.1.1-yellow)](https://hadoop.apache.org/)
[![MySQL Version](https://img.shields.io/badge/MySQL-5.7-green)](https://mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-red)](LICENSE)

基于 **Apache Spark 2.4.5 + Scala 2.12** 构建的企业级网站日志分析平台，支持海量日志数据的离线分析与可视化展示。

## ✨ 核心功能

| 功能模块 | 说明 |
|---------|------|
| 📊 PV/UV 统计 | 页面浏览量、独立访客数实时统计 |
| 🔥 热门页面 | TOP 10 页面访问排行 |
| ⏰ 时段分布 | 24小时访问趋势分析 |
| 📋 状态码分析 | HTTP状态码分布统计 |
| 📈 可视化大屏 | ECharts动态图表展示 |

## 🏗️ 系统架构
┌─────────────────────────────────────────────────────────────┐
│ 数据采集层 │
│ 日志文件(access.log) → HDFS (/data/raw_logs) │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ 数据处理层 │
│ Spark SQL → 数据清洗 → ETL → 指标计算 │
│ (PV/UV/热门页面/时段分布/状态码统计) │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ 数据存储层 │
│ HDFS(原始日志) + MySQL(统计结果) │
└─────────────────────────────────────────────────────────────┘
↓
┌─────────────────────────────────────────────────────────────┐
│ 可视化层 │
│ ECharts 可视化大屏 │
└─────────────────────────────────────────────────────────────┘

text

## 📦 技术栈

| 组件 | 版本 | 用途 |
|-----|------|------|
| Apache Spark | 2.4.5 | 核心计算引擎 |
| Scala | 2.12.13 | 开发语言 |
| Hadoop HDFS | 3.1.1 | 分布式存储 |
| YARN | 3.1.1 | 资源调度 |
| MySQL | 5.7.22 | 结果存储 |
| Maven | 3.8+ | 构建工具 |

## 🏗️ 集群架构

| 节点 | IP | 角色 |
|------|-----|------|
| fgedu81 | 192.168.1.81 | NameNode (Active) + ResourceManager + MySQL |
| fgedu82 | 192.168.1.82 | NameNode (Standby) + ResourceManager |
| fgedu91 | 192.168.1.91 | DataNode + NodeManager + Spark |
| fgedu92 | 192.168.1.92 | DataNode + NodeManager + Spark |
| fgedu93 | 192.168.1.93 | DataNode + NodeManager + Spark |

## 🚀 快速开始

### 前置要求

- JDK 1.8+
- Maven 3.6+
- Python 3.7+
- MySQL 5.7+
- Hadoop 3.1.1+ 集群

### 1. 克隆项目

```bash
git clone https://github.com/yourname/bigdata-log-analyzer.git
cd bigdata-log-analyzer
mysql -h fgedu81 -uroot -proot < scripts/mysql_schema.sql
python scripts/generate_log.py
mvn clean package -DskipTests
spark-submit \
  --class com.bigdata.log.LogAnalyzer \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 3 \
  --executor-memory 1024m \
  target/log-analyzer-1.0.0-jar-with-dependencies.jar \
  "hdfs://fgeduns/data/raw_logs/access.log" \
  "jdbc:mysql://fgedu81:3306/log_analyzer" \
  "root" \
  "root"
./run.sh
==========================================
网站日志分析平台 - 作业启动
==========================================
📁 读取日志行数: 50000
✅ 成功解析: 48532 条日志

📊 PV (页面浏览量): 48532
👥 UV (独立访客): 3420
📈 人均浏览量: 14.19

🔥 热门页面 TOP 10:
   1. /index.html                                    12,345
   2. /course.html                                    8,234
   3. /detail.html                                    6,123
   ...

⏰ 访问时段分布:
   08:00 ████████████████████████████████████         3,456
   09:00 ████████████████████████████████████████     4,567
   10:00 ██████████████████████████████████████████   5,678
   ...
bigdata-log-analyzer/
├── src/
│   └── main/scala/com/bigdata/log/
│       ├── LogAnalyzer.scala      # 主分析程序
│       └── LogParser.scala        # 日志解析器
├── scripts/
│   └── generate_log.py            # 日志生成器
├── data/                          # 测试数据
├── target/                        # 编译输出
├── pom.xml                        # Maven配置
├── run.sh                         # 一键运行脚本
└── README.md                      # 项目文档
性能指标
数据量处理时间集群配置
5万条~15秒3节点
50万条~2分钟3节点
500万条~15分钟3节点
🤝 贡献指南
Fork 本仓库

创建特性分支 (git checkout -b feature/AmazingFeature)

提交更改 (git commit -m 'Add some AmazingFeature')

推送到分支 (git push origin feature/AmazingFeature)

打开 Pull Request

📄 License
本项目采用 MIT 许可证 - 详见 LICENSE 文件

📧 联系方式
作者：Kauffeldz

邮箱：chenyuyang@nefu.edu.cn

项目链接：https://github.com/yourname/bigdata-log-analyzer
