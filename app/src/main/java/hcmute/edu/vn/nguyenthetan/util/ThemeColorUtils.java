package hcmute.edu.vn.nguyenthetan.util;

import android.graphics.Color;
import androidx.core.graphics.ColorUtils;

/**
 * Utility class để generate color palette từ 2 màu chính (Background + Primary)
 * Sử dụng HSL color space để tính toán các màu phụ
 * 
 * Single Responsibility: Chỉ xử lý color manipulation, không chứa business logic
 */
public class ThemeColorUtils {
    
    private static final float LIGHTEN_NAV_FACTOR = 0.05f;
    private static final float LIGHTEN_CARD_FACTOR = 0.12f;
    private static final float LIGHTEN_SURFACE_FACTOR = 0.20f;
    private static final float LIGHTEN_PRIMARY_FACTOR = 0.30f;
    private static final float TEXT_SECONDARY_BLEND = 0.6f;
    private static final float TEXT_TERTIARY_BLEND = 0.4f;
    
    // Luminance threshold để quyết định text trắng hay đen
    private static final float LUMINANCE_THRESHOLD = 0.5f;
    
    /**
     * Data class chứa full palette generated từ 2 màu chính
     */
    public static class ThemePalette {
        public final int backgroundDark;
        public final int navBackground;
        public final int cardDark;
        public final int cardBackground;
        public final int surfaceElevated;
        public final int primary;
        public final int primaryLight;
        public final int textMain;
        public final int textSecondary;
        public final int textTertiary;
        
        public ThemePalette(int backgroundDark, int navBackground, int cardDark, 
                           int cardBackground, int surfaceElevated, int primary, 
                           int primaryLight, int textMain, int textSecondary, int textTertiary) {
            this.backgroundDark = backgroundDark;
            this.navBackground = navBackground;
            this.cardDark = cardDark;
            this.cardBackground = cardBackground;
            this.surfaceElevated = surfaceElevated;
            this.primary = primary;
            this.primaryLight = primaryLight;
            this.textMain = textMain;
            this.textSecondary = textSecondary;
            this.textTertiary = textTertiary;
        }
    }
    
    /**
     * Generate full color palette từ 2 màu chính
     * @param backgroundColor Màu nền chính
     * @param primaryColor Màu chủ đạo (accent)
     * @return ThemePalette chứa tất cả màu cần thiết cho theme
     */
    public static ThemePalette generatePalette(int backgroundColor, int primaryColor) {
        // Background variants - lighten dần từ màu nền
        int navBackground = lighten(backgroundColor, LIGHTEN_NAV_FACTOR);
        int cardDark = lighten(backgroundColor, LIGHTEN_CARD_FACTOR);
        int cardBackground = cardDark; // Same as cardDark
        int surfaceElevated = lighten(backgroundColor, LIGHTEN_SURFACE_FACTOR);
        
        // Primary variants
        int primaryLight = lighten(primaryColor, LIGHTEN_PRIMARY_FACTOR);
        
        // Text colors - dựa trên độ sáng của background
        int textMain = getContrastColor(backgroundColor);
        int textSecondary = blendColors(primaryColor, textMain, TEXT_SECONDARY_BLEND);
        int textTertiary = blendColors(primaryColor, textMain, TEXT_TERTIARY_BLEND);
        
        return new ThemePalette(
            backgroundColor, navBackground, cardDark, cardBackground,
            surfaceElevated, primaryColor, primaryLight,
            textMain, textSecondary, textTertiary
        );
    }
    
    /**
     * Lighten color bằng cách blend với white
     * @param color Màu gốc
     * @param factor Tỷ lệ blend (0.0 - 1.0)
     * @return Màu đã lighten
     */
    public static int lighten(int color, float factor) {
        return ColorUtils.blendARGB(color, Color.WHITE, factor);
    }
    
    /**
     * Darken color bằng cách blend với black
     * @param color Màu gốc
     * @param factor Tỷ lệ blend (0.0 - 1.0)
     * @return Màu đã darken
     */
    public static int darken(int color, float factor) {
        return ColorUtils.blendARGB(color, Color.BLACK, factor);
    }
    
    /**
     * Blend 2 colors với tỷ lệ cho trước
     * @param color1 Màu 1
     * @param color2 Màu 2  
     * @param ratio Tỷ lệ color1 (0.0 - 1.0), color2 sẽ là (1 - ratio)
     * @return Màu đã blend
     */
    public static int blendColors(int color1, int color2, float ratio) {
        return ColorUtils.blendARGB(color1, color2, ratio);
    }
    
    /**
     * Tự động chọn text color (đen hoặc trắng) dựa trên luminance của background
     * @param backgroundColor Màu nền
     * @return Color.WHITE hoặc Color.BLACK
     */
    public static int getContrastColor(int backgroundColor) {
        double luminance = ColorUtils.calculateLuminance(backgroundColor);
        return luminance > LUMINANCE_THRESHOLD ? Color.BLACK : Color.WHITE;
    }
    
    /**
     * Kiểm tra xem 2 màu có đủ contrast để đọc được không (WCAG guideline)
     * @param foreground Màu chữ
     * @param background Màu nền
     * @return true nếu contrast đủ tốt
     */
    public static boolean hasEnoughContrast(int foreground, int background) {
        double contrast = ColorUtils.calculateContrast(foreground, background);
        return contrast >= 3.0; // WCAG AA standard for large text
    }
    
    /**
     * Chuyển đổi int color sang hex string (không có alpha)
     */
    public static String toHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }
    
    /**
     * Parse hex string sang int color
     */
    public static int fromHexString(String hex) {
        try {
            return Color.parseColor(hex);
        } catch (IllegalArgumentException e) {
            return Color.BLACK;
        }
    }
}
