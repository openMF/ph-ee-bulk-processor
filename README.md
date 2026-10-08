
# Bulk Transaction Processor
**Core Function**: Domain-specific bulk processing engine for financial operations.

## Key Responsibilities
- Validates and transforms raw financial data (loans, repayments, fees)
- Applies business logic (interest calculations, payment allocations)
- Generates audit trails and error reports
- Prepares processed records for downstream systems

## Inputs
- CSV/Excel files via `/api/upload`
- Direct API payloads to `/api/process`
- Database polling from staging tables

## Outputs
- Standardized JSON to message queues (RabbitMQ/Kafka)
- Callbacks to originating systems
- Error reports in `ERROR_<batch_id>.csv`

## Dependencies
- Requires `ph-ee-connector-bulk` for outbound delivery
- Integrates with accounting rule engines


## SSL Configuration
```yaml
server:
  ssl:
    key-alias: "tomcat-https"
    key-store: "classpath:keystore.jks"
    key-store-type: JKS
    key-password: "<replace-with-password>"
    key-store-password: "<replace-with-password>"
  port: 8443
```
#### NOTE: For disabling TLS, change the port to "8080" and add null values for all the "ssl" related fields.

## Checkstyle
Use below command to execute the checkstyle test.
```shell
./gradlew checkstyleMain
```

## Spotless
Use below command to execute the spotless apply.
```shell
./gradlew spotlessApply
```
