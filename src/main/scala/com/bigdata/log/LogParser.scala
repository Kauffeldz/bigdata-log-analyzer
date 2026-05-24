package com.bigdata.log

/**
 * 日志解析器
 * 格式: 192.168.1.1 - - [2025-01-01 12:00:00] "GET /index.html HTTP/1.1" 200 1234
 */
object LogParser {
  
  def parse(line: String): Option[(String, String, String, String, String, String, Long, String)] = {
    try {
      // 提取IP
      val ip = line.split(" ")(0)
      
      // 提取时间
      val timeStart = line.indexOf("[") + 1
      val timeEnd = line.indexOf("]")
      if (timeStart <= 0 || timeEnd <= timeStart) return None
      val time = line.substring(timeStart, timeEnd)
      
      // 提取请求
      val quoteStart = line.indexOf("\"") + 1
      val quoteEnd = line.indexOf("\"", quoteStart)
      if (quoteStart <= 0 || quoteEnd <= quoteStart) return None
      val request = line.substring(quoteStart, quoteEnd)
      val requestParts = request.split(" ")
      val method = if (requestParts.length > 0) requestParts(0) else "GET"
      val page = if (requestParts.length > 1) requestParts(1) else "/"
      val protocol = if (requestParts.length > 2) requestParts(2) else "HTTP/1.1"
      
      // 提取状态码和大小
      val afterRequest = line.substring(quoteEnd + 2).trim
      val parts = afterRequest.split(" ")
      val status = if (parts.length > 0) parts(0) else "200"
      val size = if (parts.length > 1) {
        try parts(1).toLong 
        catch { case _: Exception => 0L }
      } else 0L
      
      // 提取User-Agent
      var userAgent = ""
      val lastQuote = afterRequest.lastIndexOf("\"")
      if (lastQuote > 0 && lastQuote < afterRequest.length - 1) {
        userAgent = afterRequest.substring(lastQuote + 1, afterRequest.length - 1)
      }
      
      Some((ip, time, method, page, protocol, status, size, userAgent))
      
    } catch {
      case e: Exception =>
        System.err.println(s"解析失败: ${e.getMessage}")
        None
    }
  }
}
