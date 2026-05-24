package com.bigdata.log

import org.apache.spark.sql.SparkSession
import java.sql.{Connection, DriverManager, PreparedStatement}

object LogAnalyzer {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("WebsiteLogAnalyzer")
      .master("yarn")
      .config("spark.sql.adaptive.enabled", "true")
      .getOrCreate()

    try {
      val (logPath, mysqlUrl, mysqlUser, mysqlPass) = parseArgs(args)
      
      println("=" * 70)
      println("网站日志分析平台 - 作业启动")
      println(s"日志路径: $logPath")
      println(s"MySQL地址: $mysqlUrl")
      println("=" * 70)
      
      // 读取日志
      import spark.implicits._
      val rawDF = spark.read.text(logPath).filter($"value".isNotNull)
      println(s"📁 读取日志行数: ${rawDF.count()}")
      
      // 解析日志
      val logDF = rawDF.flatMap(row => LogParser.parse(row.getString(0))).toDF(
        "ip", "time", "method", "page", "protocol", "status", "size", "userAgent"
      )
      
      val validCount = logDF.count()
      println(s"✅ 成功解析: $validCount 条日志")
      
      if (validCount == 0) {
        println("❌ 没有有效日志，程序退出")
        return
      }
      
      logDF.createOrReplaceTempView("logs")
      
      // ============ PV统计 ============
      val pv = spark.sql("SELECT COUNT(*) as pv FROM logs").collect()(0).getLong(0)
      println(s"\n📊 PV (页面浏览量): ${pv}")
      
      // ============ UV统计 ============
      val uv = spark.sql("SELECT COUNT(DISTINCT ip) as uv FROM logs").collect()(0).getLong(0)
      println(s"👥 UV (独立访客): ${uv}")
      println(s"📈 人均浏览量: ${pv.toDouble / uv}")
      
      // ============ 热门页面 TOP 10 ============
      println("\n🔥 热门页面 TOP 10:")
      val hotPages = spark.sql(
        """
          |SELECT page, COUNT(*) as cnt
          |FROM logs
          |GROUP BY page
          |ORDER BY cnt DESC
          |LIMIT 10
        """.stripMargin
      ).collect()
      
      hotPages.zipWithIndex.foreach { case (row, i) =>
        val page = if (row.getString(0).length > 50) row.getString(0).take(47) + "..." else row.getString(0)
        println(f"  ${i+1}%2d. ${page}%-50s ${row.getLong(1)}%,8d")
      }
      
      // ============ 时段分布 ============
      println("\n⏰ 访问时段分布:")
      val hourStats = spark.sql(
        """
          |SELECT SUBSTR(time, 12, 2) as hour, COUNT(*) as cnt
          |FROM logs
          |GROUP BY hour
          |ORDER BY hour
        """.stripMargin
      ).collect()
      
      hourStats.foreach { row =>
        val hour = row.getString(0)
        val cnt = row.getLong(1)
        val barLen = (cnt / 100).toInt
        val bar = "█" * scala.math.min(barLen, 50)
        println(f"  $hour:00 ${bar}%-50s ${cnt}%,8d")
      }
      
      // ============ 状态码分布 ============
      println("\n📋 HTTP状态码分布:")
      val statusStats = spark.sql(
        """
          |SELECT status, COUNT(*) as cnt
          |FROM logs
          |GROUP BY status
          |ORDER BY cnt DESC
        """.stripMargin
      ).collect()
      
      statusStats.foreach { row =>
        println(f"  ${row.getString(0)}%3s : ${row.getLong(1)}%,8d")
      }
      
      // 写入MySQL
      saveToMySQL(pv, uv, hotPages, hourStats, mysqlUrl, mysqlUser, mysqlPass)
      
      println("\n" + "=" * 70)
      println("✅ 分析完成！")
      println("=" * 70)
      
    } catch {
      case e: Exception =>
        println(s"❌ 作业失败: ${e.getMessage}")
        e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
  
  def parseArgs(args: Array[String]): (String, String, String, String) = {
    val logPath = if (args.length > 0) args(0) else "data/access.log"
    val mysqlUrl = if (args.length > 1) args(1) else "jdbc:mysql://fgedu81:3306/log_analyzer?useSSL=false&characterEncoding=utf8"
    val mysqlUser = if (args.length > 2) args(2) else "root"
    val mysqlPass = if (args.length > 3) args(3) else "root"
    (logPath, mysqlUrl, mysqlUser, mysqlPass)
  }
  
  def saveToMySQL(pv: Long, uv: Long, hotPages: Array[org.apache.spark.sql.Row],
                  hourStats: Array[org.apache.spark.sql.Row], url: String, user: String, pass: String): Unit = {
    var conn: Connection = null
    try {
      Class.forName("com.mysql.jdbc.Driver")
      conn = DriverManager.getConnection(url, user, pass)
      
      // 写入每日汇总
      val stmt = conn.prepareStatement(
        "INSERT INTO log_stats_daily (stat_date, pv, uv, avg_page_views) VALUES (CURDATE(), ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE pv = VALUES(pv), uv = VALUES(uv), avg_page_views = VALUES(avg_page_views)"
      )
      stmt.setLong(1, pv)
      stmt.setLong(2, uv)
      stmt.setDouble(3, pv.toDouble / uv)
      stmt.executeUpdate()
      stmt.close()
      
      // 写入小时统计
      val hourStmt = conn.prepareStatement(
        "INSERT INTO log_stats_hour (stat_date, hour, pv) VALUES (CURDATE(), ?, ?) " +
        "ON DUPLICATE KEY UPDATE pv = VALUES(pv)"
      )
      hourStats.foreach { row =>
        hourStmt.setString(1, row.getString(0))
        hourStmt.setLong(2, row.getLong(1))
        hourStmt.addBatch()
      }
      hourStmt.executeBatch()
      hourStmt.close()
      
      // 写入热门页面
      val truncateStmt = conn.createStatement()
      truncateStmt.execute("TRUNCATE TABLE log_stats_hotpages")
      truncateStmt.close()
      
      val pageStmt = conn.prepareStatement("INSERT INTO log_stats_hotpages (page, cnt, stat_date) VALUES (?, ?, CURDATE())")
      hotPages.foreach { row =>
        pageStmt.setString(1, row.getString(0))
        pageStmt.setLong(2, row.getLong(1))
        pageStmt.addBatch()
      }
      pageStmt.executeBatch()
      pageStmt.close()
      
      println("✅ 结果已保存到MySQL")
      
    } catch {
      case e: Exception =>
        println(s"❌ MySQL写入失败: ${e.getMessage}")
    } finally {
      if (conn != null) conn.close()
    }
  }
}
