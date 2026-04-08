package hcmute.edu.vn.nguyenthetan;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;

import hcmute.edu.vn.nguyenthetan.model.CustomTheme;
import hcmute.edu.vn.nguyenthetan.model.ThemeType;
import hcmute.edu.vn.nguyenthetan.repository.ThemeRepository;
import hcmute.edu.vn.nguyenthetan.util.ThemeColorUtils;
import hcmute.edu.vn.nguyenthetan.util.ThemeColorUtils.ThemePalette;

/**
 * BaseActivity xử lý theme cho toàn bộ app
 * - Áp dụng preset themes qua XML styles
 * - Áp dụng custom theme qua dynamic color application sau inflate
 * 
 * Single Responsibility: Chỉ xử lý theme application
 */
public class BaseActivity extends AppCompatActivity {
    
    private ThemeType currentTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeRepository themeRepository = new ThemeRepository(this);
        currentTheme = themeRepository.getCurrentTheme();

        switch (currentTheme) {
            case SUMMER:
                setTheme(R.style.Theme_TickTick_Summer);
                ThemeManager.getInstance().clearPalette();
                break;
            case HELL:
                setTheme(R.style.Theme_TickTick_Hell);
                ThemeManager.getInstance().clearPalette();
                break;
            case WINTER:
                setTheme(R.style.Theme_TickTick_Winter);
                ThemeManager.getInstance().clearPalette();
                break;
            case NEON:
                setTheme(R.style.Theme_TickTick_Neon);
                ThemeManager.getInstance().clearPalette();
                break;
            case CUSTOM:
                setTheme(R.style.Theme_TickTick_Default);
                setupCustomTheme(themeRepository);
                break;
            case DEFAULT:
            default:
                setTheme(R.style.Theme_TickTick_Default);
                ThemeManager.getInstance().clearPalette();
                break;
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyCustomColorsIfNeeded();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyCustomColorsIfNeeded();
    }

    /**
     * Setup custom theme - generate palette và lưu vào ThemeManager
     */
    private void setupCustomTheme(ThemeRepository repository) {
        CustomTheme custom = repository.getCustomTheme();
        ThemePalette palette = ThemeColorUtils.generatePalette(
            custom.getBackgroundColor(), 
            custom.getPrimaryColor()
        );
        ThemeManager.getInstance().setCurrentPalette(palette);
    }
    
    /**
     * Apply custom colors cho tất cả views trong layout
     */
    private void applyCustomColorsIfNeeded() {
        if (currentTheme != ThemeType.CUSTOM) return;
        
        ThemePalette palette = ThemeManager.getInstance().getCurrentPalette();
        if (palette == null) return;
        
        // Apply window background
        getWindow().setBackgroundDrawable(new ColorDrawable(palette.backgroundDark));
        
        // Apply cho root view
        View rootView = findViewById(android.R.id.content);
        if (rootView instanceof ViewGroup) {
            applyPaletteToViewGroup((ViewGroup) rootView, palette);
        }
    }
    
    /**
     * Recursive apply palette colors cho view group
     */
    private void applyPaletteToViewGroup(ViewGroup parent, ThemePalette palette) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            applyPaletteToView(child, palette);
            
            if (child instanceof ViewGroup) {
                applyPaletteToViewGroup((ViewGroup) child, palette);
            }
        }
    }
    
    /**
     * Apply palette color cho một view cụ thể
     */
    private void applyPaletteToView(View view, ThemePalette palette) {
        try {
            // NavigationView - cần xử lý riêng
            if (view instanceof NavigationView) {
                NavigationView navView = (NavigationView) view;
                navView.setBackgroundColor(palette.navBackground);
                navView.setItemTextColor(ColorStateList.valueOf(palette.textMain));
                navView.setItemIconTintList(ColorStateList.valueOf(palette.textMain));
                
                // Apply cho header của navigation
                View header = navView.getHeaderView(0);
                if (header != null) {
                    header.setBackgroundColor(palette.navBackground);
                    applyPaletteToViewGroup((ViewGroup) header, palette);
                }
                return;
            }
            
            // Bottom Navigation Container - LinearLayout với id bottomNavigationContainer
            if (view instanceof LinearLayout && view.getId() == R.id.bottomNavigationContainer) {
                view.setBackgroundColor(palette.navBackground);
                
                // Apply primary color cho các icon được chọn
                for (int i = 0; i < ((LinearLayout) view).getChildCount(); i++) {
                    View child = ((LinearLayout) view).getChildAt(i);
                    if (child instanceof ImageView) {
                        ImageView iv = (ImageView) child;
                        // Mặc định dùng textSecondary, nếu là icon active thì dùng primary
                        iv.setColorFilter(palette.textSecondary);
                    }
                }
                return;
            }
            
            // Kiểm tra background drawable và set color nếu là ColorDrawable
            if (view.getBackground() instanceof ColorDrawable) {
                int currentColor = ((ColorDrawable) view.getBackground()).getColor();
                
                // Map màu cũ -> màu mới dựa trên palette
                // Nếu background gần với themeBgDark mặc định (#000000) -> dùng custom bg
                if (isNearColor(currentColor, Color.BLACK)) {
                    view.setBackgroundColor(palette.backgroundDark);
                } else if (isNearColor(currentColor, Color.parseColor("#1E1E1E"))) {
                    // Card dark default
                    view.setBackgroundColor(palette.cardDark);
                } else if (isNearColor(currentColor, Color.parseColor("#1C1C1E"))) {
                    // Nav background default
                    view.setBackgroundColor(palette.navBackground);
                } else if (isNearColor(currentColor, Color.parseColor("#2C2C2E"))) {
                    // Surface elevated default
                    view.setBackgroundColor(palette.surfaceElevated);
                } else if (isNearColor(currentColor, Color.parseColor("#5271FF"))) {
                    // Primary blue default
                    view.setBackgroundColor(palette.primary);
                }
            }
            
            // MaterialCardView - set card background
            if (view instanceof MaterialCardView) {
                ((MaterialCardView) view).setCardBackgroundColor(palette.cardDark);
            }
            
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    /**
     * Kiểm tra 2 màu có gần nhau không
     */
    private boolean isNearColor(int color1, int color2) {
        int tolerance = 30; // Tolerance for color difference
        int r1 = Color.red(color1), g1 = Color.green(color1), b1 = Color.blue(color1);
        int r2 = Color.red(color2), g2 = Color.green(color2), b2 = Color.blue(color2);
        return Math.abs(r1 - r2) < tolerance && Math.abs(g1 - g2) < tolerance && Math.abs(b1 - b2) < tolerance;
    }

    /**
     * Public method để views lấy palette hiện tại
     */
    public ThemePalette getCurrentPalette() {
        return ThemeManager.getInstance().getCurrentPalette();
    }

    /**
     * Kiểm tra có đang dùng custom theme không
     */
    public boolean isCustomThemeActive() {
        return currentTheme == ThemeType.CUSTOM 
            && ThemeManager.getInstance().hasCustomPalette();
    }

    /**
     * Helper method cho views để lấy màu theo attribute name
     */
    public int getCustomThemeColor(String attrName) {
        if (!isCustomThemeActive()) return 0;
        
        ThemePalette palette = getCurrentPalette();
        if (palette == null) return 0;
        
        switch (attrName) {
            case "themeBgDark": return palette.backgroundDark;
            case "themeNavBg": return palette.navBackground;
            case "themeCardDark":
            case "themeCardBg": return palette.cardDark;
            case "themeSurfaceElevated": return palette.surfaceElevated;
            case "themePrimary": return palette.primary;
            case "themePrimaryLight": return palette.primaryLight;
            case "themeTextWhite":
            case "themeTextMain": return palette.textMain;
            case "themeTextSecondary": return palette.textSecondary;
            case "themeTextTertiary": return palette.textTertiary;
            default: return 0;
        }
    }
}
