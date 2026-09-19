package at.stoneforge.gnoeckly.category;

import java.util.UUID;

public record JokeCategoryResponse(UUID id, String slug, String name, String icon, int sortOrder, boolean active) {

    public static JokeCategoryResponse from(JokeCategory category) {
        return new JokeCategoryResponse(category.getId(), category.getSlug(), category.getName(), category.getIcon(),
                category.getSortOrder(), category.isActive());
    }
}
