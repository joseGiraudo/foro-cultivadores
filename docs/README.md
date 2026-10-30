# Foro de Cultivadores

Foro/comunidad para cultivadores: los usuarios publican **consultas, artículos y guías**, votan,
comentan en hilos, siguen a otros (growers/growshops), guardan posts y reciben notificaciones. Hay
moderación, perfiles con datos de comercio para growshops, búsqueda y filtros.

> **Este README es el punto de entrada.** Para el detalle, ver el [índice](#documentación) al final.
> Última actualización: 2026-10-30.

## Qué hace hoy

- **Publicaciones** de tres tipos: **Consulta** (texto plano + fotos en galería), **Artículo** y
  **Guía** (editor visual por bloques, Markdown por debajo). Edición, estados y etiqueta "editado".
- **Comentarios** anidados (hilos), con comentario fijado por el autor del post.
- **Votos** en posts y comentarios (orden por score o cronológico según el tipo de post).
- **Follows**, **guardados** (bookmarks) y **notificaciones in-app**.
- **Perfiles**: bio en Markdown, avatar y portada; los **growshops** (CONTRIBUTOR+) suman datos de
  comercio (web, WhatsApp, Instagram, ubicación). Peticiones self-service para convertirse en growshop.
- **Feed** con pestañas Todo/Siguiendo, **búsqueda full-text** y filtros por tipo y categorías.
- **Moderación** (ocultar/eliminar contenido, suspender usuarios) y **administración** (roles,
  categorías).
- **Cuentas**: registro con **verificación de email** (obligatoria para entrar), recuperación de
  contraseña, consentimiento legal (18 + T&C/Privacidad) y baja de cuenta con anonimización.

## Stack

- **Backend** (`backend/`): Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation),
  PostgreSQL 16, Flyway, springdoc-openapi (Swagger), Maven (`mvnw`). Tests con JUnit 5 + MockMvc +
  Testcontainers. Auth por **cookie de sesión** httpOnly. Email vía Resend; imágenes en disco local o
  Cloudflare R2 (configurable).
- **Frontend** (`frontend/`): React 19 + Vite + TypeScript, React Router, TanStack Query,
  Tailwind CSS v4, react-markdown + rehype-sanitize. Linter: oxlint.

## Layout del repo

```
foro-cultivadores/
├── backend/            # Spring Boot (monolito modular, Maven)
├── frontend/           # SPA React + Vite
├── docs/               # esta documentación
├── docker-compose.yml  # Postgres para desarrollo
└── data/               # imágenes subidas en dev (git-ignored)
```

## Cómo correr en dev

```bash
# 1) Base de datos (desde la raíz del repo)
docker compose up -d                                  # Postgres en :5432

# 2) Backend — desde backend/ (perfil dev: siembra datos de ejemplo)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run     # :8080 · Swagger en /swagger-ui.html

# 3) Frontend — desde frontend/
npm install && npm run dev                            # :5173 · proxea /api → :8080
```

En Windows (PowerShell): `$env:SPRING_PROFILES_ACTIVE="dev"` y después `./mvnw.cmd spring-boot:run`.
El perfil `dev` siembra usuarios y contenido de ejemplo; las credenciales están en
[`cuentas-prueba.md`](cuentas-prueba.md).

## Documentación

| Doc | Para qué |
|---|---|
| [`arquitectura.md`](arquitectura.md) | Cómo está construido: módulos, patrones, auth, storage, reglas, seguridad |
| [`decisiones.md`](decisiones.md) | Log de decisiones técnicas y de negocio del MVP (el "por qué") |
| [`produccion-pendientes.md`](produccion-pendientes.md) | Camino a producción: qué falta, variables de entorno, orden |
| [`mejoras-futuras.md`](mejoras-futuras.md) | Ideas de mejora post-launch (no bloqueantes) |
| [`cuentas-prueba.md`](cuentas-prueba.md) | Usuarios del seed para loguearse en dev |
| [`mvp-spec.md`](mvp-spec.md) · [`frontend-spec.md`](frontend-spec.md) | **Histórico**: specs originales del MVP (sept 2026), útiles como referencia del diseño base. El estado actual está en los docs de arriba |
