Necesito que refactorices e implementes correctamente la autenticación JWT del proyecto respetando la arquitectura modular Maven existente.

CONTEXTO DEL PROYECTO

Este proyecto es un backend Spring Boot modular, NO microservicios. Es un monolito modular Maven multi-módulo con Java 21 y Spring Boot 4.x.

Estructura actual:

root
├── backend/
│   ├── api-base        -> entidades JPA, repositorios, servicios CRUD
│   ├── api-core        -> lógica de negocio general, processors, mappers, validadores
│   ├── api-rest        -> controladores REST
│   └── api-storage     -> integración S3
│
└── platform/
    ├── application     -> entrypoint @SpringBootApplication
    └── security        -> seguridad HTTP de plataforma: SecurityFilterChain, CORS, filtros

OBJETIVO

Crear un nuevo módulo Maven real e independiente:

backend/api-auth

Este módulo debe encargarse de autenticación, identidad, credenciales, password hashing y emisión/validación funcional de tokens JWT.

NO debe ser una carpeta dentro de api-core.
NO debe vivir dentro de platform/security.
Debe ser un módulo Maven al mismo nivel de api-base, api-core, api-rest y api-storage.

RESPONSABILIDADES

backend/api-auth debe encargarse de:

- login
- refresh token
- validación de credenciales
- verificación y codificación de contraseñas
- generación de JWT
- validación semántica de JWT
- parsing de claims
- definición del principal autenticado
- reglas de autenticación de usuario
- obtención de usuario, rol y clave desde api-base

platform/security debe encargarse solamente de:

- SecurityFilterChain
- CORS
- CSRF
- configuración stateless
- filtros HTTP
- extracción del Bearer token desde Authorization header
- poblar SecurityContext
- reglas HTTP de acceso por endpoint

api-rest debe encargarse solamente de:

- exponer endpoints HTTP
- AuthController
- delegar al módulo api-auth
- devolver ResponseEntity

api-core NO debe contener lógica de login ni contratos de autenticación.

REGLAS ARQUITECTÓNICAS OBLIGATORIAS

1. No mezclar lógica de login dentro de platform/security.
2. No consultar UsuarioRepository desde filtros de Spring Security.
3. No generar JWT dentro de SecurityConfig ni JwtAuthenticationFilter.
4. No poner PasswordVerifier en platform/security.
5. api-core no debe depender de Spring Security.
6. platform/security puede depender de api-auth para validar tokens.
7. api-auth puede depender de api-base para leer UsuarioEntity, RolEntity y ClaveEntity.
8. api-rest puede depender de api-auth para usar AuthenticationProcessor/AuthService.
9. Evitar dependencias circulares.
10. El rol de base de datos llamado exactamente "Super Administrador" debe mapearse a la authority Spring Security "ROLE_SUPER_ADMINISTRADOR".

DEPENDENCIAS MAVEN

Actualizar el pom raíz para agregar el nuevo módulo:

<module>backend/api-auth</module>

Orden sugerido:

backend/api-base
backend/api-core
backend/api-auth
backend/api-rest
backend/api-storage
platform/application
platform/security

Crear backend/api-auth/pom.xml con dependencias:

- api-base
- spring-context
- spring-tx
- spring-boot-starter-validation
- spring-security-crypto
- jjwt-api
- jjwt-impl
- jjwt-jackson

Agregar versión de JJWT en properties del pom raíz:

<jjwt.version>0.12.6</jjwt.version>

Dependencias JJWT sugeridas:

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>

Actualizar backend/api-rest/pom.xml:

Debe depender de:
- api-core
- api-auth
- spring-boot-starter-webmvc

Actualizar platform/security/pom.xml:

Debe depender de:
- api-auth
- spring-boot-starter-security
- spring-boot-starter-webmvc

Debe dejar de depender de api-core salvo que sea estrictamente necesario. Idealmente platform/security NO debe depender de api-core.

Actualizar platform/application/pom.xml:

Debe depender de:
- api-base
- api-core
- api-auth
- api-rest
- api-storage
- platform/security

ESTRUCTURA DEL NUEVO MÓDULO

Crear esta estructura:

backend/api-auth
└── src/main/java/ufps/edu/co/auth/
    ├── config/
    │   ├── ApiAuthModuleStartupCheck.java
    │   └── AuthProperties.java
    │
    ├── controller/                      NO USAR, los controllers van en api-rest
    │
    ├── contract/
    │   ├── AccessTokenValidator.java
    │   ├── AuthenticationUseCase.java
    │   ├── PasswordHashService.java
    │   ├── RefreshTokenUseCase.java
    │   └── TokenIssuer.java
    │
    ├── exception/
    │   ├── AuthDomainException.java
    │   ├── InvalidCredentialsException.java
    │   ├── InvalidTokenException.java
    │   └── MissingCredentialsException.java
    │
    ├── model/
    │   ├── AuthPrincipal.java
    │   ├── AuthenticatedUser.java
    │   ├── AuthRole.java
    │   └── TokenType.java
    │
    ├── processor/
    │   ├── AuthenticationProcessor.java
    │   └── RefreshTokenProcessor.java
    │
    ├── records/
    │   ├── input/
    │   │   ├── LoginInput.java
    │   │   └── RefreshTokenInput.java
    │   └── output/
    │       └── LoginOutput.java
    │
    ├── service/
    │   ├── BCryptPasswordHashService.java
    │   ├── CredentialService.java
    │   ├── JwtTokenService.java
    │   └── RoleNameNormalizer.java
    │
    └── util/
        └── BearerTokenConstants.java

CLASES Y CONTRATOS ESPERADOS

1. AuthPrincipal

Debe representar al usuario autenticado sin depender de Spring Security.

Campos sugeridos:

- Integer userId
- String username
- List<String> roles

Los roles deben venir normalizados para uso con Spring Security, por ejemplo:

"Super Administrador" -> "SUPER_ADMINISTRADOR"

No incluir el prefijo ROLE_ dentro del modelo de dominio.
El prefijo ROLE_ solo debe agregarse en platform/security al crear SimpleGrantedAuthority.

2. LoginInput

Debe estar en api-auth:

package ufps.edu.co.auth.records.input;

public record LoginInput(
    @NotBlank String username,
    @NotBlank String password
) {}

3. LoginOutput

Debe estar en api-auth:

package ufps.edu.co.auth.records.output;

public record LoginOutput(
    String accessToken,
    String refreshToken,
    Integer userId,
    String username,
    List<String> roles
) {}

4. AuthenticationUseCase

package ufps.edu.co.auth.contract;

public interface AuthenticationUseCase {
    LoginOutput login(LoginInput input);
}

5. RefreshTokenUseCase

public interface RefreshTokenUseCase {
    LoginOutput refresh(String refreshToken);
}

6. PasswordHashService

public interface PasswordHashService {
    boolean matches(String rawPassword, String storedPassword);
    String encode(String rawPassword);
    boolean needsRehash(String storedPassword);
}

7. TokenIssuer

public interface TokenIssuer {
    LoginOutput issueTokens(AuthPrincipal principal);
    LoginOutput refreshTokens(String refreshToken);
}

8. AccessTokenValidator

Este contrato será usado por platform/security.

public interface AccessTokenValidator {
    boolean isValid(String accessToken);
    AuthPrincipal parse(String accessToken);
}

IMPLEMENTACIÓN DE AUTENTICACIÓN

AuthenticationProcessor debe vivir en api-auth, no en api-core.

Debe:

1. Validar que username y password no sean null ni blank.
2. Buscar UsuarioEntity usando UsuarioRepository.findByNombreusuario(username).
3. Si no existe, lanzar InvalidCredentialsException.
4. Obtener la clave desde usuario.getClave().getValor().
5. Validar contraseña usando PasswordHashService.
6. Si no coincide, lanzar InvalidCredentialsException.
7. Obtener el rol desde usuario.getRol().getNombre().
8. Normalizar el rol con RoleNameNormalizer.
9. Crear AuthPrincipal.
10. Emitir tokens usando TokenIssuer.
11. Retornar LoginOutput.

No usar javax.security.sasl.AuthenticationException.
Crear excepciones propias del dominio auth.

PASSWORD HASHING

Implementar BCryptPasswordHashService usando BCryptPasswordEncoder desde spring-security-crypto.

Regla principal:

- Nuevas contraseñas deben guardarse con BCrypt.
- La comparación principal debe usar BCrypt.

Como actualmente el proyecto puede tener contraseñas en texto plano, implementar compatibilidad temporal controlada por property:

app.auth.password.allow-plain-text-legacy=true

Si esa propiedad está en true:
- Si storedPassword NO parece BCrypt, permitir comparación rawPassword.equals(storedPassword).
- Esta compatibilidad debe estar claramente aislada en BCryptPasswordHashService.
- No dispersar esta lógica en processors ni controllers.

Si está en false:
- Solo BCrypt.

Agregar método needsRehash(String storedPassword):
- true si storedPassword no es BCrypt.
- false si ya es BCrypt.

No implementar todavía actualización automática de clave en login salvo que sea simple y segura. Si se implementa, hacerlo dentro de CredentialService, no en controller ni platform/security.

JWT

Implementar JwtTokenService en api-auth.

Debe implementar:

- TokenIssuer
- AccessTokenValidator

Debe generar dos tokens:

- ACCESS
- REFRESH

Claims mínimos:

- sub: username
- uid: userId
- roles: lista normalizada, ejemplo ["SUPER_ADMINISTRADOR"]
- type: ACCESS o REFRESH
- iat
- exp

Propiedades:

app.jwt.secret
app.jwt.access-expiration-minutes
app.jwt.refresh-expiration-minutes
app.jwt.issuer

Defaults solo para desarrollo:

app.jwt.secret=dev-secret-change-me-must-be-long
app.jwt.access-expiration-minutes=120
app.jwt.refresh-expiration-minutes=10080
app.jwt.issuer=ufps-posgrados

IMPORTANTE:

- Validar firma.
- Validar expiración.
- Validar issuer si se configura.
- Validar type.
- refreshTokens solo acepta token type REFRESH.
- parse de access token solo acepta token type ACCESS.
- Si el token es inválido, lanzar InvalidTokenException o retornar false en isValid.

No hacer JWT manual con Base64 + Mac propio.
Usar JJWT.

NORMALIZACIÓN DE ROLES

Crear RoleNameNormalizer.

Debe convertir:

"Super Administrador" -> "SUPER_ADMINISTRADOR"
"SUPER ADMINISTRADOR" -> "SUPER_ADMINISTRADOR"
"super administrador" -> "SUPER_ADMINISTRADOR"

Reglas:

- trim
- convertir espacios a underscore
- convertir guiones a underscore
- remover dobles underscores
- uppercase
- idealmente remover tildes si aparecen

Ejemplo:

public String normalize(String roleName)

El JWT debe guardar roles ya normalizados, sin prefijo ROLE_.

En platform/security, al construir authorities:

new SimpleGrantedAuthority("ROLE_" + role)

Así "SUPER_ADMINISTRADOR" se convierte en "ROLE_SUPER_ADMINISTRADOR".

LIMPIEZA DE CÓDIGO EXISTENTE

Mover o reemplazar estas piezas:

1. backend/api-core/src/main/java/ufps/edu/co/processor/cases/AuthenticationProcessor.java

Eliminarlo de api-core o reemplazarlo por nada. La autenticación ahora vive en api-auth.

2. backend/api-core/src/main/java/ufps/edu/co/processor/abstracts/contract/LoginProcessor.java

Mover concepto a api-auth como AuthenticationUseCase.

3. backend/api-core/src/main/java/ufps/edu/co/processor/abstracts/contract/LoginTokenProvider.java

Mover concepto a api-auth como TokenIssuer y AccessTokenValidator.

4. backend/api-core/src/main/java/ufps/edu/co/processor/abstracts/contract/LoginPrincipal.java

Mover concepto a api-auth como AuthPrincipal.

5. backend/api-core/src/main/java/ufps/edu/co/processor/abstracts/contract/PasswordVerifier.java

Mover concepto a api-auth como PasswordHashService.

6. backend/api-core/src/main/java/ufps/edu/co/records/input/usecase/LoginInput.java

Mover a api-auth.records.input.LoginInput.

7. backend/api-core/src/main/java/ufps/edu/co/records/output/usecase/LoginOutput.java

Mover a api-auth.records.output.LoginOutput.

8. platform/security/src/main/java/ufps/edu/co/security/JwtTokenProvider.java

Eliminar de platform/security.
Recrear su responsabilidad correctamente en api-auth/service/JwtTokenService.java usando JJWT.

9. platform/security/src/main/java/ufps/edu/co/security/DefaultPasswordVerifier.java

Eliminar de platform/security.
Recrear en api-auth/service/BCryptPasswordHashService.java.

AUTH CONTROLLER

Actualizar:

backend/api-rest/src/main/java/ufps/edu/co/controllers/auth/AuthController.java

Debe importar desde api-auth, no desde api-core.

Debe inyectar:

AuthenticationUseCase authenticationUseCase
RefreshTokenUseCase refreshTokenUseCase

Endpoints:

POST /auth/login

Body:
{
  "username": "admin",
  "password": "..."
}

Response 200:
{
  "accessToken": "...",
  "refreshToken": "...",
  "userId": 1,
  "username": "admin",
  "roles": ["SUPER_ADMINISTRADOR"]
}

POST /auth/refresh

Puede recibir refresh token por Authorization Bearer como ya está, o por body RefreshTokenInput.
Preferencia: mantener Authorization Bearer para no romper demasiado el flujo actual.

Errores:
- credenciales inválidas -> 401
- token inválido -> 401
- body inválido -> 400

No exponer mensajes sensibles como "usuario no existe" o "contraseña incorrecta".
Responder genéricamente.

PLATFORM SECURITY

Actualizar platform/security.

SecurityConfig debe:

- ser stateless
- deshabilitar csrf
- mantener CORS
- permitir OPTIONS
- permitir Swagger
- permitir /auth/login
- permitir /auth/refresh
- exigir rol SUPER_ADMINISTRADOR para endpoints CRUD
- negar lo demás o exigir autenticación según corresponda

IMPORTANTE SOBRE CONTEXT PATH

El proyecto parece usar context path /posgrados-project.
No hardcodear innecesariamente el context path si puede evitarse.
Preferir matchers relativos:

/auth/login
/auth/refresh
/swagger-ui/**
/v3/api-docs/**
/swagger-ui.html

Si por configuración del proyecto los matchers requieren /posgrados-project, conservar compatibilidad, pero evitar duplicar reglas de forma frágil.

REGLA DE ACCESO CRUD

El rol existente en BD se llama exactamente:

Super Administrador

Este rol debe tener acceso a TODOS los endpoints CRUD del paquete:

backend/api-rest/src/main/java/ufps/edu/co/controllers/rest

Ningún otro rol debe poder acceder a esos endpoints CRUD.

Los controllers CRUD actuales usan rutas como:

/administrativo/**
/aspirante/**
/cambiodocumento/**
/cargo/**
/clave/**
/cohorte/**
/departamento/**
/documento/**
/entrevista/**
/entrevistador/**
/entrevistadores/**
/estadodocumento/**
/estado/**
/facultad/**
/genero/**
/jornada/**
/modalidad/**
/municipio/**
/ofertaacademica/**
/otrosvalores/**
/pais/**
/persona/**
/plazo/**
/programa/**
/rol/**
/sede/**
/sedes/**
/tipodocumento/**
/tipoentrevista/**
/tipoplazo/**
/ubicacion/**
/usuario/**

Todos esos endpoints deben requerir:

hasRole("SUPER_ADMINISTRADOR")

Recordatorio:
hasRole("SUPER_ADMINISTRADOR") espera authority "ROLE_SUPER_ADMINISTRADOR".

JwtAuthenticationFilter debe:

1. Extender OncePerRequestFilter.
2. Ignorar:
   - /auth/**
   - /swagger-ui/**
   - /v3/api-docs/**
   - OPTIONS
3. Extraer Authorization header.
4. Si no hay Bearer token, continuar sin autenticar.
5. Si hay Bearer token inválido, limpiar SecurityContext y dejar que Spring Security responda 401.
6. Si hay Bearer token válido:
   - usar AccessTokenValidator de api-auth
   - parsear AuthPrincipal
   - convertir roles a SimpleGrantedAuthority con prefijo ROLE_
   - crear UsernamePasswordAuthenticationToken
   - poblar SecurityContextHolder

El filtro NO debe:
- consultar BD
- validar passwords
- generar tokens
- refrescar tokens
- conocer UsuarioEntity
- conocer ClaveEntity
- conocer RolEntity

CLASES EN platform/security

Dejar o crear únicamente algo similar a:

platform/security/src/main/java/ufps/edu/co/config/SecurityConfig.java
platform/security/src/main/java/ufps/edu/co/security/JwtAuthenticationFilter.java
platform/security/src/main/java/ufps/edu/co/security/SpringSecurityPrincipalAdapter.java
platform/security/src/main/java/ufps/edu/co/security/CorsConfiguration.java
platform/security/src/main/java/ufps/edu/co/build/SpringSecurityModuleStartupCheck.java

No dejar JwtTokenProvider ni DefaultPasswordVerifier en platform/security.

PROPERTIES

Agregar properties documentadas en application.properties del módulo application o donde corresponda centralmente:

app.jwt.secret=${JWT_SECRET:dev-secret-change-me-must-be-long}
app.jwt.issuer=ufps-posgrados
app.jwt.access-expiration-minutes=120
app.jwt.refresh-expiration-minutes=10080
app.auth.password.allow-plain-text-legacy=true

No dejar secretos reales hardcodeados.

PAQUETES

Usar paquetes consistentes:

api-auth:
ufps.edu.co.auth.*

api-rest:
ufps.edu.co.controllers.auth

platform/security:
ufps.edu.co.security
ufps.edu.co.config

api-base:
mantener como está.

TESTS MÍNIMOS

Agregar tests unitarios si el proyecto ya tiene estructura de tests. Si no existe, al menos dejar la implementación lista para testear.

Casos mínimos deseables:

1. RoleNameNormalizerTest
- "Super Administrador" -> "SUPER_ADMINISTRADOR"
- " super administrador " -> "SUPER_ADMINISTRADOR"

2. BCryptPasswordHashServiceTest
- BCrypt válido debe matchear
- BCrypt inválido no debe matchear
- legacy plain text solo matchea si allow-plain-text-legacy=true

3. JwtTokenServiceTest
- genera access y refresh
- access token parsea AuthPrincipal
- refresh token genera nuevos tokens
- access token no sirve como refresh
- token expirado o mal firmado es inválido

4. SecurityConfig/JwtAuthenticationFilter
- token con rol SUPER_ADMINISTRADOR produce ROLE_SUPER_ADMINISTRADOR
- endpoint CRUD requiere SUPER_ADMINISTRADOR

CRITERIOS DE ACEPTACIÓN

La implementación se considera correcta si:

1. Existe el nuevo módulo Maven backend/api-auth.
2. El pom raíz incluye backend/api-auth.
3. api-auth compila.
4. api-rest usa api-auth para /auth/login y /auth/refresh.
5. platform/security usa api-auth solo para validar/parsear access tokens.
6. platform/security no contiene generación de JWT.
7. platform/security no contiene validación de passwords.
8. api-core ya no contiene AuthenticationProcessor ni contratos Login*.
9. No hay ciclos Maven.
10. El rol de BD "Super Administrador" se convierte en SUPER_ADMINISTRADOR.
11. Spring Security recibe ROLE_SUPER_ADMINISTRADOR.
12. Solo SUPER_ADMINISTRADOR puede acceder a endpoints CRUD.
13. /auth/login, /auth/refresh y Swagger quedan públicos.
14. Contraseñas BCrypt funcionan.
15. Contraseñas legacy en texto plano funcionan temporalmente solo si app.auth.password.allow-plain-text-legacy=true.
16. No se rompen los controllers CRUD existentes.
17. No se mueve lógica REST al módulo api-auth.
18. No se mete lógica de negocio en platform/security.

COMANDOS DE VERIFICACIÓN

Al terminar, ejecutar:

mvn clean compile

Si hay tests:

mvn test

También revisar que no queden imports desde api-core para autenticación:

- LoginProcessor
- LoginPrincipal
- LoginTokenProvider
- PasswordVerifier
- LoginInput
- LoginOutput

Esos conceptos deben estar ahora bajo ufps.edu.co.auth.*

NOTA FINAL

La separación conceptual obligatoria es:

platform/security = "¿puede pasar esta request HTTP?"
backend/api-auth = "¿quién es este usuario y cómo se autentica?"

No sacrifiques esta separación por rapidez.
