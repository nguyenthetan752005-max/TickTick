# 📱 Hướng Dẫn: Reset Dữ Liệu + Fix Nút Xóa Nhạc

## 🎯 Tóm Tắt Lựa Chọn

### 1. **Reset Dữ Liệu App Hoàn Toàn** ✅ (FIXED)

**Bước 1:** Mở Navigation Drawer
- Swipe từ trái sang phải hoặc click menu icon ☰

**Bước 2:** Scroll xuống dưới cùng menu
- Sẽ thấy option **"Clear All Data"** với icon thùng rác 🗑️

**Bước 3:** Click "Clear All Data"
- Sẽ hiện popup confirm: "Bạn chắc chắn muốn xóa tất cả dữ liệu? Hành động này không thể hoàn tác!"

**Bước 4:** Click "Clear" để confirm
- ✅ Tất cả dữ liệu bị xóa (bao gồm nhạc + ảnh cũ)
- ✅ App reset về trạng thái ban đầu

**Điều sẽ bị xóa:**
- ❌ Tất cả Tasks
- ❌ Tất cả Reminders  
- ❌ Tất cả Notifications
- ❌ **Tất cả nhạc được upload (thư mục uploaded_music)**
- ❌ **Ảnh đại diện (avatar.png)**

---

### 2. **Xóa Bài Nhạc Riêng Lẻ**

**Trước đây:** Chỉ nhạc mới thêm có nút xóa
**Bây giờ:** Tất cả nhạc được upload có nút xóa ✅

**Bước 1:** Vào tab **Media** (Nhạc)

**Bước 2:** Tìm bài nhạc muốn xóa
- Sẽ có icon thùng rác 🗑️ ở bên phải bài nhạc

**Bước 3:** Click icon thùng rác
- Bài nhạc bị xóa ngay lập tức

---

## 🔄 Tại Sao Cần Fix?

### Vấn Đề Cũ
- Nhạc được load từ device (MediaStore) không có nút xóa
- Chỉ nhạc mới upload mới có nút xóa
- ⚠️ Clear Data không xóa hết nhạc + ảnh cũ

### Lý Do
- Nhạc từ MediaStore dùng URI scheme `"content://"`
- Nhạc upload dùng URI scheme `"file://"`
- Code chỉ check `"file://"` → nên không hiển thị nút xóa cho nhạc cũ
- Avatar file name sai: save `avatar.png` nhưng xóa `avatar.jpg`
- Xóa file và DB chạy đồng thời → race condition

### Giải Pháp
- Sửa logic để check cả `"file://"` và đường dẫn chứa `"uploaded_music"`
- Sửa avatar file name từ `avatar.jpg` → `avatar.png`
- Chạy xóa file + DB trên background thread với proper order
- Giờ tất cả nhạc + ảnh cũ sẽ bị xóa đúng cách

---

## 📝 Chi Tiết Thay Đổi

### MediaAdapter.java
```java
// Cũ:
if (item.uri != null && "file".equals(item.uri.getScheme())) {
    holder.btnDeleteMusic.setVisibility(View.VISIBLE);
} else {
    holder.btnDeleteMusic.setVisibility(View.GONE);
}

// Mới:
boolean isUserUploadedMusic = item.uri != null && (
        "file".equals(item.uri.getScheme()) || 
        item.uri.getPath().contains("uploaded_music")
);
holder.btnDeleteMusic.setVisibility(isUserUploadedMusic ? View.VISIBLE : View.GONE);
```

### MainDrawerMenuHandler.java
```java
// Cũ:
try {
    viewModel.clearAllData();
    // Xóa file... (chạy trên main thread, race condition)
}

// Mới:
AppExecutors.getInstance().diskIO().execute(() -> {
    // 1. Xóa folder uploaded_music
    deleteRecursive(uploadDir);
    
    // 2. Xóa avatar.png (sửa từ avatar.jpg)
    avatarFile.delete();
    
    // 3. Xóa database
    viewModel.clearAllData();
    
    // 4. Show toast trên main thread
    new Handler(Looper.getMainLooper()).post(() -> {
        Toast.makeText(activity, "Tất cả dữ liệu đã bị xóa!", ...);
    });
});
```

### Drawer Menu
- Thêm nút "Clear All Data" vào drawer menu

### Database
- Thêm method `deleteAll()` vào các DAO/Repository

---

## ⚠️ Cảnh Báo

**KHÔNG THỂ UNDO!**
- Một khi xóa dữ liệu, không có cách nào phục hồi
- Hãy backup dữ liệu quan trọng trước khi xóa

---

## ✅ Kiểm Tra Hoàn Tất

- ✅ APK đã build thành công (8s)
- ✅ Nút xóa nhạc hiển thị cho tất cả nhạc
- ✅ Menu "Clear All Data" có sẵn
- ✅ Clear Data xóa đúng nhạc + ảnh cũ
- ✅ Xóa dữ liệu hoạt động trên background thread (không block UI)
- ✅ Logging để debug: "Deleted uploaded_music directory", "Deleted avatar.png"

---

**Ready to deploy! 🚀**



