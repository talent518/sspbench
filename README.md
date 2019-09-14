# sspbench
java版sspd压力测试工具

```
java -jar sspbench.jar
usage: sspb [options]
 -c,--conns <arg>      ssp server conns pre a thread(default: 100)
 -h,--host <arg>       ssp server host(default: 127.0.0.1)
 -H,--host <arg>       mysql host(default: localhost)
 -N,--name <arg>       mysql database name(default: ssp)
 -p,--port <arg>       ssp server port(default: 8086)
 -P,--port <arg>       mysql port(default: 3306)
 -r,--requests <arg>   ssp server requests pre a connect(default: 10000)
 -t,--threads <arg>    ssp server threads(default: 8)
 -U,--user <arg>       mysql user(default: root)
 -W,--password <arg>   mysql password(default is empty)
```

