# Mabillon – Implementierungs- und Coding-Agent-Spezifikation

**Status:** Entwurf v0.4  
**Datum:** 2026-08-16  
**Ziel:** Implementierungsgrundlage für einen LLM-Coding-Agent und menschliche Reviews  
**Primärsprache der Fachdomäne/UI:** Deutsch  
**Produktname:** `Mabillon`  
**Claim:** `Einfache und transparente Geschäftsverwaltung`  
**Gradle Group:** `guru.interlis`  
**Java-Basispaket:** `guru.interlis.mabillon`  
**Artefakt-/Repository-Name:** `mabillon`  

---

## 0. Verbindlichkeit dieser Spezifikation

Diese Spezifikation ist für die Implementierung verbindlich. Sie definiert:

- den fachlichen Scope des einfachen GEVER,
- die Use Cases,
- die Architektur,
- die Technologieentscheidungen,
- die Projekt- und Paketstruktur,
- die Klassen und wesentlichen Methoden,
- die Modell-/DB-/Cayenne-Pipeline,
- die Teststrategie,
- die Phasen und deren harte Abschlusskriterien,
- die Arbeitsregeln für einen LLM-Coding-Agent.

Der Coding Agent darf **keine spätere Phase beginnen**, bevor die aktuelle Phase vollständig abgeschlossen, getestet, dokumentiert und vom Benutzer zur Fortsetzung freigegeben wurde.

Ein „fast fertig“, „Tests folgen später“, „temporär deaktiviert“ oder „TODO für nächste Phase“ gilt **nicht** als erfolgreicher Phasenabschluss, sofern das TODO Bestandteil der aktuellen Phase ist.

---

# 1. Produktziel

Das Produkt heisst **Mabillon** mit dem Claim **„Einfache und transparente Geschäftsverwaltung“**. Mabillon implementiert ein bewusst einfaches, transparentes und robustes GEVER für Fachverwaltungen. Es ist kein generisches BPM-System und kein vollständiger Ersatz für grosse Enterprise-GEVER-Produkte.

Die Anwendung soll insbesondere folgende Eigenschaften haben:

1. serverseitig gerendertes HTML,
2. möglichst wenig JavaScript,
3. progressive Interaktion mit HTMX,
4. fachlich führendes INTERLIS-2.4-Modell,
5. PostgreSQL-Schema aus INTERLIS via ili2pg,
6. DB-first-ORM-Mapping via Apache Cayenne,
7. klarer modularer Monolith statt Microservices,
8. explizite Use-Case-Services statt „magischem CRUD“,
9. vollständige Nachvollziehbarkeit fachlich relevanter Aktionen,
10. Unterstützung des gesamten Lebenszyklus bis zur Archivablieferung/SIP.

Der Haupt-Golden-Path ist die Nomenklaturmutation:

> Gemeinde Musterwil beantragt die Umbenennung des Flurnamens „Im alten Boden“ zu „Bodenrain“.

Dieser Fall dient während der gesamten Entwicklung als Referenz- und End-to-End-Testfall.

---

# 2. Technischer Baseline-Stack

## 2.1 Verbindliche Versionen

- Java 25
- Spring Boot 4.1.0
- Spring MVC, Servlet Stack
- JTE 3.2.4, Spring-Boot-4-Starter
- HTMX 2.0.10
- Apache Cayenne 5.0-M2
- PostgreSQL als einzige produktive relationale Datenbank
- Gradle mit Groovy DSL; Wrapper im Projekt
- JUnit Jupiter / AssertJ
- Testcontainers PostgreSQL für Integrationstests
- Playwright Java für wenige, aber echte Browser-E2E-Tests
- ili2pg 5.5.2 für INTERLIS → PostgreSQL und XTF-Import/-Export
- ili2c 5.6.8 für verbindliche `.ili`-Validierung/-Kompilation
- ilivalidator 1.15.0 für verbindliche XTF-Validierung

Spring Boot 4.1.0 ist der feste Zielstand. Kein Upgrade auf 4.1.1, 4.2 oder eine andere Feature-Version ohne explizite Freigabe.

Cayenne 5.0-M2 ist bewusst ein Milestone. Der Agent darf nicht stillschweigend auf 4.2.x zurückfallen oder auf einen neueren Cayenne-Milestone wechseln.

## 2.2 Warum kein grosses UI-/Business-Framework

Nicht einsetzen:

- Jmix,
- Vaadin,
- React/Vue/Angular als SPA,
- Bootstrap als verpflichtende Basis,
- Tailwind als verpflichtende Basis,
- BPMN-Engine für den MVP.

Die UI verwendet:

- JTE,
- HTMX,
- modernes Vanilla CSS,
- die bestehende `ili2grails`-Designsprache als **normative visuelle und strukturelle Referenz**, jedoch ohne daraus eine Bootstrap-Abhängigkeit für Mabillon abzuleiten.

Normative Designreferenz für Phase 0:

```text
Repository: edigonzales/ili2grails
Referenz-Commit: 3e133a976a0ed1c704f38e81a6493501e0568ec4
CSS: target-grails/src/main/resources/grails/overlays/ui-assets/stylesheets/ili-modern.css
Mockups: mockups/01-application-shell-dashboard.png
         mockups/02-domain-list-search-filter.png
         mockups/03-object-detail-workspace.png
         mockups/04-domain-edit-form.png
         mockups/05-multi-domain-workspace.png
```

Die visuelle Referenz umfasst insbesondere:

- ruhige neutrale Flächen, klare 1px-Rahmen und nur sehr subtile Schatten,
- kleine Radien (Referenz: 3px), keine stark gerundeten Karten,
- klaren App-Shell-Aufbau mit Topbar, Navigation, Breadcrumbs und Content-Bereich,
- semantische Page Header mit Titel, Untertitel und Action-Gruppe,
- definierte Form Sections, Validierungsdarstellung und sticky Form Actions,
- Filter-/Suchleisten, aktive Filter, Tabellen, Row Actions und Pagination,
- konsistente Status-/Hinweis-Komponenten und klare Typografie.

Die Mabillon-Implementierung darf diese Muster in eigenem Vanilla CSS nachbilden. Sie soll **nicht** Bootstrap nur deshalb einführen, weil `ili-modern.css` in ili2grails Bootstrap-Variablen überschreibt. Primärfarbe/Branding wird als Token konfiguriert; Struktur, Neutralfarben, Dichte, Radien und Interaktionsmuster orientieren sich an der Referenz.

Es gibt keine Node-Build-Pipeline, solange sie nicht zwingend benötigt wird.

---

# 3. Architekturprinzipien

## 3.1 Modularer Monolith

Die Anwendung ist ein einzelnes deploybares Spring-Boot-Artefakt.

Keine Microservices für:

- Dokumente,
- Aufgaben,
- Archivierung,
- Suche,
- Kataloge,
- INTERLIS.

Eine spätere Auslagerung ist nur zulässig, wenn echte Betriebsgründe bestehen.

## 3.2 Feature-orientierte Struktur

Nicht primär:

```text
controller/
service/
repository/
entity/
```

Sondern:

```text
guru.interlis.mabillon
├── app
├── common
├── security
├── catalog
├── masterdata
├── registraturplan
├── dossier
├── geschaeft
├── beteiligung
├── unterlage
├── aufgabe
├── fachsystem
├── journal
├── search
├── quality
├── interlis
├── archivierung
└── persistence
```

Innerhalb eines Features dürfen bei Bedarf Unterpakete `web`, `application`, `view` und `internal` verwendet werden.

## 3.3 Produkt-, Code- und Modell-Naming

Verbindliche technische Identität der Anwendung:

```text
Produkt:             Mabillon
Claim:               Einfache und transparente Geschäftsverwaltung
Gradle group:        guru.interlis
Java base package:   guru.interlis.mabillon
Artifact name:       mabillon
DB schema (fachlich): mabillon
DB schema (technisch): mabillon_app
```

Gradle-Grundkonfiguration:

```groovy
// settings.gradle
rootProject.name = "mabillon"

// build.gradle
group = "guru.interlis"
```

Die Spring-Boot-Hauptklasse heisst `guru.interlis.mabillon.MabillonApplication`.

Der Java-Namespace `guru.interlis` ist die organisatorische Basis; Anwendungsklassen liegen unter `guru.interlis.mabillon`. Keine neuen Klassen unter dem alten Namespace `ch.so.agi.gever`.

**Wichtig:** Der Produktname ändert das bestehende INTERLIS-Transfermodell nicht automatisch. Die Ausgangsbasis bleibt in dieser Spezifikationsversion bewusst:

```text
MODEL SO_AGI_GEVER_20260707
AT "https://agi.so.ch/models"
```

Modellname, Modell-URI, Topic-Namen und XTF-QNames sind Teil der INTERLIS-Schnittstelle. Der Coding Agent darf sie nicht nur aus Branding-Gründen auf `Mabillon` umbenennen. Eine spätere generische Open-Source-Modellbezeichnung ist eine eigene Modell-/Migrationsentscheidung und benötigt explizite Freigabe, aktualisierte XTF-Fixtures sowie Roundtrip-Tests.

## 3.4 INTERLIS ist fachliche Source of Truth

Das führende fachliche Modell ist das INTERLIS-Modell:

```text
model/SO_AGI_GEVER_20260707.ili
```

Die aktuelle Ausgangsbasis ist das Modell `SO_AGI_GEVER_20260707` mit den Topics:

```text
Kataloge
Stammdaten
Geschaeftsdaten
```

Grundregel:

> Eine fachliche persistente Schemaänderung beginnt im INTERLIS-Modell.

Der Agent darf keine fachliche Spalte direkt in PostgreSQL oder Cayenne erfinden.

Ausnahmen sind rein technische Anwendungstabellen, die nicht Bestandteil des GEVER-Austauschmodells sind. Diese liegen in einem separaten DB-Schema `mabillon_app` und müssen explizit in dieser Spezifikation oder einem ADR begründet werden.

## 3.5 Datenbankschema aus INTERLIS

Referenzfluss:

```text
INTERLIS 2.4
    ↓
ili2c compile
    ↓
ili2pg --schemaimport
    ↓
PostgreSQL / Schema mabillon
    ↓
Cayenne DB Import
    ↓
Cayenne DataMap
    ↓
cgen
    ↓
generierte Persistenzklassen
```

## 3.6 Cayenne ist Persistenztechnologie, nicht Anwendungsarchitektur

Webcontroller arbeiten nie direkt mit `ObjectContext` oder generierten Cayenne-Klassen.

Controller → Application Service → Cayenne Unit of Work → Cayenne Persistent Objects.

JTE-Templates erhalten View Models, keine Cayenne-Objekte.

## 3.7 Keine Open-Session-in-View-Analogie

Kein `ObjectContext` wird:

- in der HTTP-Session gespeichert,
- über mehrere Requests geteilt,
- direkt in JTE verwendet,
- global als mutierbarer Singleton verwendet.

Jeder schreibende Use Case erhält einen eigenen kurzen `ObjectContext`.

---

# 4. Fachliche Begriffe

## 4.1 Registraturplan / Ordnungssystem

Im INTERLIS-Modell bleiben die Begriffe:

- `Ordnungssystem`
- `OrdnungssystemPosition`

In der UI wird standardmässig der Begriff **Registraturplan** verwendet.

Ein Dossier ist genau einer Registraturplanposition zugeordnet.

## 4.2 Dossier

Das Dossier ist die aktenmässige Einheit.

Es beantwortet:

- Wo ist die Akte klassifiziert?
- Welche Unterlagen gehören zur Akte?
- Welche Geschäfte sind in der Akte enthalten?
- Ist die Akte offen/geschlossen/archiviert/vernichtet?

## 4.3 Geschäft

Das Geschäft ist der bearbeitete Vorgang.

Es beantwortet:

- Was wird bearbeitet?
- Welche Geschäftsart liegt vor?
- Wer ist verantwortlich?
- Welcher Prozessstatus gilt?
- Welche Aufgaben sind offen?
- Welches Resultat liegt vor?

## 4.4 Beziehung Dossier ↔ Geschäft

- Ein Geschäft gehört genau zu einem Dossier.
- Ein Dossier kann mehrere Geschäfte enthalten.

Normalfall im ersten Fachbereich: häufig 1 Dossier = 1 Geschäft.

Das Modell darf trotzdem mehrere Geschäfte pro Dossier unterstützen.

## 4.5 Unterlage

Eine Unterlage ist ein fachliches Aktenstück, z. B.:

- Antrag,
- E-Mail,
- Brief,
- Plan,
- Entscheidvorlage,
- Beschluss,
- Mitteilung.

Eine Unterlage:

- gehört zwingend zu einem Dossier,
- kann optional einem Geschäft als Geschäftskontext zugeordnet sein.

Konsistenzregel:

> Ist eine Unterlage einem Geschäft zugeordnet, muss dieses Geschäft zum selben Dossier gehören wie die Unterlage.

## 4.6 Aufgabe

Eine Aufgabe ist ein konkreter Arbeitsschritt innerhalb genau eines Geschäfts.

## 4.7 Beteiligter/Beteiligung

- `Beteiligter` = Wer?
- `Beteiligung` = Welche Rolle hat dieser Beteiligte in diesem konkreten Geschäft?

## 4.8 Kataloge, Stammdaten, Geschäftsdaten

### Kataloge

- Geschäftsart
- Prozessstatus
- Resultatstatus
- Beteiligungsrolle
- Unterlagentyp
- Aufgabentyp

### Stammdaten

- Organisationseinheit
- Benutzer
- Ordnungssystem / Registraturplan
- OrdnungssystemPosition / Registraturplanposition

### Geschäftsdaten

- Dossier
- Geschäft
- Beteiligter
- Beteiligung
- Unterlage
- Aufgabe
- FachsystemReferenz
- Archivierung
- Ereignis
- die in Phase 0 neu zu ergänzenden Archiv-Ablieferungsobjekte

---

# 5. Notwendige Modellergänzungen vor Implementierungsbeginn

Die Use Cases haben Anforderungen sichtbar gemacht, die im bisherigen Modell nicht ausreichend repräsentiert sind. Diese Änderungen sind Bestandteil von **Phase 0**.

## 5.1 Geschäftsart: Resultatpflicht

`Kataloge.Geschaeftsart` erhält:

```text
resultatErforderlich : MANDATORY BOOLEAN
```

Bedeutung:

- `true`: Geschäft darf nicht abgeschlossen werden, solange `resultatStatus` fehlt.
- `false`: Ergebnisstatus ist optional.

## 5.2 Archivablieferung als eigenes Objekt

Neue Klasse `Geschaeftsdaten.ArchivAblieferung`.

Attribute:

```text
ablieferungsnummer : MANDATORY Code
titel               : MANDATORY KurzerText
status              : MANDATORY ArchivAblieferungsStatus
erstelltAm          : MANDATORY XMLDateTime
erstelltVon         : REFERENCE TO Benutzer
archivEmpfaenger    : KurzerText
uebergebenAm        : XMLDateTime
uebernommenAm       : XMLDateTime
bemerkung           : LangerText
```

Neuer Domain-Typ `ArchivAblieferungsStatus`:

```text
Entwurf
Bereit
SIP_Erstellt
Validiert
Uebergeben
Uebernommen
Abgelehnt
Korrektur_erforderlich
```

## 5.3 SIP-Paket als nachvollziehbarer Erzeugungsversuch

Neue Klasse `Geschaeftsdaten.SipPaket`.

Attribute:

```text
laufnummer            : MANDATORY 1..999
status                 : MANDATORY SipStatus
erstelltAm             : MANDATORY XMLDateTime
erstelltVon            : REFERENCE TO Benutzer
storageUri             : UriText
hashSha256             : HashSha256
dateigroesse           : DateigroesseBytes
validierungsStatus     : MANDATORY SipValidierungsStatus
validiertAm            : XMLDateTime
validierungsberichtUri : UriText
bemerkung              : LangerText
```

`SipStatus`:

```text
Erzeugt
Verworfen
Uebergeben
```

`SipValidierungsStatus`:

```text
Nicht_validiert
Gueltig
Gueltig_mit_Warnungen
Ungueltig
```

## 5.4 Beziehungen Archivierung

Neue Association:

```text
ArchivAblieferung_Dossier
```

Kardinalität:

- eine Ablieferung enthält 1..* Dossiers,
- ein Dossier kann 0..* Ablieferungsversuchen zugeordnet sein.

Neue Association:

```text
SipPaket_ArchivAblieferung
```

Kardinalität:

- jedes SIP-Paket gehört genau zu einer Archivablieferung,
- eine Archivablieferung kann mehrere SIP-Erzeugungsversuche besitzen.

## 5.5 Ereignistypen erweitern

`EreignisObjektTyp` ergänzen um:

```text
ArchivAblieferung
SipPaket
```

`EreignisTyp` ergänzen um:

```text
SIP_erzeugt
SIP_validiert
Archivablieferung_uebergeben
Archivablieferung_uebernommen
Archivablieferung_abgelehnt
```

## 5.6 Keine weiteren Modelländerungen ohne Use Case

Phase 0 darf keine spekulativen Felder ergänzen.

Insbesondere nicht vorsorglich einführen:

- Dokumentversionierung,
- detaillierte ACLs,
- BPMN-Definitionen,
- Volltextindex-Metadaten,
- elektronische Signaturen,
- Aufbewahrungsregel-Engine.

---

# 6. Identifikations- und Naming-Schema

## 6.1 Drei Identitätsebenen

Persistente Geschäftsobjekte können drei unterschiedliche IDs haben:

1. DB-interner Primärschlüssel (`t_id`),
2. stabile INTERLIS-Transferidentität (`t_ili_tid`),
3. fachliche menschenlesbare Nummer.

Diese sind nie gleichzusetzen.

## 6.2 Fachliche Nummern

Format:

```text
<ORG>-<TYP>-<JAHR>-<6-stellige Sequenz>
```

Typen:

```text
G = Geschäft
D = Dossier
A = Archivablieferung
```

Beispiele:

```text
AGI-G-2026-000421
AGI-D-2026-000007
AGI-A-2028-000001
```

Keine fachliche Klassifikation wie `NOM` in die ID aufnehmen.

## 6.3 Sequenzen

Die Nummernvergabe muss transaktions- und konkurrenzsicher sein.

Dafür ist eine technische Tabelle im Schema `mabillon_app` erlaubt:

```text
number_sequence
---------------
organisation_code
object_type
year
last_value
```

Unique Key:

```text
(organisation_code, object_type, year)
```

Die Inkrementierung muss atomar in PostgreSQL erfolgen.

## 6.4 Java Value Objects

```java
public record DossierNumber(String value) {}
public record GeschaeftNumber(String value) {}
public record ArchivAblieferungNumber(String value) {}
```

Jeweils:

```java
public static XNumber parse(String value)
public String value()
```

Validierung über zentralen Regex.

---

# 7. DB- und ili2pg-Strategie

## 7.1 DB-Schemas

```text
mabillon      fachliche, von ili2pg erzeugte Tabellen
mabillon_app  rein technische Anwendungstabellen
```

## 7.2 Feste lokale INTERLIS-Toolchain

Für die lokale Agentenentwicklung sind folgende Versionen und Pfade verbindlich:

```text
ili2pg 5.5.2
/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar

ili2c 5.6.8
/Users/stefan/apps/ili2c-5.6.8/ili2c.jar

ilivalidator 1.15.0
/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar
```

Die Pfade werden zentral in `scripts/interlis-tools-env.sh` oder einer gleichwertigen zentralen Konfiguration definiert. Unterstützte Overrides sind `ILI2PG_JAR`, `ILI2C_JAR` und `ILIVALIDATOR_JAR`; die Defaults bleiben exakt die oben genannten lokalen Pfade. Phase 0 muss für alle drei JARs Existenz und aufrufbare Version nachweisen.

Verbindliche Verantwortlichkeiten:

- **ili2c 5.6.8** validiert/kompiliert jedes produktive oder geänderte `.ili`-Modell, bevor daraus ein DB-Schema erzeugt oder Cayenne aktualisiert wird.
- **ilivalidator 1.15.0** validiert jedes eingecheckte, erzeugte, exportierte oder vor einem Import angenommene `.xtf`, sofern nicht ein expliziter Negativtest absichtlich ungültige Daten verwendet.
- **ili2pg 5.5.2** erzeugt das PostgreSQL-Schema aus dem validierten INTERLIS-Modell und übernimmt den XTF-Import/-Export.

Der normale lokale Ablauf ist damit zwingend:

```text
.ili
  → ili2c 5.6.8
  → ili2pg 5.5.2 --schemaimport
  → PostgreSQL

.xtf
  → ilivalidator 1.15.0
  → ili2pg 5.5.2 --import --importTid --importBid
```

Das Projekt stellt hierfür zentrale Scripts bereit:

```text
scripts/interlis-tools-env.sh
scripts/validate-model.sh
scripts/validate-xtf.sh
scripts/create-schema.sh
scripts/import-xtf.sh
```

CI und lokale Entwicklung verwenden dieselben Scripts/Gradle-Tasks. Toolparameter dürfen nicht separat in CI-YAML dupliziert werden.

Wichtig: Die korrekte ili2db-Option heisst `--createBasketCol` (mit `t`). Die Schreibweise `--createBaskeCol` ist **nicht** zu verwenden.

### 7.2.1 Modellvalidierung mit ili2c

`scripts/validate-model.sh` verwendet standardmässig exakt:

```bash
java -jar "${ILI2C_JAR:-/Users/stefan/apps/ili2c-5.6.8/ili2c.jar}" \
  model/SO_AGI_GEVER_20260707.ili
```

Der Agent darf nach einer Modelländerung `ili2pg --schemaimport`, Cayenne DB Import oder `cgen` erst ausführen, wenn dieser Schritt erfolgreich war. Ein fehlgeschlagener ili2c-Lauf ist ein harter Phasenfehler.

### 7.2.2 XTF-Validierung mit ilivalidator

`scripts/validate-xtf.sh` verwendet standardmässig exakt:

```bash
java -jar "${ILIVALIDATOR_JAR:-/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar}" \
  "$XTF"
```

Regeln:

1. Alle positiven XTF-Fixtures unter `model/testdata/` müssen ilivalidator-fehlerfrei sein.
2. Jeder Anwendungs-Import validiert die XTF-Datei **vor** dem ili2pg-Import; bei Validierungsfehlern findet kein DB-Import statt.
3. Jeder von Mabillon erzeugte XTF-Export wird nach dem Export erneut mit ilivalidator geprüft, bevor er als erfolgreich gemeldet oder zum Download angeboten wird.
4. Negativtests dürfen absichtlich ungültige XTFs verwenden; diese Fixtures liegen klar getrennt, z. B. unter `model/testdata/invalid/`, und der erwartete Validierungsfehler ist Teil des Tests.
5. Ein Exit-Code 0 allein genügt bei Roundtrip-Tests nicht: zusätzlich werden Objektzahlen, bekannte TIDs/BIDs und fachliche Referenzen geprüft.

## 7.3 ili2pg Schemaimport

Das Projekt muss einen reproduzierbaren Befehl/Task bereitstellen. `scripts/create-schema.sh` verwendet mindestens folgende Semantik:

```text
--schemaimport
--dbschema mabillon
--createFk
--createFkIdx
--createUnique
--createMandatoryChecks
--createNumChecks
--createTextChecks
--createDateTimeChecks
--createMetaInfo
--createTidCol
--createBasketCol
```

Beispielstruktur; Credentials/Host kommen aus Umgebungsvariablen und werden nicht geloggt:

```bash
java -jar "${ILI2PG_JAR:-/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar}" \
  --schemaimport \
  --dbschema mabillon \
  --createFk \
  --createFkIdx \
  --createUnique \
  --createMandatoryChecks \
  --createNumChecks \
  --createTextChecks \
  --createDateTimeChecks \
  --createMetaInfo \
  --createTidCol \
  --createBasketCol \
  ...connection arguments... \
  model/SO_AGI_GEVER_20260707.ili
```

Die tatsächliche Kommandozeile wird ausschliesslich in `scripts/create-schema.sh` zentralisiert. Optionen dürfen nicht dupliziert in CI/YAML/README verteilt werden; CI ruft dasselbe Script bzw. denselben Gradle-Task auf.

## 7.4 Baskets, TIDs und BIDs

Die Topic-/Basket-Trennung ist Teil des Datenmodells und muss beim Datenaustausch erhalten bleiben.

Für Test-/Entwicklungsdaten werden mindestens drei definierte Baskets erzeugt/importiert:

- Kataloge,
- Stammdaten,
- Geschäftsdaten.

Jeder XTF-Import über ili2pg muss standardmässig die Transfer- und Basket-IDs übernehmen:

```text
--importTid
--importBid
```

Damit ist der normale Importpfad in `scripts/import-xtf.sh` mindestens:

```bash
java -jar "${ILI2PG_JAR:-/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar}" \
  --import \
  --dbschema mabillon \
  --importTid \
  --importBid \
  ...connection arguments... \
  "$XTF"
```

Der Agent darf `--importTid` oder `--importBid` nicht aus Bequemlichkeit weglassen. Ein Importmodus ohne Übernahme dieser IDs muss als eigener, fachlich begründeter Use Case spezifiziert und freigegeben werden.

Pflichttests:

1. bekannte XTF-OIDs erscheinen nach Import als erwartete `t_ili_tid`-Werte,
2. bekannte Basket-IDs bleiben über `t_basket` nachvollziehbar,
3. Referenzen zwischen den drei Topics funktionieren nach Import,
4. Reimport/Export-Tests dürfen nicht unbemerkt neue Identitäten erzeugen.

## 7.5 DB-Änderungen

### Neuinstallation/Test

Immer:

```text
.ili → ili2pg --schemaimport → leere DB
```

### Produktionsmigration

Flyway darf für die Migration einer bestehenden produktiven DB verwendet werden.

Flyway ist **nicht** die fachliche Source of Truth.

Ablauf bei Modelländerung:

1. INTERLIS ändern,
2. ili2c erfolgreich,
3. neue Referenz-DB via ili2pg erzeugen,
4. DB-Schema-Diff gegen vorige Version erzeugen,
5. Flyway-Migration daraus bewusst erstellen/reviewen,
6. Migration gegen Kopie/Integrationstest ausführen,
7. Cayenne DB Import aktualisieren,
8. cgen aktualisieren,
9. Tests.

## 7.6 Kein H2

Alle DB-Integrationstests laufen gegen PostgreSQL via Testcontainers.

---

# 8. Apache Cayenne 5.0-M2 und MCP

## 8.1 Ausgangspunkt

Cayenne wird DB-first verwendet.

Cayenne erzeugt **nicht** das PostgreSQL-Schema.

## 8.2 Projektdateien

Im Repo:

```text
src/main/resources/cayenne/cayenne-project.xml
src/main/resources/cayenne/mabillon.map.xml
```

Generierte Java-Klassen:

```text
build/generated/sources/cayenne/
```

Falls Cayenne Paar-Klassen generiert:

```text
_generated/_Dossier.java   generiert, niemals manuell ändern
Dossier.java               anpassbarer Subclass, nur falls wirklich benötigt
```

Wo möglich sollen generierte Klassen ohne zusätzliche Domainlogik bleiben.

## 8.3 MCP-Server

Der lokale Coding Agent darf und soll den in Cayenne Modeler 5.0-M2 enthaltenen MCP-Server verwenden.

Verbindlich vorgesehene Tool-Funktionen:

- Projekt im Modeler öffnen,
- DB Import ausführen,
- cgen ausführen.

Der Agent wird lokal durch Code mit dem MCP-Server verbunden.

Der Agent darf **nicht** davon ausgehen, dass MCP in CI verfügbar ist.

## 8.4 MCP-Workflow bei Modelländerungen

Nach erfolgreichem `ili2pg --schemaimport` gegen eine frische lokale Referenz-DB:

1. Cayenne-Projekt via MCP öffnen.
2. DB Import gegen Schema `mabillon` ausführen.
3. Diff des DataMap prüfen.
4. Unerwartete Tabellen/Relationships melden und Phase stoppen.
5. cgen via MCP ausführen.
6. Generated Source Diff prüfen.
7. `./gradlew compileJava` ausführen.
8. Integrationstests ausführen.

Der Agent darf niemals ein unerwartetes DB-Import-Ergebnis „reparieren“, indem er das DataMap blind manuell verändert.

Ursache ist zuerst im INTERLIS-Modell, ili2pg-Mapping oder in der DB zu suchen.

## 8.5 Runtime

Eigene Spring-Konfiguration:

```java
@Configuration
public class CayenneConfiguration {
    @Bean(destroyMethod = "shutdown")
    CayenneRuntime cayenneRuntime(/* DataSource / properties */) { ... }
}
```

Die exakte Cayenne-5.0-M2-Builder-API ist in Phase 1 durch einen Compatibility Spike zu verifizieren. Keine Nutzung veralteter `ServerRuntime`-APIs aus Cayenne 4.x, falls 5.0-M2 eine direkte `CayenneRuntime`-Variante anbietet.

## 8.6 Explizite Unit of Work

Klasse:

```java
@Component
public final class CayenneUnitOfWork {
    public <T> T read(Function<ObjectContext, T> work);
    public <T> T write(Function<ObjectContext, T> work);
    public void write(Consumer<ObjectContext> work);
}
```

Semantik `write`:

1. neuen `ObjectContext` anlegen,
2. Work ausführen,
3. `commitChanges()`,
4. bei Runtime-Exception `rollbackChanges()`,
5. Exception weiterwerfen.

Keine implizite Annahme, dass Spring `@Transactional` Cayenne-ObjectContexts automatisch steuert.

---

# 9. Dateispeicherung

## 9.1 Grundsatz

Dateiinhalte werden nicht als PostgreSQL-BLOB gespeichert.

PostgreSQL speichert Metadaten und `storageUri`.

## 9.2 SPI

```java
public interface DocumentStorage {
    StagedDocument stage(DocumentUpload upload) throws IOException;
    StoredDocument commit(StagedDocument staged, StorageTarget target) throws IOException;
    InputStream open(StorageUri uri) throws IOException;
    boolean exists(StorageUri uri);
    void discard(StagedDocument staged) throws IOException;
}
```

Records:

```java
public record DocumentUpload(
    String originalFilename,
    String mimeType,
    InputStream content
) {}

public record StagedDocument(
    String token,
    String originalFilename,
    String mimeType,
    long size,
    String sha256
) {}

public record StoredDocument(
    String storageUri,
    String originalFilename,
    String mimeType,
    long size,
    String sha256
) {}
```

## 9.3 Erste Implementierung

```java
FileSystemDocumentStorage
```

Konfigurierbarer Root-Pfad.

Pfadbildung niemals aus Benutzer-Dateinamen.

Beispiel intern:

```text
<root>/objects/2a/71/<uuid>
```

Originalname nur Metadatum.

## 9.4 Löschregel

Registrierte aktenrelevante Unterlagen werden über die normale UI nicht physisch gelöscht.

Fehlerhafte Unterlagen werden storniert.

Physische Löschung ist nur für:

- nicht registrierte Staging-Dateien,
- expliziten Vernichtungsprozess,
- administrative Reparatur mit Audit

zulässig.

---

# 10. Security und Rollen

## 10.1 Rollen

```java
public enum MabillonRole {
    SACHBEARBEITER,
    ADMIN,
    GEVER_VERANTWORTLICHER,
    ARCHIVVERANTWORTLICHER
}
```

Spring Authorities:

```text
ROLE_MABILLON_SACHBEARBEITER
ROLE_MABILLON_ADMIN
ROLE_MABILLON_GEVER_VERANTWORTLICHER
ROLE_MABILLON_ARCHIVVERANTWORTLICHER
```

## 10.2 Berechtigungen

```java
public enum Permission {
    VIEW_MABILLON,
    EDIT_GESCHAEFT,
    EDIT_DOSSIER,
    EDIT_UNTERLAGE,
    EDIT_AUFGABE,
    MANAGE_CATALOGS,
    MANAGE_MASTERDATA,
    MANAGE_REGISTRATURPLAN,
    CLOSE_DOSSIER,
    RUN_DATA_QUALITY,
    MANAGE_ARCHIVE_DELIVERY
}
```

## 10.3 Permission Mapping

| Rolle | Kernberechtigungen |
|---|---|
| Sachbearbeiter | view, Geschäft/Dossier/Unterlage/Aufgabe bearbeiten |
| Admin | zusätzlich Kataloge/Stammdaten/Registraturplan |
| GEVER-Verantwortlicher | Dossierabschluss, Qualität, Geschäftskontrolle |
| Archivverantwortlicher | Archivablieferungen/SIP |

## 10.4 Authentifizierung

MVP:

- dev/test: definierte Testidentitäten,
- Produktion: Architektur für OIDC vorbereiten, aber Provider nicht hart in Fachlogik einbauen.

Fachlogik verwendet nur:

```java
public interface CurrentActor {
    ActorId id();
    String username();
    String displayName();
    Set<MabillonRole> roles();
}
```

## 10.5 Service-Layer Enforcement

Nicht nur Controller schützen.

Schreibende Application Services prüfen Berechtigungen erneut über:

```java
AuthorizationService.require(Permission permission)
```

---

# 11. UI- und HTMX-Konventionen

## 11.1 Grundsatz

Jeder wichtige Screen muss ohne HTMX als vollständiger normaler HTTP-Request funktionieren, sofern dies mit vertretbarem Aufwand möglich ist.

HTMX verbessert Interaktion, ersetzt aber nicht die Navigationsarchitektur.

## 11.2 URL-Schema

```text
/                       → Meine Arbeit
/dossiers
/dossiers/{dossiernummer}
/geschaefte
/geschaefte/{geschaeftsnummer}
/aufgaben
/suche
/admin/kataloge
/admin/stammdaten
/admin/registraturplan
/archivierung/ablieferungen
/archivierung/ablieferungen/{nummer}
```

Fachliche stabile Nummern dürfen als URL-Key verwendet werden.

## 11.3 Templates

```text
src/main/jte/
├── layout/
│   ├── page.jte
│   └── admin-page.jte
├── components/
│   ├── button.jte
│   ├── badge.jte
│   ├── field.jte
│   ├── table.jte
│   ├── pagination.jte
│   ├── notice.jte
│   └── tabs.jte
├── dashboard/
├── dossier/
├── geschaeft/
├── unterlage/
├── aufgabe/
├── admin/
└── archivierung/
```

Partials beginnen mit `_`:

```text
_aufgaben-list.jte
_status-panel.jte
_unterlagen-table.jte
```

## 11.4 HTMX Response-Regel

Controller dürfen dieselbe Application-Service-Methode für Full Page und Fragment verwenden.

Beispiel:

```java
@PostMapping("/{number}/prozessstatus")
public String changeProcessStatus(..., HttpServletRequest request, Model model) {
    var result = service.changeProcessStatus(...);
    model.addAttribute("result", result);
    return HtmxRequest.isHtmx(request)
        ? "geschaeft/_status-panel"
        : "redirect:/geschaefte/" + number;
}
```

HTMX-Erkennung zentral kapseln:

```java
public final class HtmxRequest {
    public static boolean isHtmx(HttpServletRequest request);
}
```

## 11.5 Designsprache und CSS

### 11.5.1 Normative Referenz

Die Mabillon-UI soll nicht neu erfunden werden. Als normative Designreferenz gilt die oben genannte, auf Commit `3e133a976a0ed1c704f38e81a6493501e0568ec4` gepinnte `ili2grails`-Designsprache. Der Coding Agent muss vor Phase 1 mindestens `ili-modern.css` und die fünf Mockups ansehen und im Phase-0-Bericht dokumentieren, welche Muster übernommen werden.

Die Referenz ist **Designsystem**, nicht Laufzeitabhängigkeit. Mabillon implementiert die benötigten Teile vorzugsweise in eigenem Vanilla CSS. Kein Bootstrap/Tailwind ohne explizite Freigabe.

### 11.5.2 Design Tokens

Die Basistokens orientieren sich semantisch an `ili2grails`:

```text
primary
active background
neutral ink
neutral emphasis
neutral muted
neutral border
neutral surface
neutral canvas
neutral header
neutral hover
subtle card shadow
border radius
```

Es sind CSS Custom Properties zu verwenden. Farben werden nicht in einzelnen Komponenten dupliziert. Die primäre Akzentfarbe ist brandbar; die neutralen Kontraste, kleinen Radien und zurückhaltenden Flächen bleiben Teil der Designsprache.

### 11.5.3 Layout- und Komponenten-Vokabular

Keine Utility-Class-Suppe. Semantische Klassen dürfen die bewährte `ili-*`-Sprache fortführen oder für Mabillon-spezifische Domänenkomponenten `mabillon-*` verwenden. Erwartete Basiskomponenten:

```text
.ili-topbar
.ili-shell-layout
.ili-sidebar
.ili-main-grid
.ili-breadcrumbs
.ili-page
.ili-page-header
.ili-page-title
.ili-page-subtitle
.ili-page-actions
.ili-form
.ili-form-section
.ili-list-tools
.ili-active-filters
.ili-table-wrap
.ili-pagination-bar
.ili-row-actions
.ili-notification

.mabillon-case-summary
.mabillon-status-history
.mabillon-task-list
.mabillon-dossier-contents
.mabillon-journal
```

Die tatsächlichen Namen dürfen leicht abweichen, aber ein konsistentes Komponenten-Vokabular ist Pflicht.

### 11.5.4 Typografie

Die Referenz verwendet Fira Sans. Wenn Fira Sans im Mabillon-Repository bewusst und lizenzkonform als lokales Asset bereitgestellt wird, kann sie verwendet werden. Andernfalls ist ein stabiler System-Sans-Stack zu verwenden. Keine Fonts von externen CDNs.

### 11.5.5 UI-Qualitätsregeln

- keine starken Rundungen,
- keine dekorativen grossen Schatten,
- Tabellen bleiben echte semantische HTML-Tabellen,
- Form Labels sind echte Labels,
- Fokuszustände sind sichtbar,
- relevante Aktionen haben Textlabels; Icon-only nur für klar bekannte sekundäre Aktionen,
- responsive Verhalten darf Informationsdichte reduzieren, aber keine fachlich relevanten Daten verstecken,
- HTMX-Fragmente verwenden dieselben Komponenten und Tokens wie Full Pages.

Phase 1 muss die Basis-Komponenten anhand der ili2grails-Referenz implementieren und mindestens Dashboard/App-Shell, Listen-/Filteransicht, Detailansicht und Formularzustände visuell prüfen.

---

# 12. Fehler- und Validierungsmodell

## 12.1 Exceptions

```java
public abstract class MabillonException extends RuntimeException {}

public final class NotFoundException extends MabillonException {}
public final class ValidationException extends MabillonException {}
public final class ConflictException extends MabillonException {}
public final class AuthorizationException extends MabillonException {}
public final class StorageException extends MabillonException {}
public final class SipGenerationException extends MabillonException {}
```

`ValidationException` enthält strukturierte Fehler:

```java
public record FieldError(String field, String code, String message) {}
```

## 12.2 Global Web Error Handling

```java
@ControllerAdvice
public final class WebExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ...

    @ExceptionHandler(ValidationException.class)
    ...

    @ExceptionHandler(ConflictException.class)
    ...
}
```

HTMX-Requests erhalten ein geeignetes Fragment; normale Requests eine vollständige Fehlerseite.

---

# 13. Zentrale Infrastrukturklassen

## 13.1 Zeit

Nie direkt `LocalDate.now()` / `Instant.now()` in Business Services.

Spring Bean:

```java
@Bean
Clock clock() { return Clock.systemDefaultZone(); }
```

Use Cases verwenden den injizierten `Clock`.

## 13.2 Nummernvergabe

```java
public interface NumberSequenceStore {
    long next(String organisationCode, NumberObjectType type, int year);
}

public enum NumberObjectType {
    GESCHAEFT("G"),
    DOSSIER("D"),
    ARCHIVABLIEFERUNG("A");
}
```

```java
@Service
public final class NumberingService {
    DossierNumber nextDossierNumber(String organisationCode, LocalDate date);
    GeschaeftNumber nextGeschaeftNumber(String organisationCode, LocalDate date);
    ArchivAblieferungNumber nextArchivAblieferungNumber(String organisationCode, LocalDate date);
}
```

## 13.3 Journal

```java
@Service
public final class JournalService {
    void record(ObjectContext context, JournalCommand command);
}

public record JournalCommand(
    EreignisObjektTyp objektTyp,
    String objektId,
    EreignisTyp typ,
    String bemerkung,
    ActorId actorId,
    Instant timestamp
) {}
```

`JournalService` führt keinen separaten Commit durch. Das Ereignis wird im selben `ObjectContext` wie die fachliche Änderung erzeugt.

Dadurch gilt:

> Fachänderung und fachliches Journalereignis committen atomar zusammen.

## 13.4 Journal Query

```java
@Service
public final class JournalQueryService {
    List<JournalEntryView> findForObject(EreignisObjektTyp type, String objectId, int limit);
    Page<JournalEntryView> findForGeschaeft(GeschaeftNumber number, PageRequest pageRequest);
}
```

---

# 14. Fachmodule – Klassen und Methoden

# 14.1 Catalog

## Controller

```java
@Controller
@RequestMapping("/admin/kataloge")
public final class CatalogAdminController {
    String index(Model model);
    String list(CatalogType type, Model model);
    String editForm(CatalogType type, String code, Model model);
    String createForm(CatalogType type, Model model);
    String create(CatalogType type, CatalogForm form, Model model);
    String update(CatalogType type, String code, CatalogForm form, Model model);
    String deactivate(CatalogType type, String code, Model model);
    String activate(CatalogType type, String code, Model model);
}
```

## Service

```java
@Service
public final class CatalogService {
    List<CatalogEntryView> list(CatalogType type, boolean includeInactive);
    CatalogEntryView get(CatalogType type, String code);
    CatalogEntryView create(CreateCatalogEntryCommand command);
    CatalogEntryView update(UpdateCatalogEntryCommand command);
    void activate(CatalogType type, String code);
    void deactivate(CatalogType type, String code);
}
```

Spezialisierte Methoden:

```java
List<ProzessStatusView> processStatusesForGeschaeftsart(String geschaeftsartCode);
List<ResultatStatusView> resultStatusesForGeschaeftsart(String geschaeftsartCode);
ProzessStatusView initialProcessStatus(String geschaeftsartCode);
```

Business Rules:

- Code unveränderlich nach Erstellung.
- In Verwendung befindliche Einträge nicht physisch löschen.
- Genau ein initialer Prozessstatus pro Geschäftsart, falls Prozessstatus definiert sind.
- Prozessstatus/Resultatstatus müssen zu ihrer Geschäftsart gehören.

---

# 14.2 Masterdata

```java
@Service
public final class OrganisationseinheitService {
    List<OrganisationseinheitView> list(boolean includeInactive);
    OrganisationseinheitView create(CreateOrganisationseinheitCommand command);
    OrganisationseinheitView update(UpdateOrganisationseinheitCommand command);
    void deactivate(String kuerzel);
}
```

```java
@Service
public final class BenutzerService {
    List<BenutzerView> list(boolean includeInactive);
    BenutzerView create(CreateBenutzerCommand command);
    BenutzerView update(UpdateBenutzerCommand command);
    void deactivate(String username);
}
```

---

# 14.3 Registraturplan

## Query

```java
@Service
public final class RegistraturplanQueryService {
    List<RegistraturplanView> listPlans(boolean includeReplaced);
    RegistraturplanTreeView getTree(String planCode);
    RegistraturplanPositionView getPosition(String code);
    List<RegistraturplanPositionOption> activeLeafPositions();
}
```

## Admin

```java
@Service
public final class RegistraturplanAdminService {
    RegistraturplanView createPlan(CreateRegistraturplanCommand command);
    RegistraturplanPositionView createPosition(CreatePositionCommand command);
    RegistraturplanPositionView updatePosition(UpdatePositionCommand command);
    void movePosition(MovePositionCommand command);
    void activatePlan(String code);
    void replacePlan(String code, LocalDate validTo);
    void deactivatePosition(String code);
}
```

Regeln:

- verwendete Positionen nie löschen,
- historische Dossiers behalten ihre Position,
- nur aktive Positionen für neue Dossiers,
- Baum darf keine Zyklen enthalten.

---

# 14.4 Dossier

## Commands

```java
public record OpenDossierCommand(
    String title,
    String description,
    String registraturplanPositionCode,
    String federfuehrungKuerzel,
    String verantwortlicherUsername,
    LocalDate openingDate
) {}

public record UpdateDossierCommand(
    DossierNumber number,
    String title,
    String description,
    String verantwortlicherUsername,
    String remarks
) {}
```

## Service

```java
@Service
public final class DossierService {
    DossierView open(OpenDossierCommand command);
    DossierView update(UpdateDossierCommand command);
    DossierView close(DossierNumber number);
    DossierView reopen(DossierNumber number, String reason);
}
```

`reopen` ist nur für GEVER-Verantwortliche und nicht Teil des normalen Golden Paths.

## Query

```java
@Service
public final class DossierQueryService {
    DossierDetailView get(DossierNumber number);
    Page<DossierListItem> search(DossierSearchCriteria criteria, PageRequest pageRequest);
    List<GeschaeftListItem> geschaefte(DossierNumber number);
    Page<UnterlageListItem> unterlagen(DossierNumber number, PageRequest pageRequest);
}
```

## Abschlussregeln

`close` schlägt fehl, wenn:

- mindestens ein zugeordnetes Geschäft nicht abgeschlossen ist,
- mindestens eine aktenrelevante Unterlage `In_Arbeit` ist,
- Datenqualitätsregel mit Severity ERROR für dieses Dossier verletzt ist.

Bei Erfolg:

- `status = Geschlossen`,
- `geschlossenAm = heute`,
- Ereignis `Dossier_abgeschlossen`.

---

# 14.5 Geschäft

## Commands

```java
public record OpenGeschaeftCommand(
    DossierNumber dossierNumber,
    String title,
    String shortDescription,
    String geschaeftsartCode,
    String federfuehrungKuerzel,
    String verantwortlicherUsername,
    LocalDate eingangsdatum,
    LocalDate eroeffnungsdatum,
    LocalDate dueDate,
    Integer priority
) {}
```

```java
public record ChangeProcessStatusCommand(
    GeschaeftNumber number,
    String processStatusCode,
    String comment
) {}
```

```java
public record SetResultCommand(
    GeschaeftNumber number,
    String resultStatusCode,
    String comment
) {}
```

## Service

```java
@Service
public final class GeschaeftService {
    GeschaeftView open(OpenGeschaeftCommand command);
    GeschaeftView update(UpdateGeschaeftCommand command);
    GeschaeftView changeProcessStatus(ChangeProcessStatusCommand command);
    GeschaeftView setResult(SetResultCommand command);
    GeschaeftView suspend(GeschaeftNumber number, String reason);
    GeschaeftView resume(GeschaeftNumber number, String comment);
    GeschaeftView close(GeschaeftNumber number);
}
```

## Query

```java
@Service
public final class GeschaeftQueryService {
    GeschaeftDetailView get(GeschaeftNumber number);
    Page<GeschaeftListItem> search(GeschaeftSearchCriteria criteria, PageRequest pageRequest);
    List<GeschaeftListItem> activeForUser(String username, int limit);
}
```

## Prozessstatusregeln

Für MVP gibt es **keinen Transition Graph**.

Ein Prozessstatuswechsel ist zulässig, wenn:

- Zielstatus aktiv ist,
- Zielstatus zur Geschäftsart gehört,
- Geschäft nicht abgeschlossen/archiviert/vernichtet ist.

Ein späteres `ProzessUebergang`-Modell ist explizit nicht Teil des MVP.

## Geschäftsabschlussregeln

`close` schlägt fehl wenn:

- offene/nicht abgeschlossene Aufgaben existieren,
- ein Prozessstatus existiert und nicht `terminal=true` ist,
- `Geschaeftsart.resultatErforderlich=true` und kein Resultatstatus gesetzt ist,
- Resultatstatus gesetzt ist, aber nicht zur Geschäftsart gehört,
- aktenrelevante Unterlagen des Geschäfts noch `In_Arbeit` sind.

Bei Erfolg:

```text
lifecycleStatus = Abgeschlossen
abgeschlossenAm = heute
```

Journalereignis wird im gleichen Commit geschrieben.

---

# 14.6 Beteiligung

```java
@Service
public final class BeteiligterService {
    BeteiligterView create(CreateBeteiligterCommand command);
    BeteiligterView update(UpdateBeteiligterCommand command);
    Page<BeteiligterListItem> search(BeteiligterSearchCriteria criteria, PageRequest pageRequest);
}
```

```java
@Service
public final class BeteiligungService {
    BeteiligungView add(AddBeteiligungCommand command);
    BeteiligungView update(UpdateBeteiligungCommand command);
    void end(EndBeteiligungCommand command);
    List<BeteiligungView> listForGeschaeft(GeschaeftNumber number);
}
```

Regeln:

- Rolle muss aktiver Katalogwert sein.
- Gültig-bis darf nicht vor gültig-von liegen.
- physisches Löschen einer historisch relevanten Beteiligung vermeiden; stattdessen Gültigkeit beenden.

---

# 14.7 Unterlage

## Register

```java
public record RegisterUnterlageCommand(
    DossierNumber dossierNumber,
    GeschaeftNumber geschaeftNumber,
    String title,
    String unterlagenTypCode,
    LocalDate unterlagenDatum,
    LocalDate eingangsdatum,
    LocalDate ausgangsdatum,
    boolean aktenrelevant,
    String remarks
) {}
```

`geschaeftNumber` ist nullable.

```java
@Service
public final class UnterlageService {
    UnterlageView register(RegisterUnterlageCommand command, DocumentUpload upload);
    UnterlageView updateMetadata(UpdateUnterlageCommand command);
    UnterlageView assignToGeschaeft(AssignUnterlageToGeschaeftCommand command);
    UnterlageView unassignFromGeschaeft(String unterlageTid);
    UnterlageView finalizeUnterlage(String unterlageTid);
    UnterlageView registerAktenrelevant(String unterlageTid);
    UnterlageView cancel(String unterlageTid, String reason);
}
```

## Query/Download

```java
@Service
public final class UnterlageQueryService {
    UnterlageDetailView get(String unterlageTid);
    Page<UnterlageListItem> forDossier(DossierNumber number, PageRequest pageRequest);
    Page<UnterlageListItem> forGeschaeft(GeschaeftNumber number, PageRequest pageRequest);
}
```

```java
@Service
public final class UnterlageContentService {
    DocumentDownload open(String unterlageTid);
}
```

## Konsistenz

`assignToGeschaeft` muss prüfen:

```text
Unterlage.dossier == Geschaeft.dossier
```

Andernfalls `ConflictException`.

## Staging-Ablauf

`register`:

1. Upload `DocumentStorage.stage`,
2. Metadaten inkl. SHA-256 bestimmen,
3. Cayenne-UoW öffnen,
4. Dossier/Geschäft prüfen,
5. Unterlage anlegen,
6. Journal anlegen,
7. DB committen,
8. Datei final in Storage verschieben,
9. bei Fehlern best-effort staging cleanup.

Die endgültige Reihenfolge ist in Phase 5 durch Failure-Tests abzusichern. Kein stiller Datenverlust bei DB- oder Storage-Fehlern.

---

# 14.8 E-Mail-Registrierung

Keine IMAP-/Exchange-Integration im MVP.

Use Case ist ein spezialisierter Registrierungsflow.

```java
@Service
public final class EmailRegistrationService {
    UnterlageView registerIncomingEmail(RegisterIncomingEmailCommand command, DocumentUpload eml);
    UnterlageView registerOutgoingEmail(RegisterOutgoingEmailCommand command, DocumentUpload eml);
}
```

EML kann als `message/rfc822` abgelegt werden.

Anhänge dürfen separat als Unterlagen registriert werden.

Keine automatische Parent/Attachment-Domänenstruktur im MVP.

---

# 14.9 Aufgabe

```java
@Service
public final class AufgabeService {
    AufgabeView create(CreateAufgabeCommand command);
    AufgabeView update(UpdateAufgabeCommand command);
    AufgabeView start(String aufgabeTid);
    AufgabeView complete(CompleteAufgabeCommand command);
    AufgabeView cancel(CancelAufgabeCommand command);
    AufgabeView delegate(DelegateAufgabeCommand command);
}
```

```java
@Service
public final class AufgabeQueryService {
    AufgabeDetailView get(String aufgabeTid);
    Page<AufgabeListItem> myOpenTasks(String username, PageRequest pageRequest);
    List<AufgabeListItem> forGeschaeft(GeschaeftNumber number);
    List<AufgabeListItem> overdueForUser(String username, LocalDate today);
}
```

Regeln:

- `complete`: setzt `Erledigt`, `erledigtAm`.
- Erledigte Aufgabe darf nicht normal weiterbearbeitet werden.
- Reopen einer Aufgabe nur explizit als spätere Erweiterung.
- Aufgabe gehört genau einem Geschäft.

---

# 14.10 Fachsystemreferenz

```java
@Service
public final class FachsystemReferenzService {
    FachsystemReferenzView addToGeschaeft(AddFachsystemReferenzCommand command);
    FachsystemReferenzView addToDossier(AddFachsystemReferenzToDossierCommand command);
    void remove(String referenceTid, String reason);
    List<FachsystemReferenzView> forGeschaeft(GeschaeftNumber number);
    List<FachsystemReferenzView> forDossier(DossierNumber number);
}
```

Mindestens Geschäft oder Dossier muss gesetzt sein.

---

# 14.11 Dashboard / Meine Arbeit

```java
@Service
public final class MyWorkQueryService {
    MyWorkView load(String username, LocalDate today);
}
```

```java
public record MyWorkView(
    List<AufgabeListItem> openTasks,
    List<AufgabeListItem> overdueTasks,
    List<GeschaeftListItem> activeGeschaefte,
    List<GeschaeftListItem> dueSoonGeschaefte,
    List<GeschaeftListItem> recentlyChanged
) {}
```

Maximal sinnvolle kleine Listen; vollständige Ergebnisse via eigene Suchseiten.

---

# 14.12 Suche

## MVP

Keine Elasticsearch/OpenSearch-Abhängigkeit.

PostgreSQL-basierte strukturierte Suche.

```java
@Service
public final class GlobalSearchService {
    GlobalSearchResult search(GlobalSearchCriteria criteria);
}
```

Suchfelder mindestens:

- Geschäftsnummer,
- Dossiernummer,
- Titel,
- Beteiligter Name,
- Gemeinde/Organisation als Beteiligter,
- Geschäftsart,
- Prozessstatus,
- Unterlagentitel,
- Fachsystem-ID.

Pagination verpflichtend.

Keine Volltextindizierung von PDF-Inhalten im MVP.

---

# 14.13 Datenqualität

## SPI

```java
public interface DataQualityRule {
    String code();
    QualitySeverity severity();
    String description();
    List<QualityFinding> evaluate(QualityContext context);
}
```

```java
public enum QualitySeverity {
    INFO,
    WARNING,
    ERROR
}
```

```java
public record QualityFinding(
    String ruleCode,
    QualitySeverity severity,
    String objectType,
    String objectId,
    String message
) {}
```

## Service

```java
@Service
public final class DataQualityService {
    QualityReport checkDossier(DossierNumber number);
    QualityReport checkGeschaeft(GeschaeftNumber number);
    QualityReport checkArchiveDelivery(ArchivAblieferungNumber number);
}
```

## Pflichtregeln

```text
DQ-001 Dossier ohne gültige Registraturplanposition
DQ-002 Geschäft ohne Dossier
DQ-003 Geschäft ohne gültige Geschäftsart
DQ-004 Unterlage ohne Dossier
DQ-005 Unterlage verweist auf Geschäft eines anderen Dossiers
DQ-006 abgeschlossenes Geschäft mit offenen Aufgaben
DQ-007 geschlossenes Dossier mit offenem Geschäft
DQ-008 Prozessstatus passt nicht zur Geschäftsart
DQ-009 Resultatstatus passt nicht zur Geschäftsart
DQ-010 resultatpflichtiges abgeschlossenes Geschäft ohne Resultat
DQ-011 aktenrelevante registrierte Unterlage ohne vorhandene Datei
DQ-012 Datei-Hash stimmt nicht mit Storage-Inhalt überein
DQ-013 Archivablieferung enthält nicht geschlossene Dossiers
```

DQ-005, DQ-006, DQ-007, DQ-008, DQ-009, DQ-010, DQ-011, DQ-013 sind ERROR.

---

# 14.14 INTERLIS Import/Export

## Prozessadapter

```java
public interface InterlisModelValidator {
    ValidationResult validate(Path iliModel);
}

public interface XtfValidator {
    ValidationResult validate(Path xtf);
}

public final class InterlisToolDefaults {
    public static final Path ILI2PG_JAR =
        Path.of("/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar");
    public static final Path ILI2C_JAR =
        Path.of("/Users/stefan/apps/ili2c-5.6.8/ili2c.jar");
    public static final Path ILIVALIDATOR_JAR =
        Path.of("/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar");

    private InterlisToolDefaults() {}
}

public interface Ili2pgRunner {
    Ili2pgResult schemaImport(SchemaImportRequest request);
    Ili2pgResult importXtf(ImportXtfRequest request);
    Ili2pgResult exportXtf(ExportXtfRequest request);
    Ili2pgResult validate(ValidateRequest request);
}
```

Produktive Implementierung:

```java
ProcessBuilderIli2pgRunner
ProcessBuilderInterlisModelValidator
ProcessBuilderXtfValidator
```

`ProcessBuilderInterlisModelValidator.validate()` ruft ili2c 5.6.8 als externen Prozess auf, sammelt Exit-Code und Diagnoseausgabe und liefert bei jedem Compilerfehler `ValidationResult.invalid(...)`.

`ProcessBuilderXtfValidator.validate()` ruft ilivalidator 1.15.0 als externen Prozess auf. Die Anwendung darf nach `invalid` keinen `Ili2pgRunner.importXtf(...)` aufrufen. Ebenso muss `InterlisExchangeService` nach `exportXtf(...)` den erzeugten XTF mit `XtfValidator` prüfen; ein nicht valider Export wird gelöscht/quarantänisiert und als Fehler gemeldet.

Verbindliche Defaults des Runners:

```java
`InterlisToolDefaults` ist die einzige Default-Quelle für die drei lokalen JAR-Pfade. Zusätzlich sind dort bzw. in einer gleichwertigen zentralen Konfiguration die erwarteten Versionen `5.5.2`, `5.6.8` und `1.15.0` hinterlegt. Es darf keine zweite `Ili2pgDefaults`-Klasse mit dupliziertem Pfad geben.
```

`ImportXtfRequest` muss explizit modellieren, ob TID/BID übernommen werden; die Application-Services verwenden immer `true/true`:

```java
public record ImportXtfRequest(
    Path xtf,
    ImportScope scope,
    boolean importTid,
    boolean importBid
) {}
```

`ProcessBuilderIli2pgRunner.importXtf()` setzt bei diesen Flags die ili2pg-Argumente `--importTid` und `--importBid`. Die öffentlichen Mabillon-Import-Use-Cases dürfen sie nicht deaktivieren.

Verbindlicher Importalgorithmus der drei öffentlichen Import-Use-Cases:

```text
XtfValidator.validate(xtf)
  → invalid: STOP, keine DB-Änderung
  → valid: Ili2pgRunner.importXtf(... importTid=true, importBid=true)
  → fachliche Post-Import-Checks
```

Verbindlicher Exportalgorithmus:

```text
Ili2pgRunner.exportXtf(...)
  → XtfValidator.validate(exportedXtf)
  → invalid: Export als FAILED markieren, Datei nicht ausliefern
  → valid: fachliche Roundtrip-/Count-Prüfungen, danach Erfolg
```

Kein `Runtime.exec(String)` mit unescaped Commandline.

Argumente als Liste an `ProcessBuilder`.

Passwörter nie in Logs.

## Application Service

```java
@Service
public final class InterlisExchangeService {
    ExchangeResult importCatalog(Path xtf);
    ExchangeResult importMasterData(Path xtf);
    ExchangeResult importBusinessData(Path xtf);
    Path exportCatalog(ExportSelection selection);
    Path exportMasterData(ExportSelection selection);
    Path exportBusinessData(ExportSelection selection);
    ValidationReport validateTopic(TopicSelection selection);
}
```

Importreihenfolge:

```text
Kataloge → Stammdaten → Geschäftsdaten
```

---

# 14.15 Archivierung und SIP

## Query zur Aussonderung

```java
@Service
public final class AussonderungQueryService {
    Page<DossierListItem> findEligible(ArchiveEligibilityCriteria criteria, PageRequest pageRequest);
}
```

Mindestens:

- Dossier geschlossen,
- nicht bereits erfolgreich übernommen/vernichtet,
- Qualitätscheck ohne ERROR.

## Ablieferung

```java
@Service
public final class ArchivAblieferungService {
    ArchivAblieferungView create(CreateArchivAblieferungCommand command);
    ArchivAblieferungView addDossier(AddDossierToDeliveryCommand command);
    ArchivAblieferungView removeDossier(RemoveDossierFromDeliveryCommand command);
    ArchivAblieferungView markReady(ArchivAblieferungNumber number);
    ArchivAblieferungView recordTransferred(RecordTransferredCommand command);
    ArchivAblieferungView recordAccepted(RecordAcceptedCommand command);
    ArchivAblieferungView recordRejected(RecordRejectedCommand command);
}
```

## SIP SPI

```java
public interface SipGenerator {
    GeneratedSip generate(SipGenerationRequest request);
}

public interface SipValidator {
    SipValidationResult validate(Path sipPath);
}
```

```java
public record SipGenerationRequest(
    ArchivAblieferungNumber deliveryNumber,
    SipProfile profile,
    Path targetDirectory
) {}
```

```java
public record GeneratedSip(
    Path path,
    long size,
    String sha256
) {}
```

```java
public record SipValidationResult(
    SipValidationStatus status,
    List<SipValidationMessage> messages,
    Path reportPath
) {}
```

## Orchestrator

```java
@Service
public final class SipService {
    SipPaketView generate(ArchivAblieferungNumber number);
    SipPaketView validate(String sipPaketTid);
}
```

`generate`:

1. Permission prüfen.
2. Ablieferung laden.
3. Status `Bereit` verlangen.
4. Datenqualität prüfen.
5. Dokumentdateien auf Existenz und Hash prüfen.
6. `SipGenerator.generate`.
7. Hash/Grösse berechnen.
8. `SipPaket` speichern.
9. Delivery-Status `SIP_Erstellt`.
10. Journal.

`validate`:

1. Paket laden.
2. `SipValidator.validate`.
3. Report speichern.
4. Status setzen.
5. bei gültig: Delivery `Validiert`.
6. Journal.

## SIP-Profil

SIP-Implementierung darf nicht hart auf ein einziges Archiv festverdrahtet werden.

```java
public record SipProfile(
    String id,
    String displayName,
    String echVersion,
    String archiveProfileVersion
) {}
```

Erstes Zielprofil wird in Phase 9 konkret gegen die gültigen Vorgaben des Zielarchivs festgelegt.

Die Implementierung muss mindestens einen eCH-0160-basierten Export ermöglichen und soll für ein BAR-kompatibles Profil erweiterbar sein.

---

# 15. Webcontroller

## 15.1 Dashboard

```java
@Controller
public final class DashboardController {
    @GetMapping("/")
    String myWork(Model model);
}
```

## 15.2 Dossier

```java
@Controller
@RequestMapping("/dossiers")
public final class DossierController {
    String list(...);
    String detail(String number, Model model);
    String createForm(Model model);
    String create(DossierForm form, Model model);
    String editForm(String number, Model model);
    String update(String number, DossierForm form, Model model);
    String close(String number, Model model);
    String unterlagen(String number, Model model);
    String geschaefte(String number, Model model);
    String verlauf(String number, Model model);
}
```

## 15.3 Geschäft

```java
@Controller
@RequestMapping("/geschaefte")
public final class GeschaeftController {
    String list(...);
    String detail(String number, Model model);
    String createForm(Model model);
    String create(GeschaeftForm form, Model model);
    String editForm(String number, Model model);
    String update(String number, GeschaeftForm form, Model model);
    String changeProcessStatus(String number, ProcessStatusForm form, ...);
    String setResult(String number, ResultForm form, ...);
    String close(String number, ...);
}
```

## 15.4 Unterlage

```java
@Controller
@RequestMapping("/unterlagen")
public final class UnterlageController {
    String createForm(...);
    String register(UnterlageForm form, MultipartFile file, ...);
    String detail(String tid, Model model);
    ResponseEntity<Resource> download(String tid);
    String assignToGeschaeft(String tid, AssignGeschaeftForm form, ...);
    String cancel(String tid, CancelForm form, ...);
}
```

## 15.5 Aufgaben

```java
@Controller
@RequestMapping("/aufgaben")
public final class AufgabeController {
    String myTasks(...);
    String detail(String tid, Model model);
    String create(AufgabeForm form, ...);
    String update(String tid, AufgabeForm form, ...);
    String complete(String tid, CompleteForm form, ...);
    String delegate(String tid, DelegateForm form, ...);
}
```

## 15.6 Suche

```java
@Controller
@RequestMapping("/suche")
public final class SearchController {
    String search(SearchForm form, Model model);
}
```

## 15.7 Archivierung

```java
@Controller
@RequestMapping("/archivierung")
public final class ArchivierungController {
    String candidates(...);
    String deliveries(...);
    String deliveryDetail(String number, Model model);
    String createDelivery(...);
    String addDossier(...);
    String generateSip(...);
    String validateSip(...);
    String recordTransferred(...);
    String recordAccepted(...);
    String recordRejected(...);
}
```

---

# 16. Use-Case-Spezifikation

Die folgenden Use Cases sind verbindlich. Die IDs bleiben stabil.

## UC-001 Meine Arbeit anzeigen

**Actor:** Sachbearbeiter  
**Service:** `MyWorkQueryService.load`  
**Controller:** `DashboardController.myWork`

**Preconditions:** Benutzer angemeldet, aktiver Benutzer.

**Output:**

- offene Aufgaben,
- überfällige Aufgaben,
- aktive Geschäfte,
- bald fällige Geschäfte,
- zuletzt bearbeitete Geschäfte.

**Acceptance:** Bodenrain erscheint für Anna Müller, solange aktiv/zugewiesen.

---

## UC-002 Geschäft suchen

**Service:** `GeschaeftQueryService.search`

Filter mindestens:

- Nummer,
- Titel,
- Geschäftsart,
- Prozessstatus,
- Lifecycle,
- Verantwortlicher,
- Organisationseinheit,
- Fälligkeit.

Pagination zwingend.

---

## UC-003 Dossier suchen

**Service:** `DossierQueryService.search`

Filter:

- Dossiernummer,
- Titel,
- Registraturplanposition,
- Status,
- Federführung,
- Eröffnungs-/Schliessdatum.

---

## UC-004 Neues Geschäft eröffnen

**Service:** `GeschaeftService.open`

Ablauf:

1. Dossier muss existieren/offen sein.
2. Geschäftsart aktiv.
3. initialen Prozessstatus ermitteln.
4. Nummer vergeben.
5. Geschäft anlegen.
6. Dossierbeziehung setzen.
7. Lifecycle wird beim Eröffnen auf `Eroeffnet` gesetzt. Beim ersten erfolgreichen fachlichen Prozessstatuswechsel wird `Eroeffnet` automatisch auf `In_Bearbeitung` angehoben. `suspend()` setzt `Sistiert`, `resume()` setzt wieder `In_Bearbeitung`, `close()` setzt `Abgeschlossen`. Diese Regel ist verbindlich und wird in Domain- und Integrationstests abgedeckt.
8. Journal `Erstellt`.

Nomenklaturtest:

```text
AGI-G-2026-000421
Umbenennung "Im alten Boden" zu "Bodenrain"
```

---

## UC-005 Neues Dossier eröffnen

**Service:** `DossierService.open`

Regeln:

- aktive Registraturplanposition,
- Dossiernummer automatisch,
- Status `Offen`,
- Eröffnungsdatum gesetzt,
- Journal `Erstellt`.

---

## UC-006 Geschäft bestehendem Dossier zuordnen

Im MVP nur bei Geschäftseröffnung.

Nachträgliches Verschieben eines Geschäfts in ein anderes Dossier ist **nicht** normaler Sachbearbeiter-Use-Case und wird nicht implementiert, bevor ein eigener Use Case definiert ist.

---

## UC-007 Dossier anzeigen

**Service:** `DossierQueryService.get`

Tabs:

- Übersicht,
- Unterlagen,
- Geschäfte,
- Beteiligte/abgeleitete Sicht,
- Verlauf,
- Archivierung.

Unterlagen zeigen Geschäftskontext.

---

## UC-008 Geschäft anzeigen

**Service:** `GeschaeftQueryService.get`

Enthält:

- Basisdaten,
- Dossierlink,
- Lifecycle,
- Prozessstatus,
- Resultat,
- Beteiligungen,
- Aufgaben,
- Unterlagen,
- Fachsystemreferenzen,
- Journal.

---

## UC-009 Geschäft bearbeiten

**Service:** `GeschaeftService.update`

Nicht über generischen Map-Patch.

Explizit editierbare Felder.

Nummer und Geschäftsart nach Erstellung standardmässig unveränderlich.

---

## UC-010 Prozessstatus ändern

**Service:** `GeschaeftService.changeProcessStatus`

Validierung:

- Zielstatus existiert,
- aktiv,
- gehört zur Geschäftsart,
- Geschäft bearbeitbar.

Journal atomar.

---

## UC-011 Geschäftsergebnis erfassen

**Service:** `GeschaeftService.setResult`

Resultat muss zur Geschäftsart gehören.

Journal `Entscheid_erfasst`.

---

## UC-012 Beteiligten erfassen

**Service:** `BeteiligterService.create`

Person/Organisation/interne Organisationseinheit.

Duplikaterkennung zunächst Hinweis, kein harter globaler Merge.

---

## UC-013 Beteiligten einem Geschäft zuordnen

**Service:** `BeteiligungService.add`

Benötigt Beteiligten, Geschäft, Rolle.

---

## UC-014 Unterlage registrieren

**Service:** `UnterlageService.register`

Erfasst Datei + Metadaten + Dossier + optional Geschäft.

Hash wird serverseitig berechnet.

---

## UC-015 Unterlage einem Geschäft zuordnen

**Service:** `UnterlageService.assignToGeschaeft`

Konsistenz Dossier zwingend.

---

## UC-016 Eingegangene E-Mail registrieren

**Service:** `EmailRegistrationService.registerIncomingEmail`

Keine Mailboxintegration im MVP.

---

## UC-017 Ausgangsschreiben registrieren

Kann über `UnterlageService.register` oder `EmailRegistrationService.registerOutgoingEmail` erfolgen.

`ausgangsdatum` setzen.

---

## UC-018 Unterlage anzeigen/herunterladen

**Services:** `UnterlageQueryService.get`, `UnterlageContentService.open`

Download soll korrekte MIME- und Content-Disposition-Header liefern.

---

## UC-019 Aufgabe erstellen

**Service:** `AufgabeService.create`

Geschäft zwingend.

---

## UC-020 Aufgabe bearbeiten

**Service:** `AufgabeService.update`

Erledigte/abgebrochene Aufgaben nicht normal editierbar.

---

## UC-021 Aufgabe erledigen

**Service:** `AufgabeService.complete`

Status + erledigtAm + Journal atomar.

---

## UC-022 Eigene Aufgaben verwalten

**Service:** `AufgabeQueryService.myOpenTasks`

Filter/Sortierung nach Fälligkeit, Priorität, Status.

---

## UC-023 Fachsystemreferenz erfassen

**Service:** `FachsystemReferenzService.addToGeschaeft` / `addToDossier`

Nomenklaturbeispiel:

```text
systemCode=NOMENKLATUR
objektTyp=Flurname
objektId=...
mutationId=...
```

---

## UC-024 Journal eines Geschäfts anzeigen

**Service:** `JournalQueryService.findForGeschaeft`

Journal nicht editierbar/löschbar über normale UI.

---

## UC-025 Geschäft abschliessen

**Service:** `GeschaeftService.close`

Harte Abschlussvalidierung gemäss Abschnitt 14.5.

---

## UC-026 Dossier abschliessen

**Service:** `DossierService.close`

Nur GEVER-Verantwortlicher oder explizit berechtigte Rolle.

---

## UC-027 Geschäftsart konfigurieren

**Service:** `CatalogService` spezialisiert für Geschäftsart.

Enthält `resultatErforderlich`.

---

## UC-028 Prozessstatus konfigurieren

**Service:** `CatalogService`.

Initial/terminal/sortierung.

---

## UC-029 Kataloge pflegen

CRUD mit Aktiv/Inaktiv statt physischem Löschen.

---

## UC-030 Organisationseinheiten pflegen

**Service:** `OrganisationseinheitService`.

Hierarchie unterstützen.

---

## UC-031 Benutzer pflegen

**Service:** `BenutzerService`.

Benutzer ist fachliches Stammdatum; Authentifizierung bleibt technisch/externalisierbar.

---

## UC-032 Registraturplan pflegen

**Service:** `RegistraturplanAdminService`.

---

## UC-033 Registraturplanposition pflegen

Baum, Federführung, aktiv/inaktiv.

---

## UC-034 Katalogdaten importieren/exportieren

**Service:** `InterlisExchangeService`.

XTF-Basket Kataloge.

---

## UC-035 Stammdaten importieren/exportieren

XTF-Basket Stammdaten.

---

## UC-036 Geschäftsdaten importieren/exportieren

XTF-Basket Geschäftsdaten.

Import muss Referenzen validieren.

---

## UC-037 Abgeschlossene Dossiers zur Aussonderung suchen

**Service:** `AussonderungQueryService.findEligible`.

---

## UC-038 Archivablieferung zusammenstellen

**Service:** `ArchivAblieferungService.create/addDossier/removeDossier`.

Nur geschlossene, qualitätsgeprüfte Dossiers.

---

## UC-039 SIP erzeugen

**Service:** `SipService.generate`.

Kein einfacher ZIP-Export; strukturierter SIP-Generator.

---

## UC-040 SIP validieren

**Service:** `SipService.validate`.

Validierungsbericht persistent referenzieren.

---

## UC-041 SIP-Ablieferung dokumentieren

**Service:** `ArchivAblieferungService.recordTransferred`.

Datum, Empfänger, Bemerkung, Journal.

---

## UC-042 Dossier nach erfolgreicher Ablieferung kennzeichnen

Durch `recordAccepted` ausgelöst:

- Archivierung.status = Uebernommen,
- Archivsignatur falls vorhanden,
- Dossierstatus = Archiviert nur wenn fachlich freigegeben/definiert,
- Journal.

Keine automatische Vernichtung.

---

## UC-043 Systemweite Suche

**Service:** `GlobalSearchService.search`.

---

## UC-044 Geschäftskontrolle / Fristenübersicht

```java
@Service
public final class GeschaeftskontrolleQueryService {
    GeschaeftskontrolleView load(GeschaeftskontrolleCriteria criteria);
}
```

Kennzahlen/listen:

- offene Geschäfte,
- überfällige Geschäfte,
- offene Aufgaben,
- überfällige Aufgaben,
- Verteilung nach Prozessstatus,
- inaktive Fälle seit n Tagen.

Keine BI-Plattform.

---

## UC-045 Datenqualität prüfen

**Service:** `DataQualityService`.

Ergebnis als strukturierter Report.

---

## UC-046 Historie/Audit nachvollziehen

**Service:** `JournalQueryService`.

Mindestens nachvollziehbar:

- Erstellung,
- Statuswechsel,
- Aufgabenerledigung,
- Unterlagenregistrierung,
- Abschluss,
- SIP/Archivierung.

---

# 17. Teststrategie

## 17.1 Testpyramide

Jede Phase braucht Tests auf den sinnvollen Ebenen:

1. Pure Unit Tests für Regeln/Value Objects.
2. PostgreSQL+Cayenne Integrationstests.
3. Spring MVC Tests für Controller/Formhandling.
4. Wenige Playwright-E2E-Tests für kritische User Flows.
5. INTERLIS/ili2pg Pipeline Tests.

## 17.2 Kein Mocking der Datenbank für Persistence Rules

Cayenne-Queries und Relationships werden gegen echtes PostgreSQL getestet.

## 17.3 Test Fixtures

Zentrale Fixture:

```java
public final class NomenklaturTestData {
    public static final String GEMEINDE = "Musterwil";
    public static final String OLD_NAME = "Im alten Boden";
    public static final String NEW_NAME = "Bodenrain";
}
```

Es soll jedoch möglichst XTF-Testdaten als primäre Integrationsfixture geben.

## 17.4 Pflicht-Golden-Path E2E

Spätestens Phase 7 automatisiert:

1. Anna Müller loggt sich ein.
2. Dossier eröffnen.
3. Geschäft Nomenklaturmutation eröffnen.
4. Gemeinde Musterwil als Antragstellerin zuordnen.
5. Antrag PDF registrieren.
6. Prozessstatus auf Fachliche Prüfung ändern.
7. Aufgabe erstellen/erledigen.
8. Beschluss registrieren.
9. Resultat Genehmigt setzen.
10. Geschäft abschliessen.
11. Dossier abschliessen.
12. Journal enthält die erwarteten Ereignisse.

Phase 9 erweitert Golden Path um SIP.

## 17.5 Testnamen

Beispiel:

```java
@Test
void closesGeschaeftWhenAllMandatoryConditionsAreMet()

@Test
void rejectsClosingGeschaeftWithOpenTasks()

@Test
void rejectsUnterlageAssignmentWhenGeschaeftBelongsToDifferentDossier()
```

Keine Tests namens `test1`, `works`, `happyPath` ohne Fachbezug.

## 17.6 Coverage

Kein blindes 100%-Coverage-Ziel.

Pflicht:

- jede Business Rule mindestens ein positiver und ein negativer Test,
- jeder schreibende Use Case Integrationstest,
- jeder sicherheitsrelevante Use Case Berechtigungstest,
- jeder Phase-Golden-Path Browsertest sofern UI relevant.

## 17.7 Testzustand

Tests müssen unabhängig von Reihenfolge laufen.

Kein geteilter persistenter Testzustand zwischen Testmethoden.

---

# 18. Phasenplan mit harten Gates

## Phase 0 – Fachmodell und technische Machbarkeit einfrieren

### Ziele

- aktuelles INTERLIS-Modell ins Repo übernehmen,
- Modellergänzungen aus Abschnitt 5 implementieren,
- Naming-Schema finalisieren,
- ili2c 5.6.8 erfolgreich gegen jedes `.ili`,
- XTF-Testdaten aktualisieren und mit ilivalidator 1.15.0 validieren,
- Spring Boot 4.1.0 + Java 25 + JTE + Cayenne 5.0-M2 Compatibility Spike,
- Cayenne Modeler MCP lokal nachweislich ansprechbar,
- DB Import + cgen einmal erfolgreich durchspielen.

### Zu erstellen

```text
model/SO_AGI_GEVER_20260707.ili
model/testdata/*.xtf
docs/architecture/0001-model-driven-database.md
docs/architecture/0002-cayenne-db-first.md
docs/architecture/0003-server-rendered-ui.md
docs/architecture/0004-ui-design-language.md
scripts/interlis-tools-env.sh
scripts/validate-model.sh
scripts/validate-xtf.sh
scripts/create-schema.sh
scripts/import-xtf.sh
.agents/skills/*/SKILL.md
```

`0004-ui-design-language.md` dokumentiert die gepinnte `ili2grails`-Referenz, übernommene Tokens/Komponentenmuster und bewusst nicht übernommene Bootstrap-spezifische Implementierungsdetails.

Minimaler Spring-Spike darf existieren, aber keine fachlichen Screens implementieren.

### Tests/Gate

- `ILI2PG_JAR` existiert am Default-Pfad `/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar`,
- `ILI2C_JAR` existiert am Default-Pfad `/Users/stefan/apps/ili2c-5.6.8/ili2c.jar`,
- `ILIVALIDATOR_JAR` existiert am Default-Pfad `/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar`,
- ili2pg-Version 5.5.2, ili2c-Version 5.6.8 und ilivalidator-Version 1.15.0 sind nachweislich aufrufbar,
- `scripts/validate-model.sh` ist grün,
- alle positiven XTF-Testdaten sind über `scripts/validate-xtf.sh` / ilivalidator 1.15.0 grün,
- ili2pg schemaimport gegen frische PostgreSQL-Testinstanz grün, **inkl. `--createTidCol --createBasketCol`**,
- XTF-Testdatenimport grün, **inkl. `--importTid --importBid`**,
- erwartete TIDs und BIDs nach Import erhalten,
- alle erwarteten FK/Checks vorhanden,
- Cayenne DB Import erfolgreich,
- cgen erfolgreich,
- Java compile erfolgreich,
- MCP smoke test dokumentiert,
- keine unerklärten Mapping-Diffs,
- `ili2grails`-Designreferenz auf Commit `3e133a976a0ed1c704f38e81a6493501e0568ec4` geprüft und Design-ADR erstellt,
- Agent-Skills unter `.agents/skills` syntaktisch geprüft und von Codex/OpenCode auffindbar.

### STOP

Agent erstellt `docs/phases/PHASE_0_REPORT.md` und stoppt.

Er beginnt Phase 1 erst nach expliziter Benutzerfreigabe.

---

## Phase 1 – Projektbasis, Persistence und Read-only Vertical Slice

### Ziele

- vollständiges Spring-Boot-Projekt,
- Cayenne Runtime,
- Unit of Work,
- PostgreSQL Testcontainers,
- Read-only Zugriff auf Kataloge/Stammdaten/Dossier/Geschäft,
- JTE Layout + eigene CSS-Grundsprache,
- HTMX eingebunden, noch wenig Interaktion.

### Funktionen

- Testdaten importieren,
- `/` einfache Startseite,
- `/dossiers/{number}` read-only,
- `/geschaefte/{number}` read-only.

### Klassen mindestens

```text
MabillonApplication
CayenneConfiguration
CayenneUnitOfWork
DossierQueryService
GeschaeftQueryService
DossierController
GeschaeftController
HtmxRequest
```

### Tests

- Context starts,
- Cayenne runtime integration,
- query Nomenklatur-Dossier,
- query Nomenklatur-Geschäft,
- MVC tests,
- 1 Playwright smoke test.

### Gate

`./gradlew clean check` grün + Integration + Browser Smoke.

STOP + `PHASE_1_REPORT.md`.

---

## Phase 2 – Security, Kataloge, Stammdaten, Registraturplan

### Ziele

- Rollen/Permissions,
- dev/test identity,
- Katalogadministration,
- Organisationseinheiten,
- Benutzer,
- Registraturplananzeige/-pflege.

### Use Cases

UC-027 bis UC-033.

### Tests

- Rolle Sachbearbeiter kann Adminseiten nicht ändern,
- Admin kann Katalogwert erzeugen/deaktivieren,
- verwendeter Katalogwert bleibt referenzierbar,
- genau ein initialer Prozessstatus,
- Registraturplan-Zyklus verhindert,
- historische/inaktive Position nicht für neues Dossier auswählbar.

STOP + Bericht.

---

## Phase 3 – Dossier und Geschäft Kern

### Ziele

UC-002 bis UC-011, ohne Dokumentdateien.

### Implementieren

- Dossier öffnen/bearbeiten/suchen,
- Geschäft öffnen/bearbeiten/suchen,
- Nummernvergabe,
- Dossier↔Geschäft,
- Prozessstatus,
- Resultat,
- Fachstatusvalidierung,
- Journal.

### UI

- Dossier-Sicht,
- Geschäfts-Sicht,
- Statusänderung via HTMX,
- Suchlisten.

### Kritische Tests

- konkurrierende Nummernvergabe,
- Prozessstatus falscher Geschäftsart abgelehnt,
- Resultat falscher Geschäftsart abgelehnt,
- Journal atomar mit Änderung,
- HTMX- und Full-Page-Fallback.

STOP + Bericht.

---

## Phase 4 – Beteiligte, Aufgaben, Meine Arbeit

### Use Cases

UC-001, UC-012, UC-013, UC-019 bis UC-022, UC-044 teilweise.

### Funktionen

- Beteiligte erfassen,
- Beteiligungen/Rollen,
- Aufgaben,
- Meine Arbeit Dashboard,
- Fristen/überfällige Aufgaben.

### Tests

- Rollenvalidierung,
- Aufgabe zwingend an Geschäft,
- erledigtAm,
- Dashboard nur relevante Daten,
- Berechtigungen.

STOP + Bericht.

---

## Phase 5 – Unterlagen und Storage

### Use Cases

UC-014 bis UC-018.

### Funktionen

- FileSystemDocumentStorage,
- Upload,
- SHA-256,
- Dossierzuordnung,
- optionaler Geschäftskontext,
- Download,
- Stornierung,
- Eingangs-/Ausgangs-E-Mail als Unterlage.

### Kritische Tests

- Upload success,
- DB failure cleans staging,
- missing storage detected,
- hash mismatch detected,
- Cross-Dossier assignment rejected,
- cancelled document remains audit-visible,
- path traversal filename cannot escape storage root.

STOP + Bericht.

---

## Phase 6 – Fachsystemreferenzen, Suche, Geschäftskontrolle

### Use Cases

UC-023, UC-043, UC-044.

### Funktionen

- Fachsystemlinks,
- globale strukturierte Suche,
- Filter/Pagination,
- Leitungssicht/Geschäftskontrolle.

### Keine Volltextsuche in Binärdokumenten.

STOP + Bericht.

---

## Phase 7 – Abschluss und Datenqualität

### Use Cases

UC-025, UC-026, UC-045, UC-046.

### Funktionen

- vollständige Quality Rules,
- Geschäftabschluss,
- Dossierabschluss,
- Journal/Audit UI,
- Golden Path bis Dossierabschluss.

### Gate

Kompletter Nomenklatur-Golden-Path als Playwright-E2E grün.

STOP + Bericht.

---

## Phase 8 – INTERLIS Datenaustausch

### Use Cases

UC-034, UC-035, UC-036.

### Funktionen

- Import/Export Kataloge,
- Import/Export Stammdaten,
- Import/Export Geschäftsdaten,
- Validierung,
- UI für Jobs/Resultat schlank halten.

### Tests

- jeder Input-XTF wird vor Import mit ilivalidator 1.15.0 geprüft,
- jeder Export-XTF wird nach Export mit ilivalidator 1.15.0 geprüft,
- Roundtrip XTF → DB → XTF semantisch gleich,
- `--importTid` erhält die vorgegebenen Transfer-IDs,
- `--importBid` erhält die vorgegebenen Basket-IDs,
- Kataloge → Stammdaten → Geschäftsdaten behalten ihre Basket-Zuordnung,
- Cross-Topic-Referenzen bleiben nach Import korrekt,
- falsche Importreihenfolge erzeugt verständlichen Fehler,
- ungültiges XTF wird nicht teilweise übernommen,
- Topics/Baskets korrekt,
- Export/Reimport erzeugt keine unbemerkten neuen Objektidentitäten.

STOP + Bericht.

---

## Phase 9 – Aussonderung, Archivablieferung, SIP

### Use Cases

UC-037 bis UC-042.

### Vor Implementierung zwingend

Das konkrete SIP-Zielprofil und dessen XSD/Vorgaben werden gegen die zu diesem Zeitpunkt gültige Spezifikation des Zielarchivs verifiziert und als versioniertes Testfixture ins Projekt aufgenommen, soweit lizenzrechtlich zulässig.

### Funktionen

- Kandidatensuche,
- Ablieferung zusammenstellen,
- SIP generieren,
- SIP validieren,
- Validierungsreport,
- Übergabe/Übernahme/Ablehnung dokumentieren,
- Dossier-Archivierungsstatus aktualisieren.

### Tests

- nur geschlossene Dossiers,
- DQ-Errors blockieren SIP,
- alle Dateien im SIP vorhanden,
- Hashes stimmen,
- XML gegen XSD gültig,
- absichtlich defektes SIP wird als ungültig erkannt,
- mehrere SIP-Erzeugungsversuche bleiben nachvollziehbar,
- Archive acceptance updates dossiers transactionally.

### Gate

Mindestens ein Nomenklatur-Testdossier wird in ein valides Test-SIP exportiert und mit der vorgesehenen Validierung geprüft.

STOP + Bericht.

---

## Phase 10 – Produktionshärtung

### Ziele

- Actuator Health/Metrics,
- Logging,
- Security Headers,
- CSRF mit HTMX,
- Uploadlimits,
- DB Pooling,
- Backups/Restore-Dokumentation,
- Storage Backup,
- Fehlerseiten,
- Observability,
- Performancebaselines,
- Deployment Container,
- SBOM.

### Performance-Baselines

Mit realistischen Fixtures mindestens:

- 100k Geschäfte,
- 100k Dossiers,
- 1M Unterlagen-Metadaten,
- 500k Aufgaben.

Keine künstliche Optimierung vor Messung.

STOP + Abschlussbericht.

---

# 19. Coding-Agent-Verhaltensregeln

Diese Regeln sind absolut verbindlich.

## 19.1 Eine Phase zur Zeit

Der Agent arbeitet nur an der vom Benutzer freigegebenen Phase.

Wenn Phase 3 aktiv ist, darf er keine Phase-4-Funktionen „gleich mitbauen“.

## 19.2 Nach Phase zwingend stoppen

Am Ende jeder Phase:

1. alle vorgesehenen Tests ausführen,
2. Fehler beheben,
3. vollständigen Build ausführen,
4. `PHASE_N_REPORT.md` schreiben,
5. Git-Diff prüfen,
6. offene Punkte nennen,
7. stoppen.

Nicht selbstständig mit nächster Phase fortfahren.

## 19.3 Kein Test-Deaktivieren

Nicht erlaubt:

- `@Disabled` für failing tests als „Fix“,
- Tests löschen, um grün zu werden,
- Assertions abschwächen ohne fachliche Begründung,
- Exception schlucken,
- CI-Step entfernen.

## 19.4 Fehler an der Quelle beheben

Bei Cayenne-Mappingfehler:

```text
INTERLIS → ili2pg → DB → Cayenne Mapping
```

in dieser Reihenfolge untersuchen.

Nicht generierte Klassen oder DataMap blind patchen.

## 19.5 Generated Code

Generierte Basisklassen niemals manuell editieren.

Wenn Anpassung notwendig:

- cgen-Konfiguration,
- DataMap Customization,
- nicht-generierter Subclass,
- Application-Service

verwenden.

## 19.6 Keine spekulative Abstraktion

Keine Interfaces „für später“, wenn nur eine triviale Implementierung existiert, ausser an klaren IO-/Integrationsgrenzen:

Zulässige SPIs:

- `DocumentStorage`,
- `Ili2pgRunner`,
- `SipGenerator`,
- `SipValidator`,
- `CurrentActor`.

Für einfache Fachservices keine unnötigen `FooService` + `FooServiceImpl` Paare.

## 19.7 Java-Code-Stil

- Constructor Injection.
- `final` wo sinnvoll.
- Records für Commands/View Models/Value Objects.
- Keine Field Injection.
- Keine Lombok-Abhängigkeit.
- Keine Map-basierte untypisierte Fachlogik.
- Keine `Object`/JSON-Blob-Abkürzungen für modellierte Fachdaten.
- `Optional` primär als Rückgabetyp, nicht als Entity-/Record-Feld ohne Grund.

## 19.8 SQL

Cayenne ist Standard für fachliche CRUD-Persistenz.

Explizites JDBC/SQL ist erlaubt für:

- technische Nummernsequenz,
- komplexe Read-only Reporting Queries, wenn Cayenne unverhältnismässig wird,
- Schema-/Metadatenprüfung.

Jedes Raw SQL benötigt Integrationstest gegen PostgreSQL.

## 19.9 HTMX

HTMX-Fragmente dürfen keine alternative Businesslogik enthalten.

Keine duplizierten Endpoint-Services für „HTMX“ vs „normal“.

## 19.10 Sicherheitsregel

Benutzereingaben:

- serverseitig validieren,
- Dateinamen nicht als Pfade verwenden,
- MIME nicht blind vertrauen,
- IDs nicht nur über UI-Berechtigung absichern.

## 19.11 Journaling

Jede in dieser Spezifikation als journalpflichtig definierte Änderung muss im selben Use-Case-Commit ein Ereignis erzeugen.

## 19.12 Dokumentation

Bei jeder Architekturentscheidung, die diese Spezifikation verändert:

- ADR erstellen,
- Spezifikationsabweichung benennen,
- vor Umsetzung Benutzerfreigabe einholen.

---

# 20. Phase Report Template

Datei:

```text
docs/phases/PHASE_N_REPORT.md
```

Inhalt:

```markdown
# Phase N Report

## Scope

## Implemented

## Files / Modules

## Tests
- command
- result

## INTERLIS / DB / Cayenne changes

## Manual verification

## Known issues

## Deferred items

## Gate checklist
- [x] acceptance criterion 1
- [x] acceptance criterion 2

## Final status
SUCCESS / FAILED
```

`SUCCESS` darf nur gesetzt werden, wenn alle Gate-Kriterien erfüllt sind.

---

# 21. Nomenklatur-Golden-Path Testdaten

## Kataloge

Geschäftsart:

```text
NOMENKLATURMUTATION
resultatErforderlich=true
```

Prozessstatus:

```text
ANTRAG_EINGEGANGEN      initial=true
FORMELLE_PRUEFUNG
FACHLICHE_PRUEFUNG
ERGAENZUNGEN_AUSSTEHEND
ENTSCHEIDVORBEREITUNG
TRAKTANDIERT
ENTSCHEID_GEFALLEN
UMSETZUNG_FACHSYSTEM
ABGESCHLOSSEN           terminal=true
```

Resultate:

```text
GENEHMIGT
ABGELEHNT
TEILWEISE_GENEHMIGT
ZURUECKGESTELLT
```

Beteiligungsrollen:

```text
ANTRAGSTELLERIN
FACHSTELLE
ENTSCHEIDGREMIUM
AUSFUEHRENDE_STELLE
ADRESSATIN
```

Unterlagentypen:

```text
ANTRAG
EMAIL_EINGANG
EMAIL_AUSGANG
PLAN
AKTENNOTIZ
ENTSCHEIDVORLAGE
BESCHLUSS
MITTEILUNG
```

Aufgabentypen:

```text
FORMELLE_PRUEFUNG
FACHPRUEFUNG
RUECKFRAGE
ENTSCHEIDVORBEREITUNG
TRAKTANDIERUNG
MITTEILUNG
FACHNACHFUEHRUNG
```

## Stammdaten

```text
Organisationseinheit: AGI
Benutzer: anna.mueller
Registraturplanposition: 4.3.2 Einzelgeschäfte Flur- und Ortsnamen
```

## Geschäftsdaten

```text
Dossier:
AGI-D-2026-000007
Umbenennung Flurname "Im alten Boden" zu "Bodenrain", Musterwil

Geschäft:
AGI-G-2026-000421
Umbenennung "Im alten Boden" zu "Bodenrain"

Beteiligter:
Gemeinde Musterwil
Rolle: Antragstellerin
```

Unterlagen:

```text
Antrag Gemeinde Musterwil
Gemeinderatsbeschluss
Situationsplan
Rückfrage an Gemeinde
Antwort Gemeinde
Entscheidvorlage
Beschluss Nomenklaturkommission
Mitteilung an Gemeinde
```

---

# 22. Definition of Done – global

Eine Funktion gilt nur als fertig, wenn:

- Fachregel implementiert,
- positive Tests vorhanden,
- relevante negative Tests vorhanden,
- Berechtigungen getestet,
- Journal falls erforderlich getestet,
- Controller/Formvalidierung getestet,
- UI kann Fehler verständlich darstellen,
- keine unaufgelösten Compiler-/Lint-Warnungen aus eigener Änderung,
- keine geheimen Credentials im Repo,
- keine TODOs im Scope,
- Dokumentation aktualisiert.

---

# 23. Ausdrücklich ausserhalb MVP

Nicht ohne neue Spezifikationsphase implementieren:

- BPMN Engine,
- frei konfigurierbare Prozessgraphen,
- Dokumentversionierung,
- kollaborativer Office-Editor,
- automatische Exchange/IMAP-Mailbox-Anbindung,
- OCR,
- KI-Klassifikation,
- Volltextsuche in Dokumentinhalten,
- elektronische Signatur,
- komplexe Record-Level-ACL,
- Aufbewahrungsfrist-/Archivwürdigkeitsregelengine,
- Mandantenfähigkeit mit harter Datentrennung,
- Microservices,
- Event Sourcing.

---

# 23.1 Agent-Skills für Codex und OpenCode

Projektlokale Skills liegen ausschliesslich im offenen, von beiden Zielagenten unterstützten Verzeichnis:

```text
.agents/skills/<skill-name>/SKILL.md
```

Verbindliche Skills:

```text
mabillon-phase-workflow
mabillon-domain-model
mabillon-interlis-ili2pg
mabillon-cayenne-mcp
mabillon-spring-jte-htmx
mabillon-ui-design
mabillon-testing
mabillon-archive-sip
```

`AGENTS.md` nennt diese Skills zusätzlich mit Trigger/Scope, damit ihre Verwendung auch bei langen Sessions eindeutig bleibt. Skills ersetzen diese Spezifikation nicht; sie verdichten wiederkehrende Arbeitsabläufe. Bei Widerspruch gilt: `AGENTS.md` → diese Spezifikation → Skill-Instruktion.

Jeder Skill verwendet YAML-Frontmatter mit mindestens `name` und `description`; der Verzeichnisname entspricht dem `name`. Skills dürfen eigene kleine Referenzdateien/Skripte enthalten, aber keine duplizierte Kopie der gesamten Spezifikation.

# 24. Technische Verifikation vor Phase 1

Der Agent muss in Phase 0 explizit verifizieren und dokumentieren:

1. Spring Boot 4.1.0 startet mit Java 25.
2. JTE Spring Boot 4 Starter kompiliert und rendert ein Template.
3. HTMX 2.0.10 wird lokal versioniert/ausgeliefert; kein CDN-Zwang.
4. Cayenne 5.0-M2 Runtime startet.
5. Cayenne `ObjectContext` kann lesen, schreiben, committen und rollbacken.
6. Cayenne Modeler MCP ist vom lokalen Agent-Code erreichbar.
7. MCP kann das Projekt öffnen.
8. MCP kann DB Import ausführen.
9. MCP kann cgen ausführen.
10. ili2pg-generierte FKs werden durch Cayenne als erwartete Relationships erkannt.
11. Association `Unterlage_Geschaeft` erzeugt ein brauchbares Mapping.
12. Topic-/Basket-Metadaten behindern Cayenne nicht.
13. `t_id`, `t_ili_tid`, `t_basket` werden korrekt verstanden und nicht als Fachnummern missbraucht.
14. ili2pg 5.5.2 wird exakt vom Default-Pfad `/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar` gestartet oder bewusst über `ILI2PG_JAR` überschrieben.
15. ili2c 5.6.8 wird exakt vom Default-Pfad `/Users/stefan/apps/ili2c-5.6.8/ili2c.jar` gestartet oder bewusst über `ILI2C_JAR` überschrieben.
16. ilivalidator 1.15.0 wird exakt vom Default-Pfad `/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar` gestartet oder bewusst über `ILIVALIDATOR_JAR` überschrieben.
17. Ein absichtlich ungültiges XTF wird von ilivalidator erkannt und gelangt nicht in den ili2pg-Import.
18. Ein gültiger Mabillon-XTF-Export besteht die ilivalidator-Prüfung.
15. Schemaimport verwendet `--createTidCol --createBasketCol`.
16. Testdatenimport verwendet `--importTid --importBid` und erhält die erwarteten IDs.
17. Die `ili2grails`-Designreferenz ist auf den in Abschnitt 2.2 genannten Commit gepinnt und dokumentiert.
18. Die projektspezifischen Agent-Skills liegen unter `.agents/skills/<name>/SKILL.md` und werden in der lokalen Codex- und OpenCode-Konfiguration entdeckt.

Wenn einer dieser Punkte scheitert, endet Phase 0 mit Status FAILED. Der Agent darf keine Workarounds in Phase 1 verstecken.

---

# 25. Leitprinzip für alle weiteren Entscheidungen

Bei zwei technisch möglichen Lösungen ist diejenige vorzuziehen, die:

1. weniger implizite Magie besitzt,
2. weniger Framework-spezifischen Zustand erzeugt,
3. mit normalem HTTP verständlich bleibt,
4. durch Integrationstests leicht überprüfbar ist,
5. das INTERLIS-Modell als führendes Fachmodell respektiert,
6. eine spätere Archivierung nicht erschwert,
7. von einem neuen Entwickler oder Coding Agent anhand des Codes nachvollzogen werden kann.

Kurz:

> Fachlichkeit explizit. Persistenz modellgetrieben. UI serverseitig. Interaktion progressiv. Phasen klein. Tests verbindlich.
