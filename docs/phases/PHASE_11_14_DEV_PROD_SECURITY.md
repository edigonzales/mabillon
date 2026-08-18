# Phase 11.14 – Dev/prod security separation

## Goal

The binding specification permits defined local identities for development and tests, while production must be prepared for an external identity provider and must never silently start with development credentials.

## Implementation

`SecurityConfiguration` now separates local authentication by Spring profile:

- `dev` and `test` create the two existing `InMemoryUserDetailsManager` identities (`admin` and `sachbearbeiter` by default); these remain deliberately simple local identities for development/test only.
- every other profile, including `prod`, `staging`, `qa` and the no-profile default, receives a fail-closed `UserDetailsService` that resolves no local user.
- the common authorization/filter-chain rules remain unchanged; fachliche routes are still default-deny and admin/actuator routes remain role-protected.

This deliberately does **not** invent an OIDC provider in Phase 11.14. The production side is OIDC-ready in the sense required by the specification: the fachliche code remains dependent on `CurrentActor`, while a later external authentication provider can be wired into Spring Security without reintroducing local application passwords.

## Runtime profiles

The production `Dockerfile` now sets:

```text
SPRING_PROFILES_ACTIVE=prod
```

The compose definition no longer requires `MABILLON_SECURITY_ADMIN_PASSWORD`; production/container startup therefore cannot be interpreted as enabling a local Mabillon administrator.

Tests use `spring.profiles.default=test`. This makes the local test identities available to the normal integration/Playwright suite while allowing `@ActiveProfiles("prod")` to replace the default cleanly.

For explicit local development, start Mabillon with the `dev` profile, for example by setting:

```text
SPRING_PROFILES_ACTIVE=dev
```

## Automated evidence

`SecurityConfigurationTest` now explicitly runs with the `test` profile and continues to prove the intended local test identities and route authorization.

`ProductionSecurityConfigurationTest` runs with the `prod` profile and proves both:

1. `admin/admin` and `sachbearbeiter/sachbearbeiter` receive HTTP 401 on protected fachliche routes;
2. the active production `UserDetailsService` cannot resolve the local `admin` identity and throws `UsernameNotFoundException`.

## Security consequence

The previous risk represented by `X-SEC-02` is closed: development credentials are not present in production or in any non-dev/test profile. Until an external provider such as OIDC is configured, production authentication fails closed rather than falling back to application-local credentials.
