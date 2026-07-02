# 监控看板配置

本目录包含 Prometheus + Grafana 监控栈的配置文件，用于监控瑞吉外卖系统。

## 快速开始

### 1. 启动监控栈（指标采集）

```bash
cd monitoring
docker-compose up -d
```

### 2. 启动日志收集（可选）

**方式一：Loki（推荐，轻量级）**

```bash
docker-compose --profile logs up -d
```

**方式二：ELK Stack（企业级）**

```bash
docker-compose --profile elk up -d
```

### 3. 访问监控界面

- **Grafana**: http://localhost:3000 (默认账号: `admin`, 密码: `admin`)
- **Prometheus**: http://localhost:9090
- **Alertmanager**: http://localhost:9093

**日志收集（可选）**:
- **Loki**: http://localhost:3100
- **ELK/Kibana**: http://localhost:5601

### 4. 导入仪表板

1. 登录 Grafana
2. 进入 **Dashboards** → **Import**
3. 选择 `grafana-dashboards/reggie-takeout-dashboard.json` 文件
4. 点击 **Import**

### 5. 配置数据源

如果数据源未自动配置：
1. 进入 **Connections** → **Data sources**
2. 点击 **Add data source**
3. 选择 **Prometheus** 或 **Loki**
4. 设置 URL:
   - Prometheus: `http://prometheus:9090`
   - Loki: `http://loki:3100`
5. 点击 **Save & Test**

## 目录结构

```
monitoring/
├── docker-compose.yml          # Docker Compose 编排文件
├── prometheus.yml              # Prometheus 主配置
├── alertmanager.yml            # Alertmanager 配置
├── prometheus/
│   └── alerts/
│       └── reggie_alerts.yml   # Prometheus 告警规则
└── grafana/
    └── provisioning/
        ├── datasources/         # Grafana 数据源配置
        │   └── prometheus.yml
        └── dashboards/          # Grafana 仪表板配置
            └── dashboard.yml
```

## 常用命令

```bash
# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose stop

# 停止并删除容器
docker-compose down

# 停止并删除容器及数据卷
docker-compose down -v

# 重启服务
docker-compose restart

# 查看服务状态
docker-compose ps
```

## 自定义配置

### 修改采集间隔

编辑 `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: "reggie-takeout"
    scrape_interval: 10s  # 修改为需要的间隔
```

### 添加新的告警规则

在 `prometheus/alerts/` 目录下创建新的 `.yml` 文件。

### 修改告警通知

编辑 `alertmanager.yml` 配置邮件、钉钉或其他通知渠道。

## 生产环境建议

1. **持久化存储**: 配置 volumes 确保数据持久化
2. **高可用部署**: 使用 Prometheus 集群和 Alertmanager 集群
3. **认证授权**: 配置 Grafana 和 Prometheus 的认证
4. **备份策略**: 定期备份监控数据和配置
5. **资源限制**: 为容器设置 CPU 和内存限制

## 参考文档

- [完整文档](../docs/监控看板配置示例.md)
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
