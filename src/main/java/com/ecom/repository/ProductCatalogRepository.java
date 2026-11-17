package com.ecom.repository;

import com.ecom.entity.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Paged shop listing with optional category filter, full-text search and sorting.
 *
 * Uses native SQL because MySQL's MATCH ... AGAINST full-text search has no JPQL
 * equivalent. Only values are bound as parameters; the ORDER BY comes from a fixed
 * whitelist, so nothing the client sends is ever pasted into the SQL.
 */
@Repository
public class ProductCatalogRepository {

    /** Must match the FULLTEXT index columns in V2__columns_and_indexes.sql exactly. */
    private static final String MATCH = "MATCH(p.name, p.category, p.description) AGAINST (:ft IN BOOLEAN MODE)";

    /** InnoDB's default full-text stopwords: requiring one of these (+word) would match nothing. */
    private static final Set<String> STOPWORDS = Set.of(
            "a", "about", "an", "are", "as", "at", "be", "by", "com", "de", "en", "for", "from", "how",
            "i", "in", "is", "it", "la", "of", "on", "or", "that", "the", "this", "to", "was", "what",
            "when", "where", "who", "will", "with", "und", "www");

    /** InnoDB ignores words shorter than this by default (innodb_ft_min_token_size). */
    private static final int MIN_WORD = 3;

    @PersistenceContext
    private EntityManager em;

    public record Result(List<ProductEntity> items, long total) {}

    @SuppressWarnings("unchecked")
    public Result findActive(String category, String keyword, String sort, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE p.active = TRUE");
        Map<String, Object> params = new LinkedHashMap<>();

        if (category != null) {
            where.append(" AND p.category = :category");
            params.put("category", category);
        }

        boolean fullText = false;
        if (keyword != null) {
            String ft = toBooleanQuery(keyword);
            // Full-text for Latin-script searches; other scripts (e.g. Sinhala) use LIKE,
            // because MySQL's word splitting for them isn't reliable.
            if (!ft.isEmpty() && isAscii(keyword)) {
                fullText = true;
                where.append(" AND ").append(MATCH);
                params.put("ft", ft);
            } else {
                // Very short words, only stopwords, or non-Latin text: simple name match.
                where.append(" AND LOWER(p.name) LIKE :like");
                params.put("like", "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%");
            }
        }

        Query select = em.createNativeQuery(
                "SELECT p.* FROM products p" + where + orderBy(sort, fullText), ProductEntity.class);
        Query count = em.createNativeQuery("SELECT COUNT(*) FROM products p" + where);
        params.forEach((name, value) -> {
            select.setParameter(name, value);
            count.setParameter(name, value);
        });

        select.setFirstResult(page * size);
        select.setMaxResults(size);

        List<ProductEntity> items = select.getResultList();
        long total = ((Number) count.getSingleResult()).longValue();
        return new Result(items, total);
    }

    /** Sort keys accepted from the client; anything else means "featured". */
    public static final Set<String> SORTS = Set.of("featured", "price-asc", "price-desc", "name", "newest");

    private static String orderBy(String sort, boolean fullText) {
        return switch (sort) {
            case "price-asc" -> " ORDER BY p.price ASC, p.id DESC";
            case "price-desc" -> " ORDER BY p.price DESC, p.id DESC";
            case "name" -> " ORDER BY p.name ASC, p.id ASC";
            case "newest" -> " ORDER BY p.created_at DESC, p.id DESC";
            // featured: best match first when searching, then in-stock items, then newest
            default -> fullText
                    ? " ORDER BY " + MATCH + " DESC, (p.stock_quantity > 0) DESC, p.id DESC"
                    : " ORDER BY (p.stock_quantity > 0) DESC, p.id DESC";
        };
    }

    /**
     * "Sony WH-1000XM5 headphones" becomes "+sony* +1000xm5* +headphones*":
     * every word required, each matching as a prefix. Operator characters are stripped.
     */
    static String toBooleanQuery(String keyword) {
        StringBuilder out = new StringBuilder();
        // \p{M} keeps combining marks (e.g. Sinhala vowel signs) inside their word.
        for (String word : keyword.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{M}\\p{N}]+")) {
            if (word.length() < MIN_WORD || STOPWORDS.contains(word)) continue;
            out.append('+').append(word).append("* ");
        }
        return out.toString().trim();
    }

    private static boolean isAscii(String s) {
        return s.chars().allMatch(c -> c < 128);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}