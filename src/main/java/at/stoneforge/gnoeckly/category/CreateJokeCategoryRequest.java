package at.stoneforge.gnoeckly.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateJokeCategoryRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,40}$") String slug,
        @NotBlank @Size(max = 80) String name,
        @Size(max = 80) String icon,
        int sortOrder) {
}
