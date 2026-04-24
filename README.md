# JAX-RS REST Service: Architecture & Implementation Guide

Welcome to the documentation for the JAX-RS REST Service. This guide breaks down the core design decisions, RESTful semantics, and JAX-RS mechanics implemented in this project.


---

##  Architecture & Design Patterns

### Managing Cross-Cutting Concerns (Logging)

1. **Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?

* **Answer:** Using JAX-RS filters adheres to Aspect-Oriented Programming (AOP) principles and highly improves maintainability. If we manually insert `Logger.info()` inside every resource method, the codebase becomes polluted with boilerplate logic that distracts from the actual business logic. It also violates the DRY (Don't Repeat Yourself) principle. By using a central JAX-RS filter, the logging concern is isolated in one place. It intercepts all traffic globally, ensuring 100% logging coverage without modifying individual endpoint methods.

### Modularity via Sub-Resource Locators

2. **Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity?

* **Answer:** The Sub-Resource Locator pattern is highly beneficial for separation of concerns and modularity. Instead of cramming all logic into a single bloated class, the Locator dynamically delegates traffic for a nested path (like `/readings`) to a dedicated Resource Class (`SensorReadingResource`). This keeps classes cohesive and focused on one domain entity (Single Responsibility Principle). In a large API, avoiding massive controller classes prevents merge conflicts and conceptual overload for developers.

---

##  REST Principles & Routing

### Hypermedia and HATEOAS

3. **Question:** Why is the provision of "Hypermedia" (HATEOAS) considered a hallmark of advanced RESTful design? How does it benefit developers?

* **Answer:** Hypermedia as the Engine of Application State (HATEOAS) is the highest level of REST maturity (Richardson Maturity Model Level 3). It allows the API to guide the client dynamically by providing interactive links within response payloads. This decouples the client from hardcoded URI structures; if the server rearranges its endpoints, the client won't break as long as it follows the relational links provided. It makes the API self-descriptive and easier to evolve.

### DELETE Idempotency

4. **Question:** Is the DELETE operation idempotent in your implementation? What happens if a client sends the exact same DELETE request multiple times?

* **Answer:** Yes. REST deletion semantics state that an operation is idempotent if executing it *N > 0* times yields the same structural system state. In this implementation, the first valid DELETE removes the Room. If the exact same request is sent again, the DataStore simply won't find the room and will return either a `404 Not Found` or a `204 No Content`. Either way, the end state of that resource (being absent from the system) remains identical.

### Filtering: Query Parameters vs. URL Paths

5. **Question:** Contrast filtering via `@QueryParam` versus making the type part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach superior?

* **Answer:** In REST semantics, the URL path should identify the core resource or absolute hierarchy (the collection of *all* sensors). Query parameters conceptually map to modifiers that sort, filter, or paginate that collection without changing its core identity. If 'type' was part of the path, it would incorrectly imply that "sensors of type CO2" is a fundamentally segregated sub-resource. Query params also allow combining multiple optional filters effortlessly (e.g., `?type=CO2&status=ACTIVE`), which is rigid and cumbersome with static URL paths.

---

## Payload Management & Content Negotiation

### Payload Optimization

6. **Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning full room objects?

* **Answer:** * **Returning IDs only:** Significantly reduces payload size and saves bandwidth. However, it shifts processing burden to the client, forcing an N+1 query pattern (making subsequent requests for every ID) which drastically increases latency.
    * **Returning full objects:** Increases the initial response size but allows the client to render the UI immediately, preventing multiple round-trips. 
    * *Optimal approach:* A hybrid method, returning a summarized view or allowing the client to specify fields via query parameters.

### Content Negotiation and `@Consumes`

7. **Question:** What are the technical consequences if a client sends data in text/plain when the POST method explicitly uses `@Consumes(MediaType.APPLICATION_JSON)`?

* **Answer:** The `@Consumes` annotation instructs the JAX-RS runtime to only match incoming requests if their 'Content-Type' header specifies 'application/json'. If there is a mismatch, the container fails to find a matching resource method. The runtime automatically aborts processing before hitting the method logic and returns an `HTTP 415 Unsupported Media Type`. The data parsing phase never begins, protecting the code from deserializing incompatible payloads.

---

## ⚙️ Resource Lifecycle & Concurrency

### Managing State in JAX-RS

8. **Question:** What is the default lifecycle of a JAX-RS Resource class, and how does this impact the management of in-memory data structures?

* **Answer:** The default lifecycle is **request-scoped**, meaning a new instance is instantiated for *every* incoming HTTP request. Because of this, state stored as instance variables will not persist across requests. To maintain application state (like an in-memory database), we must use external structures that outlive the request, such as Singletons or static variables. Furthermore, because multiple requests execute concurrently, shared data structures (like Maps or Lists) must be thread-safe (e.g., `ConcurrentHashMap`) to prevent race conditions and data corruption.

---

## Error Handling & Security

### Cybersecurity and Stack Traces

9. **Question:** What are the risks associated with exposing internal Java stack traces to external API consumers?

* **Answer:** Exposing raw stack traces (HTTP 500 default pages) is a severe Information Disclosure vulnerability. It reveals internal architecture, class names, package structures, library versions, and potentially database query structures. This greatly accelerates reconnaissance for an attacker, allowing them to craft highly targeted exploits (like deserialization attacks) based on known vulnerabilities in those specific internal libraries.

### Semantic HTTP Status Codes: 422 vs 404

10. **Question:** Why is HTTP 422 more semantically accurate than a 404 when the issue is a missing reference inside a valid JSON payload?

* **Answer:** `404 Not Found` implies the Request URI itself does not exist. If a client POSTs to `/api/v1/sensors`, that URI is valid. If the JSON payload contains a reference (`roomId`) to an entity that doesn't exist, the JSON is syntactically well-formed, but the server cannot process the instructions due to semantic errors (a dangling foreign key). `422 Unprocessable Entity` accurately reflects that the server understands the content-type and syntax, but intrinsically cannot process the contained instructions.
