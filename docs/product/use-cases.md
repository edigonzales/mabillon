# Fachliche Use Cases

Die IDs sind stabile Referenzen für Fachlichkeit, Tests und Diskussionen. Sie beschreiben den aktuellen Produktumfang; Implementierungsdetails wie konkrete Java-Klassennamen sind nicht Teil des fachlichen Vertrags.

## Persönliche Arbeit, Dossier und Geschäft

- **UC-001 – Meine Arbeit anzeigen:** offene und überfällige Aufgaben, aktive/fällige und zuletzt bearbeitete Geschäfte des Benutzers anzeigen.
- **UC-002 – Geschäft suchen:** Geschäfte nach fachlichen Kriterien suchen, sortieren und paginieren.
- **UC-003 – Dossier suchen:** Dossiers nach Nummer, Titel, Registraturplan, Status und Verantwortlichkeit suchen.
- **UC-004 – Neues Geschäft eröffnen:** Geschäft in einem offenen Dossier mit aktiver Geschäftsart und Initialstatus eröffnen; Nummer und Journal werden erzeugt.
- **UC-005 – Neues Dossier eröffnen:** Dossier an einer aktiven Registraturplanposition eröffnen und automatisch nummerieren.
- **UC-006 – Geschäft bestehendem Dossier zuordnen:** Zuordnung erfolgt bei der Geschäftseröffnung; nachträgliches normales Verschieben ist nicht vorgesehen.
- **UC-007 – Dossier anzeigen:** Basisdaten, Geschäfte, Unterlagen, Verlauf und Archivierungsinformationen anzeigen.
- **UC-008 – Geschäft anzeigen:** Basisdaten, Dossier, Status/Resultat, Beteiligungen, Aufgaben, Unterlagen, Referenzen und Journal anzeigen.
- **UC-009 – Geschäft bearbeiten:** explizit editierbare Geschäftsfelder ändern; Nummer und Geschäftsart bleiben unveränderlich.
- **UC-010 – Prozessstatus ändern:** nur aktive, zur Geschäftsart passende Zielstatus zulassen und die Änderung journalisieren.
- **UC-011 – Geschäftsergebnis erfassen:** zur Geschäftsart passendes Resultat erfassen und journalisieren.

## Beteiligte, Unterlagen und Aufgaben

- **UC-012 – Beteiligten erfassen:** Person, Organisation oder interne Organisationseinheit erfassen; mögliche Duplikate werden als Hinweis behandelt.
- **UC-013 – Beteiligten einem Geschäft zuordnen:** Beteiligung mit aktiver Rolle und gültigem Zeitraum führen.
- **UC-014 – Unterlage registrieren:** Datei und Metadaten registrieren, zwingend einem Dossier und optional einem Geschäft zuordnen.
- **UC-015 – Unterlage einem Geschäft zuordnen:** Zuordnung nur, wenn Geschäft und Unterlage zum selben Dossier gehören.
- **UC-016 – Eingegangene E-Mail registrieren:** eingehende E-Mail als Unterlage registrieren, ohne Mailboxintegration.
- **UC-017 – Ausgangsschreiben registrieren:** ausgehende E-Mail/Unterlage mit Ausgangsdatum registrieren.
- **UC-018 – Unterlage anzeigen/herunterladen:** Metadaten anzeigen und verwaltete Datei mit korrekten HTTP-Headern herunterladen.
- **UC-019 – Aufgabe erstellen:** Aufgabe zwingend innerhalb eines Geschäfts erzeugen.
- **UC-020 – Aufgabe bearbeiten:** offene Aufgabe ändern oder delegieren; abgeschlossene/abgebrochene Aufgaben sind nicht normal editierbar.
- **UC-021 – Aufgabe erledigen:** Status und Erledigungszeitpunkt atomar mit Journal setzen.
- **UC-022 – Eigene Aufgaben verwalten:** persönliche offene und überfällige Aufgaben suchen und bearbeiten.

## Referenzen, Abschluss und Administration

- **UC-023 – Fachsystemreferenz erfassen:** externe Referenz an Dossier und/oder Geschäft mit System-, Objekt- und optionalen Mutationsdaten führen.
- **UC-024 – Journal eines Geschäfts anzeigen:** unveränderbare fachliche Historie anzeigen.
- **UC-025 – Geschäft abschliessen:** Abschluss nur bei erfüllten Aufgaben-, Prozess-, Resultat- und Unterlagenregeln.
- **UC-026 – Dossier abschliessen:** Abschluss nur bei geschlossenen Geschäften, zulässigen Unterlagenzuständen und ohne blockierende Qualitätsbefunde.
- **UC-027 – Geschäftsart konfigurieren:** Geschäftsarten inkl. Resultatpflicht pflegen.
- **UC-028 – Prozessstatus konfigurieren:** geschäftsartspezifische Prozessstatus inkl. Initial-/Terminalkennzeichnung und Sortierung pflegen.
- **UC-029 – Kataloge pflegen:** Katalogwerte aktivieren/deaktivieren; verwendete historische Werte nicht physisch löschen.
- **UC-030 – Organisationseinheiten pflegen:** hierarchische Organisationseinheiten verwalten.
- **UC-031 – Benutzer pflegen:** fachliche Benutzer und Organisationszuordnung verwalten; Authentifizierung bleibt davon getrennt.
- **UC-032 – Registraturplan pflegen:** Registraturpläne anlegen, ersetzen und verwalten.
- **UC-033 – Registraturplanposition pflegen:** Baumpositionen anlegen, ändern, verschieben und deaktivieren; Zyklen sind verboten.

## INTERLIS-Austausch

- **UC-034 – Katalogdaten importieren/exportieren:** Katalog-Topic validiert austauschen.
- **UC-035 – Stammdaten importieren/exportieren:** Stammdaten-Topic validiert austauschen.
- **UC-036 – Geschäftsdaten importieren/exportieren:** Geschäftsdaten mit erhaltenen Transferidentitäten, Baskets und Referenzen austauschen.

## Archivierung, Suche und Qualität

- **UC-037 – Dossiers zur Aussonderung suchen:** geschlossene, nicht bereits abschliessend behandelte und qualitätsgeprüfte Dossiers als Kandidaten ermitteln.
- **UC-038 – Archivablieferung zusammenstellen:** geeignete Dossiers zu einer Ablieferung hinzufügen oder daraus entfernen.
- **UC-039 – SIP erzeugen:** strukturiertes SIP nach konfiguriertem Profil erzeugen; kein blosses ZIP der Dokumente.
- **UC-040 – SIP validieren:** Paketstruktur, XML/XSD, Dateien und Prüfsummen validieren und Ergebnis persistieren.
- **UC-041 – SIP-Ablieferung dokumentieren:** Übergabe, Empfänger und Bemerkungen nachvollziehbar dokumentieren.
- **UC-042 – Dossier nach erfolgreicher Ablieferung kennzeichnen:** erfolgreiche Übernahme samt Archivsignatur/-referenz und Dossierstatus atomar dokumentieren.
- **UC-043 – Systemweite Suche:** strukturierte Suche über Dossiers, Geschäfte, Beteiligte, Unterlagen und Fachsystemreferenzen.
- **UC-044 – Geschäftskontrolle / Fristenübersicht:** offene/überfällige Geschäfte und Aufgaben, Prozessstatusverteilung und inaktive Fälle anzeigen.
- **UC-045 – Datenqualität prüfen:** Dossiers, Geschäfte und Archivablieferungen gegen die definierten DQ-Regeln prüfen.
- **UC-046 – Historie/Audit nachvollziehen:** fachlich relevante Änderungen mit Zeitpunkt und eindeutigem Akteur nachvollziehen.

Siehe auch [Fachliche Regeln](../domain/lifecycle-and-rules.md) und [Datenqualität](../domain/data-quality.md).
