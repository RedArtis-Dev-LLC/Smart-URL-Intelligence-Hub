---
name: write-test
description: Write unit or integration tests following this project's conventions, patterns, and context-preservation rules
argument-hint: [class-or-feature-to-test]
allowed-tools: Read Grep Glob Bash(./gradlew *) Edit Write
---

You are writing tests for a Spring Boot 4.x / Java 25 project. Follow the rules and patterns below exactly.

If the user provides a class or feature name as `$ARGUMENTS`, first read the source file to understand what needs testing, then determine whether it needs an integration test, a unit test, or both.

Before writing any test, read `AbstractIntegrationTest.java` and at least one existing test in the same service to confirm current conventions.

---

# Rules

## Context preservation (CRITICAL)

- **Every integration test MUST extend `AbstractIntegrationTest`** -- no exceptions. The shared annotation set (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Import(TestContainersConfig.class)`, `@AutoConfigureTestRestTemplate`) is what keeps all tests on a single context.
- **NEVER add annotations to IT subclasses that change the context cache key:**
  - No `@SpringBootTest` or `@SpringBootTest(properties = ...)` on subclasses
  - No `@DirtiesContext`
  - No `@TestPropertySource`
  - No `@MockitoBean` / `@MockBean` on subclasses (only the ones declared in the base class)
  - No `@Import` or `@ContextConfiguration` on subclasses
- **Isolation is achieved through cleanup, not restart.** The base class `@BeforeEach` wipes all DB repos, Redis, and any other stateful resources. If the test needs extra cleanup, add it to a local `@BeforeEach` -- never dirty the context.

## Test infrastructure setup

- **Testcontainers live in a `@TestConfiguration` class** (`TestContainersConfig`), imported via `@Import` in `AbstractIntegrationTest`. Never use inheritance for container setup.
- **Use `@ServiceConnection`** for containers that Spring Boot auto-configures (PostgreSQL, RabbitMQ, MongoDB).
- **Use `DynamicPropertyRegistrar` beans** for containers or properties that need manual wiring (Redis with password, RSA keys, custom properties).
- **Config Server is disabled in tests** via `@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "spring.config.import="})`.

## Common dependencies in base class

- `AbstractIntegrationTest` declares `@Autowired protected` fields for beans used by most tests: `TestRestTemplate`, repositories, `StringRedisTemplate`.
- The base class `@BeforeEach` handles cleanup (delete all repos, flush Redis).
- Subclasses only inject additional beans specific to their scenario (e.g., `RSAPublicKey`).

## Naming conventions

- Integration tests: `*IT.java` -- one class per endpoint or feature
- Unit tests: `*Test.java` in the module under test
- API utilities: `*ApiUtils.java` in a `utils/` subpackage -- nested `OK` / `Error` classes
- Test container config: `TestContainersConfig.java` -- `@TestConfiguration` class

## Structure

- **One IT class per endpoint.** Each covers all edge cases (happy path, validation failures, error scenarios) for a single endpoint. No multi-endpoint flow tests.
- Integration tests use `//given`, `//when`, `//then` comment blocks
- Unit tests use `// Given`, `// When`, `// Then` comment blocks (capitalized)
- Unit tests group related scenarios with `@Nested` + `@DisplayName`
- Use `var` for local variables
- Use Java DTOs for request bodies -- never raw JSON strings
- Use `HttpStatus` constants for status code assertions -- never magic numbers

## What to mock vs. what to hit real

- **Real:** PostgreSQL, Redis, RabbitMQ (via Testcontainers), Liquibase migrations, Spring context
- **Mocked via WireMock:** All external HTTP providers (Feign clients, third-party APIs)
- **Unit tests mock everything** via `@Mock` + `@InjectMocks`

## HTTP client in tests

- **Use `TestRestTemplate`** for integration tests -- it's auto-configured with `@AutoConfigureTestRestTemplate`, resolves the random port automatically, and never throws on 4xx/5xx responses.
- Never use `RestClient` in tests -- it throws on error status codes by default and requires manual base URL setup.

---

# Patterns

## Pattern 1: TestContainersConfig -- @TestConfiguration with @ServiceConnection

```java
@TestConfiguration
public class TestContainersConfig {

    private static final String REDIS_PASSWORD = "test-redis-pass";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.2"))
                .withDatabaseName("auth_db")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    GenericContainer<?> redisContainer() {
        var container = new GenericContainer<>(DockerImageName.parse("redis:8.0.1-alpine"))
                .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());
        container.start();
        return container;
    }

    @Bean
    DynamicPropertyRegistrar redisProperties(GenericContainer<?> redisContainer) {
        return registry -> {
            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
            registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
        };
    }
}
```

Key points:
- `@ServiceConnection` for containers with built-in Spring Boot support (Postgres, RabbitMQ, MongoDB)
- `DynamicPropertyRegistrar` beans for custom property wiring (Redis with password, file paths, external URLs)
- Container beans are singletons -- started once, shared across all tests

## Pattern 2: AbstractIntegrationTest -- shared base class

```java
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import="
        }
)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected StringRedisTemplate redis;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
}
```

Key points:
- All annotations that define the context cache key live here -- never on subclasses
- Common beans as `protected` fields -- subclasses use them directly
- `@BeforeEach` cleanup ensures test isolation without restarting the context
- `@AutoConfigureTestRestTemplate` provides `TestRestTemplate` auto-configured with the random port

## Pattern 3: API test utility -- OK/Error nested classes with DTOs

```java
@UtilityClass
public class AuthApiUtils {

    public static final String REGISTER_URL = "/auth/register";
    public static final String LOGIN_URL = "/auth/login";

    @UtilityClass
    public static final class OK {

        public static AuthResponse register(RegisterRequest request, TestRestTemplate restTemplate) {
            var response = restTemplate.postForEntity(REGISTER_URL, request, AuthResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            return response.getBody();
        }

        public static AuthResponse login(LoginRequest request, TestRestTemplate restTemplate) {
            var response = restTemplate.postForEntity(LOGIN_URL, request, AuthResponse.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            return response.getBody();
        }

        public static void logout(String accessToken, TestRestTemplate restTemplate) {
            var headers = bearerHeaders(accessToken);
            var response = restTemplate.exchange(
                    LOGOUT_URL, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @UtilityClass
    public static final class Error {

        public static ResponseEntity<ProblemDetail> register(RegisterRequest request, TestRestTemplate restTemplate) {
            return restTemplate.postForEntity(REGISTER_URL, request, ProblemDetail.class);
        }

        public static ResponseEntity<ProblemDetail> login(LoginRequest request, TestRestTemplate restTemplate) {
            return restTemplate.postForEntity(LOGIN_URL, request, ProblemDetail.class);
        }
    }

    private static HttpHeaders bearerHeaders(String accessToken) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

Key points:
- URL paths as constants -- never hardcoded strings in tests
- `OK` methods assert success status and return unwrapped domain DTOs
- `Error` methods return raw `ResponseEntity<ProblemDetail>` so the test asserts status and error details
- Request bodies are Java DTOs -- never raw JSON strings
- Status assertions use `HttpStatus` constants -- never magic numbers
- Bearer token handling extracted to a private helper

## Pattern 4: WireMock mock utility

One `@UtilityClass` per external provider. Default-200 method plus an overloaded variant with `Consumer` for customization.

```java
@UtilityClass
public class GeoApiMocks {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String GEO_LOOKUP_URL = "/json/.*";

    public static void mockGeoLookup_200(WireMockServer wireMockServer) {
        mockGeoLookup_200(wireMockServer, builder -> {});
    }

    @SneakyThrows
    public static void mockGeoLookup_200(WireMockServer wireMockServer,
            Consumer<GeoResponse.GeoResponseBuilder> modifier) {
        var builder = GeoResponse.builder()
                .status("success")
                .country("Germany")
                .city("Berlin")
                .lat(52.52)
                .lon(13.405);
        modifier.accept(builder);

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathMatching(GEO_LOOKUP_URL))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(OBJECT_MAPPER.writeValueAsBytes(builder.build()))));
    }

    public static void mockGeoLookup_500(WireMockServer wireMockServer) {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathMatching(GEO_LOOKUP_URL))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }
}
```

Key points:
- Always provide both a no-arg convenience method and a `Consumer<Builder>` variant
- Use `wireMockServer.stubFor` for all stubs
- Response JSON: inline (`ObjectMapper.writeValueAsBytes`) for dynamic content, file-based (`withBodyFile`) for static fixtures
- One mock class per external provider -- keeps stubs organized and reusable
- Error methods (500, 404, timeout) for testing resilience scenarios

## Pattern 5: Integration test -- one class per endpoint

```java
class RegisterIT extends AbstractIntegrationTest {

    @Test
    void register_happyPath_returnsTokens() {
        //given
        var request = new RegisterRequest("alice@example.com", "hunter22pass");

        //when
        var result = AuthApiUtils.OK.register(request, restTemplate);

        //then
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void register_duplicateEmail_returns409() {
        //given
        AuthApiUtils.OK.register(new RegisterRequest("carol@example.com", "hunter22pass"), restTemplate);

        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("carol@example.com", "hunter22pass"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
    }

    @Test
    void register_weakPassword_returns400() {
        //when
        var response = AuthApiUtils.Error.register(
                new RegisterRequest("bob@example.com", "short"), restTemplate);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }
}
```

Key points:
- Extends `AbstractIntegrationTest` -- no additional annotations
- No `@BeforeEach` needed -- base class handles cleanup
- Only `@Autowired` fields specific to this test class (e.g., `RSAPublicKey`)
- Each test is self-contained -- uses `AuthApiUtils.OK.*` to set up preconditions
- Error tests use `AuthApiUtils.Error.*` and assert against `HttpStatus` constants

## Pattern 6: Unit test -- Nested classes with @DisplayName

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService Unit Tests")
class ConfigServiceTest {

    @Mock private ConfigRepository repository;
    @Mock private ExternalApiPort externalApiPort;
    @InjectMocks private ConfigService service;

    private static final Long PROJECT_ID = 123L;

    @Nested
    @DisplayName("getByProjectId() tests")
    class GetByProjectIdTests {

        @Test
        @DisplayName("Should return default config when project not exists in database")
        void shouldReturnDefaultConfig_WhenProjectNotExistsInDatabase() {
            // Given
            when(repository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            // When
            var result = service.getByProjectId(PROJECT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isEqualTo(PROJECT_ID);
        }
    }
}
```

Key points:
- One `@Nested` class per method-under-test
- `@DisplayName` on both the nested class and each test
- AssertJ (`assertThat`) for fluent assertions, Mockito `verify` for interaction checks
- `// Given`, `// When`, `// Then` capitalized (distinguishes from IT tests)

---

# Checklist: Writing New Tests

## Integration test

1. Extend `AbstractIntegrationTest`. No extra class-level annotations.
2. Name it `<Feature>IT.java` -- one class per endpoint or feature.
3. Check if API utilities already exist in `utils/`. Create new ones following Pattern 3 if needed.
4. Use DTOs for request bodies, `HttpStatus` constants for assertions.
5. Cover at least: one happy-path test, one validation-failure test (400), one business-error test (401/403/409).
6. Verify context safety -- confirm no annotations that would split the Spring context.

## Unit test

1. Create in the same module as the code under test. Name it `<Class>Test.java`.
2. Annotate with `@ExtendWith(MockitoExtension.class)` and `@DisplayName`.
3. Declare dependencies as `@Mock` fields. Inject with `@InjectMocks`.
4. Group with `@Nested` -- one nested class per public method being tested.
5. Use `// Given`, `// When`, `// Then` (capitalized) comment blocks.
6. Prefer AssertJ (`assertThat`) for assertions. Use `verify()` for interaction assertions.

## Creating new utilities

- **API utility:** `<Feature>ApiUtils.java` in `utils/` -- `@UtilityClass`, URL constants, nested `OK`/`Error` classes, DTOs for bodies, `HttpStatus` for assertions
- **Mock utility:** `<Provider>Mocks.java` in `mocks/` -- `@UtilityClass`, URL constants, default-200 + Consumer variant + error methods per endpoint
- **Test container config:** Add new container beans to `TestContainersConfig` -- use `@ServiceConnection` when possible, `DynamicPropertyRegistrar` otherwise

---

# Anti-Patterns -- Never Do These

## Adding annotations on an IT subclass that change context

```java
// WRONG -- creates a new Spring context
@MockitoBean private SomeService someService;
@SpringBootTest(properties = {"app.some-flag=true"})
@DirtiesContext
@TestPropertySource(...)
```

Instead: stub via WireMock, or configure behavior via existing mocked beans in the base class.

## Using raw JSON strings for request bodies

```java
// WRONG
var response = restTemplate.postForEntity("/auth/register",
        """
        {"email":"bob@example.com","password":"short"}
        """, ProblemDetail.class);
```

Instead: `restTemplate.postForEntity(REGISTER_URL, new RegisterRequest("bob@example.com", "short"), ProblemDetail.class);`

## Using magic numbers for status codes

```java
// WRONG
assertThat(response.getStatusCode().value()).isEqualTo(401);
```

Instead: `assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);`

## Using RestClient instead of TestRestTemplate

```java
// WRONG -- throws on errors, needs manual base URL, needs onStatus suppression
var client = RestClient.builder().baseUrl("http://localhost:" + port).build();
var response = client.post().uri("/auth/register").body(request)
        .retrieve().onStatus(HttpStatusCode::isError, (req, res) -> {}).toEntity(String.class);
```

Instead: `restTemplate.postForEntity(REGISTER_URL, request, AuthResponse.class);`

## Multi-endpoint flow tests

```java
// WRONG -- testing multiple endpoints in one test
@Test
void fullFlow_register_login_refresh_me_logout() { ... }
```

Instead: one IT class per endpoint, each test covers a single scenario.

## Hardcoding URL paths in tests

```java
// WRONG
var response = restTemplate.postForEntity("/auth/register", request, AuthResponse.class);
```

Instead: `var response = restTemplate.postForEntity(AuthApiUtils.REGISTER_URL, request, AuthResponse.class);`

## Duplicating common beans in every IT subclass

```java
// WRONG -- repeated in every test class
@Autowired TestRestTemplate restTemplate;
@Autowired UserRepository userRepository;
@BeforeEach void setUp() { userRepository.deleteAll(); }
```

Instead: declare once in `AbstractIntegrationTest` as `protected` fields with shared `@BeforeEach`.
