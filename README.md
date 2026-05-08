# Spring-Boot-Config-Drift-Detector

> CLI that compares `application.yml`, `application-*.yml` and `application.properties` across all stages of a Spring Boot project. Finds drift, missing keys, dangerous defaults and exports a Markdown report.

**Codename:** spring-drift

## Usage

```bash
spring-drift scan ./src/main/resources
```

## Build

```bash
mvn package -Pnative
```
