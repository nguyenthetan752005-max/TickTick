package hcmute.edu.vn.nguyenthetan.ui.theme;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import hcmute.edu.vn.nguyenthetan.BaseActivity;
import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.CustomTheme;
import hcmute.edu.vn.nguyenthetan.repository.ThemeRepository;
import hcmute.edu.vn.nguyenthetan.util.ThemeColorUtils;
import hcmute.edu.vn.nguyenthetan.util.ThemeColorUtils.ThemePalette;

/**
 * Activity cho phép người dùng chọn 2 màu để tạo custom theme
 * Single Responsibility: Chỉ xử lý UI và user interaction cho custom theme
 */
public class CustomThemeActivity extends BaseActivity {

    private TextView tvBgColorHex;
    private TextView tvPrimaryColorHex;
    private LinearLayout previewBackground;
    private LinearLayout previewCard;
    private TextView previewText;
    private Button previewButton;
    
    private int selectedBgColor = Color.parseColor("#1A2332");  // Default dark navy
    private int selectedPrimaryColor = Color.parseColor("#F59E0B"); // Default orange
    
    private ThemeRepository themeRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_theme);
        
        themeRepository = new ThemeRepository(this);
        
        // Load existing custom theme if available
        if (themeRepository.hasCustomTheme() && themeRepository.getCurrentTheme().name().equals("CUSTOM")) {
            CustomTheme savedTheme = themeRepository.getCustomTheme();
            selectedBgColor = savedTheme.getBackgroundColor();
            selectedPrimaryColor = savedTheme.getPrimaryColor();
        }
        
        initViews();
        setupClickListeners();
        updateUI();
    }
    
    private void initViews() {
        tvBgColorHex = findViewById(R.id.tvBgColorHex);
        tvPrimaryColorHex = findViewById(R.id.tvPrimaryColorHex);
        previewBackground = findViewById(R.id.previewBackground);
        previewCard = findViewById(R.id.previewCard);
        previewText = findViewById(R.id.previewText);
        previewButton = findViewById(R.id.previewButton);
    }
    
    private void setupClickListeners() {
        // Background color picker
        findViewById(R.id.btnPickBgColor).setOnClickListener(v -> showColorPicker(true));
        
        // Primary color picker  
        findViewById(R.id.btnPickPrimaryColor).setOnClickListener(v -> showColorPicker(false));
        
        // Save button
        findViewById(R.id.btnSave).setOnClickListener(v -> saveCustomTheme());
        
        // Cancel button
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }
    
    private void showColorPicker(boolean isBackground) {
        // Sử dụng SimpleColorPickerDialog (tạo sau) hoặc thư viện có sẵn
        // Tạm thời dùng dialog đơn giản với preset colors
        ColorPickerDialog dialog = new ColorPickerDialog(this, color -> {
            if (isBackground) {
                selectedBgColor = color;
            } else {
                selectedPrimaryColor = color;
            }
            updateUI();
        }, isBackground ? selectedBgColor : selectedPrimaryColor);
        dialog.show();
    }
    
    private void updateUI() {
        // Update hex displays
        tvBgColorHex.setText(ThemeColorUtils.toHexString(selectedBgColor));
        tvPrimaryColorHex.setText(ThemeColorUtils.toHexString(selectedPrimaryColor));
        
        // Update color boxes
        ImageView ivBgColor = findViewById(R.id.ivBgColor);
        ImageView ivPrimaryColor = findViewById(R.id.ivPrimaryColor);
        ivBgColor.setColorFilter(selectedBgColor);
        ivPrimaryColor.setColorFilter(selectedPrimaryColor);
        
        // Generate preview palette
        ThemePalette palette = ThemeColorUtils.generatePalette(selectedBgColor, selectedPrimaryColor);
        
        // Update preview views
        previewBackground.setBackgroundColor(palette.backgroundDark);
        previewCard.setBackgroundColor(palette.cardDark);
        previewText.setTextColor(palette.textMain);
        previewButton.setBackgroundColor(palette.primary);
        previewButton.setTextColor(palette.textMain);
    }
    
    private void saveCustomTheme() {
        CustomTheme customTheme = new CustomTheme(selectedBgColor, selectedPrimaryColor);
        themeRepository.saveCustomTheme(customTheme);
        
        // Recreate main activity to apply theme
        Intent intent = new Intent(this, hcmute.edu.vn.nguyenthetan.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
