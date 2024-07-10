# 测试说明
先安装依赖

```bash
pip install -r requirements.txt
```

安装被测应用到手机上
https://github.com/openatx/u2testdemo/releases/download/0.1.0/app-debug.apk

先将jar包run起来，然后运行pytest测试

```bash
pytest --html report.html
```