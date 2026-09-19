package at.stoneforge.gnoeckly.category;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/categories")
public class PublicCategoryController {

    private final JokeCategoryService service;

    public PublicCategoryController(JokeCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<JokeCategoryResponse> list() {
        return service.listActive();
    }
}
