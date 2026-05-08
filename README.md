# Spring-Boot-Config-Drift-Detector

> CLI that compares `application.yml`, `application-*.yml` and `application.properties` across all stages of a Spring Boot project. Finds drift, missing keys, dangerous defaults and exports a Markdown report.

**Codename:** spring-drift

---

## Installation

Download the latest binary for your platform from [GitHub Releases](https://github.com/jolle93/spring-boot-config-drift-detector/releases).

### macOS

```bash
chmod +x spring-drift
# macOS Gatekeeper will block unsigned binaries on first run.
# Remove the quarantine flag once after downloading:
xattr -d com.apple.quarantine ./spring-drift
```

### Linux

```bash
chmod +x spring-drift
./spring-drift scan ./src/main/resources
```

---

## Usage

```bash
spring-drift scan <path-to-config-directory>
```

**Example:**

```bash
spring-drift scan ./src/main/resources
```

**Example output:**

```
✓ Found 4 profiles: default, dev, staging, prod
✗ 5 drift issues found

[DANGEROUS_DEFAULT] management.endpoints.web.exposure.include
  default:  health,info
  dev:      *
  staging:  *
  prod:     *           ← matches dev — likely accidental

[DANGEROUS_DEFAULT] spring.jpa.hibernate.ddl-auto
  default:  validate
  dev:      update
  staging:  update
  prod:     update      ← destructive in production

[MISSING_KEY] feature.new-checkout-flow.enabled
  dev:      true
  staging:  true
  prod:     <missing>   ← will fall back to null

Report written to drift-report.md
```

---

## Build from source

```bash
# JVM build
mvn package

# Native Image (requires GraalVM)
mvn package -Pnative
```

---

## License

[spring-drift](https://github.com/jolle93/spring-boot-config-drift-detector) is a commercial tool.
Get your license at [julianpaul.dev/spring-drift](https://julianpaul.dev/spring-drift).