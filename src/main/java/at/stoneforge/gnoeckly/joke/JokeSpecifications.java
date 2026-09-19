package at.stoneforge.gnoeckly.joke;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Oeffentlicher Feed: nur APPROVED, nicht geloescht, optional nach Kategorie und Freigabe-Zeitraum
 * gefiltert. Sortierung liegt hier statt im {@code Pageable}, weil HOT geboostete Witze
 * (boostedUntil > now) per CASE-Ausdruck nach vorne zieht. Spring Data entfernt die Order fuer die
 * Count-Query selbst ({@code SimpleJpaRepository.getCountQuery}), der {@code Pageable} bleibt unsorted.
 */
public final class JokeSpecifications {

    private JokeSpecifications() {
    }

    public static Specification<Joke> approvedFeed(Collection<UUID> categoryIds, Instant since, FeedSort sort, Instant now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), JokeStatus.APPROVED));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("categoryId").in(categoryIds));
            }
            if (since != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("approvedAt"), since));
            }
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(orders(sort, root.get("boostedUntil"), root.get("hotScore"),
                        root.get("score"), root.get("approvedAt"), root.get("id"), cb, now));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static List<Order> orders(FeedSort sort, Expression<Instant> boostedUntil, Expression<Double> hotScore,
                                      Expression<Integer> score, Expression<Instant> approvedAt,
                                      Expression<UUID> id, CriteriaBuilder cb, Instant now) {
        return switch (sort) {
            case HOT -> {
                Expression<Integer> boosted = cb.<Integer>selectCase()
                        .when(cb.greaterThan(boostedUntil, now), 1)
                        .otherwise(0);
                yield List.of(cb.desc(boosted), cb.desc(hotScore), cb.desc(approvedAt), cb.asc(id));
            }
            case TOP -> List.of(cb.desc(score), cb.desc(approvedAt), cb.asc(id));
            case NEW -> List.of(cb.desc(approvedAt), cb.asc(id));
        };
    }
}
