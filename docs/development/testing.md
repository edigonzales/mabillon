# Teststrategie

Testing ist Bestandteil jeder Verhaltensänderung.

## Testebenen

| Änderung | Mindestnachweis |
|---|---|
| Reine Fachregel | positiver Test + relevante negative/boundary Fälle |
| Cayenne Mapping/Query | PostgreSQL-Testcontainers-Integration |
| Schreibender Use Case | Fachregel + PostgreSQL/Cayenne + Journalassertion, falls journalpflichtig |
| Controller/Formular | Spring MVC Full Page + Validation + Authorization |
| HTMX-Interaktion | Fragment + normaler HTTP-Fallback |
| INTERLIS Modell/Schema | ili2c + frisches ili2pg-Schema + DB-Assertions + Cayenne DB Import/cgen |
| XTF Import/Export | ilivalidator + echter Java-API-Import/Export + TID/BID/Basket/REF + Negativtest |
| Kritische User Journey | Playwright Java |
| SIP/Archiv | Eligibility + gültiges/ungültiges Paket + Audit/History |

## Datenbank

Persistenztests verwenden echtes PostgreSQL über Testcontainers, nicht H2. Tests dürfen SQL/Mapping nicht wegmocken, wenn genau diese Schicht Gegenstand des Tests ist.

## Isolation

Persistente Integrationstests verwenden eine definierte Baseline und stellen PostgreSQL-/Storage-/SIP-Zustand vor Testmethoden reproduzierbar wieder her. Testmethoden dürfen nicht von Ausführungsreihenfolge oder Mutationen anderer Tests abhängen.

## Golden Path

Der Playwright-Java-Test führt den Nomenklaturfall über die reale Spring-Boot-Anwendung, JTE-Formulare, Spring Security/CSRF, PostgreSQL und den Dokument-Storage aus. Er umfasst Dossier/Geschäft, Beteiligung, Unterlagen, Aufgabe, Prozess/Resultat und Abschluss.

## INTERLIS

Neben Prozess-/API-Erfolg werden fachliche Semantik und Identitäten geprüft. Der Roundtrip in eine frische Datenbank schützt BID, TID, REF und fachliche Attribute.

## Verbotene Abkürzungen

- failing Test löschen,
- `@Disabled` zum „Grünmachen“,
- Assertions ohne fachliche Begründung abschwächen,
- H2 als Persistenzersatz,
- Persistenz mocken, obwohl Mapping/SQL geprüft werden soll,
- Exceptions verschlucken, um einen Test weiterlaufen zu lassen.

## Kommandos

```bash
./gradlew test
./gradlew playwrightInstall
./gradlew test --tests guru.interlis.mabillon.PlaywrightGoldenPathE2ETest
```

Die CI führt die normale Test-Suite mit Java 25, Testcontainers und Chromium aus.
