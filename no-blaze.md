no-blaze: Fetch all pets for a user (pseudocode and guidance)

Goal
----
Provide a clear, no-Blaze (plain Spring Data JPA) design and pseudocode for an endpoint that returns all pets for a given user. The design should avoid the N+1 select problem.

Contract
--------
- Input: userName (String)
- Output: List<Pet DTO> (each Pet includes id, petName, owner { id, username, displayName })
- Error modes: user not found -> 404 / empty list; DB errors -> 500
- Success criteria: single query (or small fixed number) to fetch pets for the user; no N+1 on owner mapping

Design summary
--------------
- Use Spring Data JPA repositories only.
- Resolve the user id by username, then load pets for that owner id using a repository query that fetches the owner relationship eagerly (JOIN FETCH or @EntityGraph) to avoid N+1.
- Map JPA entities to DTOs via MapStruct (existing mappers) or manual mapping.
- Expose a controller endpoint: GET /users/{userName}/pets

Repository approaches (pick one)
--------------------------------
1) Use a repository method with JOIN FETCH to eagerly fetch owner:

```java
    public interface PetRepository extends JpaRepository<PetEntity, Long> {
        @Query("SELECT p FROM PetEntity p JOIN FETCH p.owner WHERE p.owner.id = :ownerId")
        List<PetEntity> findByOwnerIdWithOwner(@Param("ownerId") Long ownerId);
    }
```

2) Use @EntityGraph to ask JPA to fetch owner in one query:

```bash
 @EntityGraph(attributePaths = {"owner"})
    List<PetEntity> findByOwnerId(Long ownerId);
```

Either approach results in a single SQL query that fetches pets and their owners (prevents N+1).

Service layer pseudocode
------------------------

Interface (already present)

    List<Pet> getPetsOfUser(String userName);

Implementation (pseudocode)

```java
    public List<Pet> getPetsOfUser(String userName) {
        // 1 - find user id
        UserEntity user = userRepository.findByUsername(userName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Long userId = user.getId();

        // 2 - fetch pets with owner eagerly to avoid N+1
        List<PetEntity> pets = petRepository.findByOwnerIdWithOwner(userId);

        // 3 - map entities to DTOs using existing PetMapper
        return pets.stream().map(petMapper::toDto).collect(Collectors.toList());
    }
```

Controller pseudocode
---------------------

```java

    @GetMapping("/users/{userName}/pets")
    public ResponseEntity<List<Pet>> getUserPets(@PathVariable String userName) {
        List<Pet> pets = helloService.getPetsOfUser(userName);
        return ResponseEntity.ok(pets);
    }
```

Mapping considerations
----------------------
- PetMapper should map owner -> User DTO; mapping will read owner fields that are already loaded because of the JOIN FETCH / @EntityGraph.
- Ensure PetEntity.owner is mapped lazily (recommended). We rely on a fetch-join at query time to eagerly load owner for this specific query.

Tests to demonstrate no N+1
---------------------------
Option A: Integration test with Hibernate statistics enabled
```java

 @SpringBootTest
    @AutoConfigureTestDatabase
    public class PetIntegrationTest {

        @Autowired
        private EntityManagerFactory emf;

        @Autowired
        private HelloService helloService;

        @BeforeEach
        void setup() {
            // insert one user and multiple pets (2-3) assigned to that user
        }

        @Test
        void fetchPetsShouldNotTriggerNPlusOne() {
            // enable statistics
            SessionFactoryImplementor sfi = emf.unwrap(SessionFactoryImplementor.class);
            Statistics stats = sfi.getStatistics();
            stats.setStatisticsEnabled(true);
            stats.clear();

            List<Pet> pets = helloService.getPetsOfUser("test-user");

            // assert we got the pets
            assertThat(pets).hasSize(3);

            // check SQL execution count: expecting 1 select for pets(+owner) and maybe 0 additional selects
            // depending on second-level cache / foreign key access, expecting <= 2
            long queryCount = stats.getPrepareStatementCount();
            assertThat(queryCount).isLessThanOrEqualTo(2);

            stats.setStatisticsEnabled(false);
        }
    }
 ```
   

Option B: Use datasource-proxy (QueryCountHolder) to count executed queries in a test - assert only 1 query executed for the fetch operation.

Notes about the assertion: exact query count depends on JPA provider and configuration; the point of the test is to assert there is no per-pet additional select (N+1) beyond the initial fetch.

Example SQL produced by JOIN FETCH query
--------------------------------------
SELECT p.id, p.pet_name, p.owner_id, o.id, o.username, o.display_name
FROM pet p
JOIN app_user o ON p.owner_id = o.id
WHERE o.id = ?

This returns pet rows with owner columns in the same result set (single round-trip).

Edge cases and error handling
-----------------------------
- userName not found -> throw ResponseStatusException(HttpStatus.NOT_FOUND)
- owner has zero pets -> return empty list [] with 200 OK
- large number of pets -> consider pagination: add pageable parameter to repository and controller (Pageable) and return Page<PetDTO>

Pagination sketch
-----------------
- Controller: GET /users/{userName}/pets?page=0&size=50
- Service: use petRepository.findByOwnerId(ownerId, pageable) with @EntityGraph where supported

Summary checklist (implementation steps)
---------------------------------------
- [ ] Add repository method with JOIN FETCH or @EntityGraph
- [ ] Ensure service uses that repository method (resolve user id first)
- [ ] Ensure PetMapper maps owner -> User DTO
- [ ] Add integration test enabling Hibernate Statistics or QueryCountHolder asserting no N+1
- [ ] (Optional) Add pagination support

Example quick curl
------------------
GET /users/test-user/pets

Response (200):

[
  {"id":1, "petName":"Fido", "owner":{"id":1, "username":"test-user", "displayName":"Test User"}},
  {"id":2, "petName":"Mittens", "owner":{"id":1, "username":"test-user", "displayName":"Test User"}}
]

Appendix: repository code examples
---------------------------------
Using JOIN FETCH:

```java
    @Repository
    public interface PetRepository extends JpaRepository<PetEntity, Long> {
        @Query("SELECT p FROM PetEntity p JOIN FETCH p.owner WHERE p.owner.id = :ownerId")
        List<PetEntity> findByOwnerIdWithOwner(@Param("ownerId") Long ownerId);
    }
```

Using @EntityGraph (recommended for readability):

```java
@Repository
    public interface PetRepository extends JpaRepository<PetEntity, Long> {
        @EntityGraph(attributePaths = {"owner"})
        List<PetEntity> findByOwnerId(Long ownerId);
    }
```
