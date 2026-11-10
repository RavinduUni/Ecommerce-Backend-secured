# 🛒 Ecommerce Backend — Spring Boot + JWT Security

A production-style REST API for an e-commerce platform, secured with stateless JWT authentication and role-based access control. Built with Spring Boot 4, Spring Security, Spring Data JPA, MySQL, MapStruct, and JJWT.

---

## 📑 Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Dependencies](#2-tech-stack--dependencies)
3. [Project Structure](#3-project-structure)
4. [Domain Model & Database Schema](#4-domain-model--database-schema)
5. [The Big Picture — What Happens to Every Request?](#5-the-big-picture--what-happens-to-every-request)
6. [Layer by Layer — The Spring Security Filter Chain](#6-layer-by-layer--the-spring-security-filter-chain)
7. [Role-Based Access Control (RBAC)](#7-role-based-access-control-rbac)
8. [Detailed Flow: Registering a User (POST /api/users/register)](#8-detailed-flow-registering-a-user-post-apiusersregister)
9. [Detailed Flow: Logging In (POST /api/users/login)](#9-detailed-flow-logging-in-post-apiuserslogin)
10. [Detailed Flow: Accessing a Protected Endpoint](#10-detailed-flow-accessing-a-protected-endpoint)
11. [Detailed Flow: Placing an Order (POST /api/order/)](#11-detailed-flow-placing-an-order-post-apiorder)
12. [Class-by-Class Breakdown](#12-class-by-class-breakdown)
13. [Method-by-Method: JWTService](#13-method-by-method-jwtservice)
14. [Error Handling — End-to-End](#14-error-handling--end-to-end)
15. [Stateful (Session) vs Stateless (JWT) Security](#15-stateful-session-vs-stateless-jwt-security)
16. [CORS Configuration](#16-cors-configuration)
17. [MapStruct — Entity ↔ DTO Mapping](#17-mapstruct--entity--dto-mapping)
18. [Concurrency & Stock Safety](#18-concurrency--stock-safety)
19. [Configuration Reference (application.yml)](#19-configuration-reference-applicationyml)
20. [Complete API Reference](#20-complete-api-reference)
21. [How to Run Locally](#21-how-to-run-locally)
22. [Quick Reference: Postman Walkthrough](#22-quick-reference-postman-walkthrough)

---

## 1. Project Overview

This is a full-featured e-commerce REST API. Users can register, log in, browse products, manage a shopping cart, and place orders. Administrators get extra powers: manage all users, all products, and view the full cart across all users.

**Key design decisions:**
- **Stateless JWT auth** — the server never stores session state; every request is self-contained.
- **Role-based access** — `CUSTOMER` and `ADMIN` roles enforced at the URL level in `SecurityConfig`, with fine-grained ownership checks in controllers.
- **Consistent JSON errors** — every error, whether from a filter, controller, or unexpected crash, comes back as the same `ApiError` JSON shape.
- **Safe checkout** — stock is decremented with an atomic SQL `UPDATE … WHERE stock >= qty`, so two users can't buy the last item simultaneously.
- **DTO/Entity separation** — MapStruct mappers translate between JPA entities (the database layer) and plain DTO objects (the API layer). Sensitive fields like `password` are write-only in DTOs.

---

## 2. Tech Stack & Dependencies

| Dependency | Purpose |
|---|---|
| Spring Boot 4.1 | Application framework and auto-configuration |
| Spring Security | Filter chain, authentication, authorization |
| Spring Data JPA | Repository abstraction over Hibernate/MySQL |
| MySQL (`mysql-connector-j`) | Relational database |
| JJWT 0.13 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | JWT creation, signing, and parsing |
| Lombok 1.18 | Boilerplate reduction (`@Data`, `@Builder`, etc.) |
| MapStruct 1.5 | Compile-time Entity ↔ DTO mapping |
| Spring Boot Actuator | `/actuator/health` and `/actuator/info` endpoints |
| Java 21 | LTS runtime |

---

## 3. Project Structure

```
src/main/java/com/ecom/
│
├── Main.java                         ← Spring Boot entry point
│
├── config/
│   └── SecurityConfig.java           ← Security rules, filter chain, CORS, beans
│
├── filter/
│   └── JWTFilter.java                ← Runs on every request; validates JWT
│
├── security/
│   └── JsonAuthHandlers.java         ← JSON responses for 401 / 403 from filters
│
├── controller/
│   ├── UserController.java           ← /api/users/**
│   ├── ProductController.java        ← /api/products/**
│   ├── CartController.java           ← /api/cart/**
│   ├── OrderController.java          ← /api/order/**
│   ├── SystemController.java         ← /api/system/** (admin)
│   └── CustomErrorController.java    ← /error fallback
│
├── service/
│   ├── UserService.java              ← Registration, login, user CRUD
│   ├── ProductService.java           ← Product CRUD + search + soft-delete
│   ├── CartService.java              ← Add/remove cart items, stock check
│   ├── OrderService.java             ← Checkout: cart → order + stock decrement
│   ├── JWTService.java               ← Token generation, parsing, validation
│   └── MyUserDetailsService.java     ← Spring Security user loader (by email)
│
├── dto/                              ← API-facing objects (never expose entities)
│   ├── User.java
│   ├── UserPrincipal.java            ← Spring Security UserDetails wrapper
│   ├── Product.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Address.java
│
├── entity/                           ← JPA entities (database tables)
│   ├── UserEntity.java
│   ├── ProductEntity.java
│   ├── CartItemEntity.java
│   ├── OrderEntity.java
│   ├── OrderItemEntity.java
│   └── AddressEntity.java
│
├── enums/
│   ├── UserRole.java                 ← CUSTOMER, ADMIN
│   └── OrderStatus.java              ← PENDING, CONFIRMED, SHIPPED, DELIVERED
│
├── mapper/                           ← MapStruct interfaces (compile-time generated)
│   ├── UserMapper.java
│   ├── ProductMapper.java
│   ├── CartItemMapper.java
│   └── OrderMapper.java
│
├── repository/                       ← Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CartItemRepository.java
│   └── OrderRepository.java
│
└── exception/
    ├── ApiException.java             ← Business exceptions with HTTP status
    ├── ApiError.java                 ← Uniform JSON error response shape
    └── GlobalExceptionHandler.java   ← @RestControllerAdvice — maps all exceptions
```

---

## 4. Domain Model & Database Schema

```
+------------------+        +------------------+        +------------------+
|   UserEntity     |        | CartItemEntity   |        |  ProductEntity   |
|------------------|1     * |------------------|*     1 |------------------|
| id (PK)          +--------+ id (PK)          +--------+ id (PK)          |
| firstName        |        | user_id (FK)     |        | name             |
| lastName         |        | product_id (FK)  |        | description      |
| email            |        | quantity         |        | price            |
| password         |        | price            |        | stockQuantity    |
| phone            |        | createdAt        |        | category         |
| role             |        | updatedAt        |        | imageUrl         |
| address_id (FK)  |        +------------------+        | active           |
| createdAt        |                                    +------------------+
| updatedAt        |
+--------+---------+
         |
         | 1
         |                +------------------+
         |                |   OrderEntity    |
         |1             * |------------------|
         +----------------+ id (PK)          |       +--------------------+
                          | user_id (FK)     |1    * |  OrderItemEntity   |
                          | totalAmount      +-------+-------------------|
                          | status           |       | id (PK)            |
                          | createdAt        |       | product_id (FK)    |
                          | updatedAt        |       | order_id (FK)      |
                          +------------------+       | quantity           |
                                                     | price              |
+------------------+                                 +--------------------+
|  AddressEntity   |
|------------------|
| id (PK)          |  <-- 1:1 with UserEntity (cascade ALL, orphan removal)
| street           |
| city             |
| state            |
| country          |
| zipcode          |
+------------------+
```

**Key relationships:**
- `UserEntity` has an optional 1:1 `AddressEntity` (cascade ALL, orphan removal).
- `CartItemEntity` is a many-to-many join table between `UserEntity` and `ProductEntity`, enriched with `quantity` and `price`.
- `OrderEntity` owns a list of `OrderItemEntity` (cascade ALL, orphan removal).
- Orders reference a `UserEntity` (many-to-one) and each `OrderItemEntity` references a `ProductEntity`.

---

## 5. The Big Picture — What Happens to Every Request?

Think of the application as a building with a layered security checkpoint. Every HTTP request must pass through each checkpoint in order.

```
CLIENT (Browser / Postman / Mobile App)
         |
         |  HTTP Request
         |  (with or without "Authorization: Bearer <token>")
         v
+-------------------------------------------------------------+
|                  SERVLET CONTAINER (Tomcat)                  |
|                                                             |
|  +------------------------------------------------------+   |
|  |            SPRING SECURITY FILTER CHAIN              |   |
|  |                                                      |   |
|  |  1. CorsFilter          <- checks browser CORS rules |   |
|  |  2. >>> JWTFilter <<<   <- OUR custom filter         |   |
|  |  3. UsernamePasswordAuthenticationFilter (bypassed)  |   |
|  |  4. ExceptionTranslationFilter                       |   |
|  |  5. AuthorizationFilter <- checks URL permissions    |   |
|  +------------------------------------------------------+   |
|                          |                                  |
|                          v                                  |
|          DispatcherServlet (Spring MVC front controller)     |
|                          |                                  |
|                          v                                  |
|          @RestController (business logic)                    |
|    UserController / ProductController / CartController ...   |
+-------------------------------------------------------------+
         |
         |  HTTP Response (JSON data or JWT token)
         v
CLIENT
```

**Key rule:** If any filter rejects the request (bad token → 401, wrong role → 403), the request **stops immediately** and never reaches the DispatcherServlet or any controller.

---

## 6. Layer by Layer — The Spring Security Filter Chain

### What is a Filter?

A `Filter` is code that wraps every request and response. Spring Security builds a chain of them. Each filter either:
- **Passes through** → calls `filterChain.doFilter(request, response)` to hand off to the next filter.
- **Stops the request** → writes a response directly (e.g., 401 Unauthorized) and returns without calling `doFilter`.

### The Filters in This Application

```
Request
  -> [CorsFilter]        Handles browser preflight OPTIONS; rejects cross-origin requests
  -> [JWTFilter]         Reads "Authorization: Bearer …"; validates token; populates SecurityContext
  -> [AuthorizationFilter] Checks the SecurityContext: does this user have permission?
  -> DispatcherServlet
```

### How `filterChain.doFilter()` Works

```java
// Inside JWTFilter — the two code paths:

// Path 1: No token header -> pass through without setting auth
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response); // <- "pass it on"
    return;
}

// Path 2: Bad/expired token -> STOP, write 401, never call doFilter
try {
    userName = jwtService.extractUserName(token);
} catch (JwtException e) {
    JsonAuthHandlers.write(response, HttpStatus.UNAUTHORIZED, "Your session has expired.");
    return; // <- "stop here, controller never sees this request"
}
```

### The DispatcherServlet

After all security filters pass, the **DispatcherServlet** acts as a "traffic director":

```
DispatcherServlet receives request
         |
         v  Looks at URL and HTTP method (e.g. GET /api/products/active)
         |
         v  Finds matching @Controller method via HandlerMapping
         |
         v  Calls that controller method
         |
         v  Converts return value to JSON (via Jackson/HttpMessageConverter)
         |
         v  HTTP Response -> Client
```

---

## 7. Role-Based Access Control (RBAC)

The application has two roles: `CUSTOMER` (the default) and `ADMIN`.

### How Roles are Stored

```java
// UserRole.java
public enum UserRole { CUSTOMER, ADMIN; }

// UserEntity.java
@Enumerated(EnumType.STRING)
private UserRole role = UserRole.CUSTOMER; // default for every new user
```

### How Roles are Exposed to Spring Security

`UserPrincipal` wraps a `UserEntity` and implements `UserDetails`. It prefixes the role name with `ROLE_` as required by Spring Security:

```java
// UserPrincipal.java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    UserRole role = user.getRole() != null ? user.getRole() : UserRole.CUSTOMER;
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    // -> "ROLE_CUSTOMER" or "ROLE_ADMIN"
}
```

### URL-Level Permission Rules (`SecurityConfig`)

```java
http.authorizeHttpRequests(auth -> auth
    // Allow browser preflight
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

    // Public — no token needed
    .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/register").permitAll()
    .requestMatchers("/error", "/actuator/health").permitAll()

    // Admin only
    .requestMatchers("/actuator/**", "/api/system/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET, "/api/users/").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET, "/api/cart/").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET, "/api/products/").hasRole("ADMIN")
    .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

    // Everything else needs any authenticated user
    .anyRequest().authenticated()
)
```

### Fine-Grained Ownership Checks in Controllers

Some rules can't be expressed as URL patterns. Users can only view/update **their own** account (unless admin):

```java
// UserController.java
@GetMapping("/{id}")
public ResponseEntity<User> getUserById(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
    requireSelfOrAdmin(id, principal); // throws 403 if accessing someone else's account
    return ResponseEntity.ok(userService.getUserById(id));
}

private void requireSelfOrAdmin(Long id, UserPrincipal principal) {
    if (!principal.isAdmin() && !principal.getId().equals(id)) {
        throw ApiException.forbidden("You can only view or change your own account.");
    }
}
```

### Role Escalation Protection

New users are **always** registered as `CUSTOMER`:

```java
// UserService.java
user.setRole(UserRole.CUSTOMER); // never trust a role sent by the client
```

The update endpoint silently strips any role field sent by a non-admin:

```java
if (!callerIsAdmin) {
    updatedUser.setRole(null); // role changes need admin
}
```

---

## 8. Detailed Flow: Registering a User (POST /api/users/register)

This endpoint is public — no token required.

**Request:**
```json
POST /api/users/register
Content-Type: application/json

{
  "firstName": "Alice",
  "lastName":  "Smith",
  "email":     "alice@example.com",
  "password":  "secret123",
  "phone":     "0771234567"
}
```

**Step-by-step:**

```
1. Request hits Tomcat
   |
2. JWTFilter runs
   +-- No "Authorization" header found
   +-- Calls filterChain.doFilter() -> passes through [OK]
   |
3. AuthorizationFilter runs
   +-- URL "/api/users/register" is in permitAll() list
   +-- Allowed [OK]
   |
4. DispatcherServlet -> UserController.createUser()
   |
5. UserService.createUser(user)
   +-- Validates email format (regex check)
   +-- Validates password length (minimum 6 chars)
   +-- Validates firstName is not blank
   +-- Checks for duplicate email -> throws 409 CONFLICT if taken
   +-- Forces role = CUSTOMER (ignores any client-supplied role)
   +-- BCrypt-encodes password: "secret123" -> "$2a$10$Kx3B..."
   +-- Saves UserEntity to MySQL
   |
6. JWTService.generateToken("alice@example.com")
   +-- Creates signed JWT (expires in 30 minutes)
   |
7. Response: 201 CREATED
```

**Response:**
```json
{
  "student": {
    "id": 1,
    "firstName": "Alice",
    "lastName": "Smith",
    "email": "alice@example.com",
    "phone": "0771234567",
    "role": "CUSTOMER"
  },
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> **Note:** The `password` field uses `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` on the `User` DTO, so it is accepted in requests but **never serialized into responses**. You will never see a password hash in an API response.

---

## 9. Detailed Flow: Logging In (POST /api/users/login)

**Request:**
```json
POST /api/users/login
Content-Type: application/json

{
  "email":    "alice@example.com",
  "password": "secret123"
}
```

**Step-by-step:**

```
1. JWTFilter: no Authorization header -> passes through [OK]
   |
2. AuthorizationFilter: "/api/users/login" in permitAll() -> allowed [OK]
   |
3. DispatcherServlet -> UserController.login()
   |
4. UserService.login(user)
   |
5. AuthenticationManager.authenticate(
       new UsernamePasswordAuthenticationToken("alice@example.com", "secret123")
   )
   |
6. DaoAuthenticationProvider delegates to MyUserDetailsService
   +-- Calls loadUserByUsername("alice@example.com")
   +-- SELECT * FROM user_entity WHERE email = 'alice@example.com'
   +-- Returns UserPrincipal(alice)
   +-- Compares "secret123" with BCrypt hash from DB
       +-- Match    -> Authentication succeeds [OK]
       +-- No match -> throws BadCredentialsException -> 401
   |
7. JWTService.generateToken("alice@example.com")
   |
8. Response: 201 CREATED
   Body: "eyJhbGciOiJIUzI1NiJ9..."
```

The client **must save this token** and include it in every subsequent request to protected endpoints.

---

## 10. Detailed Flow: Accessing a Protected Endpoint

Example: getting your own cart items.

**Request:**
```
GET /api/cart/items
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Step-by-step:**

```
1. Request hits Tomcat
   |
2. JWTFilter.doFilterInternal() runs:
   |
   a) Read header: "Authorization: Bearer eyJ..."
   b) Extract token = "eyJhbGciOiJIUzI1NiJ9..."
   c) Try to extract username:
      jwtService.extractUserName(token)
        -> decodes JWT payload
        -> reads "sub" claim -> "alice@example.com"
      If the token is malformed or the signature doesn't match:
        -> JwtException caught -> 401 "Your session has expired." [STOPS HERE]
   |
   d) userName = "alice@example.com"
      SecurityContext has no existing auth (stateless)
   |
   e) Load user from DB:
      MyUserDetailsService.loadUserByUsername("alice@example.com")
        -> SELECT from DB -> UserPrincipal(alice)
      If account deleted after token was issued:
        -> UsernameNotFoundException -> 401 "Account not found." [STOPS HERE]
   |
   f) Validate token:
      jwtService.validateToken(token, userDetails)
        [OK] username matches ("alice@example.com" == "alice@example.com")
        [OK] token not expired (exp > now)
   |
   g) Set authentication in SecurityContext:
      SecurityContextHolder.getContext().setAuthentication(authToken)
      (Spring now knows "alice@example.com with ROLE_CUSTOMER is authenticated")
   |
   h) filterChain.doFilter() -> passes to next filter [OK]
   |
3. AuthorizationFilter runs:
   +-- SecurityContext has authenticated user alice
   +-- GET /api/cart/items matches anyRequest().authenticated()
   +-- alice IS authenticated -> allowed [OK]
   |
4. DispatcherServlet -> CartController.getCart(@AuthenticationPrincipal UserPrincipal principal)
   |
5. principal.getId() -> alice's DB id (from the JWT, not from a header)
   |
6. CartService.getCart(aliceId)
   -> SELECT cart_item WHERE user_id = aliceId
   |
7. Response: 200 OK
   [{ "id": 1, "product": {...}, "quantity": 2, "price": 29.98 }, ...]
```

---

## 11. Detailed Flow: Placing an Order (POST /api/order/)

The most complex flow — it touches multiple tables in a single database transaction.

**Request:**
```
POST /api/order/
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Step-by-step:**

```
1. JWT validated (see section 10)
   |
2. OrderController.createOrder(@AuthenticationPrincipal UserPrincipal principal)
   |
3. OrderService.createOrder(principal.getId()) — @TRANSACTIONAL
   |
   a) Load UserEntity from DB (throws 404 if not found)
   |
   b) Load all CartItemEntity rows for this user
      If cart is empty -> throw 400 "Your cart is empty."
   |
   c) For each cart item:
      +-- Check product is still active (not soft-deleted)
      |   -> If inactive: throw 409 "[product] is no longer available."
      |
      +-- Atomic stock decrement:
      |   UPDATE products SET stockQuantity = stockQuantity - :qty
      |   WHERE id = :id AND stockQuantity >= :qty
      |   Returns 1 (success) or 0 (not enough stock)
      |   -> If 0: throw 409 "Not enough stock for [product]."
      |
      +-- Build OrderItemEntity at current price (not the cart price)
          Total amount accumulates
   |
   d) Save OrderEntity with status = CONFIRMED + all items (cascade)
   |
   e) cartService.clearCart(userId) — deletes all cart rows for this user
   |
4. If ANY step above throws, the ENTIRE transaction rolls back:
   -> Stock is NOT decremented
   -> Order is NOT saved
   -> Cart is NOT cleared
   |
5. Response: 201 CREATED
   {
     "id": 42,
     "user": { ... },
     "totalAmount": 59.97,
     "status": "CONFIRMED",
     "items": [ { "product": {...}, "quantity": 3, "price": 59.97 } ]
   }
```

> **Important:** The `@Transactional` annotation on `OrderService.createOrder()` means the entire checkout is **atomic**. If the stock decrement fails halfway through, all earlier decrements roll back automatically.

---

## 12. Class-by-Class Breakdown

### `SecurityConfig.java` — The Security Blueprint

Registered as a Spring `@Configuration` bean, read once at startup.

| Method | What It Does |
|---|---|
| `securityFilterChain()` | Assembles the filter chain: disables CSRF + form login + HTTP basic, sets STATELESS sessions, defines URL rules, registers 401/403 handlers, injects `JWTFilter` |
| `corsConfigurationSource()` | Reads `app.cors.allowed-origins` from config, produces a `CorsConfigurationSource` bean |
| `daoAuthenticationProvider()` | Wires `MyUserDetailsService` + `BCryptPasswordEncoder` so Spring can look up and verify users |
| `passwordEncoder()` | Returns `BCryptPasswordEncoder` with cost factor 10 |
| `authenticationManager()` | Exposes `AuthenticationManager` so `UserService.login()` can use it |

**Important decisions:**
```java
http.csrf(csrf -> csrf.disable())                        // REST APIs don't need CSRF protection
http.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Never create HttpSession
http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // JWT runs first
http.exceptionHandling(ex -> ex
    .authenticationEntryPoint(JsonAuthHandlers.unauthorized())   // 401 as JSON
    .accessDeniedHandler(JsonAuthHandlers.forbidden()))          // 403 as JSON
```

---

### `JWTFilter.java` — The Token Inspector

Extends `OncePerRequestFilter` — Spring guarantees this runs **exactly once per request**.

```
Incoming Request
       |
       v
Has "Authorization: Bearer ..." header?
  NO  -> filterChain.doFilter() (pass through, no auth set)
  YES -> Extract token (strip "Bearer " prefix)
              |
              v
         Parse JWT -> extract username
              |
         Error? (malformed / bad signature / expired)
              -> write 401 JSON + return (STOP)
              |
              v (username extracted OK)
         SecurityContext already has auth?
              YES -> skip to filterChain.doFilter()
              NO  -> Load UserDetails from DB
                          |
                     Not found? -> write 401 + return (STOP)
                          |
                          v
                     validateToken: username matches AND not expired?
                          YES -> set auth in SecurityContext
                          |
                          v
                     filterChain.doFilter() -> next filter
```

> **Note:** `JWTFilter` uses `ApplicationContext.getBean(MyUserDetailsService.class)` instead of `@Autowired` to avoid a circular dependency in the Spring context initialization.

---

### `JWTService.java` — The Token Factory

Encapsulates all JWT operations using JJWT 0.13's non-deprecated API.

---

### `MyUserDetailsService.java` — The User Loader

Spring Security's `AuthenticationManager` calls `loadUserByUsername()` to verify credentials. The "username" in this application is the user's **email address**.

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserEntity user = userRepository.findByEmail(username);
    if (user == null) throw new UsernameNotFoundException("User not found");
    return new UserPrincipal(user);
}
```

---

### `UserPrincipal.java` — The Security Wrapper

Spring Security doesn't know about `UserEntity`. `UserPrincipal` acts as an **adapter** — it wraps your entity and implements `UserDetails`.

```java
public class UserPrincipal implements UserDetails {
    private final UserEntity user;

    @Override public String getUsername()  { return user.getEmail(); }
    @Override public String getPassword()  { return user.getPassword(); } // BCrypt hash
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        // "ROLE_CUSTOMER" or "ROLE_ADMIN"
    }

    public Long getId()       { return user.getId(); }
    public boolean isAdmin()  { return user.getRole() == UserRole.ADMIN; }
}
```

Controllers receive `UserPrincipal` via `@AuthenticationPrincipal` — Spring resolves it directly from the `SecurityContext`, meaning user identity always comes from the validated JWT, never from request headers or body parameters.

---

### `JsonAuthHandlers.java` — JSON Security Error Responses

By default, Spring Security returns HTML pages for auth errors. This class overrides that:

| Scenario | Handler | HTTP Status | Message |
|---|---|---|---|
| No token / bad token | `AuthenticationEntryPoint` | 401 | "Sign in to continue." |
| Valid token, wrong role | `AccessDeniedHandler` | 403 | "You don't have permission to do that." |

The `write()` method manually builds JSON (instead of using Jackson) because the full Spring MVC context is not available at the filter level.

---

### `GlobalExceptionHandler.java` — Centralized Error Handling

A `@RestControllerAdvice` that intercepts exceptions from controllers and services:

| Exception Type | HTTP Status | Message |
|---|---|---|
| `ApiException` | As specified | Custom business message |
| `BadCredentialsException` / `DisabledException` | 401 | "Email or password is incorrect." |
| `HttpMessageNotReadableException` | 400 | "Request body is missing or invalid JSON." |
| `MethodArgumentTypeMismatchException` | 400 | "A parameter is missing or wrong type." |
| `AccessDeniedException` | 403 | "You don't have permission to do that." |
| Any other `Exception` | 500 | "Something went wrong." (internals hidden) |

> **Important:** `GlobalExceptionHandler` only handles exceptions that **reach Spring MVC**. Errors from security filters (bad tokens, wrong role) are handled by `JsonAuthHandlers` instead.

---

### `ApiError.java` — The Standard Error Shape

Every error produces this identical JSON:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Your session has expired. Sign in again.",
  "timestamp": "2026-11-10T11:48:19.123Z"
}
```

`ApiError` is a Java `record`. Its `toJson()` method manually builds JSON (bypassing Jackson) for use in security filters, with a built-in string escaper to prevent injection.

---

### `ApiException.java` — Business Layer Exceptions

Services throw `ApiException` to signal a recoverable error:

```java
throw ApiException.badRequest("Enter a valid email address.");        // 400
throw ApiException.notFound("User not found.");                        // 404
throw ApiException.conflict("An account with that email exists.");     // 409
throw ApiException.forbidden("You can only change your own account."); // 403
```

---

### `CustomErrorController.java` — Last-Resort Error Page

Implements Spring's `ErrorController` to intercept the `/error` path, which Tomcat forwards to when an error happens **outside Spring MVC** (e.g., a 404 for an unmapped URL). Without this, Tomcat would return an HTML error page.

---

## 13. Method-by-Method: JWTService

### `generateToken(String username)`

Builds and signs a compact JWT string:

```
username = "alice@example.com"
        |
        v
Jwts.builder()
    .claims({})                        <- empty extra claims map
    .subject("alice@example.com")      <- "sub" claim
    .issuedAt(now)                     <- "iat" claim
    .expiration(now + 30 minutes)      <- "exp" claim
    .signWith(getKey())                <- HMAC-SHA256 with the secret key
    .compact()
        |
        v
"eyJhbGci..." . "eyJzdWIi..." . "SflKxwRJSMeK..."
 |-- header --| |-- payload --| |-- signature --|
```

**JWT structure:**

| Section | Decoded content |
|---|---|
| Header | `{"alg":"HS256","typ":"JWT"}` |
| Payload | `{"sub":"alice@example.com","iat":1731234000,"exp":1731235800}` |
| Signature | `HMAC-SHA256(base64(header) + "." + base64(payload), secretKey)` |

> **Caution:** The header and payload are **Base64-encoded, not encrypted**. Anyone can decode them. The signature only proves the token wasn't tampered with — keep the secret key private.

---

### `validateToken(String token, UserDetails userDetails)`

Two checks must both pass:

```java
public boolean validateToken(String token, UserDetails userDetails) {
    final String userName = extractUserName(token);
    return (userName.equals(userDetails.getUsername())  // username matches
            && !isTokenExpired(token));                 // not past exp date
}
```

---

### `extractUserName()` → `extractClaim()` → `extractAllClaims()` chain

```java
extractUserName(token)
  -> extractClaim(token, Claims::getSubject)
       -> extractAllClaims(token)
             -> Jwts.parser()
                     .verifyWith(getKey())          // validates signature
                     .build()
                     .parseSignedClaims(token)      // decodes all three sections
                     .getPayload()                  // returns Claims object
             -> claimResolver.apply(claims)
             -> claims.getSubject()                 // "alice@example.com"
```

If the signature is invalid or the token is expired, JJWT throws a `JwtException`, which `JWTFilter` catches and converts to a 401 response.

---

## 14. Error Handling — End-to-End

The application has **two distinct error handling layers** that together ensure every error produces consistent JSON.

```
Request lifecycle and where errors are caught:

Tomcat -> [JWTFilter] -> [AuthFilter] ->
                                      |
                              ERROR LAYER 1: Security Filters (before Spring MVC)
                              - Bad token       -> 401 (JsonAuthHandlers)
                              - Wrong role      -> 403 (JsonAuthHandlers)

                    DispatcherServlet
                              |
                   Controller -> Service ->
                                      |
                              ERROR LAYER 2: GlobalExceptionHandler
                              - ApiException    -> custom status
                              - BadCredentials  -> 401
                              - Bad JSON        -> 400
                              - Unexpected      -> 500

                   /error path (Tomcat fallback):
                              -> CustomErrorController (last resort, always JSON)
```

### Error Response Shape

All three layers produce the same `ApiError` JSON:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found.",
  "timestamp": "2026-11-10T11:48:19.123456Z"
}
```

### Common Error Scenarios

| Scenario | Layer | Status | Message |
|---|---|---|---|
| Missing/malformed JWT | `JWTFilter` | 401 | "Your session has expired. Sign in again." |
| Token for a deleted account | `JWTFilter` | 401 | "Your account could not be found." |
| No token at all (protected URL) | `JsonAuthHandlers` | 401 | "Sign in to continue." |
| Valid token, wrong role | `JsonAuthHandlers` | 403 | "You don't have permission to do that." |
| Wrong email or password | `GlobalExceptionHandler` | 401 | "Email or password is incorrect." |
| Duplicate email on register | `GlobalExceptionHandler` | 409 | "An account with that email already exists." |
| Product not found | `GlobalExceptionHandler` | 404 | "That product is no longer available." |
| Cart item out of stock | `GlobalExceptionHandler` | 409 | "Only N of [product] in stock." |
| Empty cart on checkout | `GlobalExceptionHandler` | 400 | "Your cart is empty." |
| Invalid JSON body | `GlobalExceptionHandler` | 400 | "The request body is missing or isn't valid JSON." |
| Unexpected server error | `GlobalExceptionHandler` | 500 | "Something went wrong on the server." |
| Unknown URL (no mapping) | `CustomErrorController` | 404 | "Not Found" |

---

## 15. Stateful (Session) vs Stateless (JWT) Security

### The Classic Stateful Way

```
STATEFUL FLOW:

1. POST /api/users/login { email, password }
           |
2. Server verifies credentials [OK]
           |
3. Server creates HTTP Session in memory:
   Sessions = { "JSESSIONID=abc123" -> { user: "alice", role: "CUSTOMER" } }
           |
4. Server: Set-Cookie: JSESSIONID=abc123
           |
5. Browser automatically sends cookie on EVERY future request:
   GET /api/cart/items   Cookie: JSESSIONID=abc123
           |
6. Server looks up "abc123" in session store -> finds alice -> she's in
```

**Problems with stateful sessions:**
- Server must store one session per logged-in user (RAM/DB overhead).
- Doesn't scale horizontally — server #2 doesn't know about sessions on server #1.
- Browsers auto-send cookies, making CSRF attacks possible.

---

### The Stateless JWT Way (this application)

```
STATELESS JWT FLOW:

1. POST /api/users/login { email, password }
           |
2. Server verifies credentials [OK]
           |
3. Server creates signed JWT and returns it.
   Server stores NOTHING — gives token to client and forgets.
           |
4. Client stores token (localStorage, memory, etc.)
           |
5. Client manually sends token on every protected request:
   GET /api/cart/items   Authorization: Bearer eyJhbGci...
           |
6. Server receives token -> verifies signature -> reads username from claims
   -> No DB session lookup needed -> grants access
```

### Side-by-Side Comparison

| Feature | Stateful (Session) | Stateless (JWT) |
|---|---|---|
| Server stores login state? | YES (memory/DB) | NO |
| Client sends per request | Cookie (automatic) | `Authorization: Bearer` (manual) |
| Scales horizontally? | Hard (shared session store) | Easy (any server verifies) |
| Logout | Easy — delete session | Hard — token stays valid until expiry |
| Token expiry | Server controls session lifetime | Baked into token (`exp` claim) |
| CSRF risk | YES (cookies are auto-sent) | NO (header is not auto-sent) |
| Best for | Traditional web apps | REST APIs, Mobile apps, Microservices |

### Why This App Uses `SessionCreationPolicy.STATELESS`

```java
http.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
```

Without this, Spring Security might still create an `HttpSession` as a fallback, making the app accidentally stateful.

### The Logout Problem with JWT

Since the server stores nothing, you cannot "invalidate" a token. A logged-out user's token remains valid until its 30-minute expiry.

| Approach | Trade-off |
|---|---|
| Short expiry (this app: 30 min) | Simple; user re-authenticates more often |
| Token blacklist in Redis | Adds state back; works but needs Redis |
| Refresh token pattern | Short-lived access tokens + long-lived refresh tokens |

---

## 16. CORS Configuration

CORS (Cross-Origin Resource Sharing) is a browser security mechanism. When your React frontend at `http://localhost:5173` calls this API at `http://localhost:5000`, the browser first sends an `OPTIONS` **preflight request** to ask "is this allowed?".

### How it's configured

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    // Reads comma-separated origins from application.yml:
    // app.cors.allowed-origins: http://localhost:5173
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L); // Cache preflight for 1 hour

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

All `OPTIONS` preflight requests are explicitly permitted without authentication:
```java
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
```

### To add more frontend origins

Change `application.yml` only — no code changes needed:
```yaml
app:
  cors:
    allowed-origins: http://localhost:5173,https://myshop.example.com
```

---

## 17. MapStruct — Entity ↔ DTO Mapping

The application strictly separates:
- **JPA Entities** (`UserEntity`, `ProductEntity`, etc.) — the database layer.
- **DTOs** (`User`, `Product`, etc.) — the API layer.

MapStruct generates the boilerplate conversion code at **compile time** — no runtime reflection, no magic, just generated Java classes.

### How it works

```java
// UserMapper.java
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toUserDto(UserEntity entity);
    UserEntity toUserEntity(User userDto);

    @Mapping(target = "id", ignore = true)         // Never overwrite the PK
    @Mapping(target = "address.id", ignore = true)  // Never overwrite nested PK
    @Mapping(target = "createdAt", ignore = true)   // Hibernate manages this
    @Mapping(target = "updatedAt", ignore = true)   // Hibernate manages this
    void updateUserEntityFromDto(User updatedUser, @MappingTarget UserEntity existingUser);
}
```

`NullValuePropertyMappingStrategy.IGNORE` enables **partial updates** — if the DTO has a `null` field, the entity's existing value is preserved.

### The `OrderMapper` — handling bi-directional relationships

Orders have items; each item references its parent order. The `@AfterMapping` hook solves the circular reference:

```java
@AfterMapping
default void linkOrderItems(@MappingTarget OrderEntity order) {
    if (order.getItems() != null) {
        for (OrderItemEntity item : order.getItems()) {
            item.setOrder(order); // Set back-reference on each child
        }
    }
}
```

---

## 18. Concurrency & Stock Safety

A naive checkout would have a race condition where two users buy the last item. This application prevents it with an **atomic SQL UPDATE**:

```java
// ProductRepository.java
@Modifying
@Query("UPDATE products p " +
       "SET p.stockQuantity = p.stockQuantity - :qty " +
       "WHERE p.id = :id AND p.stockQuantity >= :qty")
int decrementStock(@Param("id") Long id, @Param("qty") int qty);
```

This runs as one atomic statement. The database's row-level locking ensures only one transaction wins when two requests race. Returns `1` on success, `0` if stock is insufficient:

```java
// OrderService.createOrder()
int reserved = productRepository.decrementStock(product.getId(), line.getQuantity());
if (reserved == 0) {
    throw ApiException.conflict("Not enough stock for " + product.getName() + ".");
}
```

Combined with `@Transactional` on `createOrder()`, any failure mid-order causes all prior stock decrements to roll back automatically.

---

## 19. Configuration Reference (`application.yml`)

```yaml
server:
  port: ${PORT:5000}          # Uses PORT env var if set, else defaults to 5000

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce
    username: root
    password: 123             # Use environment variables in production!
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update        # Auto-creates/updates tables. Use 'validate' in production.
    show-sql: true            # Logs all SQL. Set to false in production.
    properties:
      hibernate:
        format_sql: true

jwt:
  # Base64 string of at least 32 random bytes.
  # Generate with: openssl rand -base64 32
  secret: YourSuperSecretKeyThatIsAtLeast32BytesLong123!

app:
  cors:
    # Comma-separated frontend origins allowed to call the API from a browser.
    allowed-origins: http://localhost:5173

management:
  endpoints:
    web:
      exposure:
        # Only safe endpoints. Never expose 'env' or 'heapdump' in production.
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
      roles: ADMIN

info:
  app:
    name: Ecommerce-App
    description: Ecommerce Backend Application
    version: 1.0.0
```

> **Caution:** Never commit real credentials or JWT secrets to source control. Use environment variables or a secrets manager (e.g., Spring Cloud Config, AWS Secrets Manager) in production.

---

## 20. Complete API Reference

### Authentication Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `POST` | `/api/users/register` | ❌ Public | Register new user; returns `{ user, token }` |
| `POST` | `/api/users/login` | ❌ Public | Authenticate; returns JWT string |

### User Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/` | 🔐 ADMIN | List all users |
| `GET` | `/api/users/me` | 🔑 Any | Get own profile (from JWT) |
| `GET` | `/api/users/{id}` | 🔑 Self or ADMIN | Get user by ID |
| `PUT` | `/api/users/{id}` | 🔑 Self or ADMIN | Update user (password/email locked; role ADMIN-only) |

### Product Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/products/` | 🔐 ADMIN | List all products (including inactive) |
| `GET` | `/api/products/active` | 🔑 Any | List active, in-stock products |
| `GET` | `/api/products/search?keyword=` | 🔑 Any | Search active products by name |
| `POST` | `/api/products/` | 🔐 ADMIN | Create new product |
| `PUT` | `/api/products/{id}` | 🔐 ADMIN | Update product |
| `DELETE` | `/api/products/{id}` | 🔐 ADMIN | Soft-delete (sets `active = false`) |

### Cart Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/api/cart/` | 🔐 ADMIN | View all users' cart items |
| `GET` | `/api/cart/items` | 🔑 Any | View own cart |
| `POST` | `/api/cart/` | 🔑 Any | Add item to own cart |
| `DELETE` | `/api/cart/{productId}` | 🔑 Any | Remove item from own cart |

### Order Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `POST` | `/api/order/` | 🔑 Any | Checkout: convert cart to order |

### System & Health Endpoints

| Method | URL | Auth | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | ❌ Public | App health check |
| `GET` | `/actuator/info` | ❌ Public | App version info |
| `GET` | `/api/system/info` | 🔐 ADMIN | Server OS/JVM info |

**Legend:** ❌ Public (no token) · 🔑 Authenticated (any role) · 🔐 ADMIN role required

---

## 21. How to Run Locally

### Prerequisites

- Java 21+
- Maven 3.8+
- MySQL 8+ running locally

### 1. Create the database

```sql
CREATE DATABASE ecommerce;
```

### 2. Configure `application.yml`

The defaults expect MySQL at `localhost:3306`, database `ecommerce`, username `root`, password `123`. Adjust as needed.

```yaml
# Change the JWT secret for local dev (keep it at least 32 bytes base64-encoded):
jwt:
  secret: YourSuperSecretKeyThatIsAtLeast32BytesLong123!
```

### 3. Build and run

```bash
# From the project root (where pom.xml lives):
mvn clean spring-boot:run
```

Or build a JAR first:
```bash
mvn clean package -DskipTests
java -jar target/ECommerceApp.jar
```

The server starts on port `5000` by default. Set the `PORT` environment variable to change it.

### 4. Hibernate will auto-create tables

`ddl-auto: update` means Hibernate reads your entities and creates or updates the MySQL tables automatically on startup.

---

## 22. Quick Reference: Postman Walkthrough

### Step 1 — Register

```
POST http://localhost:5000/api/users/register
Content-Type: application/json

{
  "firstName": "Alice",
  "lastName":  "Smith",
  "email":     "alice@example.com",
  "password":  "secret123"
}
```

Copy the `token` from the response.

### Step 2 — Login (get a fresh token any time)

```
POST http://localhost:5000/api/users/login
Content-Type: application/json

{
  "email":    "alice@example.com",
  "password": "secret123"
}
```

The response body IS the token string.

### Step 3 — Browse products

```
GET http://localhost:5000/api/products/active
Authorization: Bearer <your-token-here>
```

### Step 4 — Add to cart

```
POST http://localhost:5000/api/cart/
Authorization: Bearer <your-token-here>
Content-Type: application/json

{
  "product": { "id": 1 },
  "quantity": 2
}
```

### Step 5 — Place order

```
POST http://localhost:5000/api/order/
Authorization: Bearer <your-token-here>
```

No body needed — the order is built from your cart.

### Step 6 — Admin: Create a product

```
POST http://localhost:5000/api/products/
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Wireless Mouse",
  "description": "Ergonomic 2.4GHz wireless mouse",
  "price": 29.99,
  "stockQuantity": 150,
  "category": "Electronics",
  "imageUrl": "https://example.com/mouse.jpg"
}
```

> **Tip:** To promote a user to ADMIN, update their role directly in MySQL:
> ```sql
> UPDATE user_entity SET role = 'ADMIN' WHERE email = 'admin@example.com';
> ```
> Then log in again to get a new token with the ADMIN role baked in.

---

*Generated for the **Ecommerce-Backend-secured** project — a Spring Boot 4 + JWT e-commerce REST API.*
