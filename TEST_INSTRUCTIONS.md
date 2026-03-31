# 📱 Hướng Dẫn Test Pixel_4 (API 29) vs Medium Phone (API 36)

## 🎯 Mục Tiêu
Xác minh rằng ứng dụng chạy được trên cả hai emulator mà không crash.

---

## 📋 Checklist Test

### ✅ Test trên Pixel_4 (API 29)

#### 1. Khởi Động Ứng Dụng
- [ ] App tải splash screen
- [ ] App hiển thị màn hình chính (Inbox/Tasks)
- [ ] Không có crash

#### 2. Notification Service
- [ ] Kiểm tra Logcat không có error liên quan đến:
  - `ForegroundServiceType`
  - `SecurityException` từ AlarmManager
  - `Resources$NotFoundException` (icon)

#### 3. Tạo Task với Reminder
- [ ] Tạo task mới (nhấn FAB button)
- [ ] Thêm reminder cho task
- [ ] Nhấn Save
- [ ] Notification được tạo (kiểm tra Logcat)
- [ ] Không có crash

#### 4. Xem Menu Drawer
- [ ] Mở drawer menu (swipe left hoặc nhấn menu icon)
- [ ] Không có crash
- [ ] Các items render đúng

#### 5. Kiểm tra Permission
- [ ] App xin permission POST_NOTIFICATIONS (Android 13+)
- [ ] Không xin các permission không cần thiết

---

### ✅ Test trên Medium Phone (API 36)

#### 1. Khởi Động Ứng Dụng
- [ ] App tải splash screen
- [ ] App hiển thị màn hình chính
- [ ] Không có crash

#### 2. Notification Service
- [ ] Exact alarm được sử dụng (SCHEDULE_EXACT_ALARM available)
- [ ] Kiểm tra Logcat: không có warning/error

#### 3. Tạo Task với Reminder
- [ ] Tạo task mới
- [ ] Thêm reminder chính xác (exact alarm)
- [ ] Nhấn Save
- [ ] Notification được tạo
- [ ] Không có crash

#### 4. Xem Menu Drawer
- [ ] Mở drawer menu
- [ ] Không có crash

#### 5. Kiểm tra Permission
- [ ] Xin POST_NOTIFICATIONS
- [ ] Xin READ_MEDIA_IMAGES, READ_MEDIA_AUDIO (nếu access media)
- [ ] Xin READ_CONTACTS (nếu access contacts)

---

## 🔍 Logcat Inspection

### Lệnh Xem Logcat
```bash
# Terminal (PowerShell)
adb logcat | findstr "TickTick\|ERROR\|Exception"
```

### Những Error Cần Tránh
❌ `ForegroundServiceType` - Lỗi manifest  
❌ `SecurityException` - Permission denied  
❌ `ClassNotFoundException` - Class không tìm thấy  
❌ `ResourcesNotFoundException` - Icon/drawable không tìm thấy  
❌ `NullPointerException` - Code bug  

### Những Log OK
✅ `Đã đặt alarm cho reminder` - AlarmManager hoạt động  
✅ `Đã lên lịch lại reminders` - Boot receiver hoạt động  
✅ Không có crash traces  

---

## 🚀 Deploy & Test Steps

### Bước 1: Build APK
```bash
cd D:\TickTick
.\gradlew.bat build
```

**Expected Output:**
```
BUILD SUCCESSFUL in XXs
```

### Bước 2: Liệt kê Emulator
```bash
adb devices
```

**Expected Output:**
```
List of attached devices
emulator-5554   device    (Pixel_4 - API 29)
emulator-5556   device    (Medium Phone - API 36)
```

### Bước 3: Install APK trên Pixel_4
```bash
# Select Pixel_4 emulator
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

**Expected Output:**
```
Success
```

### Bước 4: Launch App trên Pixel_4
```bash
adb -s emulator-5554 shell am start -n hcmute.edu.vn.nguyenthetan/.MainActivity
```

### Bước 5: Check Logcat
```bash
adb -s emulator-5554 logcat | findstr "TickTick"
```

### Bước 6: Repeat cho Medium Phone
```bash
adb -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5556 shell am start -n hcmute.edu.vn.nguyenthetan/.MainActivity
adb -s emulator-5556 logcat | findstr "TickTick"
```

---

## 📊 Expected Behavior Comparison

| Feature | Pixel_4 (API 29) | Medium Phone (API 36) |
|---|---|---|
| App Startup | ✅ No crash | ✅ No crash |
| Notification Icon | `ic_dialog_info` | `ic_dialog_info` |
| Alarm Type | Inexact (fallback) | Exact (SCHEDULE_EXACT_ALARM) |
| Foreground Service | No type specified | Type: SPECIAL_USE / MEDIA_PLAYBACK |
| Permission Level | Basic | Full (API 31+ features) |

---

## 💡 Troubleshooting

### Issue: APK Size Too Large
**Solution:** ứng dụng build debug, size lớn là bình thường

### Issue: App Crashes on Startup
**Check:**
1. Logcat for `ClassNotFoundException`
2. Build file có error không
3. Manifest syntax valid không

### Issue: Notification Not Appearing
**Check:**
1. `FOREGROUND_SERVICE` permission declared
2. `POST_NOTIFICATIONS` permission granted (API 13+)
3. Notification channel created

### Issue: Alarm Not Triggering
**Check:**
1. `SCHEDULE_EXACT_ALARM` permission (API 31+ only)
2. Time set in future
3. AlarmManager not cancelled

---

## ✅ Success Criteria

- ✅ App runs on both Pixel_4 (API 29) and Medium Phone (API 36)
- ✅ No crash on startup
- ✅ Notifications display correctly
- ✅ Alarms trigger on time
- ✅ No relevant errors in Logcat
- ✅ All features work on both devices

---

## 📝 Notes
- Pixel_4 API 29: Uses inexact alarms (fallback)
- Medium Phone API 36: Uses exact alarms (full feature)
- Both should work seamlessly without crashes

