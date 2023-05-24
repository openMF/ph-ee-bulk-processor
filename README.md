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
#### NOTE: For disabling TLS, change the port to "8080" and ass null values for all the "ssl" related fields. 
