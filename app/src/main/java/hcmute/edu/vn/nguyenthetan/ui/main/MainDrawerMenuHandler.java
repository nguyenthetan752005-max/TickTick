package hcmute.edu.vn.nguyenthetan.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import hcmute.edu.vn.nguyenthetan.CategoryDialogHelper;
import hcmute.edu.vn.nguyenthetan.ContactsActivity;
import hcmute.edu.vn.nguyenthetan.MediaActivity;
import hcmute.edu.vn.nguyenthetan.ProfileActivity;
import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.repository.CategoryRepository;
import hcmute.edu.vn.nguyenthetan.view.MainViewModel;
import hcmute.edu.vn.nguyenthetan.util.DialogUtils;
import hcmute.edu.vn.nguyenthetan.util.AppExecutors;

public final class MainDrawerMenuHandler {
    private MainDrawerMenuHandler() {}

    public interface CategoryFilterSetter {
        void setCategoryFilter(int categoryId);
    }

    /**
     * Xóa tất cả dữ liệu từ database và tệp tin
     */
    private static void clearAllData(Activity activity, MainViewModel viewModel) {
        // Chạy trên background thread để không block UI
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Xóa tệp nhạc được upload
                java.io.File uploadDir = new java.io.File(activity.getFilesDir(), "uploaded_music");
                if (uploadDir.exists()) {
                    deleteRecursive(uploadDir);
                    android.util.Log.d("ClearData", "Deleted uploaded_music directory");
                }

                // Xóa ảnh đại diện
                java.io.File avatarFile = new java.io.File(activity.getFilesDir(), "avatar.png");
                if (avatarFile.exists()) {
                    avatarFile.delete();
                    android.util.Log.d("ClearData", "Deleted avatar.png");
                }

                // Xóa từ database (cũng chạy trên background thread)
                viewModel.clearAllData();

                // Show toast trên main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    android.widget.Toast.makeText(activity, "Tất cả dữ liệu đã bị xóa!", android.widget.Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                android.util.Log.e("ClearData", "Error clearing data: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    android.widget.Toast.makeText(activity, "Lỗi khi xóa dữ liệu: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static void deleteRecursive(java.io.File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            java.io.File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (java.io.File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    public static void setup(
            Activity activity,
            NavigationView navigationView,
            DrawerLayout drawerLayout,
            MainViewModel viewModel,
            CategoryRepository categoryRepository,
            Runnable refreshMenu,
            CategoryFilterSetter categoryFilterSetter
    ) {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) viewModel.setFilterMode(1);
            else if (id == R.id.nav_next_7_days) viewModel.setFilterMode(2);
            else if (id == R.id.nav_inbox) viewModel.setFilterMode(0);
            else if (id == R.id.nav_completed) viewModel.setFilterMode(4);
            else if (id == R.id.nav_all) viewModel.setFilterMode(5);
            else if (id == R.id.nav_drafts) viewModel.setFilterMode(6);
            else if (id == R.id.nav_contacts) {
                activity.startActivity(new Intent(activity, ContactsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_media) {
                activity.startActivity(new Intent(activity, MediaActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_profile) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_theme) {
                ThemeDialogHelper.showDialog(activity);
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_add_list) {
                CategoryDialogHelper.showAddEditDialog(activity, null, (category, action) -> {
                    if ("ADD".equals(action)) {
                        categoryRepository.addCategory(category.getName());
                        if (refreshMenu != null) refreshMenu.run();
                    }
                });
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (id == R.id.nav_clear_data) {
                DialogUtils.showConfirmDialog(
                        activity,
                        "Clear All Data",
                        "Bạn chắc chắn muốn xóa tất cả dữ liệu? Hành động này không thể hoàn tác!",
                        "Clear",
                        () -> clearAllData(activity, viewModel),
                        "Cancel"
                );
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else {
                if (categoryFilterSetter != null) categoryFilterSetter.setCategoryFilter(id);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
}

