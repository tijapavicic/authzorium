Blaze vs JPA Pagination — Comparison

Summary
-------
This document compares Blaze-Persistence (Entity Views + CriteriaBuilder) pagination with plain Spring Data JPA pagination, focusing on real-world trade-offs: query shape, DTO mapping, N+1 behavior, performance, counting strategies, and when to pick one over the other.

Key concepts
------------
- JPA pagination (Spring Data): uses repository methods returning Page<T> and underlying JPA/Hibernate SQL with LIMIT/OFFSET. Mapping to DTOs is typically done post-query (MapStruct or manual mapping) or via JPQL constructor expressions.
- Blaze-Persistence: uses Entity Views (declarative DTO projections) together with CriteriaBuilderFactory to build queries that directly return view projections. Paging is supported by the Blaze CriteriaBuilder. Blaze can generate optimized joins only for requested fields.

1) Query generation and select shape
-----------------------------------
- JPA pageable (entity fetch + @EntityGraph / JOIN FETCH):
  - Typical repository call: Page<PetEntity> findByOwnerId(Long ownerId, Pageable pageable);
  - With @EntityGraph(attributePaths={"owner"}) the SQL will include a JOIN to fetch owner columns along with pet columns. Result: single query that returns pet rows with owner columns (good for N+1 avoidance).
  - If you instead fetch entities lazily then map to DTOs later by accessing owner properties, you risk N+1.

- Blaze pagination (EntityView + CriteriaBuilderFactory):
  - You define PetView and OwnerView and then request a paged result via cbf.create(em, PetView.class).
  - Blaze will generate a projection query including only the columns referenced by the view and appropriate joins.
  - Benefit: the selected columns and joins are tight and controlled by view shape; Blaze avoids unnecessary columns and joins.

2) DTO mapping & wiring
-----------------------
- JPA approach:
  - Query returns entity instances (or DTOs via constructor JPQL). Mapping to API DTOs is usually done via MapStruct or manual mapping. This mapping runs in JVM memory and typically uses already-loaded entity fields.

- Blaze approach:
  - The query returns view projections (interfaces/classes) and you map these to API DTOs. Often the view shape matches the API DTO closely, so mapping is thin or unnecessary.
  - Blaze can also return view objects that are already shaped like the API response.

3) N+1 select problem
---------------------
- JPA: N+1 arises when you load parent entities and then access lazy children inside loops. Mitigations:
  - Use fetch-join queries or @EntityGraph to load associations eagerly for that query.
  - Use batch fetching (hibernate.default_batch_fetch_size) or second-level cache.

- Blaze: Designed to avoid N+1 by generating a single projection query with needed joins. When using entity views properly, Blaze will not trigger per-entity lazy loads for projected attributes.

4) Pagination specifics and counts
----------------------------------
- Both approaches need to return the page content and total count.
- Typical pattern with OFFSET/LIMIT pagination:
  - Query 1: SELECT ... FROM pet JOIN owner ... WHERE ... LIMIT size OFFSET offset (returns content)
  - Query 2: SELECT COUNT(*) FROM pet WHERE ... (returns total)
  - Some providers (or Blaze helpers) can combine/optimize the count but generally a separate count query is executed.

- Blaze notes:
  - Blaze CriteriaBuilder supports applying setFirstResult and setMaxResults on the built criteria. It will execute the projection query for the page and you still need a count (either via repository.countBy... or via a separate Blaze count query).
  - Blaze gives more flexibility when you want an optimized count (for example, by removing joins that are not necessary for count).

5) Performance & memory
------------------------
- JPA entity-based pagination can be heavier in memory when loading full entities (especially with deep associations), unless you only select specific columns (JPQL constructor or projections).
- Blaze projections are lightweight: only requested fields are selected and populated, reducing object graph construction cost.
- For large result sets, Blaze can be more efficient because it avoids hydrating full entity graphs.

6) Consistency and correctness
-------------------------------
- OFFSET/LIMIT pagination can produce inconsistent paging when underlying data changes between requests. This is true for both JPA and Blaze.
- Consider using stable ordering (ORDER BY with unique key) or switch to cursor/seek pagination when needed.

7) Developer ergonomics
------------------------
- JPA (Spring Data) is straightforward and requires fewer dependencies. Repository methods, Pageable, and Page<T> are familiar to most Spring developers.
- Blaze introduces new concepts (EntityView, EntityViewManager, CriteriaBuilderFactory) and a learning curve, but pays off when you need many projections and tight SQL control.

8) Error modes & integration
----------------------------
- If Blaze integration artifacts (blaze-persistence-integration-hibernate5/hibernate6) are missing, the project will fail to create EntityViewManager/CriteriaBuilderFactory beans — you'll see messages like "No EntityManagerFactoryIntegrator was found". Use a conditional profile or classpath gating to enable Blaze only when the integration artifacts are present.
- In this repository we implemented a runtime-safe approach:
  - A reflection-based `BlazePetService` guarded by @ConditionalOnClass so the project compiles and runs without Blaze on the classpath.
  - Controller endpoint `GET /users/{userName}/pets-blaze` uses the Blaze service if available and falls back to the JPA pageable endpoint otherwise.

9) Practical examples (this project)
------------------------------------
- JPA pageable endpoint (already present):
  - GET /users/{userName}/pets?page=0&size=20
  - Implementation: `HelloServiceImpl.getPetsOfUser(userName, pageable)` which delegates to `PetRepository.findByOwnerId(ownerId, pageable)` with an @EntityGraph to fetch owner relationship.

- Blaze pageable endpoint:
  - GET /users/{userName}/pets-blaze?page=0&size=20
  - Implementation notes: uses `BlazePetService` which (when Blaze is present) creates a CriteriaBuilder for `PetView`, applies where owner.id = :ownerId, sets firstResult/maxResults, and maps the returned PetView objects to API Pet DTOs.
  - When Blaze is absent it falls back to the JPA pageable endpoint.

10) Recommendations
-------------------
- Use Spring Data JPA pageable + @EntityGraph if:
  - Your projection needs are simple, or
  - You want minimal dependencies and a familiar developer experience, or
  - You don't have many different DTO shapes to support.

- Use Blaze pagination (Entity Views) if:
  - You need many reusable, type-safe projections across the codebase, or
  - You need fine-grained control over select columns and joins to reduce data transfer and object hydration, or
  - You have complex nested projections that would otherwise cause N+1 or awkward JPQL.

11) Tests and validation
------------------------
- Integration test added: `PetPaginationIntegrationTest` asserts both endpoints return paged payloads and the expected total.
- To validate Blaze behavior specifically, enable Blaze integration in the POM, run tests with the `blaze-integration` profile (if present), and assert a single SQL statement is executed for the projection using Hibernate statistics or a query-counting proxy.

12) Quick checklist for enabling Blaze safely in this repo
---------------------------------------------------------
- Add the blaze-persistence integration artifact for the Hibernate version you run (match hibernate 5.6 dependency in pom). Use the `blaze-integration` profile already in the POM or enable with -Denable.blaze.integration=true.
- Ensure the Blazebit repository entry points to https://blazebit.com/repository/maven-public/ (this repo URL avoids a certificate subjectAltName mismatch encountered with repo.blazebit.com).
- Run `mvn -DskipTests=false -Denable.blaze.integration=true test` and inspect integration tests.

Concluding notes
----------------
Blaze offers powerful projection and pagination capabilities and generally produces more efficient SQL for non-trivial DTO shapes. For small projects the added complexity might not be worth it; for medium+ projects with many DTO shapes, Blaze can significantly simplify and optimize data access. In this repo we've added both options and safe fallbacks so you can enable Blaze incrementally.

