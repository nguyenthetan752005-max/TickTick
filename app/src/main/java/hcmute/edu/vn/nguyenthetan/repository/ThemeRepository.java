package hcmute.edu.vn.nguyenthetan.repository;

import android.content.Context;
import android.content.SharedPreferences;

import hcmute.edu.vn.nguyenthetan.model.CustomTheme;
import hcmute.edu.vn.nguyenthetan.model.ThemeType;

/**
 * Repository quản lý theme preferences
 * Hỗ trợ cả preset themes và custom themes
 * 
 * Single Responsibility: Chỉ xử lý data persistence cho theme
 */
public class ThemeRepository {
    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_THEME = "current_theme";
    private static final String KEY_CUSTOM_BG = "custom_bg_color";
    private static final String KEY_CUSTOM_PRIMARY = "custom_primary_color";

    private final SharedPreferences prefs;

    public ThemeRepository(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lấy theme hiện tại (preset hoặc custom)
     */
    public ThemeType getCurrentTheme() {
        String themeStr = prefs.getString(KEY_THEME, ThemeType.DEFAULT.name());
        return ThemeType.fromString(themeStr);
    }

    /**
     * Lưu preset theme
     */
    public void saveTheme(ThemeType themeType) {
        prefs.edit().putString(KEY_THEME, themeType.name()).apply();
    }

    /**
     * Lưu custom theme với 2 màu
     */
    public void saveCustomTheme(CustomTheme customTheme) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_THEME, ThemeType.CUSTOM.name());
        editor.putString(KEY_CUSTOM_BG, customTheme.getBackgroundColorHex());
        editor.putString(KEY_CUSTOM_PRIMARY, customTheme.getPrimaryColorHex());
        editor.apply();
    }

    /**
     * Lấy custom theme đã lưu
     * @return CustomTheme hoặc null nếu chưa có
     */
    public CustomTheme getCustomTheme() {
        String bgHex = prefs.getString(KEY_CUSTOM_BG, null);
        String primaryHex = prefs.getString(KEY_CUSTOM_PRIMARY, null);
        
        if (bgHex == null || primaryHex == null) {
            // Trả về default nếu chưa có custom
            return new CustomTheme(android.graphics.Color.parseColor("#000000"), 
                                   android.graphics.Color.parseColor("#5271FF"));
        }
        
        return CustomTheme.fromHex(bgHex, primaryHex);
    }

    /**
     * Kiểm tra có custom theme đã lưu không
     */
    public boolean hasCustomTheme() {
        return prefs.contains(KEY_CUSTOM_BG) && prefs.contains(KEY_CUSTOM_PRIMARY);
    }
}
