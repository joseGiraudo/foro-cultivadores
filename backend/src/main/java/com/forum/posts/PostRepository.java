package com.forum.posts;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /**
     * Búsqueda full-text (columna generada {@code search_vector}, diccionario español) ordenada por
     * relevancia ({@code ts_rank}; el título pesa más que el cuerpo). Los demás filtros son opcionales
     * vía {@code (:param IS NULL OR ...)}; categorías (unión/OR) y autores seguidos vía sus flags
     * {@code filterByCategories}/{@code filterByAuthors} + listas (siempre no vacías: ids reales o el
     * placeholder [-1]), así los IN nunca quedan vacíos. Los enums llegan como String (columnas VARCHAR).
     */
    @Query(value = """
            SELECT p.id, p.title, p.content, p.author_id, p.post_type, p.status,
                   p.created_at, p.updated_at, p.edited_at, p.pinned_comment_id
            FROM posts p
            WHERE p.search_vector @@ websearch_to_tsquery('spanish', :q)
              AND (:status IS NULL OR p.status = :status)
              AND (:authorId IS NULL OR p.author_id = :authorId)
              AND (:postType IS NULL OR p.post_type = :postType)
              AND (:filterByAuthors = FALSE OR p.author_id IN (:authorIds))
              AND (:filterByCategories = FALSE OR EXISTS (
                    SELECT 1 FROM post_categories pc
                    WHERE pc.post_id = p.id AND pc.category_id IN (:categoryIds)))
            ORDER BY ts_rank(p.search_vector, websearch_to_tsquery('spanish', :q)) DESC,
                     p.created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM posts p
            WHERE p.search_vector @@ websearch_to_tsquery('spanish', :q)
              AND (:status IS NULL OR p.status = :status)
              AND (:authorId IS NULL OR p.author_id = :authorId)
              AND (:postType IS NULL OR p.post_type = :postType)
              AND (:filterByAuthors = FALSE OR p.author_id IN (:authorIds))
              AND (:filterByCategories = FALSE OR EXISTS (
                    SELECT 1 FROM post_categories pc
                    WHERE pc.post_id = p.id AND pc.category_id IN (:categoryIds)))
            """,
            nativeQuery = true)
    Page<Post> search(@Param("q") String q, @Param("authorId") Long authorId,
                      @Param("postType") String postType, @Param("status") String status,
                      @Param("filterByAuthors") boolean filterByAuthors,
                      @Param("authorIds") Collection<Long> authorIds,
                      @Param("filterByCategories") boolean filterByCategories,
                      @Param("categoryIds") Collection<Long> categoryIds, Pageable pageable);

    /**
     * Feed filtrado por categorías (unión/OR: posts en alguna de las elegidas) priorizando los que
     * matchean MÁS categorías de las seleccionadas, y luego por fecha. El JOIN restringe a la unión y
     * el COUNT mide cuántas categorías elegidas tiene cada post. GROUP BY por PK (Postgres permite
     * seleccionar el resto de columnas de {@code p} por dependencia funcional). Demás filtros opcionales.
     */
    @Query(value = """
            SELECT p.id, p.title, p.content, p.author_id, p.post_type, p.status,
                   p.created_at, p.updated_at, p.edited_at, p.pinned_comment_id
            FROM posts p
            JOIN post_categories pc ON pc.post_id = p.id AND pc.category_id IN (:categoryIds)
            WHERE (:status IS NULL OR p.status = :status)
              AND (:authorId IS NULL OR p.author_id = :authorId)
              AND (:postType IS NULL OR p.post_type = :postType)
              AND (:filterByAuthors = FALSE OR p.author_id IN (:authorIds))
            GROUP BY p.id
            ORDER BY count(pc.category_id) DESC, p.created_at DESC
            """,
            countQuery = """
            SELECT count(DISTINCT p.id) FROM posts p
            JOIN post_categories pc ON pc.post_id = p.id AND pc.category_id IN (:categoryIds)
            WHERE (:status IS NULL OR p.status = :status)
              AND (:authorId IS NULL OR p.author_id = :authorId)
              AND (:postType IS NULL OR p.post_type = :postType)
              AND (:filterByAuthors = FALSE OR p.author_id IN (:authorIds))
            """,
            nativeQuery = true)
    Page<Post> rankByCategories(@Param("categoryIds") Collection<Long> categoryIds,
                                @Param("authorId") Long authorId, @Param("postType") String postType,
                                @Param("status") String status,
                                @Param("filterByAuthors") boolean filterByAuthors,
                                @Param("authorIds") Collection<Long> authorIds, Pageable pageable);
}
