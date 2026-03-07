/**
 * MainActivity: Màn hình chính của ứng dụng.
 */
package hcmute.edu.vn.nguyenthetan;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.adapter.TaskAdapter;
import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.CategoryRepository;
import hcmute.edu.vn.nguyenthetan.repository.TaskRepository;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenu;
    private CategoryRepository repository;
    private TaskRepository taskRepository;

    private RecyclerView rvTasks;
    private View layoutEmptyState;
    private TaskAdapter taskAdapter;

    private View layoutNormalBar, layoutDeleteBar;
    private TextView tvSelectedCount;
    private ImageView btnCloseDeleteMode, btnDeleteSelected, btnEnterDeleteMode;

    // 0: Hộp thư đến (Tất cả), 1: Hôm nay, 2: 7 ngày tới, 3: Theo Danh mục cụ thể, 4: Đã hoàn thành
    private int currentFilterMode = 0;
    private int currentCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        long startTime = System.currentTimeMillis();
        SplashScreen.installSplashScreen(this).setKeepOnScreenCondition(() -> System.currentTimeMillis() - startTime < 2000);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        repository = new CategoryRepository(this);
        taskRepository = new TaskRepository(this);

        deleteData();
        initViews();
        setupRecyclerView();
        setupMenuListeners();
    }

    private void deleteData(){
        new Thread(() -> {
            AppDatabase.getInstance(this).clearAllTables();
            runOnUiThread(() -> {
                checkAndInitDefaultCategories();
                refreshMenu();
                refreshTaskList();
            });
        }).start();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        rvTasks = findViewById(R.id.rvTasks);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // Contextual Bar Views
        layoutNormalBar = findViewById(R.id.layoutNormalBar);
        layoutDeleteBar = findViewById(R.id.layoutDeleteBar);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnCloseDeleteMode = findViewById(R.id.btnCloseDeleteMode);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnEnterDeleteMode = findViewById(R.id.btnEnterDeleteMode);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.fab).setOnClickListener(v -> {
            List<Category> categories = repository.getAllCategories();
            if (categories.isEmpty()) {
                Toast.makeText(this, "Vui lòng tạo danh mục trước!", Toast.LENGTH_SHORT).show();
                return;
            }
            TaskDialogHelper.showTaskDialog(this, categories, null, task -> {
                taskRepository.addTask(task);
                refreshTaskList();
            });
        });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setItemIconTintList(null);

        // Enter Delete Mode via Trash Icon
        btnEnterDeleteMode.setOnClickListener(v -> {
            taskAdapter.startMultiSelect();
            updateContextualBar(true);
        });

        // Exit Delete Mode
        btnCloseDeleteMode.setOnClickListener(v -> {
            taskAdapter.clearSelection();
            updateContextualBar(false);
        });

        // Delete Action
        btnDeleteSelected.setOnClickListener(v -> {
            List<Task> selected = taskAdapter.getSelectedTasks();
            if (selected.isEmpty()) return;
            
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Xóa nhiệm vụ")
                    .setMessage("Bạn chắc chắn muốn xóa " + selected.size() + " nhiệm vụ này?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        taskRepository.deleteMultiple(selected);
                        taskAdapter.clearSelection();
                        updateContextualBar(false);
                        refreshTaskList();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void updateContextualBar(boolean isDeleteMode) {
        if (layoutNormalBar == null || layoutDeleteBar == null) return;
        
        layoutNormalBar.setVisibility(isDeleteMode ? View.GONE : View.VISIBLE);
        layoutDeleteBar.setVisibility(isDeleteMode ? View.VISIBLE : View.GONE);
        if (isDeleteMode) {
            tvSelectedCount.setText(taskAdapter.getSelectedTasks().size() + " selected");
        }
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupMenuListeners() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) currentFilterMode = 1;
            else if (id == R.id.nav_next_7_days) currentFilterMode = 2;
            else if (id == R.id.nav_inbox) currentFilterMode = 0;
            else if (id == R.id.nav_completed) currentFilterMode = 4;
            else if (id == R.id.nav_add_list) {
                CategoryDialogHelper.showAddEditDialog(this, null, (category, action) -> {
                    if (action.equals("ADD")) {
                        repository.addCategory(category.getName());
                        refreshMenu();
                    }
                });
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else {
                currentFilterMode = 3;
                currentCategoryId = id;
            }
            refreshTaskList();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    public void refreshTaskList() {
        List<Task> tasks;
        // Chặn đánh dấu hoàn thành nếu đang ở mục "Đã hoàn thành"
        taskAdapter.setCanToggleComplete(currentFilterMode != 4);

        switch (currentFilterMode) {
            case 1: tasks = taskRepository.getTasksToday(); break;
            case 2: tasks = taskRepository.getTasksNext7Days(); break;
            case 3: tasks = taskRepository.getTasksByCategoryId(currentCategoryId); break;
            case 4: tasks = taskRepository.getCompletedTasks(); break;
            default: tasks = taskRepository.getAllTasks(); break;
        }

        if (tasks == null || tasks.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            taskAdapter.setData(tasks);
        }
    }

    private void refreshMenu() {
        NavigationMenuHelper.loadCategories(this, navigationView, repository, drawerLayout);
    }

    private void checkAndInitDefaultCategories() {
        if (repository.getAllCategories().isEmpty()) {
            repository.addCategory("Personal");
            repository.addCategory("Work");
            repository.addCategory("Shopping");
            repository.addCategory("Learning");
            repository.addCategory("Fitness");
            repository.addCategory("Wish List");
        }
    }

    @Override
    public void onTaskClick(Task task) {
        if (taskAdapter.getSelectedTasks().size() > 0) {
            updateContextualBar(true);
            return;
        }
        List<Category> categories = repository.getAllCategories();
        TaskDialogHelper.showTaskDialog(this, categories, task, updatedTask -> {
            taskRepository.updateTask(updatedTask);
            refreshTaskList();
        });
    }

    @Override
    public void onTaskLongClick(Task task) {
        updateContextualBar(true);
    }

    @Override
    public void onCompleteClick(Task task) {
        // Chỉ hiện hộp thoại xác nhận nếu task chưa hoàn thành
        if (!task.isCompleted()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Hoàn thành nhiệm vụ")
                    .setMessage("Bạn chắc chắn muốn đánh dấu hoàn thành nhiệm vụ này?")
                    .setPositiveButton("Đồng ý", (d, w) -> {
                        task.setCompleted(true);
                        taskRepository.updateTask(task);
                        refreshTaskList();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count == 0) {
            updateContextualBar(false);
        } else {
            updateContextualBar(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (taskAdapter.getSelectedTasks().size() > 0) {
            taskAdapter.clearSelection();
            updateContextualBar(false);
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void setCategoryFilter(int categoryId) {
        this.currentFilterMode = 3;
        this.currentCategoryId = categoryId;
        refreshTaskList();
    }
}