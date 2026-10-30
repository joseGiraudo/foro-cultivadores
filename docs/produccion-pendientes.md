# Camino a producción — pendientes

Qué falta para salir a producción con una primera versión pública del Foro de Cultivadores.
Priorizado por: 🔴 bloqueante · 🟡 importante (recomendado antes de abrir) · 🟢 puede esperar.

> Última revisión: 2026-10-30.

## Ya resuelto (contexto)

- Funcionalidad del foro completa (posts: consulta/artículo/guía con editor por bloques, comentarios,
  votos, follows, guardados, notificaciones, moderación, perfiles/growshops, búsqueda, filtros).
- **Fase A** — Auth por **cookie de sesión** httpOnly (reemplazó HTTP Basic).
- **Fase B** — **Verificación de email** (gate duro: no se puede loguear sin verificar) +
  **recuperación de contraseña**. Infra de email con Resend (prod) o log por consola (dev).
- **Fase C** — Consentimiento en el registro (18 + T&C/Privacidad), páginas legales
  (`/terminos`, `/privacidad`, `/normas`) y **baja de cuenta** self-service con anonimización.
- **Bootstrap de prod** — con `FORUM_BOOTSTRAP_ENABLED=true` al arrancar se siembran las categorías
  base y se crea el primer ADMIN (`BOOTSTRAP_ADMIN_*`) si no hay ninguno. Idempotente. (ver abajo)
- Tests backend en verde (243) y front compilando/lint OK.

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

3. **✅ RESUELTO — Bootstrap de datos iniciales.**
   `UserSeeder`/`CategorySeeder` son solo perfil `dev`, así que en prod la base arrancaba sin
   categorías (nadie podía publicar) ni admin (nadie podía crear categorías ni promover). Resuelto
   con un **bootstrap de prod gateado por env var**: con `FORUM_BOOTSTRAP_ENABLED=true`, al iniciar
   corren `CategoryProdSeeder` (siembra las **categorías base** de forma idempotente) y
   `AdminBootstrap` (crea el **primer ADMIN** desde `BOOTSTRAP_ADMIN_USERNAME/-EMAIL/-PASSWORD`, con
   email verificado y términos aceptados, solo si todavía no hay ningún ADMIN). Es idempotente —
   se puede dejar activado, aunque conviene apagarlo tras el primer boot. Tests en
   `BootstrapIntegrationTest`. **Al desplegar: setear esas env vars en el primer arranque.**

4. **✅ RESUELTO (adaptador) — Storage de imágenes.** Ya hay impl para **Cloudflare R2**
   (`R2ImageStorage`, S3-compatible vía AWS SDK v2, path-style). Se elige por config en
   `ImageStorageConfig`: con `R2_ENABLED=true` + las credenciales va a R2; si no, disco local
   (default dev/tests). Probado contra un servidor S3 real (`R2ImageStorageTest` con adobe/s3mock).
   Para prod: crear cuenta Cloudflare + bucket R2 + API token y setear `R2_*` (ver tabla). Con esto el
   storage **deja de depender del hosting** (VPS o PaaS, da igual; incluso se puede migrar de hosting
   sin mover archivos). Alternativa VPS si no se quiere R2: disco local en un volumen persistente.

5. **Base de datos de producción** (Postgres gestionado o propio) + **backups**.

6. **Secrets/entorno sin defaults**: credenciales de DB, `COOKIE_SECURE=true` (requiere HTTPS),
   `FORUM_EMAIL_RESEND_API_KEY`, `EMAIL_FROM`, `FRONTEND_BASE_URL`.

7. **Completar placeholders legales**: `[nombre del responsable]`, `[email de contacto]`,
   `[localidad, Argentina]` en `TermsPage`/`PrivacyPage`. Idealmente revisión legal del texto
   (los borradores son plantilla).

8. **Artefactos de deploy (empaquetar + servir).** Hoy no existe ninguno: el back corre con
   `spring-boot:run` y el front con `npm run dev` + proxy de Vite. Estos artefactos son del **camino
   VPS + Caddy** (si se va a PaaS cambian). Faltan crear:
   - **Dockerfile del backend** — multi-stage: etapa Maven (`mvnw package`) → etapa liviana con JRE 21
     que corre el `.jar`.
   - **Build del front + servido estático** — `npm run build` (`dist/`) servido por Caddy (Dockerfile
     multi-stage node→caddy, o build afuera y montar el `dist/`).
   - **Caddyfile** — HTTPS automático (Let's Encrypt), sirve el `dist/` del front y proxea `/api/*` →
     `backend:8080` **quitando el prefijo `/api`** (igual que el proxy de Vite). Todo **mismo origen**
     → la cookie `SameSite=Strict` funciona.
   - **docker-compose de prod** — Postgres (con **volumen nombrado** para BD + imágenes si es disco
     local) + backend (env vars de prod) + Caddy. Distinto del compose de dev (solo Postgres).
   - **Backup** — cron de `pg_dump` + copia offsite (bucket/otro server).
   - ✅ **Config de proxy ya lista**: `server.forward-headers-strategy` se activa con
     `FORWARD_HEADERS_STRATEGY=framework` (ver tabla) para que detrás de Caddy el backend vea la IP
     real (rate-limit) y el esquema HTTPS. Default `none` (dev/sin proxy).

---

## 🟡 Importante — Fase D (hardening, muy recomendado antes de abrir)

- ✅ **Rate-limiting** en `/auth/login`, `/auth/register`, `/auth/forgot-password`,
  `/auth/reset-password`, `/auth/resend-verification` (fuerza bruta y abuso). `RateLimitFilter`
  in-memory por IP+endpoint, ventana fija configurable (`forum.rate-limit`, default 10/60s), responde
  429 + Retry-After. Apagable con `FORUM_RATE_LIMIT_ENABLED=false`. Test: `RateLimitIntegrationTest`.
- ✅ **Headers de seguridad**: HSTS (1 año, includeSubDomains), Referrer-Policy
  (strict-origin-when-cross-origin), X-Content-Type-Options nosniff y anti-clickjacking
  (X-Frame-Options DENY + CSP `frame-ancestors 'none'`) en `SecurityConfig`. El CSP completo
  (default-src, etc.) corresponde al front que sirve HTML, no a esta API JSON.
- ✅ **Swagger/OpenAPI apagado en prod**: `SPRINGDOC_ENABLED=false` apaga la UI y el `/v3/api-docs`
  (default true por comodidad de dev). **Acordate de ponerlo en false en el deploy.**
- **Captcha en el registro** (Cloudflare Turnstile / hCaptcha) — PENDIENTE. Doble importancia: cada
  alta dispara un email → un bot puede **quemar la cuota de Resend**. Necesita cuenta en el proveedor
  + widget en el front + verificación en el back. Único item de la Fase D que depende de algo externo.
- **Backups automáticos** de Postgres (y de las imágenes) — operacional, va con el hosting.
- **CORS**: decisión acoplada al hosting y a la cookie. Con el front servido en el **mismo origen**
  (reverse proxy de `/api`) **no hace falta CORS** y la cookie `SameSite=Strict` funciona — es la
  opción recomendada. Si el front quedara en **otro origen**, no alcanza con abrir CORS: habría que
  pasar la cookie a `SameSite=None; Secure` + CORS con `allowCredentials` al dominio real. Por eso se
  deja el default seguro (sin CORS, same-origin) y se revisará junto con la decisión de hosting.

La Fase D es **código y NO depende del hosting**, salvo el **captcha** (proveedor externo) y **CORS**
(acoplado al hosting). Hecho: rate-limit, headers, Swagger off.

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
   - ~~**Bootstrap de prod** (categorías base + primer admin).~~ ✅ hecho (ver bloqueante #3).
   - **Fase D** (rate-limit, captcha, headers, Swagger off). ← siguiente candidato.
3. **Resend + dominio verificado** + completar **placeholders legales**.
4. **Deploy** y **validación en navegador**.

## Variables de entorno de producción (resumen)

| Variable | Para qué | Nota |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a Postgres | Sin defaults en prod |
| `COOKIE_SECURE` | Cookie de sesión `Secure` | `true` (requiere HTTPS) |
| `FORWARD_HEADERS_STRATEGY` | Leer IP/esquema reales detrás del proxy | `framework` en prod (detrás de Caddy); default `none` |
| `FORUM_EMAIL_RESEND_API_KEY` | Activa Resend (si falta, loguea por consola) | Secreto |
| `EMAIL_FROM` | Remitente de los emails | Dominio verificado en Resend |
| `FRONTEND_BASE_URL` | Base para los links de los emails | URL real del front |
| `FORUM_BOOTSTRAP_ENABLED` | Activa el bootstrap de prod (categorías base + primer admin) | `true` en el primer arranque; apagar después |
| `BOOTSTRAP_ADMIN_USERNAME` | Username del primer ADMIN | Solo con bootstrap activado |
| `BOOTSTRAP_ADMIN_EMAIL` | Email del primer ADMIN (queda verificado) | Solo con bootstrap activado |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del primer ADMIN (≥8) | Secreto; cambiala tras el 1er login |
| `SPRINGDOC_ENABLED` | Swagger UI + `/v3/api-docs` | `false` en prod (default true en dev) |
| `FORUM_RATE_LIMIT_ENABLED` | Rate-limiting de `/auth` | `true` (default); ver `forum.rate-limit` |
| `R2_ENABLED` | Guardar imágenes en Cloudflare R2 (si no, disco local) | `true` en prod con object storage |
| `R2_ENDPOINT` | Endpoint S3 de R2 | `https://<account-id>.r2.cloudflarestorage.com` |
| `R2_BUCKET` | Bucket de R2 | — |
| `R2_ACCESS_KEY` / `R2_SECRET_KEY` | Credenciales del token R2 | Secreto |
| `R2_REGION` | Región para la firma S3 | `auto` (default) |
