package at.stoneforge.gnoeckly.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** midgard CLAUDE.md Regel 10: Verwaltung unter /api/admin/** nur fuer SUPERADMIN. */
@RestController
@RequestMapping("/api/admin/joke-categories")
@PreAuthorize("hasAuthority('SUPERADMIN')")
public class AdminCategoryController {

    private final JokeCategoryService service;

    public AdminCategoryController(JokeCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<JokeCategoryResponse> list() {
        return service.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JokeCategoryResponse create(@Valid @RequestBody CreateJokeCategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public JokeCategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateJokeCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.softDelete(id);
    }
}
