# Lifecycle und zentrale Fachregeln

## Dossier

Ein neues Dossier startet im Status `Offen`. Für die Eröffnung gelten mindestens:

- aktive Registraturplanposition,
- gültige Federführung/Verantwortlichkeit,
- automatisch vergebene fachliche Dossiernummer.

Ein Dossier kann nur geschlossen werden, wenn:

- alle enthaltenen Geschäfte fachlich abgeschlossen sind,
- keine aktenrelevante Unterlage in einem unzulässigen Arbeitszustand verbleibt,
- die Datenqualitätsprüfung keinen blockierenden Fehler für das Dossier liefert.

Nach erfolgreicher Archivübernahme kann das Dossier als `Archiviert` gekennzeichnet werden. Eine Archivübernahme löst keine automatische Vernichtung aus.

## Geschäft

Lifecycle und Prozessstatus sind getrennte Konzepte.

Typischer Lifecycle:

```text
Eroeffnet -> In_Bearbeitung -> Abgeschlossen
                  |
                  +-> Sistiert -> In_Bearbeitung
```

Archivierte oder vernichtete Geschäfte sind ebenfalls terminal.

Ein Prozessstatus gehört zu genau einer Geschäftsart. Mabillon verwendet keinen generischen Transition Graph: Ein Statuswechsel ist fachlich zulässig, wenn der Zielstatus aktiv, der Geschäftsart zugeordnet und das Geschäft noch bearbeitbar ist.

Ein Resultatstatus muss ebenfalls zur Geschäftsart gehören. Eine Geschäftsart kann `resultatErforderlich=true` definieren.

### Abschluss eines Geschäfts

Der Abschluss wird abgewiesen, wenn mindestens eine Bedingung verletzt ist:

- offene/nicht abgeschlossene Aufgaben existieren,
- ein vorhandener Prozessstatus ist nicht terminal,
- ein erforderliches Resultat fehlt,
- ein gesetzter Resultatstatus gehört nicht zur Geschäftsart,
- aktenrelevante Unterlagen des Geschäfts befinden sich noch in einem nicht zulässigen Zustand.

Nummer und Geschäftsart sind nach der Erstellung im normalen Bearbeitungsflow unveränderlich.

## Aufgabe

Eine Aufgabe gehört genau einem Geschäft. Relevante Zustände sind offen/registriert, in Bearbeitung, erledigt und abgebrochen.

- Erledigen setzt den Erledigungszeitpunkt.
- Erledigte oder abgebrochene Aufgaben werden nicht normal weiterbearbeitet.
- Delegation ändert die Zuweisung und bleibt journalisiert.

## Beteiligung

- Beteiligungsrolle muss aktiv sein.
- `gültig bis` darf nicht vor `gültig von` liegen.
- Beteiligungen eines abgeschlossenen, archivierten oder vernichteten Geschäfts werden nicht normal verändert.
- Beenden ist dem physischen Löschen historisch relevanter Beteiligungen vorzuziehen.
- Potenzielle Beteiligten-Duplikate erzeugen einen Hinweis; es gibt keinen automatischen globalen Merge.

## Kataloge

- Codes bleiben nach der Erstellung stabil.
- In Verwendung befindliche Werte werden nicht physisch gelöscht.
- Prozess- und Resultatstatus gehören zu ihrer Geschäftsart.
- Pro Geschäftsart existiert höchstens ein aktiver Initialstatus; wenn ein Initialstatus verlangt wird, muss er eindeutig sein.

## Registraturplan

- Neue Dossiers dürfen nur aktive, zulässige Positionen verwenden.
- Historische Dossiers behalten ihre Position, auch wenn sie später deaktiviert wird.
- Positionen können verschoben werden, aber der Baum darf keinen Zyklus enthalten.
- Rootpositionen besitzen keinen Parent.

## Fachliche Nummern

Format:

```text
<ORG>-<TYP>-<JAHR>-<6-stellige Sequenz>
```

Typen sind mindestens `D` für Dossier, `G` für Geschäft und `A` für Archivablieferung. Die Vergabe ist transaktions- und konkurrenzsicher über eine technische Sequenz im Schema `mabillon_app`.

## Zeit und Audit

Fachservices verwenden einen injizierten `Clock`, damit Zeitwerte testbar und konsistent sind. Journalereignisse werden mit demselben fachlichen Schreibvorgang atomar persistiert.
