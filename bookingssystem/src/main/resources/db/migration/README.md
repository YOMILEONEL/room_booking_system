# Flyway migrations

The existing dev schema (created by the old `ddl-auto=update`) is treated as the baseline
(`spring.flyway.baseline-on-migrate=true`, `baseline-version=1`) — no `V1__*.sql` was written
by hand, since the live schema and the JPA entities already match.

Add future schema changes here as `V2__description.sql`, `V3__description.sql`, etc.
`ddl-auto` is now `validate`, so Hibernate no longer changes the schema itself.
