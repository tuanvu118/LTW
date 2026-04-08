# MySQL to Elasticsearch CDC

## Stack hien tai sau khi nang cap

- Spring Boot la application chinh.
- MySQL la source of truth.
- Kafka la event backbone.
- Kafka Connect chay Debezium MySQL connector.
- Elasticsearch la search/read model.
- Kibana va Kafka UI dung de quan sat.

## Luong du lieu khuyen nghi

`Spring Boot write -> MySQL commit -> Debezium (Kafka Connect) doc binlog -> Kafka topic per table -> Spring Boot CDC consumer -> Elasticsearch index -> /products/search doc tu Elasticsearch`

## Vi sao chon phuong an nay

- Khong dual-write tu application qua MySQL va Elasticsearch.
- De local chay bang Docker Compose, khong can them Kafka Streams hay mot service reindex rieng.
- Search document hien tai la aggregate tu nhieu bang (`product`, `product_variation`, `category`), nen Elasticsearch sink connector truc tiep se kho dung cho domain doc.
- Consumer trong Spring Boot cho phep rehydrate tu MySQL hien trang thai moi nhat, nen eventual consistency hoi tu tot hon khi co event tu nhieu bang lien quan.

## Service can co trong Docker

- `mysql`: luu du lieu goc, bat row-based binlog cho Debezium.
- `redis`: stack hien tai dang dung.
- `kafka`: backbone cho CDC events.
- `kafka-connect`: host Debezium MySQL connector.
- `kafka-ui`: xem topic, message, connector state.
- `elasticsearch`: search/read model.
- `kibana`: inspect index.
- `backend`: Spring Boot app vua ghi MySQL vua consume CDC de index Elasticsearch.

Luu y: MySQL trong Compose map host port `3307` de tranh xung dot voi mot MySQL host san co. Ben trong Docker, Debezium va backend van dung `mysql:3306`.

## Topic va state quan trong

- Debezium schema history: `schemahistory.mysql-ltw`
- Kafka Connect internal: `connect-configs`, `connect-offsets`, `connect-status`
- CDC topics:
- `mysql-ltw.ltw.product`
- `mysql-ltw.ltw.product_variation`
- `mysql-ltw.ltw.category`
- Consumer group Spring Boot: `btl-search-indexer`
- Elasticsearch index: `products_v1`

## Chay local tu dau den cuoi

1. Khoi dong stack:

```powershell
docker compose up -d --build
```

2. Kiem tra Kafka Connect da len:

```powershell
Invoke-RestMethod http://localhost:8083/connectors
```

3. Dang ky Debezium MySQL connector:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\register-mysql-cdc-connector.ps1
```

4. Kiem tra connector status:

```powershell
Invoke-RestMethod http://localhost:8083/connectors/mysql-ltw-source/status | ConvertTo-Json -Depth 10
```

5. Tao hoac sua du lieu trong MySQL thong qua app hoac SQL truc tiep. Vi du:

```powershell
docker compose exec mysql mysql -uspringstudent -pspringstudent ltw -e "INSERT INTO category(name) VALUES ('Shoes');"
docker compose exec mysql mysql -uspringstudent -pspringstudent ltw -e "INSERT INTO product(title, description, price, category_id, created_at) VALUES ('Air Local', 'CDC test product', 199.99, 1, NOW());"
```

6. Kiem tra Kafka co event:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic mysql-ltw.ltw.product --from-beginning --max-messages 5
```

7. Kiem tra Elasticsearch nhan document:

```powershell
Invoke-RestMethod http://localhost:9200/products_v1/_search?pretty
```

8. Goi API search de xac nhan read model:

```powershell
Invoke-RestMethod "http://localhost:8080/products/search?keyword=Air&page=0&size=5"
```

## Lenh xac minh tung lop

### MySQL emit event

```powershell
docker compose exec mysql mysql -uroot -proot -e "SHOW VARIABLES LIKE 'log_bin';"
docker compose exec mysql mysql -uroot -proot -e "SHOW VARIABLES LIKE 'binlog_format';"
```

Gia tri ky vong:

- `log_bin = ON`
- `binlog_format = ROW`

### Kafka nhan duoc event

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic mysql-ltw.ltw.product_variation --from-beginning --max-messages 5
```

### Debezium dang hoat dong

```powershell
Invoke-RestMethod http://localhost:8083/connectors/mysql-ltw-source/status | ConvertTo-Json -Depth 10
```

Ky vong:

- `connector.state = RUNNING`
- `tasks[0].state = RUNNING`

### Elasticsearch nhan du lieu

```powershell
Invoke-RestMethod http://localhost:9200/products_v1/_count?pretty
Invoke-RestMethod http://localhost:9200/products_v1/_search?pretty
```

## Trade-off ngan gon

- Phuong an duoc chon: Debezium source + Spring Boot consumer + Elasticsearch repository.
- Loi ich: kiem soat duoc aggregate doc, delete/update cross-table, local de chay, sau nay len production de tach consumer thanh service rieng neu can.
- Danh doi: backend phai co them consumer logic, indexing do app dam nhiem thay vi chi cau hinh connector.

## Rui ro va diem de loi

- Debezium MySQL bat buoc can row-based binlog; thieu `log_bin` hoac `binlog_format=ROW` la connector khong chay.
- Neu MySQL purges binlog qua som trong khi connector down lau, Debezium co the phai snapshot lai.
- Topic ordering chi dam bao theo key trong cung partition; vi vay consumer dang rehydrate tu MySQL current state de hoi tu cuoi cung.
- Doi ten category khong can product event, nen consumer phai lang nghe ca topic `category`.
- Xoa index Elasticsearch ma khong reset consumer group thi du lieu khong tu rebuild; khi do can xoa group offsets hoac re-run snapshot.
- Debezium images tu `quay.io` phu hop local/test; production nen dung Kafka Connect va connector image da qua hardening.
