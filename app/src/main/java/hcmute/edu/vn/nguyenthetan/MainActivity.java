/**
 * MainActivity: Màn hình chính của ứng dụng.
 * Chức năng: Hiển thị giao diện và quan sát dữ liệu từ ViewModel.
 */
package hcmute.edu.vn.nguyenthetan;

<<<<<<< Updated upstream
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
=======
import android.media.MediaPlayer;
>>>>>>> Stashed changes
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
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener, InboxAdapter.OnInboxItemClickListener {

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
<<<<<<< Updated upstream
    private TextView tvSelectedCount, tvAppTitle;
    private ImageView btnCloseDeleteMode, btnDeleteSelected, btnEnterDeleteMode;
=======
    private TextView tvSelectedCount;
    private ImageView btnCloseDeleteMode, btnDeleteSelected, btnEnterDeleteMode, btnMusic;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
>>>>>>> Stashed changes

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
                if (rvTasks.getAdapter() != taskAdapter) rvTasks.setAdapter(taskAdapter);
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
                if (rvTasks.getAdapter() != inboxAdapter) rvTasks.setAdapter(inboxAdapter);
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    /**
     * Khởi động Foreground Service cho Reminder.
     */
    private void startReminderService() {
        Intent serviceIntent = new Intent(this, ReminderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
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
            try { tvAppTitle = (TextView) ((android.view.ViewGroup)layoutNormalBar).getChildAt(1); } catch(Exception e){}
        }
        btnCloseDeleteMode = findViewById(R.id.btnCloseDeleteMode);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnEnterDeleteMode = findViewById(R.id.btnEnterDeleteMode);
        btnMusic = findViewById(R.id.btnMusic);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        btnEnterDeleteMode.setOnClickListener(v -> {
            taskAdapter.startMultiSelect();
            updateContextualBar(true);
        });

        btnCloseDeleteMode.setOnClickListener(v -> {
            taskAdapter.clearSelection();
            updateContextualBar(false);
        });

        btnDeleteSelected.setOnClickListener(v -> {
            List<Task> selected = taskAdapter.getSelectedTasks();
            if (selected.isEmpty()) return;
            
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Xóa nhiệm vụ")
                    .setMessage("Bạn chắc chắn muốn xóa " + selected.size() + " nhiệm vụ này?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        viewModel.deleteTasks(selected);
                        taskAdapter.clearSelection();
                        updateContextualBar(false);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        btnMusic.setOnClickListener(v -> toggleMusic());
    }

    private void toggleMusic() {
        if (isPlaying) {
            pauseMusic();
        } else {
            playMusic();
        }
    }

    private void playMusic() {
        try {
            if (mediaPlayer == null) {
                // Lưu ý: Bạn cần thêm file nhạc (ví dụ: chill_rain_piano.mp3) vào thư mục res/raw
                // Nếu chưa có file, app có thể bị crash hoặc không kêu. 
                // Ở đây tôi giả định file tên là 'chill_music'
                int resId = getResources().getIdentifier("chill_music", "raw", getPackageName());
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(this, resId);
                    mediaPlayer.setLooping(true);
                } else {
                    Toast.makeText(this, "Vui lòng thêm file nhạc vào res/raw/chill_music.mp3", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            mediaPlayer.start();
            isPlaying = true;
            btnMusic.setImageResource(android.R.drawable.ic_lock_silent_mode); // Đổi icon sang đang phát
            Toast.makeText(this, "Đang phát nhạc chill...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            btnMusic.setImageResource(android.R.drawable.ic_lock_silent_mode_off); // Đổi icon sang tắt
            Toast.makeText(this, "Đã dừng nhạc", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (taskAdapter != null && taskAdapter.getSelectedTasks().size() > 0) {
                    taskAdapter.clearSelection();
                    updateContextualBar(false);
                } else if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
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

    private void updateTitleBar() {
        if (tvAppTitle == null) return;
        int mode = viewModel.getCurrentFilterMode();
        switch (mode) {
            case 0: tvAppTitle.setText("Hộp thư đến"); break;
            case 1: tvAppTitle.setText("Hôm nay"); break;
            case 2: tvAppTitle.setText("7 ngày kế tiếp"); break;
            case 4: tvAppTitle.setText("Đã hoàn thành"); break;
            case 5: tvAppTitle.setText("Tất cả nhiệm vụ"); break;
            case 6: tvAppTitle.setText("Nhiệm vụ nháp"); break;
            default: tvAppTitle.setText("Nhiệm vụ"); break;
        }
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);
        inboxAdapter = new InboxAdapter(this);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupMenuListeners() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) viewModel.setFilterMode(1);
            else if (id == R.id.nav_next_7_days) viewModel.setFilterMode(2);
            else if (id == R.id.nav_inbox) viewModel.setFilterMode(0);
            else if (id == R.id.nav_completed) viewModel.setFilterMode(4);
            else if (id == R.id.nav_all) viewModel.setFilterMode(5);
            else if (id == R.id.nav_drafts) viewModel.setFilterMode(6);
            else if (id == R.id.nav_add_list) {
                CategoryDialogHelper.showAddEditDialog(this, null, (category, action) -> {
                    if (action.equals("ADD")) {
                        categoryRepository.addCategory(category.getName());
                        refreshMenu();
                    }
                });
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else {
                viewModel.setCategoryFilter(id);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
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
        if (taskAdapter.getSelectedTasks().size() > 0) return;
        List<Category> categories = categoryRepository.getAllCategories();
        TaskDialogHelper.showTaskDialog(this, categories, task,
                (TaskDialogHelper.TaskCallback) (updatedTask, pendingReminders) -> {
                    viewModel.updateTask(updatedTask);
                    // Pending reminders cho edit mode đã được xử lý trong TaskDialogHelper
                });
    }

    @Override
    public void onTaskLongClick(Task task) {
        updateContextualBar(true);
    }

    @Override
    public void onCompleteClick(Task task) {
        if (!task.isCompleted()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Hoàn thành nhiệm vụ")
                    .setMessage("Bạn chắc chắn muốn đánh dấu hoàn thành nhiệm vụ này?")
                    .setPositiveButton("Đồng ý", (d, w) -> {
                        task.setCompleted(true);
                        viewModel.completeTask(task);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        updateContextualBar(count > 0);
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
}
