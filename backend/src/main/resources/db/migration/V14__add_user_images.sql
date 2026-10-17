-- Foto de perfil (avatar) y portada del usuario. Guardamos la storage_key y el content_type;
-- el archivo vive en el ImageStorage (mismo que las imágenes de posts). Todo nullable.
ALTER TABLE users
    ADD COLUMN avatar_storage_key  VARCHAR(100),
    ADD COLUMN avatar_content_type VARCHAR(30),
    ADD COLUMN cover_storage_key   VARCHAR(100),
    ADD COLUMN cover_content_type  VARCHAR(30);
