package at.stoneforge.gnoeckly.category;

import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JokeCategoryService {

    private final JokeCategoryRepository repository;

    public JokeCategoryService(JokeCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<JokeCategoryResponse> listActive() {
        return repository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc().stream()
                .map(JokeCategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<JokeCategoryResponse> listAll() {
        return repository.findByDeletedAtIsNullOrderBySortOrderAscNameAsc().stream()
                .map(JokeCategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public JokeCategory requireActive(UUID id) {
        JokeCategory category = requireExisting(id);
        if (!category.isActive()) {
            throw new IllegalArgumentException("Kategorie ist deaktiviert");
        }
        return category;
    }

    @Transactional(readOnly = true)
    public Map<UUID, JokeCategory> byIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(JokeCategory::getId, Function.identity()));
    }

    @Transactional
    public JokeCategoryResponse create(CreateJokeCategoryRequest request) {
        JokeCategory category = new JokeCategory();
        category.setSlug(request.slug());
        category.setName(request.name());
        category.setIcon(request.icon());
        category.setSortOrder(request.sortOrder());
        category.setActive(true);
        return JokeCategoryResponse.from(repository.save(category));
    }

    @Transactional
    public JokeCategoryResponse update(UUID id, UpdateJokeCategoryRequest request) {
        JokeCategory category = requireExisting(id);
        category.setName(request.name());
        category.setIcon(request.icon());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active());
        return JokeCategoryResponse.from(category);
    }

    @Transactional
    public void softDelete(UUID id) {
        requireExisting(id).setDeletedAt(Instant.now());
    }

    private JokeCategory requireExisting(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategorie " + id + " nicht gefunden"));
    }
}
