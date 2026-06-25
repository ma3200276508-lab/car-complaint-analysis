# 贡献指南

欢迎为「汽车投诉数据可视化分析」项目贡献代码、文档或反馈！

## 如何贡献

### 报告问题

在 Issues 中提交 Bug 或功能建议，请包含：
- 问题描述（预期行为 vs 实际行为）
- 复现步骤
- 环境信息（OS、JDK版本、Hadoop/Spark版本）

### 提交代码

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feat/your-feature`
3. 遵循现有代码风格
4. 确保代码可编译运行
5. 提交 Pull Request 到 `main` 分支

### 代码风格

- **Scala**: 遵循 Spark 官方代码风格，使用 `camelCase` 命名
- **Java**: 遵循 Google Java Style，类名 `PascalCase`，方法名 `camelCase`
- **JavaScript**: ES6+，使用 `const`/`let`，箭头函数
- 所有代码添加必要的中文注释

### 项目结构约定

```
src/main/scala/car/complaint/analysis/   # Spark分析代码
src/main/java/car/complaint/web/         # Spring Boot代码
docs/                                     # 文档教程
scripts/                                  # 辅助脚本
```

## 开发环境

见 [docs/01-环境搭建指南.md](docs/01-环境搭建指南.md)

## 联系方式

提交 Issue 或 Pull Request 即可，我们会尽快回复。
