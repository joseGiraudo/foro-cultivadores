-- edited_at: marca de la última edición de contenido (null = nunca editado).
-- Se distingue de updated_at, que también cambia al moderar (cambios de estado).
ALTER TABLE posts ADD COLUMN edited_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE comments ADD COLUMN edited_at TIMESTAMP(6) WITH TIME ZONE;
