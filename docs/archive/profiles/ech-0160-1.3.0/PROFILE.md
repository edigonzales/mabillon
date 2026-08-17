# SIP-Profil eCH-0160 1.3.0

- Profil-ID: `ech-0160-1.3.0`
- Standard: eCH-0160, genehmigte Version 1.3.0
- XSD-interne Version: `5.1`
- Ziel: generisches GEVER-SIP nach eCH-0160, nicht BAR-spezifische Abnahme
- XSD-Quelle: https://www.ech.ch/sites/default/files/imce/eCH-Dossier/0151-0180/eCH-0160/1.3.0/Beilagen/eCH-0160-1.3.0.xsd.zip
- Abgerufen: 2026-08-16

Die entpackten XSD-Dateien unter `xsd/` sind das versionierte Testfixture. Das
SIP wird als strukturierter Verzeichnisbaum mit `header/metadata.xml`,
`header/xsd/` und `content/` erzeugt. Ein Containerformat wird nicht behauptet;
eCH-0160 beschreibt das Verpacken als mit dem Zielarchiv zu vereinbarende
Option.

Das Schweizerische Bundesarchiv veröffentlicht zusätzlich eigene SIP-
Spezifikationen. Diese werden in Mabillon nicht als BAR-Kompatibilität
ausgegeben. Eine BAR-Ablieferung braucht eine separate, mit dem Zielarchiv
vereinbarte Profilentscheidung und Validierung.
