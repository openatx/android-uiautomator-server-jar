---
name: sync-methods
description: |-
  当 AutomatorService.java 接口有新增/删除/修改方法时，同步更新 tools/methods.json。
  触发条件：用户说"同步methods"、"更新methods.json"、"sync methods"、"sync jsonrpc"、
  或者你发现 AutomatorService.java 被修改后需要更新 methods.json。
---

# 同步 methods.json

## 目标

读取 `app/src/main/java/com/wetest/uia2/stub/AutomatorService.java`，解析出所有 JSON-RPC 方法定义，
写入 `tools/methods.json`。

**原则**：不需要写任何 Python/脚本解析器，你作为 AI 直接阅读理解 Java 源码，提取信息，手写 JSON。

---

## 1. 读取源文件

```bash
app/src/main/java/com/wetest/uia2/stub/AutomatorService.java
```

跳过文件开头的 license 注释和 package/import 声明，从 `public interface AutomatorService {` 开始解析。

---

## 2. 解析规则

### 2.1 分区识别

Java 文件中以 `/**********` 开头的大注释块是分区标记：

| 注释中的关键词 | 映射 category |
|---|---|
| `UiDevice` | `uidevice` |
| `UiObject`  | `uiobject` |
| `UiScrollable` | `uiscrollable` |

分区标记之后、下一个分区标记之前的所有方法，默认属于该 category。
第一个分区标记之前的方法（`ping`、`getLastToast`、`clearLastToast` 等），category 为 `core`。
最后一个分区标记之后的方法（Configurator、Clipboard、launchApp 等），按覆盖表决定 category。

### 2.2 方法提取

对每个方法，提取以下信息：

**a) description** — 从 Javadoc 注释（`/** ... */`）的第一句话提取。只取 `@param` / `@return` / `@throws` 之前的内容，合并为一行。

**b) name** — 方法名。不包含返回类型和参数。

**c) params** — 参数列表，每个参数提取 `{"name": "...", "type": "..."}`：
- 跳过 `final` 修饰符
- 类型保留完整写法（如 `int[]`、`Selector[]`、`String`、`boolean`、`long`）
- `@JsonRpcErrors` 等注解跳过，不影响解析

**d) 忽略**的条目：
- `final static int ERROR_CODE_BASE = -32000` 等字段声明
- 被 `/** Deprecated APIs ... */` 包裹的注释中的方法签名
- 空行和纯注释行

**注意**：重载方法是独立条目，每个重载单独生成一条记录（如 `click` 有多个重载）。

### 2.3 分类覆盖表

以下方法不跟随分区的默认 category，必须使用指定的分类：

```
# watcher 分类
hasWatcherTriggered
hasAnyWatcherTriggered
registerClickUiObjectWatcher
registerPressKeyskWatcher
removeWatcher
resetWatcherTriggers
runWatchers
getWatchers

# configurator 分类
getConfigurator
setConfigurator

# clipboard 分类
setClipboard
getClipboard
pasteClipboard
clearInputText

# app 分类
launchApp
executeShellCommand
```

---

## 3. 输出格式

写入 `tools/methods.json`，UTF-8 编码，2 空格缩进。完整 schema：

```json
{
  "methods": [
    {
      "name": "方法名",
      "category": "core|uidevice|uiobject|uiscrollable|watcher|configurator|clipboard|app",
      "description": "从Javadoc第一句话提取的描述，英文原文",
      "params": [
        {"name": "参数名", "type": "参数类型"}
      ],
      "example": []
    }
  ],
  "categories": {
    "core": "Core (connection, device info, toast)",
    "uidevice": "UiDevice (click, swipe, key, screenshot)",
    "uiobject": "UiObject (find, text, click, drag)",
    "uiscrollable": "UiScrollable (scroll, fling)",
    "watcher": "Watcher",
    "configurator": "Configurator",
    "clipboard": "Clipboard",
    "app": "App (launch, shell)"
  }
}
```

### example 字段生成规则

按参数类型依次生成示例值：

| Java 类型 | 示例值 |
|---|---|
| `int` | `0` |
| `long` | `5000` |
| `float` | `1.0` |
| `boolean` | `false` |
| `String` | `""` |
| `Selector` | `{"text": "Example"}` |
| `Selector[]` | `[{"text": "A"}, {"text": "B"}]` |
| `String[]` | `["a", "b"]` |
| `int[]` | `[100, 200]` |
| `Point` | `{"x": 100, "y": 200}` |
| `ConfiguratorInfo` | `{"actionAcknowledgmentTimeout": 3000}` |
| 其他未知类型 | `""` |

---

## 4. 写入 & 校验

### 4.1 写入文件

```
tools/methods.json
```

### 4.2 JSON 合法性校验

写完之后必须执行以下命令验证：

```bash
python3 -c "import json; data=json.load(open('tools/methods.json')); print(f'OK: {len(data[\"methods\"])} methods, {len(data[\"categories\"])} categories')"
```

如果报错，根据错误信息定位并修复 JSON，重新写入后再次校验，直到输出 `OK: N methods, 8 categories`。

---

## 5. 完成确认

校验通过后，告知用户同步结果：总共多少个方法，与上次相比有什么变化（如果有旧版本 methods.json 可以对比）。
