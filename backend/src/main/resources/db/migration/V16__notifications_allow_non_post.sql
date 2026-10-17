-- Hasta ahora toda notificación nacía de un comentario sobre un post. Las notificaciones de
-- resultado de una petición de contributor no tienen post asociado, así que post_id/post_title
-- pasan a ser opcionales.
ALTER TABLE notifications ALTER COLUMN post_id DROP NOT NULL;
ALTER TABLE notifications ALTER COLUMN post_title DROP NOT NULL;
