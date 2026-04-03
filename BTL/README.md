# Forgot Password Feature

This project now includes a token-based forgot password flow.

## API Endpoints

- `POST /auth/forgot-password`
  - Body:
    ```json
    {
      "email": "user@example.com"
    }
    ```
  - Behavior: always returns success message to avoid email enumeration.

- `POST /auth/reset-password`
  - Body:
    ```json
    {
      "token": "token-from-email-link",
      "newPassword": "newPassword123"
    }
    ```
  - Behavior: validates token, checks expiry, updates password, marks token as used.

## Configuration

Set these properties in `src/main/resources/application.properties`:

- `app.reset-password.base-url` - frontend page that receives `?token=...`
- `app.reset-password.expiry-minutes` - token lifetime
- `spring.mail.*` and `app.mail.from` - SMTP configuration

## Run Tests

```powershell
./mvnw -Dtest=AuthenticationServicePasswordResetTest test
```

