# Security fixes

## Before you run it

1. **Set environment variables.** `application.yml` no longer contains secrets.
   In IntelliJ: Run → Edit Configurations → Environment variables. See `.env.example`.

   | Variable | Example |
   |---|---|
   | `DB_URL` | `jdbc:mysql://your-rds-host:3306/ecommerce` |
   | `DB_USERNAME` | `admin` |
   | `DB_PASSWORD` | your database password |
   | `JWT_SECRET` | output of `openssl rand -base64 32` |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,https://your-shop.com` |

2. **Rotate the old secrets.** The previous database password and JWT secret are in your
   Git history, so treat them as public: change the RDS password and use a new JWT secret.

3. **Rebuild:** `mvn clean package`, then start the app.

## Making an admin

Register can no longer create admins. Promote an account in the database:

```sql
UPDATE user_entity SET role = 'ADMIN' WHERE email = 'you@example.com';
```

After that, an admin can promote others with `PUT /api/users/{id}` and `{"role": "ADMIN"}`.
The user has to sign out and back in (or reload) to see the admin area.

## What changed

**Who you are comes from the token.** Cart and order endpoints ignore `X-User-ID` and use
the signed-in user from the JWT, so nobody can read or change another person's cart.

**Roles are enforced** (`SecurityConfig`). `UserPrincipal` now grants `ROLE_ADMIN` or
`ROLE_CUSTOMER`.

| Admin only | Any signed-in user |
|---|---|
| `GET /api/users/` | `GET /api/users/me` (new) |
| `GET /api/cart/` | `GET`/`PUT /api/users/{id}` for your own id (admins: any id) |
| `GET /api/products/` (includes hidden) | `GET /api/products/active`, `/search` |
| `POST`/`PUT`/`DELETE /api/products/**` | Cart and order endpoints |
| `/api/system/**`, `/actuator/**` (except health) | |

**Accounts**
- Register always creates a `CUSTOMER`, rejects a duplicate email (409) and checks email,
  password length (6+) and first name (400).
- `PUT /api/users/{id}` never changes email or password, and only admins can change `role`.
- Fixed: the `User` DTO defaulted `role` to `CUSTOMER`, so every profile update silently
  demoted admins. The default is gone; register sets the role explicitly.
- Password hashes are no longer included in any response.
- Unknown user id returns 404 instead of crashing with a 500.

**Errors are real status codes with JSON bodies:** `{ status, error, message, timestamp }`.
- Wrong login: 401 "Email or password is incorrect."
- No token or expired token: 401. Signed in but not allowed: 403.
- Cart: 400 for a missing product or quantity below 1; 404 for an unknown or hidden product,
  or an item not in the cart; 409 when the cart total would exceed stock.
  Success messages are unchanged ("Item added successfully", "Item removed successfully").
- Order: 400 for an empty cart, 409 when stock ran out or a product was hidden.
- `/error` now works for every HTTP method (it was mapped to `/error/` for GET only).

**Orders**
- Stock is checked and reduced in the same transaction with an atomic
  `UPDATE ... WHERE stockQuantity >= :qty`, so two buyers can't take the last unit.
  If any line fails, nothing is saved and the cart stays as it was.
- Each line is charged at the product's current price.

**Configuration**
- CORS is configured from `CORS_ALLOWED_ORIGINS`.
- Actuator exposes only `health` and `info`. `/actuator/env` and `/actuator/heapdump`
  were reachable by any signed-in user and expose secrets.
- `show-sql` is off by default (`SHOW_SQL=true` to turn it on).

## Files

New: `exception/ApiException.java`, `exception/ApiError.java`,
`exception/GlobalExceptionHandler.java`, `security/JsonAuthHandlers.java`, `.env.example`.

Changed: `SecurityConfig`, `JWTFilter`, `UserPrincipal`, `User` (DTO), `UserController`,
`UserService`, `CartController`, `CartService`, `OrderController`, `OrderService`,
`ProductRepository`, `CustomErrorController`, `application.yml`, `.gitignore`.

## Not changed

The `Dockerfile` still copies `ECommerceApp.jar` but runs `job-app.jar` and exposes 8080
while the app listens on 5000. Maven builds `target/EcommerceApp-1.0-SNAPSHOT.jar`.
