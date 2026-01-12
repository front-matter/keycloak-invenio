# Magic Link Extension Tests

## Test Coverage

### MagicLinkActionTokenTest
- Token creation with all parameters
- Token with null rememberMe
- Setters and getters
- Token type constant

### MagicLinkAuthenticatorTest  
- Authenticate flow (shows email form)
- Action with missing email (error handling)
- requiresUser() returns false
- configuredFor() always returns true
- setRequiredActions() no-op
- close() no-op

### MagicLinkAuthenticatorFactoryTest
- Provider ID
- Display type and category
- Configuration properties (createUser, tokenValidity)
- Requirement choices
- Help text
- Authenticator creation
- Lifecycle methods

### MagicLinkActionTokenHandlerFactoryTest
- Provider ID
- Handler creation
- Lifecycle methods

## Running Tests

```bash
cd magic-link
mvn test
```

## Test Dependencies
- JUnit 5 (Jupiter)
- Mockito 5.8.0
