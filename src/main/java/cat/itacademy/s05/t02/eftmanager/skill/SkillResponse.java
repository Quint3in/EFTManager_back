package cat.itacademy.s05.t02.eftmanager.skill;

public record SkillResponse(
        String id,
        String name,
        String normalizedName,
        String imageLink
) {}