# Minimum test matrix

| Change type | Minimum tests |
|---|---|
| Pure domain rule | Unit positive + relevant negative/boundary |
| Cayenne mapping/query | PostgreSQL Testcontainers integration |
| Write use case | Unit/domain + PostgreSQL/Cayenne integration + journal assertion |
| Controller/form | Spring MVC full-page + validation/authorization |
| HTMX interaction | MVC fragment + normal HTTP fallback |
| INTERLIS model/schema | ili2c 5.6.8 + fresh ili2pg schema + DB assertions + Cayenne import/cgen |
| XTF import/export | ilivalidator 1.15.0 before import and after export + real ili2pg process + TID/BID/basket assertions + semantic roundtrip + invalid-XTF rejection |
| Critical user journey | Targeted Playwright E2E |
| SIP/archive | Eligibility + valid/invalid package validation + audit/history integration |
