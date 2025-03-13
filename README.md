## Quick start

- Just add the dependency to an existing Spring Boot project

```xml
<dependency>
    <groupId>com.atviettelsolutions</groupId>
    <artifactId>spring-kpi-log-reactive</artifactId>
    <version>1.0.0</version>
</dependency>
```

- Then, add the following properties to your `application.properties` file.

```properties
kpilog.grpc.enable=true #if you wanna kpi log for grpc request
```

- Then, create a repository class that implements `KpiLogRepository` class if you want to write log to database, default is logging to console

```java
@Repository
public class LogRepitory extends ReactiveMongoRepository<KpiLog, String>, KpiLogRepository {
    @Override
    default Mono<Void> writeLog(KpiLog kpiLog) {
        return save(kpiLog).then(Mono.empty());
    }
}
```

## Usage

- Enable KPI Log by adding this line to your main application class:

```java
@SpringBootApplication
@EnableKpiLog // adding this line
public JavaMainApplication {
    public static void main(String[] args) {
        StringApplication.run(JavaMainApplication.class, args);
    }
}
```
