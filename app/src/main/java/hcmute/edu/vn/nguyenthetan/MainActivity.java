package hcmute.edu.vn.nguyenthetan;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.repository.CategoryRepository;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenu;
    private CategoryRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1. Khởi tạo Repository
        repository = new CategoryRepository(this);

        // 2. Tự động nạp dữ liệu mặc định nếu trống
        if (repository.getAllCategories().isEmpty()) {
            repository.addCategory("Personal");
            repository.addCategory("Work");
            repository.addCategory("Shopping");
            repository.addCategory("Learning");
            repository.addCategory("Fitness");
            repository.addCategory("Wish List");
        }

        // 3. Ánh xạ các View
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);

        // 4. Xử lý Insets (Tránh bị che bởi Status Bar)
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 5. Mở Menu trượt
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // 6. Giữ màu gốc cho các icon
        navigationView.setItemIconTintList(null);

        // 7. Xử lý sự kiện bấm vào các mục menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_today) {
                Toast.makeText(this, "Mở danh sách Hôm nay", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_add_list) {
                CategoryDialogHelper.showAddEditDialog(this, null, (category, action) -> {
                    if (action.equals("ADD")) {
                        repository.addCategory(category.getName());
                        loadCategoriesToMenu();
                    }
                });
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Nạp danh sách lần đầu
        loadCategoriesToMenu();
    }

    private void loadCategoriesToMenu() {
        Menu menu = navigationView.getMenu();
        MenuItem listsItem = menu.findItem(R.id.nav_lists_header);
        if (listsItem == null) return;
        SubMenu subMenu = listsItem.getSubMenu();

        // Xóa group cũ
        subMenu.removeGroup(R.id.main_group_lists);

        List<Category> categories = repository.getAllCategories();
        int index = 0;
        for (Category cat : categories) {
            // Dùng ID của database làm ID cho MenuItem
            MenuItem item = subMenu.add(R.id.main_group_lists, cat.getId(), index++, cat.getName());
            item.setIcon(android.R.drawable.ic_menu_directions);

            // 1 CHẠM (1 NGÓN): HIỆN TOAST
            item.setOnMenuItemClickListener(it -> {
                Toast.makeText(this, "Bạn vừa ấn vào: " + cat.getName(), Toast.LENGTH_SHORT).show();
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // MẸO: ĐỢI MENU VẼ XONG RỒI GÁN SỰ KIỆN NHẤN GIỮ (Surrogate cho 2 ngón)
        navigationView.post(() -> {
            for (Category cat : categories) {
                View itemView = navigationView.findViewById(cat.getId());
                if (itemView != null) {
                    itemView.setOnLongClickListener(v -> {
                        // NHẤN GIỮ: HIỆN DIALOG SỬA/XÓA
                        CategoryDialogHelper.showAddEditDialog(this, cat, (updatedCat, action) -> {
                            if (action.equals("UPDATE")) {
                                repository.updateCategory(updatedCat);
                            } else if (action.equals("DELETE")) {
                                repository.deleteCategory(updatedCat);
                            }
                            loadCategoriesToMenu();
                        });
                        return true;
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}