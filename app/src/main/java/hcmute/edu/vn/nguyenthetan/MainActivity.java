/**
 * MainActivity: Màn hình chính của ứng dụng.
 * Chức năng: Hiển thị giao diện và quan sát dữ liệu từ ViewModel.
 */
package hcmute.edu.vn.nguyenthetan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.PopupMenu;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.adapter.InboxAdapter;
import hcmute.edu.vn.nguyenthetan.adapter.InboxItem;
import hcmute.edu.vn.nguyenthetan.adapter.TaskAdapter;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.CategoryRepository;
import hcmute.edu.vn.nguyenthetan.service.ReminderService;
import hcmute.edu.vn.nguyenthetan.view.MainViewModel;
import hcmute.edu.vn.nguyenthetan.util.DialogUtils;
import hcmute.edu.vn.nguyenthetan.util.EdgeInsetsUtil;
import hcmute.edu.vn.nguyenthetan.util.ProfilePrefsUtil;
import hcmute.edu.vn.nguyenthetan.ui.main.MainSearchDialog;
import hcmute.edu.vn.nguyenthetan.ui.main.MainTitleResolver;
import hcmute.edu.vn.nguyenthetan.ui.main.MainDrawerMenuHandler;
import hcmute.edu.vn.nguyenthetan.ui.main.MultiSelectDeleteController;
import hcmute.edu.vn.nguyenthetan.ui.main.NavHeaderActions;

public class MainActivity extends BaseActivity
        implements TaskAdapter.OnTaskClickListener, InboxAdapter.OnInboxItemClickListener {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private CategoryRepository categoryRepository;
    private MainViewModel viewModel;

    private RecyclerView rvTasks;
    private View layoutEmptyState;
    private TaskAdapter taskAdapter;
    private InboxAdapter inboxAdapter;

    private View layoutNormalBar, layoutDeleteBar;
    private TextView tvSelectedCount, tvAppTitle;
    private ImageView btnCloseDeleteMode, btnDeleteSelected, btnEnterDeleteMode;
    private MultiSelectDeleteController multiSelectDeleteController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Splash Screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        long startTime = System.currentTimeMillis();
        splashScreen.setKeepOnScreenCondition(() -> System.currentTimeMillis() - startTime < 2000);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // Khởi tạo ViewModel và Repository
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        categoryRepository = new CategoryRepository(this);

        initViews();
        setupRecyclerView();
        setupMenuListeners();
        setupOnBackPressed();

        // Quan sát dữ liệu Task (các filter khác)
        viewModel.getTasks().observe(this, tasks -> {
            if (viewModel.getCurrentFilterMode() != 0) {
                if (rvTasks.getAdapter() != taskAdapter)
                    rvTasks.setAdapter(taskAdapter);
                updateTitleBar();

                if (tasks == null || tasks.isEmpty()) {
                    rvTasks.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                } else {
                    rvTasks.setVisibility(View.VISIBLE);
                    layoutEmptyState.setVisibility(View.GONE);
                    taskAdapter.setData(tasks);
                }
            }
        });

        // Quan sát dữ liệu Inbox
        viewModel.getInboxData().observe(this, inboxItems -> {
            if (viewModel.getCurrentFilterMode() == 0) {
                if (rvTasks.getAdapter() != inboxAdapter)
                    rvTasks.setAdapter(inboxAdapter);
                updateTitleBar();

                if (inboxItems == null || inboxItems.isEmpty()) {
                    rvTasks.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                } else {
                    rvTasks.setVisibility(View.VISIBLE);
                    layoutEmptyState.setVisibility(View.GONE);
                    inboxAdapter.setData(inboxItems);
                }
            }
        });

        // Nạp danh mục mặc định và refresh menu
        checkAndInitDefaultCategories();
        refreshMenu();
        viewModel.loadTasks(); // Load lần đầu

        // Xin quyền thông báo (Android 13+) và khởi động Reminder Service
        requestNotificationPermission();
        startReminderService();
    }

    /**
     * Xin quyền POST_NOTIFICATIONS cho Android 13+.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    /**
     * Khởi động Foreground Service cho Reminder.
     */
    private void startReminderService() {
        try {
            Intent serviceIntent = new Intent(this, ReminderService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            // Fallback: nếu startForegroundService fail, dùng startService
            try {
                Intent serviceIntent = new Intent(this, ReminderService.class);
                startService(serviceIntent);
            } catch (Exception ignored) {}
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        rvTasks = findViewById(R.id.rvTasks);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        layoutNormalBar = findViewById(R.id.layoutNormalBar);
        layoutDeleteBar = findViewById(R.id.layoutDeleteBar);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvAppTitle = findViewById(R.id.tvAppTitle); // Reference to the Title textview
        if (tvAppTitle == null) {
            // Fallback, see if there's a title TextView in normal bar
            // Normally TickTick clone has a TextView acting as app title
            // Let's assume there is one if not found we will just wrap it later.
            try {
                tvAppTitle = (TextView) ((android.view.ViewGroup) layoutNormalBar).getChildAt(1);
            } catch (Exception e) {
            }
        }
        btnCloseDeleteMode = findViewById(R.id.btnCloseDeleteMode);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnEnterDeleteMode = findViewById(R.id.btnEnterDeleteMode);

        EdgeInsetsUtil.applySystemBarsPadding(findViewById(R.id.main));

        findViewById(R.id.fab).setOnClickListener(v -> {
            List<Category> categories = categoryRepository.getAllCategories();
            TaskDialogHelper.showTaskDialog(this, categories, null,
                    (TaskDialogHelper.TaskCallback) (task, pendingReminders) -> {
                        if (pendingReminders != null && !pendingReminders.isEmpty()) {
                            viewModel.addTaskWithReminders(task, pendingReminders);
                        } else {
                            viewModel.addTask(task);
                        }
                    });
        });

        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setItemIconTintList(null);

        // === Bottom Navigation Icons ===
        findViewById(R.id.ivNavTasks).setOnClickListener(v -> {
            viewModel.setFilterMode(5); // Tất cả nhiệm vụ
        });

        findViewById(R.id.ivNavCalendar).setOnClickListener(v -> {
            viewModel.setFilterMode(1); // Hôm nay
        });

        findViewById(R.id.ivNavSettings).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START); // Mở Navigation Drawer
        });

        // === Top Bar: Nút More (Sắp xếp) ===
        View btnMore = findViewById(R.id.btnMore);
        btnMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_sort, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.sort_by_name) {
                    viewModel.sortCurrentTasks(0);
                    Toast.makeText(this, "Sắp xếp theo tên", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.sort_by_deadline) {
                    viewModel.sortCurrentTasks(1);
                    Toast.makeText(this, "Sắp xếp theo deadline", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.sort_by_newest) {
                    viewModel.sortCurrentTasks(2);
                    Toast.makeText(this, "Mới tạo nhất", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            popupMenu.show();
        });

        NavHeaderActions.setup(
                this,
                drawerLayout,
                navigationView,
                viewModel,
                keyword -> {
                    if (tvAppTitle != null) tvAppTitle.setText("Kết quả: \"" + keyword + "\"");
                }
        );
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (multiSelectDeleteController != null
                        && multiSelectDeleteController.handleBackPressedIfNeeded()) {
                    return;
                }
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void updateTitleBar() {
        if (tvAppTitle == null)
            return;
        int mode = viewModel.getCurrentFilterMode();
        tvAppTitle.setText(MainTitleResolver.resolveTitle(mode));
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        inboxAdapter = new InboxAdapter(this);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        multiSelectDeleteController = new MultiSelectDeleteController(
                this,
                viewModel,
                taskAdapter,
                layoutNormalBar,
                layoutDeleteBar,
                tvSelectedCount,
                btnCloseDeleteMode,
                btnDeleteSelected,
                btnEnterDeleteMode
        );
        multiSelectDeleteController.bind();
    }

    private void setupMenuListeners() {
        MainDrawerMenuHandler.setup(
                this,
                navigationView,
                drawerLayout,
                viewModel,
                categoryRepository,
                this::refreshMenu,
                this::setCategoryFilter
        );
    }

    private void refreshMenu() {
        NavigationMenuHelper.loadCategories(this, navigationView, categoryRepository, drawerLayout);
    }

    private void checkAndInitDefaultCategories() {
        if (categoryRepository.getAllCategories().isEmpty()) {
            categoryRepository.addCategory("Personal");
            categoryRepository.addCategory("Work");
            categoryRepository.addCategory("Shopping");
            categoryRepository.addCategory("Learning");
            categoryRepository.addCategory("Fitness");
            categoryRepository.addCategory("Wish List");
        }
    }

    @Override
    public void onTaskClick(Task task) {
        if (task.isCompleted() || viewModel.getCurrentFilterMode() == 4) {
            Toast.makeText(this, "Nhiệm vụ đã hoàn thành, chỉ có thể chọn để xóa", Toast.LENGTH_SHORT).show();
            return;
        }
        if (taskAdapter.getSelectedTasks().size() > 0)
            return;
        List<Category> categories = categoryRepository.getAllCategories();
        TaskDialogHelper.showTaskDialog(this, categories, task,
                (TaskDialogHelper.TaskCallback) (updatedTask, pendingReminders) -> {
                    viewModel.updateTask(updatedTask);
                    // Pending reminders cho edit mode đã được xử lý trong TaskDialogHelper
                });
    }

    @Override
    public void onTaskLongClick(Task task) {
        if (multiSelectDeleteController != null) multiSelectDeleteController.onSelectionChanged(1);
    }

    @Override
    public void onCompleteClick(Task task) {
        if (!task.isCompleted()) {
            DialogUtils.showConfirmDialog(
                    this,
                    "Hoàn thành nhiệm vụ",
                    "Bạn chắc chắn muốn đánh dấu hoàn thành nhiệm vụ này?",
                    "Đồng ý",
                    () -> {
                        task.setCompleted(true);
                        viewModel.completeTask(task);
                    },
                    "Hủy"
            );
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        if (multiSelectDeleteController != null) multiSelectDeleteController.onSelectionChanged(count);
    }

    public void setCategoryFilter(int categoryId) {
        viewModel.setCategoryFilter(categoryId);
    }

    // --- InboxAdapter Callbacks ---
    @Override
    public void onNotificationClick(AppNotification notification) {
        notification.setRead(true);
        viewModel.updateNotification(notification);
    }

    @Override
    public void onNotificationDeleteClick(AppNotification notification) {
        viewModel.deleteNotification(notification);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavHeaderActions.loadProfileHeaderInfo(this, navigationView);
    }
}
