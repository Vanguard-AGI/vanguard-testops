# MeterSphere 本地部署文档

## 1 ├── Jenkinsfile                                     # 构建镜像使用的 jenkinsfile
├── Dockerfile                                      # 构建镜像使用的 Dockerfile
├── LICENSE
├── OWNERS
├── README.md                                       # 项目中文介绍
├── README-EN.md                                    # 项目英文介绍
├── SECURITY.md                                     # 安全说明
├── CODE_OF_CONDUCT.md                              # 
├── CONTRIBUTING.md 
├── build.md                                        # 构建过程
├── spotter-metersphere-app                         # 后端主应用模块
├── spotter-metersphere-domain                      # 领域模型模块
├── spotter-metersphere-sdk                         # SDK 模块
├── ... (其他业务模块直接平铺在根目录)
├── frontend                                        # 前端项目主目录
│   ├── src                                   # 前端代码目录
│   └── ...                               
├── .gitignore
├── mvnw
├── mvnw.cmd
├── pom.xml                                           # 整体 monorepo maven 项目使用的 pom 文件
```

## 2 环境要求

### 2.1 基础环境
- **操作系统**: Windows 10/11, Linux, macOS
- **Java**: JDK 21
- **Maven**: 3.8.6+
- **Node.js**: 18+ (用于前端开发)
- **Git**: 最新版本

### 2.2 中间件服务
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Kafka**: 2.8+
- **MinIO**: 最新版本

## 3 环境准备

### 3.1 安装基础软件

#### Windows 环境
```bash
# 1. 安装 JDK 21
# 下载并安装 OpenJDK 21 或 Oracle JDK 21

# 2. 安装 Maven
# 下载 Maven 3.8.6+ 并配置环境变量

# 3. 安装 Node.js
# 下载 Node.js 18+ 并安装

# 4. 验证安装
java -version
mvn -version
node -v
npm -v
```

### 3.2 启动中间件服务

#### 使用 Docker Compose (推荐)
创建 `docker-compose.yml` 文件：

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: metersphere-mysql
    environment:
      MYSQL_ROOT_PASSWORD: Password123@mysql
      MYSQL_DATABASE: metersphere_dev
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql.cnf:/etc/mysql/conf.d/mysql.cnf
    command: --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:6.2
    container_name: metersphere-redis
    command: redis-server --requirepass Password123@redis
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.0.0
    container_name: metersphere-kafka
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
      - "29092:29092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.0.0
    container_name: metersphere-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  minio:
    image: minio/minio:latest
    container_name: metersphere-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: Password123@minio
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

volumes:
  mysql_data:
  minio_data:
```

启动服务：
```bash
docker-compose up -d
```

## 4 数据库配置

### 4.1 MySQL 配置
创建 `mysql.cnf` 配置文件：

```ini
[mysqld]
default-storage-engine=INNODB
character_set_server=utf8mb4
lower_case_table_names=1
performance_schema=off
table_open_cache=128
transaction_isolation=READ-COMMITTED
max_connections=1000
max_connect_errors=6000
max_allowed_packet=64M
innodb_file_per_table=1
innodb_buffer_pool_size=512M
innodb_flush_method=O_DIRECT
innodb_lock_wait_timeout=1800

character-set-client-handshake = FALSE
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
init_connect='SET default_collation_for_utf8mb4=utf8mb4_general_ci'

sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION

skip-name-resolve

[mysql]
default-character-set=utf8mb4

[mysql.server]
default-character-set=utf8mb4
```

### 4.2 创建数据库
```sql
CREATE DATABASE metersphere_dev 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_general_ci;
```

## 5 MeterSphere 配置

### 5.1 创建配置目录
```bash
mkdir -p /opt/metersphere/conf
```

### 5.2 主配置文件
创建 `/opt/metersphere/conf/metersphere.properties`：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3307/metersphere_dev?autoReconnect=false&useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false
spring.datasource.password=Password123@mysql
spring.datasource.username=root

# Kafka 配置
kafka.bootstrap-servers=localhost:9092

# MinIO 配置
minio.endpoint=http://localhost:9000
minio.access-key=admin
minio.secret-key=Password123@minio

# 应用配置
server.port=8081
spring.application.name=metersphere
```

### 5.3 Redis 配置文件
创建 `/opt/metersphere/conf/redisson.yml`：

```yaml
singleServerConfig:
  password: Password123@redis
  address: "redis://localhost:6379"
  database: 1
```

## 6 项目构建

### 6.1 拉取代码
```bash
git clone https://github.com/metersphere/metersphere.git
cd metersphere
git checkout v3.x  # 切换到 v3.x 分支
```

### 6.2 后端构建
```bash
# 直接从根目录构建（跳过测试）
./mvnw clean install -DskipTests
```
url=jdbc:mysql://localhost:3307/metersphere_dev?autoReconnect=false&useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false
spring.datasource.password=Password123@mysql
spring.datasource.username=root

# Kafka 配置
kafka.bootstrap-servers=localhost:9092

# MinIO 配置
minio.endpoint=http://localhost:9000
minio.access-key=admin
minio.secret-key=Password123@minio

# 应用配置
server.port=8081
spring.application.name=metersphere
```

### 5.3 Redis 配置文件
创建 `/opt/metersphere/conf/redisson.yml`：

```yaml
singleServerConfig:
  password: Password123@redis
  address: "redis://localhost:6379"
  database: 1
```

## 6 项目构建

### 6.1 拉取代码
```bash
git clone https://github.com/metersphere/metersphere.git
cd metersphere
git checkout v3.x  # 切换到 v3.x 分支
```

### 6.2 后端构建
```bash
# 构建当前聚合工程（跳过测试）
./mvnw clean package -DskipTests
```

### 6.3 前端构建
```bash
cd frontend

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build
```

## 7 启动服务

### 7.1 后端启动
```bash
# 方式1: 直接运行 jar 包
java -jar start/target/start-3.x.jar

# 方式2: 使用 Maven 启动
cd start
mvn spring-boot:run
```

### 7.2 前端启动
```bash
cd frontend
npm run dev
```

## 8 访问验证

### 8.1 服务地址
- **前端**: http://localhost:3000 (开发模式)
- **后端**: http://localhost:8081
- **MinIO 控制台**: http://localhost:9001

### 8.2 默认账号
- **用户名**: admin
- **密码**: metersphere

## 9 开发模式配置

### 9.1 前端开发配置
修改 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8081
VITE_APP_TITLE=MeterSphere Dev
```

### 9.2 后端开发配置
在 `start/src/main/resources/application-dev.properties` 中添加：

```properties
# 开发环境配置
spring.profiles.active=dev
logging.level.io.metersphere=DEBUG
```

## 10 常见问题

### 10.1 端口冲突
如果端口被占用，可以修改配置文件中的端口号：
- 后端: `server.port=8081`
- 前端: `vite.config.dev.ts` 中的 `server.port`

### 10.2 数据库连接失败
检查：
1. MySQL 服务是否启动
2. 数据库连接配置是否正确
3. 防火墙是否开放端口

### 10.3 前端构建失败
```bash
# 清理缓存
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```



