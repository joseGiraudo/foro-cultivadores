-- Comentario fijado por el autor del post (uno por post, o ninguno). FK a comments.
ALTER TABLE posts ADD COLUMN pinned_comment_id BIGINT REFERENCES comments (id);
