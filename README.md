# Banker's Briefcase

一个基于 Spring Boot 和原生前端实现的网页小游戏。玩家先选择自己的专属箱子，再按轮次打开其他箱子，系统会根据剩余奖金计算银行报价，玩家可以选择成交、继续开箱，或在最后阶段保留/交换箱子。

## 功能

- 26 个箱子随机分配不同奖金金额
- 按轮次控制开箱数量
- 根据剩余奖金期望值生成银行报价
- 支持成交、拒绝报价、最终保留或交换箱子
- 前端包含奖金面板、开箱状态、结果弹窗和简单音效
- 后端包含核心游戏逻辑测试

## 技术栈

- Java 17
- Spring Boot 4
- Gradle
- JUnit 5
- HTML / CSS / JavaScript

## 本地运行

```bash
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

启动后访问：

```text
http://localhost:8080
```

## 测试

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

