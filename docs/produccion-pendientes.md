# Camino a producción — pendientes

Qué falta para salir a producción con una primera versión pública del Foro de Cultivadores.
Priorizado por: 🔴 bloqueante · 🟡 importante (recomendado antes de abrir) · 🟢 puede esperar.

> Última revisión: 2026-10-27.

## Ya resuelto (contexto)

- Funcionalidad del foro completa (posts: consulta/artículo/guía con editor por bloques, comentarios,
  votos, follows, guardados, notificaciones, moderación, perfiles/growshops, búsqueda, filtros).
- **Fase A** — Auth por **cookie de sesión** httpOnly (reemplazó HTTP Basic).
- **Fase B** — **Verificación de email** (gate duro: no se puede loguear sin verificar) +
  **recuperación de contraseña**. Infra de email con Resend (prod) o log por consola (dev).
- **Fase C** — Consentimiento en el registro (18 + T&C/Privacidad), páginas legales
  (`/terminos`, `/privacidad`, `/normas`) y **baja de cuenta** self-service con anonimización.
- Tests backend en verde (241) y front compilando/lint OK.

Falta la **Fase D** (hardening de seguridad), el **deploy** y un par de **bloqueantes no obvios**
(ver abajo).

---

## 🔴 Bloqueantes (sin esto no se puede abrir al público)

1. **Hosting + dominio + HTTPS.** Decisión madre, pendiente. Define storage de imágenes, secrets,
   TLS y cómo se sirve el front. Opciones analizadas: VPS + Docker + Caddy (disco persistente,
   same-origin para la cookie) vs PaaS (Railway/Render, requiere object storage) vs cloud grande.

2. **⚠️ El email tiene que funcionar en prod — es crítico por el gate de login.**
   Como el login está bloqueado hasta verificar el email, si Resend no está configurado con un
   **dominio verificado**, **nadie puede registrarse y entrar**. No hay launch sin email andando.
   Requiere: cuenta Resend + API key (`FORUM_EMAIL_RESEND_API_KEY`) + **dominio verificado** (DNS
   SPF/DKIM) + `EMAIL_FROM` con ese dominio + `FRONTEND_BASE_URL` apuntando al front real.

3. **⚠️ Bootstrap de datos iniciales (hueco silencioso).**
   `UserSeeder` y `CategorySeeder` son **solo perfil `dev`** → en prod la base arranca **sin usuarios
   y sin categorías**. Problemas:
   - Sin categorías → **nadie puede publicar** (el form exige elegir 1–4 categorías).
   - Sin admin → **nadie puede crear categorías** desde `/admin` ni promover a nadie.
   Hay que resolver el arranque de prod: sembrar **categorías base** + crear el **primer ADMIN**
   (opciones: una migración/seeder de prod gateado por env var, o pasos manuales documentados sobre
   la base). Es chico pero bloqueante. **Es código y NO depende del hosting → se puede hacer ya.**

4. **Storage de imágenes.** Hoy es disco local (`forum.storage.local.base-path = ./data/uploads`).
   - VPS: alcanza con un volumen persistente.
   - PaaS: disco efímero → se pierden las imágenes en cada deploy → hace falta un adaptador de
     `ImageStorage` para **object storage (S3/R2)**. La interfaz ya está abstraída (solo sumar impl).

5. **Base de datos de producción** (Postgres gestionado o propio) + **backups**.

6. **Secrets/entorno sin defaults**: credenciales de DB, `COOKIE_SECURE=true` (requiere HTTPS),
   `FORUM_EMAIL_RESEND_API_KEY`, `EMAIL_FROM`, `FRONTEND_BASE_URL`.

7. **Completar placeholders legales**: `[nombre del responsable]`, `[email de contacto]`,
   `[localidad, Argentina]` en `TermsPage`/`PrivacyPage`. Idealmente revisión legal del texto
   (los borradores son plantilla).

8. **Build y serving del frontend.** Hoy corre con `npm run dev` + proxy de Vite. En prod: compilar
   el front y servirlo + reverse proxy de `/api` **al mismo origen** (clave para la cookie de sesión).

---

## 🟡 Importante — Fase D (hardening, muy recomendado antes de abrir)

- **Rate-limiting** en `/auth/login`, `/auth/register`, `/auth/forgot-password`,
  `/auth/reset-password`, `/auth/resend-verification` (fuerza bruta y abuso). No depende del hosting.
- **Captcha en el registro** (Cloudflare Turnstile / hCaptcha). Doble importancia: cada alta dispara
  un email → un bot puede **quemar la cuota de Resend**.
- **Headers de seguridad**: HSTS, CSP, X-Content-Type-Options, Referrer-Policy, etc.
- **Swagger/OpenAPI apagado en prod** (`springdoc.*.enabled=false`); hoy está habilitado por defecto.
- **Backups automáticos** de Postgres (y de las imágenes).
- Revisar **CORS**: con front servido en el mismo origen (proxy) no hace falta abrir CORS; si el front
  queda en otro origen, acotarlo al dominio real.

La Fase D (rate-limit, captcha, headers, Swagger off) es **código y NO depende del hosting**.

---

## 🟢 Puede esperar (post-launch)

- Monitoreo / error tracking (Sentry o similar) + logging estructurado.
- Sesiones multi-instancia: hoy la sesión es **en memoria** (sirve para una sola instancia); para
  escalar horizontalmente, Spring Session con Postgres/Redis.
- Code-splitting del bundle del front (hoy ~550KB, warning menor).
- Validación manual end-to-end en navegador de todos los flujos (quedó pendiente en varias fases).
- Enlaces internos dentro de los modales legales del registro (si se clickea un link cruzado, navega
  a la página completa y saca del registro) — mejora de borde.

---

## Orden recomendado

1. **Decidir hosting** (VPS vs PaaS) → destraba storage de imágenes, secrets, TLS y serving del front.
2. En paralelo, **sin depender del hosting** (se puede hacer ya):
   - **Bootstrap de prod** (categorías base + primer admin). ← bloqueante menos obvio.
   - **Fase D** (rate-limit, captcha, headers, Swagger off).
3. **Resend + dominio verificado** + completar **placeholders legales**.
4. **Deploy** y **validación en navegador**.

## Variables de entorno de producción (resumen)

| Variable | Para qué | Nota |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a Postgres | Sin defaults en prod |
| `COOKIE_SECURE` | Cookie de sesión `Secure` | `true` (requiere HTTPS) |
| `FORUM_EMAIL_RESEND_API_KEY` | Activa Resend (si falta, loguea por consola) | Secreto |
| `EMAIL_FROM` | Remitente de los emails | Dominio verificado en Resend |
| `FRONTEND_BASE_URL` | Base para los links de los emails | URL real del front |
