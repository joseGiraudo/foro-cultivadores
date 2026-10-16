# Cuentas de prueba

Usuarios para loguearse durante el desarrollo. **Solo para uso local / entorno de prueba.**

## Usuarios del seed

Los carga `backend/.../users/UserSeeder.java` automáticamente al arrancar el backend
**con el perfil `dev`** y **solo si la tabla `users` está vacía** (si ya hay usuarios, no
hace nada). Si querés regenerarlos, vaciá la tabla (o recreá la base) y reiniciá el backend.

**Todos los usuarios del seed usan la misma contraseña:** `password123`

| Usuario        | Contraseña    | Email                    | Rol         | Estado     | Notas                                   |
|----------------|---------------|--------------------------|-------------|------------|-----------------------------------------|
| `admin`        | `password123` | admin@forum.test         | ADMIN       | ACTIVE     | Acceso a `/admin`                       |
| `mod`          | `password123` | mod@forum.test           | MODERATOR   | ACTIVE     | Acceso a `/moderation`                  |
| `grow_norte`   | `password123` | grow_norte@forum.test    | CONTRIBUTOR | ACTIVE     | Puede crear ARTICLE/GUIDE; seguible     |
| `grow_sur`     | `password123` | grow_sur@forum.test      | CONTRIBUTOR | ACTIVE     | Puede crear ARTICLE/GUIDE; seguible     |
| `user1`        | `password123` | user1@forum.test         | USER        | ACTIVE     | Usuario común                           |
| `user2`        | `password123` | user2@forum.test         | USER        | ACTIVE     | Usuario común                           |
| `user3`        | `password123` | user3@forum.test         | USER        | ACTIVE     | Usuario común                           |
| `suspended`    | `password123` | suspended@forum.test     | USER        | SUSPENDED  | Para probar la UI de cuenta suspendida  |

## Cuentas creadas a mano

Agregá acá las cuentas que vayas registrando desde la app para reusarlas.

| Usuario | Email | Contraseña | Rol | Notas |
|---------|-------|------------|-----|-------|
|         |       |            |     |       |
