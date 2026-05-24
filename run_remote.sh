#!/bin/bash

echo "=========================================="
echo "日志分析平台 - 远程触发"
echo "本地: fgedu81 → 远程: fgedu91"
echo "=========================================="

# 直接SSH到91执行run.sh
ssh hadoop@192.168.1.91 "cd /data/soft/bigdata-log-analyzer && ./run.sh"

echo "✅ 远程执行完成"
