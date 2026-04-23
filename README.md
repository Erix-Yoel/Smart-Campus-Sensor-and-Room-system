# JAX-RS REST Service Questions & Answers

## 1. LoggingFilter.java
**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?

**Answer:**
Using JAX-RS filters for cross-cutting concerns adheres to Aspect-Oriented Programming (AOP) principles and highly improves maintainability. If we manually insert `Logger.info()` inside every resource method, the codebase becomes polluted with boilerplate logging logic that distracts from the actual business logic of the endpoint. It also violates the DRY (Don't Repeat Yourself) principle. 
If the logging format needs to change, developers would have to update hundreds of methods manually. By using a central JAX-RS filter, the logging concern is isolated in one place. It intercepts all traffic globally, ensuring 100% logging coverage without modifying individual endpoint methods.

---

## 2. SensorRoomResource.java
**Question 1:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

**Answer:**
Returning only IDs significantly reduces the payload size, thus saving network bandwidth, especially when lists are large. However, it shifts the processing burden and network overhead to the client side—the client must perform an N+1 query pattern (making subsequent requests for every ID to fetch the full details), which increases latency drastically. 
Returning full room objects increases the initial response size but allows the client to render the UI immediately with all necessary data, preventing multiple round-trips. Often a hybrid approach (returning a summarized view or allowing the client to specify fields via query params) is structurally optimal.

**Question 2 (DELETE idempotency):** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:**
Yes, the normal REST deletion semantic states that an operation is idempotent if executing it N > 0 times yields the same structural system state. In my implementation, the first valid DELETE removes the Room from the DataStore. If the client sends the exact same DELETE request again, the DataStore simply won't find the room. It could either return a 404 Not Found or a 204 No Content. 
Either way, the internal state of the map doesn't change after the first successful deletion. Therefore, whether called once or ten times, the end state of that resource (it being absent from the system) is identical.

---

## 3. SensorResource.java
**Question 1:** We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

**Answer:**
By specifying `@Consumes(MediaType.APPLICATION_JSON)`, we instruct the JAX-RS runtime to only match incoming HTTP requests to this method if their 'Content-Type' header specifies 'application/json'. 
If a client sends data in a different format (like 'text/plain' or 'application/xml'), the JAX-RS container will fail to find a matching resource method for that specific Content-Type. As a technical consequence, the runtime will automatically abort request processing before hitting our method logic, and it will return an HTTP 415 Unsupported Media Type response back to the client. The data parsing phase doesn't even begin, protecting our code from trying to deserialize incompatible payloads.

**Question 2:** You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:**
The query parameter approach (`?type=CO2`) is superior for filtering because in REST semantics, the URL path should identify the core resource or absolute hierarchy (e.g., the collection of ALL sensors). Query parameters conceptually map to modifiers, modifiers that sort, filter, or paginate a collection without changing its core identity. 
If 'type' was part of the path, it would imply that "sensors of type CO2" is a fundamentally segregated sub-resource in the system's hierarchy. Moreover, query params allow combining multiple optional filters effortlessly (e.g., `?type=CO2&status=ACTIVE`), which is extremely cumbersome and rigid to model using static URL paths (e.g., `/sensors/type/CO2/status/ACTIVE`).

**Question 3:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?

**Answer:**
The Sub-Resource Locator pattern is highly beneficial for separation of concerns and modularity. Instead of cramming all sensor logic alongside reading logic in a single bloated class, the Sub-Resource Locator dynamically delegates traffic for a nested path (like `/readings`) to a dedicated Resource Class (`SensorReadingResource`). 
This keeps classes cohesive and specifically focused on one domain entity. It manages complexity by making the code easier to navigate, test, and maintain. In a large API, avoiding massive controller classes prevents merge conflicts and conceptual overload for developers, adhering strictly to the Single Responsibility Principle.

---

## 4. RestApplication.java
**Question:** In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

**Answer:**
The default lifecycle of a JAX-RS Resource class is request-scoped. This means that a new instance of the resource class is instantiated for *every* incoming HTTP request. 
Because a new instance is created on each request, state stored directly as instance variables within the resource class will not persist across different requests. 
Therefore, to maintain application state (like an in-memory database of rooms and sensors) across requests, we cannot rely on the resource instance's fields. Instead, we must manage state using external data structures that outlive the request, such as a Singleton pattern, static variables, or an injected singleton service. 
Furthermore, since multiple HTTP requests can execute concurrently, any shared in-memory data structure (like Maps or Lists) must be thread-safe (e.g., using `ConcurrentHashMap`, synchronized collections, or explicit locking) to prevent race conditions, data corruption, or data loss when concurrent modifications occur.

---

## 5. DiscoveryResource.java
**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

**Answer:**
Hypermedia as the Engine of Application State (HATEOAS) is the highest level of REST maturity (Richardson Maturity Model Level 3). It allows the API to guide the client dynamically by providing interactive links within the response payloads rather than relying entirely on out-of-band static documentation. 
This greatly benefits client developers because clients can discover available actions and endpoints on the fly based on the current state of the resource. It decouples the client from hardcoded URI structures; if the server rearranges its endpoints, the client won't break as long as it follows the relational links provided in the responses. It essentially makes the API self-descriptive and easier to evolve without introducing breaking changes.

---

## 6. GenericExceptionMapper.java
**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

**Answer:**
Exposing raw Java stack traces (HTTP 500 default pages) constitutes a severe Information Disclosure vulnerability. A stack trace reveals the internal architecture of the application. 
An attacker can gather exact class names, package structures, library versions (e.g., confirming the use of an outdated or vulnerable version of a JSON parser or database driver), and potentially database query structures, file paths, or hardcoded logic flaws. 
This significantly accelerates reconnaissance for an attacker, allowing them to craft highly targeted exploits (like deserialization attacks or SQL injections) based precisely on the identified internal libraries and frameworks.

---

## 7. LinkedResourceNotFoundExceptionMapper.java
**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:**
HTTP 404 Not Found specifically means that the Request URI itself does not point to an existing resource on the server. If the client POSTs to `/api/v1/sensors`, the URI is perfectly valid and found. 
The issue is that the JSON payload contains a reference (`roomId`) to an entity that doesn't exist. The JSON itself is well-formed (not a 400 Bad Request syntax error), but the server cannot process the instructions because of unprocessable semantic errors within the payload (a dangling foreign key). 
Therefore, 422 Unprocessable Entity accurately reflects that the server understands the content-type and syntax, but intrinsically cannot process the contained instructions.
