# 配置文件说明

## 重要提示

⚠️ **本项目配置文件已加入 .gitignore，不会上传到仓库**

为了保护敏感信息（数据库密码、Redis密码等），配置文件默认不被 git 追踪。

## 如何启动项目

### 方法一：复制示例配置文件（推荐）

1. 复制示例配置文件：

```bash
# Windows (PowerShell)
Copy-Item "src\main\resources\application.yml.example" "src\main\resources\application.yml"
Copy-Item "src\main\resources\application-dev.yml.example" "src\main\resources\application-dev.yml"

# Linux/Mac
cp src/main/resources/application.yml.example src/main/resources/application.yml
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

2. 修改配置文件中的数据库密码等敏感信息

3. 启动项目

### 方法二：手动创建

如果示例文件不存在，手动创建以下配置文件：

#### application.yml (必填)

```yaml
server:
  port: 8080

spring:
  application:
    name: reggie_take_out
  profiles:
    active: dev  # dev 或 prod

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: ASSIGN_ID

reggie:
  path: ./uploads
```

#### application-dev.yml (开发环境)

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 123456  # 修改为你的数据库密码
      driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

#### application-prod.yml (生产环境，可选)

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/reggie?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: ${DB_PASSWORD:}  # 建议使用环境变量
      driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    log-impl:

reggie:
  path: ./uploads
```

## 数据库准备

1. 安装 MySQL 8.0+
2. 创建数据库：`CREATE DATABASE reggie CHARACTER SET utf8mb4;`
3. 导入 SQL：`mysql -u root -p reggie < reggie.sql`

## 启动项目

```bash
mvn clean package spring-boot:run
```

访问：
- 管理后台：http://localhost:8080/backend/index.html
- 移动端：http://localhost:8080/front/index.html

## 安全建议

1. ✅ 不要提交包含真实密码的配置文件
2. ✅ 生产环境使用环境变量或配置中心
3. ✅ 定期更换数据库密码
4. ✅ 使用强密码策略
