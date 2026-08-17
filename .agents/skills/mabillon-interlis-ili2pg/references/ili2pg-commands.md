# Mabillon INTERLIS tool command contract

Default JARs:

```bash
ILI2PG_JAR="${ILI2PG_JAR:-/Users/stefan/apps/ili2pg-5.5.2/ili2pg-5.5.2.jar}"
ILI2C_JAR="${ILI2C_JAR:-/Users/stefan/apps/ili2c-5.6.8/ili2c.jar}"
ILIVALIDATOR_JAR="${ILIVALIDATOR_JAR:-/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar}"
```

## Model validation

Before schema generation:

```bash
java -jar "$ILI2C_JAR" model/SO_AGI_GEVER_20260707.ili
```

Do not continue to ili2pg/Cayenne after a failed model validation.

## XTF validation

Before every normal import and after every export:

```bash
java -jar "$ILIVALIDATOR_JAR" "$XTF"
```

A validation failure blocks import or successful export delivery.

## Schema import

The project script must include:

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

Connection arguments are passed as separate ProcessBuilder/shell arguments. Never print passwords.

## XTF import

Validate with ilivalidator first. Every normal application/test import then includes:

```text
--import
--dbschema mabillon
--importTid
--importBid
```

Import Kataloge before Stammdaten before Geschaeftsdaten.

## Validation assertions

After test import, assert known fixture TIDs and BIDs, object counts, cross-topic references and expected Dossier/Geschaeft/Unterlage associations. A successful process exit alone is not sufficient.

## Export validation

After `ili2pg --export`, run ilivalidator against the resulting XTF before returning success. Roundtrip tests also assert known TIDs/BIDs, object counts and cross-topic references.
