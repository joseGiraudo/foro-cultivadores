-- Búsqueda full-text de posts: columna tsvector generada (diccionario español),
-- con peso A para el título y B para el contenido, e índice GIN para consultarla.
-- Al ser STORED se mantiene sola en cada insert/update (sin trigger); el ALTER la calcula
-- para las filas existentes. to_tsvector('spanish', ...) es IMMUTABLE → columna generada válida.
ALTER TABLE posts ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('spanish', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('spanish', coalesce(content, '')), 'B')
    ) STORED;

CREATE INDEX idx_posts_search_vector ON posts USING gin (search_vector);
