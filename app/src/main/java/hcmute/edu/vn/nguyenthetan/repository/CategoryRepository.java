package hcmute.edu.vn.nguyenthetan.repository;

import android.content.Context;
import java.util.List;
import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.model.dao.CategoryDao;

public class CategoryRepository {
    private CategoryDao categoryDao;

    // Constructor: Cần Context để khởi tạo Database
    public CategoryRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        categoryDao = db.categoryDao();
    }

    // --- CREATE (Thêm mới) ---
    public void addCategory(String name) {
        Category newCategory = new Category(name);
        categoryDao.insert(newCategory);
    }

    // --- READ (Lấy danh sách) ---
    public List<Category> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    // --- UPDATE (Cập nhật) ---
    public void updateCategory(Category category) {
        categoryDao.update(category);
    }

    // --- DELETE (Xóa) ---
    public void deleteCategory(Category category) {
        categoryDao.delete(category);
    }

}