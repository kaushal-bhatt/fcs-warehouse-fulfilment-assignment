# Warehouse Fulfilment Code Assignment

[![CI](https://github.com/kaushal-bhatt/fcs-warehouse-fulfilment-assignment/actions/workflows/ci.yml/badge.svg)](https://github.com/kaushal-bhatt/fcs-warehouse-fulfilment-assignment/actions/workflows/ci.yml)

This is my solution to the fulfilment code assignment: a small Quarkus application that manages
warehouses, stores and products. The first commit is the code base exactly as I received it, and
every commit after that covers one task or improvement, so the history shows how the solution was
built step by step.

| What | Where |
|---|---|
| Tasks of the assignment | [java-assignment/CODE_ASSIGNMENT.md](java-assignment/CODE_ASSIGNMENT.md) |
| Domain briefing | [case-study/BRIEFING.md](case-study/BRIEFING.md) |
| My answers to the code questions | [java-assignment/QUESTIONS.md](java-assignment/QUESTIONS.md) |
| My answers to the case study | [case-study/CASE_STUDY.md](case-study/CASE_STUDY.md) |

## Running it

You need JDK 17 and Docker. Docker is used by Quarkus Dev Services to start PostgreSQL
automatically for tests and dev mode, so no database has to be installed.

```bash
cd java-assignment

# all tests, integration tests and the 80% coverage check
./mvnw clean verify

# run the application with live reload on http://localhost:8080
./mvnw quarkus:dev
```

The coverage report ends up in `java-assignment/target/jacoco-report/index.html`, and the same
report is attached to every CI run.

To try the API by hand, import
[postman/FnCS_Casestudy2.postman_collection.json](postman/FnCS_Casestudy2.postman_collection.json).
It has a request for every endpoint and rule, each with tests, and expects the application on
port 8081 with a fresh database. Running the whole collection top to bottom with the Collection
Runner goes through all tasks, including the bonus. Health checks are available at `/q/health/live` and
`/q/health/ready`; readiness includes the database connection.

Two things I ran into on my own machine, in case they help:
- If port 8080 is taken, start dev mode with `-Dquarkus.http.port=8081`. In PowerShell the
  argument needs quotes: `./mvnw quarkus:dev "-Dquarkus.http.port=8081"`.
- Tests and dev mode run in UTC (configured in the `pom.xml`). Some host time zones, like
  `Asia/Calcutta`, are rejected by the PostgreSQL container. If you run tests from the IDE
  without delegating to Maven, add `-Duser.timezone=UTC` to the JUnit run configuration.

## What I implemented

### Task 1: Location

`LocationGateway.resolveByIdentifier` returns the location with an exact match, or `null` for
unknown and blank identifiers. Deciding what an unknown location means is left to the warehouse
use cases. I also made the gateway a CDI bean, because the use cases need to inject it.

### Task 2: Store and the legacy system

The resource used to call the legacy gateway inside the transaction, so a failed commit still
reached the legacy system. Now the resource fires a `StoreChangedEvent`, and `LegacyStoreSyncObserver` handles it with
`@Observes(during = TransactionPhase.AFTER_SUCCESS)`. If the transaction rolls back, the observer
is never called. A test proves this by creating a duplicate store name, which fails on commit.
While there, I fixed two bugs: update and patch sent the request body (without id) to the legacy
system instead of the saved store, and PATCH reset the stock to 0 when only the name was sent.

### Task 3: Warehouses

All endpoints of the OpenAPI spec are implemented: list, get, create, archive and replace. The
rules live in plain Java use cases, so they are tested without starting Quarkus:
- create: unique business unit code, existing location, maximum number of warehouses per
  location, total capacity of the location, stock within capacity
- replace: an active warehouse with the code must exist, the new capacity must hold the current
  stock and the stock must match; the old warehouse is archived and the new one created in the
  same transaction, so the history is kept
- archive: sets `archivedAt`; archived warehouses disappear from list and get, but stay in the
  database

### Bonus: warehouses as fulfilment units

A warehouse can be assigned to fulfil a product for a store (`/fulfilment`). The three limits are
checked in `FulfilmentService`: a product can be fulfilled by at most 2 warehouses per store, a
store by at most 3 warehouses, and a warehouse can hold at most 5 product types. A unique
constraint prevents the same assignment from being stored twice.

The assignment points to the warehouse by its business unit code, not its id. When a warehouse
is replaced, the new one has the same code, so its assignments simply carry over. Only active
warehouses count: once a warehouse is archived without a replacement, its assignments are no
longer listed and stop counting towards the limits.

### Beyond the tasks

One global exception mapper instead of two duplicated ones, so errors look the same on every
endpoint and never expose internal class names. Unique constraint violations return 409 instead
of 500. Store and product input is validated. CI runs on every push, health checks are enabled,
and the `WarehouseEndpointIT` that came with the assignment now actually runs during
`mvn verify`, against the packaged jar.

## API overview

| Endpoint | Success | Errors |
|---|---|---|
| `GET /warehouse` | 200, active warehouses only | |
| `GET /warehouse/{id}` | 200 | 404 unknown or archived |
| `POST /warehouse` | 201 | 400 rule violated |
| `DELETE /warehouse/{id}` | 204, archives the warehouse | 404 |
| `POST /warehouse/{businessUnitCode}/replacement` | 200 | 400 rule violated, 404 no active warehouse |
| `GET/POST/PUT/PATCH/DELETE /store` | 200 / 201 / 204 | 404, 409 duplicate name, 422 invalid input |
| `GET/POST/PUT/DELETE /product` | 200 / 201 / 204 | 404, 409 duplicate name, 422 invalid input |
| `GET /fulfilment?storeId=` | 200, assignments to active warehouses | |
| `POST /fulfilment` | 201 | 400 missing field, 404 unknown store, product or warehouse, 409 limit reached or duplicate |
| `DELETE /fulfilment/{id}` | 204 | 404 |

All errors use the same body: `{"code": 400, "error": "Location 'PARIS-001' does not exist"}`.

## Decisions worth explaining

### Architecture

The warehouse module already used ports and adapters, and I kept to that because it has real
business rules. `WarehouseValidator` holds the location and capacity rules, and it is shared by
create and replace; replace passes the warehouse being replaced so it is not counted against the
location limits. Replace also reuses the archive operation instead of repeating its logic.
Product and Store are simple CRUD, so I left their structure mostly as it was. The fulfilment
module sits in between: a repository and a service, without its own domain model. Domain
exceptions know nothing about HTTP; `WarehouseExceptionMapper` turns them into the 400 and 404
responses declared in the spec.

### Warehouse id

`{id}` in the get and archive endpoints is the database id. The integration test that came with
the assignment archives `/warehouse/1`, which only works that way. The replace endpoint uses the
business unit code, as the spec defines.

### Business unit codes are not reused by create

Even after archiving, a code can only come back through a replacement. Otherwise two unrelated
warehouses would end up sharing one history, and in the bonus task their fulfilment assignments
as well.

### Status codes

Warehouse endpoints return 400 for rule violations because that is what the OpenAPI spec
declares; I would add 409 to the spec in a next iteration. Store and product keep the 422 they
already used for invalid input. The fulfilment endpoints are new, so there I used 409 for a
reached limit: the request itself is valid, it just conflicts with the current state.

### Duplicates are caught by the database

I don't check whether a name exists before saving, because two requests at the same time could
both pass that check. The unique constraint is the real guard, and the global mapper translates
its violation into a 409.

### 201 for creates

RESTEasy reads `@ResponseStatus` from the generated interface, which we cannot edit, so the
create endpoint returned 200. `ImplementationResponseStatusFilter` applies the annotation from
the implementing method instead.

## Testing

There are 108 tests plus 2 integration tests, and line coverage is around 98% (the build fails
below 80%; generated code is excluded).

- Unit tests cover every warehouse rule with a passing and a failing case, using Mockito instead
  of a database.
- `@QuarkusTest` tests run against a real PostgreSQL started by Dev Services, for the REST
  endpoints, the repository and the fulfilment limits. Some guarantees only exist in a real
  transaction, such as the legacy system not being called after a rollback.
- `WarehouseEndpointIT` runs against the packaged jar through the failsafe plugin.

## Known limitations

These are things I would handle before production, but left out on purpose to keep the scope of
the assignment:

- Location limits, fulfilment limits and the uniqueness of business unit codes are checked in
  code only. Two requests at the same moment, for example two replacements of the same
  warehouse, could both pass. A partial unique index on the code of active warehouses would close
  part of this, but it needs an explicit flush after archiving, because Hibernate executes
  inserts before updates and the new warehouse would otherwise collide with the one being
  archived. The limits themselves would need locking.
- Archiving a warehouse that still holds stock is allowed, because the provided integration test
  archives one with stock 10. In reality the stock would be moved first.
- Fulfilment assignments store the store and product ids without foreign keys, so deleting a
  store or product leaves its assignments behind. I would either block the delete or remove the
  assignments with it.
- PATCH on a store treats a stock of 0 as "not provided", because the field is a primitive `int`.
  Use PUT to set it to 0.
- The legacy call happens right after the commit, in the same request. A failure is logged, but
  not retried. For production I would use a transactional outbox with retries.
- The seed data in `import.sql` already breaks one rule: `MWH.001` has capacity 100 at
  `ZWOLLE-001`, which allows 40. The rules apply to new changes; I didn't change the provided data.
- `drop-and-create` and the database credentials in `application.properties` come from the
  original project. In production I would use Flyway migrations, and pass the credentials as
  environment variables (for example `QUARKUS_DATASOURCE_PASSWORD`).

Parts of the original code base are based on the
[Quarkus quickstarts](https://github.com/quarkusio/quarkus-quickstarts).
