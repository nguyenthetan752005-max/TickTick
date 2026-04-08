package hcmute.edu.vn.nguyenthetan.model;

public enum ThemeType {
    DEFAULT("Mặc định"),
    SUMMER("Mùa hè"),
    HELL("Địa ngục"),
    WINTER("Mùa đông"),
    NEON("Neon"),
    CUSTOM("Tùy chỉnh");

    private final String displayName;

    ThemeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ThemeType fromString(String name) {
        for (ThemeType type : ThemeType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return DEFAULT;
    }
}
