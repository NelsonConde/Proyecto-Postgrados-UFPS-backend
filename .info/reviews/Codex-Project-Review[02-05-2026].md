Sí: tu idea de crear un **módulo aparte** es buena, y de hecho resolvería mejor la ambigüedad que dejarlo como carpeta dentro de `api-core`.

Yo no lo llamaría simplemente `security`, porque ya tienes `platform/security` y volverías al mismo problema conceptual. Lo llamaría algo como:

```text
backend/api-auth
```

o, todavía más claro:

```text
backend/identity
```

Mi recomendación: **`backend/api-auth`**, porque encaja con tus nombres actuales (`api-base`, `api-core`, `api-rest`, `api-storage`) y comunica que es parte del backend funcional, no de la plataforma.

**Idea Central**

Tu arquitectura debería quedar así:

```text
root
├── backend/
│   ├── api-base        → entidades, repositorios, servicios CRUD
│   ├── api-core        → lógica de negocio general
│   ├── api-auth        → autenticación, identidad, contraseñas, emisión de tokens
│   ├── api-rest        → controladores HTTP
│   └── api-storage     → S3
│
└── platform/
    ├── application     → ensamble Spring Boot
    └── security        → seguridad HTTP, filtros, CORS, SecurityFilterChain
```

La diferencia conceptual quedaría así:

```text
platform/security = seguridad de entrada HTTP
backend/api-auth  = identidad, login y credenciales
```

O en una frase:

> `platform/security` responde “¿esta request puede entrar?”  
> `backend/api-auth` responde “¿quién es este usuario y cómo se autentica?”

**Responsabilidad Del Nuevo Módulo**

El módulo `backend/api-auth` debería encargarse de:

```text
- Login
- Refresh token
- Validación de credenciales
- Verificación/hash de contraseñas
- Generación de JWT
- Definición de claims
- Construcción del usuario autenticado
- Reglas de autenticación
- Casos de uso de identidad
```

Y NO debería encargarse de:

```text
- SecurityFilterChain
- CORS
- CSRF
- OncePerRequestFilter
- SecurityContextHolder
- hasRole / authorizeHttpRequests
- Configuración HTTP de Spring Security
```

Eso debe seguir en `platform/security`.

**Estructura Sugerida**

```text
backend/api-auth
├── pom.xml
└── src/main/java/ufps/edu/co/auth/
    ├── processor/
    │   ├── AuthenticationProcessor.java
    │   └── TokenRefreshProcessor.java
    │
    ├── service/
    │   ├── CredentialService.java
    │   ├── PasswordHashService.java
    │   ├── JwtIssuerService.java
    │   └── AuthenticatedUserService.java
    │
    ├── contract/
    │   ├── TokenIssuer.java
    │   ├── TokenParser.java
    │   ├── PasswordVerifier.java
    │   ├── PasswordEncoder.java
    │   └── AuthPrincipalProvider.java
    │
    ├── model/
    │   ├── AuthPrincipal.java
    │   ├── AuthTokenPair.java
    │   ├── AuthenticatedUser.java
    │   └── AuthRole.java
    │
    ├── records/
    │   ├── input/
    │   │   ├── LoginInput.java
    │   │   └── RefreshTokenInput.java
    │   └── output/
    │       └── LoginOutput.java
    │
    ├── exception/
    │   ├── InvalidCredentialsException.java
    │   ├── DisabledUserException.java
    │   ├── ExpiredRefreshTokenException.java
    │   └── AuthDomainException.java
    │
    └── config/
        └── AuthModuleConfiguration.java
```

**Qué Movería Conceptualmente**

Desde `api-core` hacia `api-auth`:

```text
AuthenticationProcessor
LoginProcessor
LoginPrincipal
LoginTokenProvider
PasswordVerifier
LoginInput
LoginOutput
```

Desde `platform/security` hacia `api-auth`:

```text
JwtTokenProvider
DefaultPasswordVerifier
```

Pero con una mejora de nombres:

```text
JwtTokenProvider      → JwtIssuerService / JwtTokenService
DefaultPasswordVerifier → BCryptPasswordHashService
```

Y dejaría en `platform/security` solamente:

```text
SecurityConfig
CorsConfiguration
JwtAuthenticationFilter
JwtAccessTokenValidator
SpringSecurityPrincipalAdapter
```

**Dependencias Entre Módulos**

La parte delicada es evitar ciclos. Yo lo dejaría así:

```text
api-base
  ↑
api-auth
  ↑
api-rest

api-auth
  ↑
platform/security

api-base, api-core, api-auth, api-rest, api-storage, platform/security
  ↑
platform/application
```

En Maven, sería algo así:

```text
backend/api-auth
  depende de:
  - api-base
  - spring-context
  - spring-tx
  - spring-boot-starter-validation
  - biblioteca JWT
  - bcrypt / spring-security-crypto, si decides usar BCryptPasswordEncoder

backend/api-rest
  depende de:
  - api-core
  - api-auth

platform/security
  depende de:
  - api-auth
  - spring-boot-starter-security
  - spring-boot-starter-webmvc

platform/application
  depende de:
  - api-base
  - api-core
  - api-auth
  - api-rest
  - api-storage
  - platform/security
```

Ojo: si usas `BCryptPasswordEncoder`, no necesariamente tienes que meter todo `spring-boot-starter-security` en `api-auth`. Podrías depender solo de:

```xml
spring-security-crypto
```

Eso mantiene `api-auth` lejos de `HttpSecurity`, filtros y configuración web.

**Flujo Ideal**

Login:

```text
POST /auth/login
   ↓
api-rest/AuthController
   ↓
api-auth/AuthenticationProcessor
   ↓
api-base/UsuarioRepository
   ↓
api-auth/PasswordHashService
   ↓
api-auth/JwtIssuerService
   ↓
LoginOutput(accessToken, refreshToken, user, roles)
```

Request protegida:

```text
GET /usuarios
   ↓
platform/security/JwtAuthenticationFilter
   ↓
platform/security valida access token usando contrato de api-auth
   ↓
platform/security pobla SecurityContext
   ↓
api-rest
   ↓
api-core
   ↓
api-base
```

La clave es esta separación:

```text
api-auth genera y entiende el token como parte del login.
platform/security valida el token para dejar pasar la request.
```

**Contrato Recomendado Entre `api-auth` Y `platform/security`**

Para que `platform/security` no conozca detalles internos de JWT, `api-auth` puede exponer un contrato simple:

```java
public interface AccessTokenValidator {
    boolean isValid(String token);
    AuthPrincipal parse(String token);
}
```

Entonces el filtro de plataforma solo hace esto:

```java
String token = extractBearerToken(request);

if (accessTokenValidator.isValid(token)) {
    AuthPrincipal principal = accessTokenValidator.parse(token);
    // Adaptar AuthPrincipal a Authentication de Spring Security
}
```

Ese filtro no debería saber:

```text
- cómo se firma el JWT
- qué algoritmo usa
- cómo se refresca
- cómo se generan claims
- cómo se valida contraseña
- cómo se consulta usuario
```

**Nombres Que Yo Usaría**

Evitaría nombres genéricos como `SecurityService`, porque vuelven a mezclar conceptos.

Mejor:

```text
AuthenticationProcessor
CredentialService
PasswordHashService
TokenIssuer
AccessTokenValidator
RefreshTokenService
AuthenticatedUser
AuthPrincipal
```

Y para plataforma:

```text
JwtAuthenticationFilter
SecurityConfig
CorsConfiguration
SpringSecurityAuthenticationAdapter
```

**Qué Haría Con `api-core`**

Yo sacaría de `api-core` lo que sea estrictamente autenticación. `api-core` debería quedarse con lógica de negocio general: aspirantes, programas, documentos, entrevistas, cohortes, etc.

`api-auth` sería un módulo de negocio especializado, no infraestructura.

Es decir:

```text
api-core = casos de uso del sistema académico
api-auth = casos de uso de identidad y autenticación
```

Eso es más claro que meter autenticación en `api-core`, porque login, password, token y roles son un subdominio suficientemente importante.

**Regla Mental Final**

Te propongo esta regla para documentarla en el proyecto:

```text
Si toca HttpSecurity, filtros, CORS o SecurityContext → platform/security.

Si toca usuario, contraseña, login, refresh token, claims o roles de negocio → backend/api-auth.

Si toca endpoints HTTP → api-rest.

Si toca entidades/repositorios → api-base.
```

**Conclusión**

Sí, propondría un **nuevo módulo Maven real**:

```text
backend/api-auth
```

No como carpeta dentro de `api-core`, ni como otra cosa dentro de `platform/security`.

Ese módulo sería el dueño de la identidad y autenticación del sistema. `platform/security` quedaría reducido a infraestructura HTTP. Así corriges el desastre de Copilot sin romper tu arquitectura modular y sin convertir Spring Security en el centro de tu lógica de negocio.