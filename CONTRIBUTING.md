# 贡献指南

感谢您考虑为AI-LiuYao项目做出贡献！本指南将帮助您了解如何参与项目开发。

## 开发环境设置

在开始开发之前，请确保您的环境已正确设置：

1. **安装必要软件**
   - JDK 11或更高版本
   - Maven 3.6或更高版本
   - MySQL 8.0或更高版本
   - Redis 6.0或更高版本
   - IDE (推荐IntelliJ IDEA或Eclipse)

2. **克隆仓库**
   ```bash
   git clone https://github.com/your-username/AI-LiuYao.git
   cd AI-LiuYao
   ```

3. **配置开发环境**
   - 复制`src/main/resources/application-example.yml`为`src/main/resources/application-dev.yml`
   - 修改配置文件中的数据库、Redis和API密钥等信息
   - 创建数据库(MySQL)：`ai_liuyao`

4. **构建项目**
   ```bash
   mvn clean install
   ```

## 分支管理

- `main`: 主分支，只接受来自`dev`分支的合并请求
- `dev`: 开发分支，所有功能开发完成后合并到此分支
- `feature/*`: 功能分支，用于开发新功能
- `bugfix/*`: 修复分支，用于修复bug
- `release/*`: 发布分支，用于准备新版本发布

## 开发流程

1. **创建新分支**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **编写代码并测试**
   - 遵循现有的代码风格和架构
   - 为新功能编写单元测试
   - 确保所有测试通过

3. **提交更改**
   ```bash
   git add .
   git commit -m "feat: 添加了新功能XYZ"
   ```
   提交信息请遵循[约定式提交](https://www.conventionalcommits.org/zh-hans/v1.0.0/)规范

4. **发起Pull Request**
   - 将您的分支推送到您的GitHub仓库
   - 创建一个新的Pull Request到`dev`分支
   - 在PR描述中详细说明您的更改

## 代码规范

- 遵循Java代码规范
- 使用4个空格进行缩进
- 类名使用PascalCase命名法
- 方法名和变量名使用camelCase命名法
- 常量使用全大写的SNAKE_CASE命名法
- 添加必要的JavaDoc注释

## 提交规范

提交信息应遵循以下格式：
```
<类型>: <描述>

[可选的正文]

[可选的脚注]
```

类型包括：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更改
- `style`: 代码风格更改(不影响代码运行的变动)
- `refactor`: 代码重构(既不是新增功能，也不是修改bug的代码变动)
- `perf`: 性能优化
- `test`: 测试
- `chore`: 构建过程或辅助工具的变动

## 问题和讨论

如果您有任何问题或想法，请通过以下方式与我们交流：
- 在GitHub Issues中提出问题
- 在Pull Request中讨论特定代码更改

感谢您的贡献！ 