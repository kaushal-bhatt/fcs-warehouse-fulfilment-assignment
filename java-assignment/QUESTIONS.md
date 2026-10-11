# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would, but I wouldn't push everything into the same shape either.

Right now there are three different styles. Store uses Active Record (Store.findById(),
store.persist()), Product uses a Panache repository, and Warehouse goes all the way with a
domain model, a WarehouseStore port and a separate DbWarehouse entity behind it.

The one I would change first is Store. While working on Task 2 I noticed how much ends up
inside StoreResource: persistence, validation and the legacy integration all live in the REST
class, and because the data access is static every StoreResource test needs a running Quarkus
application and database. Moving it to a repository with a small service on top would give the
transaction and the after-commit legacy sync one clear home, and make it much easier to test.

I would not turn Product and Store into full hexagonal modules though. Warehouse deserves that
structure because it has real rules: capacity, location limits, replacement. Thanks to that I
could test all of them with plain unit tests and reuse the same validator for create and replace.
Product and Store are simple CRUD, and adding a domain model there would mostly add mapping code.

Two more things I'd fix along the way. First, I'd stop returning JPA entities directly from the
API, because today the table and the contract are tied together and every endpoint has to guard
fields like the id by hand.
Second, I'd replace drop-and-create plus import.sql with Flyway migrations, and let the database
protect important rules as well, for example a unique index on the business unit code of active
warehouses. At the moment the location limits are only checked in code, so two requests arriving
at the same time could both get through.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
I like spec-first, but this assignment also showed me where it hurts.

The big advantage is that the contract comes first. Other teams can review it, generate clients
or mocks and start working before the backend is ready, and the documentation can't drift
because the server interface is generated from the same file.

The downside is that you have to live with what the generator gives you. A few things I ran into:
- The spec says create returns 201, but the generated interface owns the annotations, so my
  @ResponseStatus on the implementation was simply ignored and the endpoint returned 200. I had
  to add a small response filter to make it behave.
- The generated methods return the bean instead of a Response, so I can't set things like a
  Location header.
- @NotNull is generated but does nothing, because the validation extension isn't on the classpath.
- The spec itself has gaps: no error response format, no 409, and the id is a string while the
  database uses a number.

Code-first, as in Product and Store, is quicker and you control everything. But then the API is
just whatever the code happens to do. The PATCH bug in Store, where sending only a name reset
the stock to 0, is the kind of thing a reviewed contract would probably have caught.

So my choice is spec-first for anything other teams or external clients depend on, but with
more care for the spec itself (error format, all status codes, proper id types) and a contract
test that checks the real responses against it. For small internal CRUD endpoints I'm fine with
code-first, as long as the OpenAPI document is still generated from the code, so every API ends
up documented the same way.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would spend the testing effort where a mistake costs the most: the business rules and data
integrity first, and simple getters or plain CRUD last.

Most of my tests are unit tests on the warehouse use cases. Every rule has a test where it passes
and a test where it fails, with the exact error message checked. They use mocks instead of a
database, so they run in milliseconds and I can run them constantly while coding.

Next come integration tests against a real PostgreSQL started by Quarkus Dev Services, not an
in-memory database. Some things can only be proven with a real transaction. The best example is
Task 2: to show that the legacy system is not called when the commit fails, I create a store with
a duplicate name, let the unique constraint fail and check that the legacy call never happened.

On top of that I have REST tests with RestAssured that check status codes and error bodies, since
that's what clients actually see, and a small end-to-end test that runs against the packaged jar
during "mvn verify".

To keep this useful over time:
- The build fails below 80% line coverage, and CI runs it on every push. I treat that as a
  minimum, not a target. In reviews I care more about whether the failure paths are tested.
- Every bug fix gets a test that would have caught it, like the PATCH stock reset.
- Tests must not depend on each other or on the order they run in. I actually hit this here,
  because tests shared database rows, and fixed it with unique test data and rolled-back
  transactions. A flaky test gets fixed, not retried.
- Now and then I'd run mutation testing (PIT) on the domain code, to find tests that run the
  code but don't really check anything.
```
