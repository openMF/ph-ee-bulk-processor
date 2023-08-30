#Auto-Trigger

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
