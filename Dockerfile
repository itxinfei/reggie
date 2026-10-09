# Reggie 外卖系统 — 多阶段构建
# 阶段一：JDK 8 + Maven 编译（enforcer 强制 [1.8,1.9)，必须用 JDK 8 镜像）
# 阶段二：JRE 8 运行时
FROM maven:3.8-openjdk-8 AS build
WORKDIR /build

# 先拷 pom 单独拉依赖，源码不变时命中缓存层，二次构建不重下依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:8-jre-jammy
RUN apt-get update -qq \
    && apt-get install -y -qq --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -Duser.timezone=Asia/Shanghai"

# 上传根目录（reggie.path 指向此卷，随容器卷持久化）
RUN mkdir -p /data/uploads
WORKDIR /app

COPY --from=build /build/target/reggie_take_out-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=10 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
