# Fachliche Begriffe und Beziehungen

## Kernmodell

```text
Registraturplan
  └─ Registraturplanposition
       └─ Dossier
            ├─ Geschäft
            │    ├─ Aufgabe
            │    ├─ Beteiligung ──> Beteiligter
            │    └─ Fachsystemreferenz
            └─ Unterlage
                 └─ optionaler Geschäftskontext
```

## Registraturplan

Im INTERLIS-Modell heissen die fachlichen Objekte `Ordnungssystem` und `OrdnungssystemPosition`; in der Benutzeroberfläche verwendet Mabillon den Begriff **Registraturplan**. Ein Dossier ist genau einer Registraturplanposition zugeordnet.

## Dossier

Das Dossier ist die Akte. Es beantwortet insbesondere:

- wo die Akte klassifiziert ist,
- welche Geschäfte und Unterlagen sie enthält,
- wer verantwortlich ist,
- ob sie offen, geschlossen, archiviert oder vernichtet ist.

Ein Dossier kann mehrere Geschäfte enthalten.

## Geschäft

Das Geschäft ist der bearbeitete Vorgang. Es besitzt unter anderem Geschäftsart, Lifecycle, Prozessstatus, optionales Resultat, Verantwortlichkeit, Aufgaben, Beteiligungen und Fachsystemreferenzen.

**Ein Geschäft gehört genau zu einem Dossier.**

## Unterlage

Eine Unterlage ist ein Aktenstück wie Antrag, E-Mail, Brief, Plan, Aktennotiz oder Beschluss.

- Sie gehört zwingend zu genau einem Dossier.
- Sie kann optional ein Geschäft als Geschäftskontext referenzieren.
- Ist ein Geschäft gesetzt, muss dieses Geschäft zum selben Dossier gehören.

## Aufgabe

Eine Aufgabe ist ein konkreter Arbeitsschritt innerhalb genau eines Geschäfts. Aufgaben sind Arbeitssteuerung, nicht Archivklassifikation.

## Beteiligter und Beteiligung

- **Beteiligter:** Wer ist beteiligt? Person, Organisation oder interne Organisationseinheit.
- **Beteiligung:** Welche Rolle hat dieser Beteiligte in einem konkreten Geschäft und in welchem Gültigkeitszeitraum?

## Kataloge und Stammdaten

Kataloge sind fachliche Konfigurationswerte wie Geschäftsart, Prozessstatus, Resultatstatus, Beteiligungsrolle, Unterlagen- und Aufgabentyp.

Stammdaten sind insbesondere Organisationseinheiten, fachliche Benutzer und Registraturpläne.

Katalog- und Stammdaten werden bei historischer Verwendung nicht physisch gelöscht; sie können deaktiviert oder ersetzt werden.

## Drei Identitätsebenen

Mabillon trennt konsequent:

1. **`t_id`** – interner relationaler DB-Schlüssel.
2. **`t_ili_tid`** – stabile INTERLIS-Transferidentität.
3. **Fachliche Nummer** – menschenlesbare Identität, z. B. `AGI-G-2026-000421`.

DB-IDs sind weder fachliche Nummern noch Berechtigungsmerkmale. Transferidentitäten werden beim INTERLIS-Austausch erhalten.

## Journal

Fachlich relevante Änderungen erzeugen ein Ereignis im Journal. Änderung und Ereignis werden im selben Persistenz-Unit-of-Work geschrieben. Ein normaler Benutzer kann Journalereignisse nicht nachträglich editieren oder löschen.
