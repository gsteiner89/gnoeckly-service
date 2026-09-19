package at.stoneforge.gnoeckly.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateJokeCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 80) String icon,
        int sortOrder,
        boolean active) {
}
