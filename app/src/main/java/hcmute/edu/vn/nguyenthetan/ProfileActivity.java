package hcmute.edu.vn.nguyenthetan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ProfileActivity: Màn hình hồ sơ cá nhân.
 * Cho phép xem/sửa tên, email, ghi chú, và ảnh đại diện.
 */
public class ProfileActivity extends BaseActivity {
    private ImageView ivProfileAvatar;
    private EditText etDisplayName, etEmail, etBio;

    private ActivityResultLauncher<String> avatarPickerLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View mainView = findViewById(android.R.id.content);
        hcmute.edu.vn.nguyenthetan.util.EdgeInsetsUtil.applySystemBarsAndImePadding(mainView);

        initViews();
        setupAvatarPicker();
        setupCropLauncher();
        loadProfileData();
    }

    private void initViews() {
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etBio = findViewById(R.id.etBio);

        // Back
        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        // Đổi ảnh đại diện
        findViewById(R.id.btnChangeAvatar).setOnClickListener(v -> avatarPickerLauncher.launch("image/*"));

        // Xóa ảnh đại diện
        findViewById(R.id.btnRemoveAvatar).setOnClickListener(v -> {
            ivProfileAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            hcmute.edu.vn.nguyenthetan.util.ProfilePrefsUtil.removeAvatar(this);
            Toast.makeText(this, "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show();
        });

        // Lưu thông tin
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfileData());
    }

    private void setupAvatarPicker() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startCrop(uri);
                    }
                });
    }

    private void startCrop(Uri sourceUri) {
        try {
            // Copy source sang file tạm để tránh SecurityException
            File tempSource = new File(getCacheDir(), "temp_crop_source.png");
            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                    OutputStream out = new FileOutputStream(tempSource)) {
                if (in != null) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }

            File avatarFile = new File(getFilesDir(), "avatar.png");
            Uri destUri = Uri.fromFile(avatarFile);

            UCrop.Options options = new UCrop.Options();
            options.setCircleDimmedLayer(true);
            options.setShowCropGrid(false);
            options.setToolbarTitle("Cắt Ảnh Đại Diện");

            Intent intent = UCrop.of(Uri.fromFile(tempSource), destUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512)
                    .withOptions(options)
                    .getIntent(this);

            cropImageLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi đọc ảnh gốc", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCropLauncher() {
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            ivProfileAvatar.setImageURI(null); // xóa cache
                            ivProfileAvatar.setImageURI(resultUri);
                            hcmute.edu.vn.nguyenthetan.util.ProfilePrefsUtil.saveAvatarUri(
                                    this,
                                    resultUri.toString()
                            );

                            Toast.makeText(this, R.string.avatar_updated, Toast.LENGTH_SHORT).show();
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        Throwable cropError = UCrop.getError(result.getData());
                        if (cropError != null)
                            cropError.printStackTrace();
                        Toast.makeText(this, "Lỗi cắt ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Tải dữ liệu profile đã lưu từ SharedPreferences.
     */
    private void loadProfileData() {
        hcmute.edu.vn.nguyenthetan.util.ProfilePrefsUtil.loadProfileData(
                this,
                ivProfileAvatar,
                etDisplayName,
                etEmail,
                etBio
        );
    }

    /**
     * Lưu dữ liệu profile vào SharedPreferences.
     */
    private void saveProfileData() {
        String name = etDisplayName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        // 1. Chặn nếu tên bị bỏ trống
        if (name.isEmpty()) {
            Toast.makeText(this, "Tên hiển thị không được để trống!", Toast.LENGTH_SHORT).show();
            etDisplayName.requestFocus(); // Đưa con trỏ nhấp nháy về lại ô nhập tên
            return; // Dừng hàm ngay lập tức, các lệnh lưu bên dưới sẽ không được chạy
        }

        hcmute.edu.vn.nguyenthetan.util.ProfilePrefsUtil.saveProfileData(this, name, email, bio);

        Toast.makeText(this, "✅ Đã lưu thông tin hồ sơ", Toast.LENGTH_SHORT).show();
    }
}
