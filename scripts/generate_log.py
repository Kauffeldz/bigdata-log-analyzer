#!/usr/bin/env python
# -*- coding: utf-8 -*-
import random
from datetime import datetime, timedelta

IPS = ["192.168.1." + str(i) for i in range(1, 101)]

PAGES = [
    "/index.html", "/course.html", "/detail.html", "/about.html",
    "/contact.html", "/blog/1", "/blog/2", "/product/1",
    "/search?q=spark", "/login.html", "/register.html", "/user/center"
]

STATUS = [200, 200, 200, 200, 200, 200, 200, 200, 200, 304, 404, 500]

def generate_log(dt):
    ip = random.choice(IPS)
    time_str = dt.strftime("%d/%b/%Y:%H:%M:%S +0800")
    page = random.choice(PAGES)
    status = random.choice(STATUS)
    size = random.randint(500, 50000) if status == 200 else random.randint(0, 500)
    return '{} - - [{}] "GET {} HTTP/1.1" {} {}'.format(ip, time_str, page, status, size)

def main():
    lines = []
    start = datetime(2025, 1, 1)

    for i in range(50000):
        offset = random.randint(0, 604800)
        dt = start + timedelta(seconds=offset)
        if 8 <= dt.hour <= 22:
            lines.append(generate_log(dt))
        elif random.random() < 0.3:
            lines.append(generate_log(dt))

        if (i + 1) % 10000 == 0:
            print("生成: " + str(i+1) + "/50000")

    with open("data/access.log", "w") as f:
        f.write("\n".join(lines))

    print("✅ 完成! 共 " + str(len(lines)) + " 条日志")

if __name__ == "__main__":
    main()
