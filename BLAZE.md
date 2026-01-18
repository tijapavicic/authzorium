Blaze-Persistence Entity Views — Why we use it & why we wrote View classes
======================================================================

Elevator pitch
--------------
Blaze-Persistence Entity Views ("Blaze") let you define type-safe, reusable DTO projections over JPA entities. Instead of hand-writing queries that map entities to DTOs (and risk N+1, duplicated joins, and fragile field selection), you declare View interfaces or classes that describe exactly the shape of data you need. Blaze compiles those views into efficient queries with automatic join/fetch handling and (optionally) updatable mappings.

Use this doc as presentation slides and speaker notes for developers.

Slide 1 — Why projection matters
--------------------------------
- Entities model persistence; DTOs model data we send/receive in APIs or use in UI.
- Mapping logic sprinkled across repositories and services -> duplication and inconsistency.
- Naive fetch strategies cause N+1 queries, large unnecessary fetches, or overly complex SQL.

Speaker notes:
- Emphasize separation of concerns. We want to control what data is read and how it is fetched.

Slide 2 — What is Blaze-Persistence Entity Views?
-------------------------------------------------
- Library that integrates with JPA/Hibernate.
- Lets you declare views (interfaces or classes) annotated with @EntityView that map to entities.
- The EntityViewManager compiles those views into optimized queries (based on CriteriaBuilderFactory).
- Supports nested views, collections, mappings, and updatable views (for mapping back to entities).

Slide 3 — Key benefits (short bullets)
-------------------------------------
- Type-safe, compile-time binding of projections.
- Single place to describe the shape for a use-case (reusable across services/controllers).
- Efficient SQL generation: Blaze builds joins only for fields you request.
- Avoids N+1 and excessive SELECT * patterns.
- Supports updatable views for safer DTO->entity updates.
- Cleaner tests: instantiate entities, persist, then use evm.find to assert projections.

Slide 4 — Best Blaze feature (highlight)
--------------------------------------
- One-liner: Declarative, type-safe Entity Views that compile into efficient JPA/SQL queries with automatic join/fetch handling.

Why this matters (short):
- Solves the common DTO/projection problem in a single, reusable abstraction.
- Gives compile-time safety and IDE support for projection shapes.
- Produces optimized SQL: only selected fields and joins are included, preventing accidental N+1 and large payloads.

Quick example (reminder):
- Define a `PetView` mapped to `PetEntity` that exposes id, name and nested `OwnerView`.
- Read it with: `evm.find(em, PetView.class, petId)` or `cbf.create(em, PetView.class)...`

Demo notes:
- Show the view interface, then show the generated SQL for a list and for a detail call.
- Measure number of SQL statements and response size vs a naive entity-returning approach.

Slide 5 — What is the N+1 problem? (explanation)
---------------------------------------------
- Short definition: N+1 is a performance anti-pattern where loading a collection of N parent entities triggers 1 query for the parents plus N additional queries for their related children (or related data), resulting in N+1 total queries instead of a single join or efficient batch fetch.

Why it happens:
- ORMs (like JPA/Hibernate) often default to lazy-loading associations.
- Accessing an association inside a loop causes the ORM to issue a new query per parent to fetch its children.
- Example (pseudo-Java/JPA):
  - `List<User> users = em.createQuery("select u from User u").getResultList();`
  - `for (User u : users) { u.getPets().size(); }` // each `getPets()` may trigger a separate query

SQL view (naive vs efficient):
- Naive (N+1):
  - Query 1: SELECT * FROM user WHERE ...; // returns N rows
  - Query 2..N+1: SELECT * FROM pet WHERE owner_id = ?; // executed N times
- Efficient: single joined query (or controlled batched fetch):
  - SELECT u.id, u.name, p.id, p.name FROM user u LEFT JOIN pet p ON p.owner_id = u.id WHERE ...;

How Blaze helps:
- Blaze entity views are projection-based: when you request a view that includes a nested view or collection, Blaze generates a single SQL statement with the appropriate joins to fetch the requested shape.
- No per-parent lazy loading calls are needed at render time, so the N+1 pattern is avoided by construction for most projection shapes.

How to detect it in a demo or CI:
- Enable SQL logging (hibernate.show_sql or datasource proxy) and count queries for a single request.
- Compare naive endpoint vs Blaze-projected endpoint: show number of SQL statements and timings.

Speaker notes:
- Use a dataset with many parents (e.g., 50 users each with several pets) to make N+1 visible.
- Show before/after SQL and emphasize fewer round trips and less data transfer with Blaze.

Slide 6 — Why we wrote View classes in this project
---------------------------------------------------
- Clear API contract: the view defines what the rest of the app can read (reduces accidental data leaks).
- Performance: we want exact control of joins and selected columns for list/detail endpoints.
- Reusability: same view used by controllers, services, and tests (see PetViewIntegrationTest).
- Maintainability: changing the view updates all call sites at once, avoiding scattered mapping code.

Slide 7 — Example (presentation-friendly)
-----------------------------------------
Java (example view interfaces):

```java
// PetView.java
@EntityView(PetEntity.class)
public interface PetView {
    @IdMapping
    Long getId();

    String getPetName();

    @Mapping("owner")
    OwnerView getOwner();
}

// OwnerView.java
@EntityView(UserEntity.class)
public interface OwnerView {
    @IdMapping
    Long getId();

    String getUsername();
    String getDisplayName();
}
```

Usage (in code/tests):

```java
PetView view = evm.find(em, PetView.class, petId);
// or via criteria builder: cbf.create(em, PetView.class).where("id").eq(petId).getSingleResult();
```

Speaker notes:
- Show how test `PetViewIntegrationTest` persists entities then reads back the view with `evm.find`.
- Highlight that only requested associations are joined.

Slide 8 — Performance & correctness patterns
-------------------------------------------
- For lists, prefer views that include only id and fields needed for presentation.
- For details, include nested views for related entities.
- Use pagination with Blaze views; Blaze will still generate efficient SQL.
- Use updatable views when you want to map a REST DTO directly to an entity in a controlled way.

Slide 9 — When NOT to use Blaze
-------------------------------
- Small projects with trivial domain where hand-written queries are simpler.
- Extremely dynamic ad-hoc queries that change fields completely run-time (Blaze is best for well-defined shapes).
- When learning curve/time-to-market for a team outweighs benefits.

Slide 10 — Common objections & responses
---------------------------------------
- "Another abstraction over JPA?"
  - Response: it replaces brittle mapping code with a declarative, type-safe mapping and reduces duplication.
- "Won’t this add complexity?"
  - Response: initial learning cost exists but daily development becomes faster and safer.
- "How does it play with Hibernate caching/transactions?"
  - Response: Blaze builds JPA queries; caching and transactions behave like normal JPA queries.

Slide 11 — Practical tips (project-specific)
-------------------------------------------
- Centralize view definitions in `src/main/java/.../view` (easier discoverability).
- Keep list and detail views separate (list views return minimal fields).
- Write integration tests like `PetViewIntegrationTest` to assert the mapping in a real persistence context.
- Use `CriteriaBuilderFactory` + `EntityViewManager` for complex queries and pagination.

Slide 12 — Quick Q&A you can use in the presentation
----------------------------------------------------
Q: Can a view contain computed fields?
A: Yes — you can use mapping expressions and constructor mappings when needed.

Q: Is it safe to expose nested entities in views?
A: Use nested view interfaces to limit the shape. Don't expose raw entities.

Q: How do we validate updates?
A: Use updatable entity views with explicit setters or map DTOs and validate them before applying.

Closing slide — demo idea
-------------------------
- Live demo: Persist a User and Pet (like `PetViewIntegrationTest`), then show reading a PetView with only the fields you want.
- Show generated SQL (Blaze logs the SQL) to demonstrate no N+1 and minimal joins.

Appendix — Notes for maintainers
--------------------------------
- Add `EntityViewManager` and `CriteriaBuilderFactory` beans via your Spring Boot configuration (we already have them in the project).
- Keep view interfaces small and focused per-use-case.
- Document views with Javadoc so teams know which view to use where.


End of BLAZE.md
