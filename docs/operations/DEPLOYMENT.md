# Deployment-Hinweise

Der Container wird aus einem zuvor geprüften `bootJar` gebaut:

```bash
./gradlew clean check bootJar cyclonedxBom --no-daemon
docker build --tag mabillon:local .
```

`compose.yaml` stellt die Laufzeitdienste bereit, importiert aber bewusst kein
fachliches Schema. Die PostgreSQL-Datenbank muss vor dem App-Start mit der
freigegebenen INTERLIS-/ili2pg-Pipeline provisioniert werden; danach werden
Cayenne DB Import und cgen gegen dasselbe Schema ausgeführt. Ein leerer
PostgreSQL-Container ist kein gültiger Mabillon-Produktionszustand.

Das Image enthält keine Datenbankpasswörter. `compose.yaml` verlangt Passwörter über die Umgebung. In Produktion sind Secrets über den Secret-Store des Orchestrators bereitzustellen.

Vor Freigabe:

- `/actuator/health` muss `UP` melden.
- `/actuator/metrics` und `/actuator/info` sind nur für Administratoren erreichbar.
- Upload- und Request-Limits müssen zum Storage-/Proxy-Limit passen.
- `build/reports/bom.json` wird als CycloneDX-SBOM archiviert.
- Datenbank- und Storage-Backup müssen gemeinsam erfolgreich getestet sein.
