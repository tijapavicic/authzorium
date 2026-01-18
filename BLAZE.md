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

Slide 4 — Why we wrote View classes in this project
---------------------------------------------------
- Clear API contract: the view defines what the rest of the app can read (reduces accidental data leaks).
- Performance: we want exact control of joins and selected columns for list/detail endpoints.
- Reusability: same view used by controllers, services, and tests (see PetViewIntegrationTest).
- Maintainability: changing the view updates all call sites at once, avoiding scattered mapping code.

Slide 5 — Example (presentation-friendly)
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

Slide 6 — Performance & correctness patterns
-------------------------------------------
- For lists, prefer views that include only id and fields needed for presentation.
- For details, include nested views for related entities.
- Use pagination with Blaze views; Blaze will still generate efficient SQL.
- Use updatable views when you want to map a REST DTO directly to an entity in a controlled way.

Slide 7 — When NOT to use Blaze
-------------------------------
- Small projects with trivial domain where hand-written queries are simpler.
- Extremely dynamic ad-hoc queries that change fields completely run-time (Blaze is best for well-defined shapes).
- When learning curve/time-to-market for a team outweighs benefits.

Slide 8 — Common objections & responses
---------------------------------------
- "Another abstraction over JPA?"
  - Response: it replaces brittle mapping code with a declarative, type-safe mapping and reduces duplication.
- "Won’t this add complexity?"
  - Response: initial learning cost exists but daily development becomes faster and safer.
- "How does it play with Hibernate caching/transactions?"
  - Response: Blaze builds JPA queries; caching and transactions behave like normal JPA queries.

Slide 9 — Practical tips (project-specific)
-------------------------------------------
- Centralize view definitions in `src/main/java/.../view` (easier discoverability).
- Keep list and detail views separate (list views return minimal fields).
- Write integration tests like `PetViewIntegrationTest` to assert the mapping in a real persistence context.
- Use `CriteriaBuilderFactory` + `EntityViewManager` for complex queries and pagination.

Slide 10 — Quick Q&A you can use in the presentation
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
