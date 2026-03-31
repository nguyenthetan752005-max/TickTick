package hcmute.edu.vn.nguyenthetan.repository;

import android.content.Context;
import android.content.SharedPreferences;

import hcmute.edu.vn.nguyenthetan.model.ThemeType;

public class ThemeRepository {
    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_THEME = "current_theme";

    private final SharedPreferences prefs;

    public ThemeRepository(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public ThemeType getCurrentTheme() {
        String themeStr = prefs.getString(KEY_THEME, ThemeType.DEFAULT.name());
        return ThemeType.fromString(themeStr);
    }

    public void saveTheme(ThemeType themeType) {
        prefs.edit().putString(KEY_THEME, themeType.name()).apply();
    }
}
