# BTL Backend

README ngan gon de chay du an local.

## Yeu cau

- Docker Desktop
- Java 21
- Maven
- PowerShell

Kiem tra nhanh:

```powershell
java -version
mvn -version
docker --version
docker compose version
```

## Cach 1: Chay nhanh bang Docker Compose

### 1. Mo terminal tai thu muc project

```powershell
cd c:\Users\ADMIN\Downloads\LTW\BTL
```

### 2. Khoi dong toan bo stack

```powershell
docker compose up -d --build
```

### 3. Dang ky Debezium connector

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\register-mysql-cdc-connector.ps1
```

### 4. Kiem tra connector da chay

```powershell
Invoke-RestMethod http://localhost:8083/connectors/mysql-ltw-source/status | ConvertTo-Json -Depth 10
```

Neu thay `RUNNING` la duoc.

## Cach 2: Chay ha tang bang Docker, chay backend bang Maven

### 1. Khoi dong ha tang

```powershell
docker compose up -d mysql redis kafka kafka-connect elasticsearch kibana kafka-ui
```

### 2. Dang ky Debezium connector

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\register-mysql-cdc-connector.ps1
```

### 3. Set bien moi truong

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3307"
$env:MYSQL_DATABASE="ltw"
$env:MYSQL_USER="ltwuser"
$env:MYSQL_PASSWORD="ltwpassword"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:29092"
$env:ELASTICSEARCH_HOST="localhost"
$env:ELASTICSEARCH_PORT="9200"
```

### 4. Chay backend

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## URL can dung

- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Kafka Connect: `http://localhost:8083`
- Kafka UI: `http://localhost:8090`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

## Kiem tra nhanh

Danh sach san pham:

```powershell
Invoke-RestMethod http://localhost:8080/products
```

Tim kiem san pham:

```powershell
Invoke-RestMethod "http://localhost:8080/products/search?keyword=test&page=0&size=10"
```

## Dung du an

```powershell
docker compose down
```

Xoa ca du lieu local:

```powershell
docker compose down -v
```
