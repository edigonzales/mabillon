# SIP-Profil eCH-0160 1.3.0

- Profil-ID: `ech-0160-1.3.0`
- Standard: eCH-0160, genehmigte Version 1.3.0
- XSD-interne Version: `5.1`
- Ziel: generisches GEVER-SIP nach eCH-0160, nicht automatisch eine archiv-spezifische Abnahme
- XSD-Quelle: `https://www.ech.ch/sites/default/files/imce/eCH-Dossier/0151-0180/eCH-0160/1.3.0/Beilagen/eCH-0160-1.3.0.xsd.zip`
- XSDs im Projekt: `xsd/`

Das SIP wird als strukturierter Verzeichnisbaum mit `header/metadata.xml`, `header/xsd/` und `content/` erzeugt. Ein Containerformat wird nicht behauptet; das Verpacken ist mit dem Zielarchiv zu vereinbaren.

## Schema als Source of Truth

Die lokal versionierten XSD-Dateien sind die technische Source of Truth für die XML-Struktur dieses Profils:

1. Beim Build erzeugt XJC daraus Jakarta-XML-Binding-Klassen unter `build/generated/sources/ech0160/java`. Diese generierten Quellen werden nicht versioniert.
2. `Ech0160SipGenerator` bildet die Mabillon-Daten auf diese generierten Typen ab und marshalt `header/metadata.xml` mit Jakarta XML Binding.
3. Bereits beim Marshalling wird das Ergebnis gegen `arelda.xsd` validiert.
4. Die XSD-Dateien werden zusätzlich nach `header/xsd/` in das SIP kopiert.
5. `Ech0160SipValidator` validiert das fertige SIP unabhängig nochmals gegen die mitgelieferten XSDs und prüft zusätzlich Paketdateien und SHA-256-Werte.

Damit wird die eCH-0160-Struktur nicht als handgeschriebenes XML im Anwendungscode dupliziert. Die unabhängige Validierung bleibt dennoch erhalten, weil XSD-Constraints und die Konsistenz des fertigen Pakets nicht allein durch generierte Java-Typen garantiert werden.

Das Schweizerische Bundesarchiv und andere Zielarchive können zusätzliche SIP-Spezifikationen oder Profilregeln verlangen. Mabillon gibt das generische eCH-0160-Profil nicht als automatische Kompatibilität mit solchen zusätzlichen Anforderungen aus. Dafür ist ein separates, mit dem Zielarchiv vereinbartes Profil samt Validierung erforderlich.
