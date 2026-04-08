# BÁO CÁO CHI TIẾT: CHỨC NĂNG DANH BẠ TRONG TICKTICK

## 1. TỔNG QUAN

Chức năng Danh bạ trong ứng dụng TickTick được thiết kế để quản lý liên lạc toàn diện với **3 tab chính**:
- **Tab Danh bạ**: Hiển thị danh sách contacts từ thiết bị và SMS đã nhận (thông qua Broadcast Receiver)
- **Tab Tin nhắn**: Đọc lịch sử tin nhắn từ Content Provider `content://sms/inbox`
- **Tab Lịch sử gọi**: Đọc lịch sử cuộc gọi từ Content Provider `content://call_log/calls`

Báo cáo này tập trung phân tích vai trò của **Broadcast Receiver** và **Content Provider** trong việc xử lý dữ liệu liên lạc.

---

## 2. BROADCAST RECEIVER (SMS RECEIVER)

### 2.1 Khái niệm

Broadcast Receiver là một trong 4 thành phần chính của Android (cùng với Activity, Service, Content Provider). Nó cho phép ứng dụng nhận các sự kiện (broadcast) từ hệ thống hoặc từ các ứng dụng khác.

**Trong TickTick**: Broadcast Receiver được sử dụng để nhận tin nhắn SMS đến theo thời gian thực.

### 2.2 Luồng hoạt động

```
SMS den thiet bi → He thong phat broadcast → SmsReceiver nhan su kien
                                                       ↓
                                              Parse PDU → SmsMessage
                                                       ↓
                                              Luu vao AppDatabase
                                                       ↓
                                              Hien thi o Tab Danh ba
```

### 2.3 Triển khai chi tiết

#### File: `receiver/SmsReceiver.java`

```java
public class SmsReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED_ACTION = 
        "android.provider.Telephony.SMS_RECEIVED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Kiem tra action
        if (intent == null || !SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        
        // 1. Trich xuat SMS tu PDUs
        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        
        // 2. Parse tung PDU thanh SmsMessage
        StringBuilder messageBody = new StringBuilder();
        String senderPhone = null;
        
        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (senderPhone == null) {
                senderPhone = smsMessage.getOriginatingAddress();
            }
            messageBody.append(smsMessage.getMessageBody());
        }
        
        // 3. Luu vao Database tren background thread
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppNotificationRepository repo = new AppNotificationRepository(context);
            AppNotification notification = new AppNotification(
                0,                          // taskId = 0
                "SMS: " + senderPhone,      // taskName = so dien thoai
                messageBody.toString(),     // message = noi dung
                System.currentTimeMillis()  // timestamp
            );
            repo.addNotification(notification);
        });
    }
}
```

#### Đăng ký trong AndroidManifest.xml

```xml
<receiver
    android:name=".receiver.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

#### Các class liên quan

| File | Class | Vai trò |
|------|-------|---------|
| `receiver/SmsReceiver.java` | `SmsReceiver` | Nhận SMS từ hệ thống |
| `model/AppNotification.java` | `AppNotification` | Model lưu SMS vào DB |
| `repository/AppNotificationRepository.java` | `AppNotificationRepository` | Thao tác DB |
| `adapter/ReceivedSmsAdapter.java` | `ReceivedSmsAdapter` | Hiển thị SMS đã lưu |
| `ContactsActivity.java` | `loadReceivedSms()` | Đọc và hiển thị từ DB |

### 2.4 Đặc điểm của Broadcast Receiver

| Đặc điểm | Giá trị |
|----------|---------|
| **Trigger** | Khi có SMS mới đến (real-time) |
| **Dữ liệu** | Chỉ SMS vừa nhận |
| **Persistence** | Cần lưu vào Database để xem lại |
| **Lifecycle** | Chạy ngắn, không được chạy lâu |
| **Thread** | onReceive() chạy trên main thread → phải dùng background thread |

---

## 3. CONTENT PROVIDER (SMS VÀ CALL LOG)

### 3.1 Khái niệm

Content Provider là cơ chế quản lý và chia sẻ dữ liệu giữa các ứng dụng trong Android. Nó cung cấp giao diện thống nhất để truy cập dữ liệu thông qua **URI** và **ContentResolver**.

**Trong TickTick**: Đọc lịch sử tin nhắn và cuộc gọi từ ứng dụng hệ thống.

### 3.2 Các Content Provider sử dụng

| Content Provider | URI | Mục đích |
|-----------------|-----|----------|
| SMS Provider | `content://sms/inbox` | Đọc tin nhắn đến |
| Call Log Provider | `content://call_log/calls` | Đọc lịch sử cuộc gọi |

### 3.3 Triển khai: SMS Content Provider

#### File: `ContactsActivity.java` - Hàm `loadSmsFromProvider()`

```java
private void loadSmsFromProvider() {
    // 1. Kiểm tra permission
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
        tvEmptySms.setVisibility(View.VISIBLE);
        return;
    }
    
    // 2. Chạy trên background thread
    executorService.execute(() -> {
        List<SmsItem> smsList = new ArrayList<>();
        
        // 3. Định nghĩa URI và projection
        Uri smsUri = Uri.parse("content://sms/inbox");
        String[] projection = {
            "_id",      // ID tin nhắn
            "address",  // Số điện thoại
            "body",     // Nội dung
            "date",     // Timestamp
            "type",     // 1 = inbox, 2 = sent
            "read"      // 0 = chưa đọc, 1 = đã đọc
        };
        
        // 4. Query ContentResolver
        Cursor cursor = getContentResolver().query(
            smsUri,
            projection,
            null,              // selection
            null,              // selectionArgs
            "date DESC LIMIT 100"  // Mới nhất trước, giới hạn 100
        );
        
        // 5. Parse Cursor thành List<SmsItem>
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                int read = cursor.getInt(cursor.getColumnIndexOrThrow("read"));
                
                // Tìm tên contact từ số điện thoại
                String contactName = getContactNameFromNumber(address);
                
                smsList.add(new SmsItem(id, address, contactName, body, date, type, read == 1));
            }
            cursor.close();
        }
        
        // 6. Cập nhật UI trên main thread
        runOnUiThread(() -> {
            smsListAdapter.setData(smsList);
        });
    });
}
```

### 3.4 Triển khai: Call Log Content Provider

#### File: `ContactsActivity.java` - Hàm `loadCallLogFromProvider()`

```java
private void loadCallLogFromProvider() {
    // 1. Kiểm tra permission
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
        tvEmptyCallLog.setVisibility(View.VISIBLE);
        return;
    }
    
    // 2. Chạy trên background thread
    executorService.execute(() -> {
        List<CallLogItem> callLogList = new ArrayList<>();
        
        // 3. Định nghĩa projection
        String[] projection = {
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        };
        
        // 4. Query ContentResolver
        Cursor cursor = getContentResolver().query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            CallLog.Calls.DATE + " DESC LIMIT 100"
        );
        
        // 5. Parse Cursor
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls._ID));
                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                
                callLogList.add(new CallLogItem(id, number, name, date, type, duration));
            }
            cursor.close();
        }
        
        // 6. Cập nhật UI
        runOnUiThread(() -> {
            callLogAdapter.setData(callLogList);
        });
    });
}
```

### 3.5 Bảng cột dữ liệu

**SMS Provider (`content://sms/inbox`)**:

| Cột | Kiểu | Ý nghĩa |
|-----|------|---------|
| `_id` | long | ID tin nhắn |
| `address` | String | Số điện thoại |
| `body` | String | Nội dung |
| `date` | long | Timestamp |
| `type` | int | 1=inbox, 2=sent, 3=draft |
| `read` | int | 0=unread, 1=read |

**Call Log Provider (`content://call_log/calls`)**:

| Cột | Kiểu | Ý nghĩa |
|-----|------|---------|
| `_id` | long | ID cuộc gọi |
| `number` | String | Số điện thoại |
| `cached_name` | String | Tên trong danh bạ (nullable) |
| `type` | int | 1=incoming, 2=outgoing, 3=missed, 5=rejected |
| `date` | long | Timestamp |
| `duration` | long | Thời lượng (giây) |

---

## 4. SO SÁNH BROADCAST RECEIVER vs CONTENT PROVIDER

| Tiêu chí | Broadcast Receiver | Content Provider |
|----------|-------------------|------------------|
| **Thời điểm** | Real-time khi SMS đến | Khi user mở tab |
| **Dữ liệu** | Chỉ SMS mới đến | Toàn bộ lịch sử (100 items) |
| **Nguồn** | Hệ thống phát broadcast | Query từ app hệ thống |
| **Cách lấy** | Đăng ký intent-filter | ContentResolver.query() |
| **Lưu trữ** | Lưu vào AppDatabase | Không lưu, đọc trực tiếp |
| **Hiển thị** | Tab Danh bạ (SMS Receiver) | Tab Tin nhắn / Lịch sử gọi |
| **Thread** | Main thread (cần background) | Background thread (ExecutorService) |
| **Permission** | RECEIVE_SMS | READ_SMS, READ_CALL_LOG |

---

## 5. TỔNG HỢP FILE VÀ HÀM

### 5.1 Danh sách file liên quan

| Chức năng | File | Class/Hàm chính |
|-----------|------|-----------------|
| Nhận SMS mới | `receiver/SmsReceiver.java` | `SmsReceiver.onReceive()` |
| Lưu SMS vào DB | `repository/AppNotificationRepository.java` | `addNotification()` |
| Hiển thị SMS (cũ) | `adapter/ReceivedSmsAdapter.java` | `ReceivedSmsAdapter` |
| Đọc SMS từ CP | `ContactsActivity.java` | `loadSmsFromProvider()` |
| Đọc Call Log từ CP | `ContactsActivity.java` | `loadCallLogFromProvider()` |
| Model SMS | `model/SmsItem.java` | `SmsItem` |
| Model Call Log | `model/CallLogItem.java` | `CallLogItem` |
| Adapter SMS | `adapter/SmsListAdapter.java` | `SmsListAdapter` |
| Adapter Call Log | `adapter/CallLogAdapter.java` | `CallLogAdapter` |
| Chuyển tab | `ContactsActivity.java` | `switchTab()` |
| Tìm tên contact | `ContactsActivity.java` | `getContactNameFromNumber()` |

### 5.2 Layout files

| File | Mô tả |
|------|-------|
| `activity_contacts.xml` | Layout chính với 3 tab |
| `item_sms.xml` | Layout item tin nhắn |
| `item_call_log.xml` | Layout item cuộc gọi |

---

## 6. LUỒNG 3 TAB

```
┌─────────────────────────────────────────────────────────────┐
│                    ContactsActivity                        │
├──────────────┬──────────────────┬──────────────────────────┤
│   Tab Danh   │    Tab Tin nhắn  │    Tab Lịch sử gọi       │
│    bạ        │                  │                          │
├──────────────┼──────────────────┼──────────────────────────┤
│              │                  │                          │
│  Danh bạ     │  Content Provider│   Content Provider        │
│  (Contacts   │  content://sms/  │   content://call_log/    │
│   Provider)  │  inbox           │   calls                   │
│              │                  │                          │
│  SMS từ      │  Query trực      │   Query trực             │
│  Broadcast   │  tiếp 100 tin    │   tiếp 100 cuộc gọi      │
│  Receiver    │  nhắn mới nhất   │   mới nhất               │
│              │                  │                          │
└──────────────┴──────────────────┴──────────────────────────┘
```

---

## 7. PERMISSIONS

### 7.1 AndroidManifest.xml

```xml
<!-- Broadcast Receiver -->
<uses-permission android:name="android.permission.RECEIVE_SMS" />

<!-- Content Provider -->
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />

<!-- Khác -->
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.SEND_SMS" />
```

### 7.2 Runtime Permissions

Tất cả permissions đều cần xin runtime từ Android 6.0+ (API 23):

```java
String[] REQUIRED_PERMISSIONS = {
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_SMS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.SEND_SMS,
    Manifest.permission.RECEIVE_SMS
};

ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE);
```

---

## 8. KẾT LUẬN

### 8.1 Vai trò của Broadcast Receiver

- **Mục đích**: Nhận sự kiện real-time (SMS đến)
- **Ưu điểm**: Phản ứng ngay lập tức khi có tin nhắn
- **Nhược điểm**: Chỉ nhận được tin nhắn khi app đang chạy hoặc đã đăng ký
- **Sử dụng trong app**: Lưu SMS vào database để hiển thị trong Tab Danh bạ

### 8.2 Vai trò của Content Provider

- **Mục đích**: Đọc lịch sử dữ liệu lớn từ hệ thống
- **Ưu điểm**: Truy cập toàn bộ dữ liệu (không giới hạn real-time)
- **Nhược điểm**: Cần permission đặc biệt, có thể bị hạn chế trên Android mới
- **Sử dụng trong app**: Đọc lịch sử SMS và Call Log trong Tab Tin nhắn / Lịch sử gọi

### 8.3 Kết hợp 2 cơ chế

App TickTick kết hợp cả 2 cơ chế:
- **Broadcast Receiver**: Để nhận và lưu SMS mới vào database
- **Content Provider**: Để đọc lịch sử đầy đủ khi user cần xem

Cách tiếp cận này đảm bảo:
1. User nhận được thông báo SMS ngay lập tức (Broadcast Receiver)
2. User có thể xem toàn bộ lịch sử liên lạc (Content Provider)

---

## PHỤ LỤC: ĐƯỜNG DẪN FILE

```
app/src/main/java/hcmute/edu/vn/nguyenthetan/
├── ContactsActivity.java
├── receiver/
│   └── SmsReceiver.java
├── model/
│   ├── SmsItem.java
│   ├── CallLogItem.java
│   └── AppNotification.java
├── adapter/
│   ├── SmsListAdapter.java
│   ├── CallLogAdapter.java
│   └── ReceivedSmsAdapter.java
└── repository/
    └── AppNotificationRepository.java

app/src/main/res/layout/
├── activity_contacts.xml
├── item_sms.xml
└── item_call_log.xml

app/src/main/
└── AndroidManifest.xml
```
