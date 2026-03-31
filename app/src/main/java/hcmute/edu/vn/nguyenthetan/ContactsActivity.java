package hcmute.edu.vn.nguyenthetan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import hcmute.edu.vn.nguyenthetan.adapter.ContactAdapter;
import hcmute.edu.vn.nguyenthetan.model.Contact;
import hcmute.edu.vn.nguyenthetan.util.EdgeInsetsUtil;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends BaseActivity implements ContactAdapter.OnContactActionListener {

    // Request codes cho runtime permissions
    private static final int REQUEST_ALL_PERMISSIONS = 2000;
    private static final int REQUEST_CALL_PHONE = 2002;
    private static final int REQUEST_SEND_SMS = 2003;

    // Danh sách quyền cần xin
    private static final String[] REQUIRED_PERMISSIONS = getRequiredPermissions();

    private static String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
            };
        } else {
            return new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
            };
        }
    }

    private RecyclerView rvContacts;
    private TextView tvEmptyContacts;
    private TextView tvMyNumber;
    private EditText etSearchContacts;
    private EditText etManualPhone;
    private View btnManualCall;
    private View btnManualSms;
    private ContactAdapter adapter;

    // Lưu tạm thông tin khi chờ cấp quyền
    private String pendingCallNumber;
    private Contact pendingSmsContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);

        EdgeInsetsUtil.applySystemBarsPadding(findViewById(android.R.id.content));

        initViews();
        requestAllPermissions();
    }

    private void initViews() {
        rvContacts = findViewById(R.id.rvContacts);
        tvEmptyContacts = findViewById(R.id.tvEmptyContacts);
        tvMyNumber = findViewById(R.id.tvMyNumber);
        etSearchContacts = findViewById(R.id.etSearchContacts);
        etManualPhone = findViewById(R.id.etManualPhone);
        btnManualCall = findViewById(R.id.btnManualCall);
        btnManualSms = findViewById(R.id.btnManualSms);

        btnManualCall.setOnClickListener(v -> {
            String phone = etManualPhone.getText().toString().trim();
            if (!phone.isEmpty()) {
                onCallClick(new Contact("Số gọi ngoài", phone));
            } else {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            }
        });

        btnManualSms.setOnClickListener(v -> {
            String phone = etManualPhone.getText().toString().trim();
            if (!phone.isEmpty()) {
                onSmsClick(new Contact("Số gọi ngoài", phone));
            } else {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            }
        });

        adapter = new ContactAdapter(this);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(adapter);

        // Nút quay lại
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Tìm kiếm danh bạ
        etSearchContacts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ──────────── PERMISSIONS ────────────

    /**
     * Xin tất cả quyền cần thiết cùng lúc (READ_CONTACTS, CALL_PHONE, SEND_SMS).
     */
    private void requestAllPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (missingPermissions.isEmpty()) {
            loadContacts();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]), REQUEST_ALL_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            // Kiểm tra quyền READ_CONTACTS để load danh bạ
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, R.string.permission_contacts_required, Toast.LENGTH_LONG).show();
                // Hiện dummy data khi không có quyền đọc danh bạ
                loadDummyContacts();
            }
        } else if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCallNumber != null) {
                    makeCall(pendingCallNumber);
                    pendingCallNumber = null;
                }
            } else {
                Toast.makeText(this, R.string.permission_call_required, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSmsContact != null) {
                    showSmsDialog(pendingSmsContact);
                    pendingSmsContact = null;
                }
            } else {
                Toast.makeText(this, "Cần cấp quyền gửi SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ──────────── LOAD CONTACTS ────────────

    /**
     * Đọc danh bạ từ thiết bị qua ContentResolver.
     * Nếu rỗng, load dummy data để test trên emulator.
     */
    private void loadContacts() {
        loadMyPhoneNumber();
        List<Contact> contacts = new ArrayList<>();

        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.add(new Contact(name, phone));
            }
            cursor.close();
        }

        // Nếu danh bạ rỗng → dùng dummy data để test trên emulator
        if (contacts.isEmpty()) {
            loadDummyContacts();
        } else {
            rvContacts.setVisibility(View.VISIBLE);
            tvEmptyContacts.setVisibility(View.GONE);
            adapter.setData(contacts);
        }
    }

    /**
     * Tạo dummy contacts để test trên máy ảo (Emulator).
     * Số 5554, 5556 là số mặc định của các emulator instance khác.
     */
    private void loadDummyContacts() {
        List<Contact> dummyContacts = new ArrayList<>();
        dummyContacts.add(new Contact("Emulator 5554", "5554"));
        dummyContacts.add(new Contact("Emulator 5556", "5556"));

        rvContacts.setVisibility(View.VISIBLE);
        tvEmptyContacts.setVisibility(View.GONE);
        adapter.setData(dummyContacts);
    }

    // ──────────── CALL ────────────

    @Override
    public void onCallClick(Contact contact) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            makeCall(contact.getPhone());
        } else {
            pendingCallNumber = contact.getPhone();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
        }
    }

    /**
     * Thực hiện cuộc gọi bằng Intent.ACTION_CALL.
     */
    private void makeCall(String phone) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phone));
        startActivity(callIntent);
    }

    // ──────────── SMS ────────────

    @Override
    public void onSmsClick(Contact contact) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            showSmsDialog(contact);
        } else {
            pendingSmsContact = contact;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
        }
    }

    /**
     * Hiện AlertDialog cho người dùng nhập nội dung SMS rồi gửi bằng SmsManager.
     */
    private void showSmsDialog(Contact contact) {
        EditText editText = new EditText(this);
        editText.setHint("Nhập nội dung tin nhắn...");
        editText.setMinLines(3);

        new AlertDialog.Builder(this)
                .setTitle("Gửi SMS đến " + contact.getName())
                .setMessage("Số: " + contact.getPhone())
                .setView(editText)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String message = editText.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(this, "Nội dung tin nhắn không được trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendSms(contact.getPhone(), message);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Gửi SMS bằng SmsManager.getDefault().
     */
    private void sendSms(String phone, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
            Toast.makeText(this, "Đã gửi SMS thành công!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gửi SMS thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Lấy số điện thoại của thiết bị hiện tại
     */
    private void loadMyPhoneNumber() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    String myNumber = telephonyManager.getLine1Number();
                    if (myNumber != null && !myNumber.isEmpty()) {
                        tvMyNumber.setText("Số của máy này: " + myNumber);
                    } else {
                        tvMyNumber.setText("Số của máy này: Không lấy được từ thiết bị");
                    }
                } catch (SecurityException e) {
                    tvMyNumber.setText("Số của máy này: Bị chặn quyền truy xuất");
                }
            }
        } else {
            tvMyNumber.setText("Số của máy này: Chưa cấp quyền READ_PHONE_STATE");
        }
    }
}


