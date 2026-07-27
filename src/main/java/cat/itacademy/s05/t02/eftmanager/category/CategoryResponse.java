package cat.itacademy.s05.t02.eftmanager.category;

import java.util.List;

public record CategoryResponse(
        String id,
        String name,
        String normalizedName,
        String parentId,
        List<String> childrenIds
) {}