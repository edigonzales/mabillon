# Mabillon specification v0.4 changes

- Added fixed local ili2c 5.6.8 path: `/Users/stefan/apps/ili2c-5.6.8/ili2c.jar`.
- Added fixed local ilivalidator 1.15.0 path: `/Users/stefan/apps/ilivalidator-1.15.0/ilivalidator-1.15.0.jar`.
- Added `ILI2C_JAR` and `ILIVALIDATOR_JAR` overrides alongside `ILI2PG_JAR`.
- Made ili2c validation a hard prerequisite before ili2pg schema generation and Cayenne DB import/cgen.
- Made ilivalidator validation mandatory before every normal XTF import and after every XTF export.
- Added negative-test requirement proving invalid XTF is rejected before DB import.
- Added Java process adapter contracts for model and XTF validation.
- Updated Phase 0 and Phase 8 gates and the portable Codex/OpenCode skills.
- Corrected the remaining DB schema label from `gever` to `mabillon`.
