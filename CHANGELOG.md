# Changelog

All notable changes to spring-drift will be documented in this file.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-05-08

### Added
- `ConfigLoader`: scans `application*.yml`, `application*.yaml` and `application*.properties`
  across all Spring Boot profiles in a given directory
- `DriftAnalyzer`: detects three drift types across profile pairs:
  - `DANGEROUS_DEFAULT` — known dangerous Spring Boot defaults active in production profiles
  - `MISSING_KEY` — key present in one profile but absent in another
  - `VALUE_DRIFT` — same key, different values across profiles
- `ReportGenerator`: outputs a Markdown drift report ready to link in pull requests
- Built-in severity rules for common Spring Boot pitfalls:
  - `management.endpoints.web.exposure.include=*` leaking into production
  - `spring.jpa.hibernate.ddl-auto` set to destructive values in production
  - `logging.level.*=DEBUG/TRACE` active in production profiles
  - `spring.datasource.url` referencing localhost outside dev profiles
  - `server.error.include-stacktrace=always` in production
- 19 unit tests covering profile resolution and drift detection logic
- GraalVM Native Image build via GitHub Actions matrix (Linux x64, macOS arm64)
- Single-binary distribution — no JRE required

[Unreleased]: https://github.com/jolle93/spring-boot-config-drift-detector/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/jolle93/spring-boot-config-drift-detector/releases/tag/v1.0.0
