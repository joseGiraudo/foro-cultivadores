-- Aceptación de términos en el registro + baja de cuenta (anonimización, derecho de supresión).

-- Momento y versión de T&C/Privacidad que aceptó el usuario al registrarse. Null en usuarios
-- previos (predatan el requisito) y en los cargados por seeders antiguos.
ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE users ADD COLUMN terms_version     VARCHAR(20);

-- Marca de baja. Al eliminar la cuenta se anonimizan los datos personales (email, bio, imágenes,
-- datos de comercio) y se setea este timestamp; los posts/comentarios quedan como "[cuenta eliminada]".
ALTER TABLE users ADD COLUMN deleted_at        TIMESTAMP(6) WITH TIME ZONE;
