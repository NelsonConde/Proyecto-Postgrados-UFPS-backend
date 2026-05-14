# CONTEXTO DEL PROYECTO — BACKEND SPRING BOOT MODULAR (UFPS POSGRADOS)

## 🧩 Tipo de sistema

Proyecto backend desarrollado en **Java 21 + Spring Boot (4.x)** bajo arquitectura **monolítica modular (Maven multi-módulo)**.

NO es microservicios. Todos los módulos se ensamblan en un único artefacto ejecutable.

---

## 🏗️ Estructura de alto nivel

```
root (pom packaging)
├── backend/
│   ├── api-base        → persistencia (JPA, entidades, repositorios, services CRUD)
│   ├── api-core        → lógica de negocio (processors, mappers, dominio)
│   ├── api-rest        → controladores HTTP (REST API)
│   └── api-storage     → integración con AWS S3
│
├── platform/
│   ├── application     → entry point (@SpringBootApplication)
│   └── security        → configuración de seguridad (infraestructura)
```

---

## 🔄 Flujo de ejecución

```
HTTP Request
   ↓
[platform/security] ← filtros, seguridad HTTP (NO lógica de negocio)
   ↓
[api-rest]          ← controllers
   ↓
[api-core]          ← processors (lógica de negocio)
   ↓
[api-base]          ← services + repositories
   ↓
Base de datos (MySQL)
```

---

## 🧠 Responsabilidad de cada módulo

### 🔹 backend/api-base

* Entidades JPA (`@Entity`)
* Repositorios (`JpaRepository`)
* Servicios CRUD (con ModelMapper)
* Acceso a base de datos

---

### 🔹 backend/api-core

* Lógica de negocio
* `Processor` (casos de uso)
* `Mapper` (Input → DTO → Output)
* Excepciones de dominio (`DomainException`)
* Validadores

---

### 🔹 backend/api-rest

* `@RestController`
* Expone endpoints HTTP
* NO contiene lógica de negocio
* Delega a `api-core`

---

### 🔹 backend/api-storage

* Integración con AWS S3
* Servicios de documentos
* ⚠️ Actualmente tiene una mala práctica: depende de `api-rest` (esto debe evitarse)

---

### 🔹 platform/application

* Clase `@SpringBootApplication`
* Punto de entrada
* Ensambla TODOS los módulos
* Contiene configuración global (properties, swagger)

---

### 🔹 platform/security (IMPORTANTE)

Este módulo es **infraestructura de seguridad**, NO lógica de negocio.

Contiene:

* `SecurityFilterChain`
* Configuración CORS
* Filtros HTTP (futuro JWT filter)
* Integración con Spring Security

NO debe contener:

* lógica de login
* validación de credenciales
* acceso a usuarios
* reglas de negocio

---

## ⚠️ REGLA CRÍTICA — SEPARACIÓN DE SEGURIDAD

Existen DOS tipos de seguridad en este sistema:

### 1. Seguridad de plataforma (platform/security)

Responsabilidad:

* interceptar requests
* validar tokens (JWT)
* permitir o bloquear acceso
* poblar SecurityContext

Depende de Spring Security.

---

### 2. Seguridad de dominio (backend/api-core)

Responsabilidad:

* login (username/password)
* validación de credenciales
* generación de JWT
* reglas de autenticación
* manejo de usuarios y roles

NO depende de Spring Security.

---

## ❗ REGLA FUNDAMENTAL

* `platform/security` → NO contiene lógica de negocio
* `backend/api-core` → NO depende de Spring Security

---

## 🔗 Dependencias entre módulos

```
api-base → (ninguna)
api-core → api-base
api-rest → api-core
api-storage → api-core   (idealmente, NO api-rest)

platform/security → (opcional) api-core o api-base
platform/application → TODOS
```

---

## ⚙️ Tecnologías usadas

* Spring Boot
* Spring MVC
* Spring Data JPA
* ModelMapper
* MySQL
* AWS S3
* Maven multi-módulo
* Lombok

---

## 📌 Estado actual de seguridad

* Existe configuración básica con `SecurityFilterChain`
* Swagger está permitido sin autenticación
* Resto de endpoints requieren autenticación
* Actualmente se usa (temporalmente) HTTP Basic o configuración mínima
* JWT aún no está completamente implementado
* Contraseñas están en texto plano (pendiente de mejora)

---

## 🚫 ERRORES A EVITAR (MUY IMPORTANTE)

NO hacer:

* implementar login dentro de `platform/security`
* acceder a base de datos desde filtros de seguridad directamente
* mezclar JWT generation con configuración HTTP
* hacer que `api-core` dependa de Spring Security

---

## 🎯 OBJETIVO DE LA IA

Cuando generes código:

1. Mantén la separación entre:

   * infraestructura (platform)
   * lógica de negocio (backend)

2. Si implementas autenticación:

   * login → `api-core`
   * JWT generation → `api-core`
   * JWT validation → `platform/security`

3. No rompas la arquitectura modular

4. No introduzcas dependencias circulares

---

## 🧠 RESUMEN CLAVE

* platform/security = "¿puede pasar esta request?"
* backend/api-core = "¿quién es el usuario?"

NO mezclar ambos conceptos.

---

## 📎 NOTA FINAL

El sistema sigue una arquitectura por capas modularizada.
Cualquier implementación debe respetar:

* separación de responsabilidades
* independencia del dominio
* bajo acoplamiento entre módulos

---
