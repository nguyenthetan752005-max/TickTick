package hcmute.edu.vn.nguyenthetan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.nguyenthetan.adapter.CallLogAdapter;
import hcmute.edu.vn.nguyenthetan.adapter.ContactAdapter;
import hcmute.edu.vn.nguyenthetan.adapter.ReceivedSmsAdapter;
import hcmute.edu.vn.nguyenthetan.adapter.SmsListAdapter;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.CallLogItem;
import hcmute.edu.vn.nguyenthetan.model.Contact;
import hcmute.edu.vn.nguyenthetan.model.SmsItem;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;
import hcmute.edu.vn.nguyenthetan.util.EdgeInsetsUtil;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ContactsActivity extends BaseActivity implements ContactAdapter.OnContactActionListener {

    private static final int REQUEST_ALL_PERMISSIONS = 2000;
    private static final int REQUEST_CALL_PHONE = 2002;
    private static final int REQUEST_SEND_SMS = 2003;

    private static final int TAB_CONTACTS = 0;
    private static final int TAB_SMS = 1;
    private static final int TAB_CALL_LOG = 2;

    private static String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
            };
        } else {
            return new String[]{
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE
            };
        }
    }

    private RecyclerView rvContacts, rvSmsList, rvCallLogList, rvReceivedSms;
    private TextView tvEmptyContacts, tvEmptySms, tvEmptyCallLog, tvNoSms, tvMyNumber;
    private TextView tvContactsSectionTitle, tvSmsSectionTitle, tvCallLogSectionTitle, tvSmsReceivedTitle;
    private EditText etSearchContacts, etManualPhone;
    private View layoutManual;
    private Button btnTabContacts, btnTabSms, btnTabCallLog;

    private ContactAdapter contactAdapter;
    private SmsListAdapter smsListAdapter;
    private CallLogAdapter callLogAdapter;
    private ReceivedSmsAdapter receivedSmsAdapter;

    private AppNotificationRepository notificationRepository;
    private ExecutorService executorService;

    private int currentTab = TAB_CONTACTS;
    private String pendingCallNumber;
    private Contact pendingSmsContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);

        EdgeInsetsUtil.applySystemBarsPadding(findViewById(android.R.id.content));

        executorService = Executors.newSingleThreadExecutor();
        notificationRepository = new AppNotificationRepository(this);

        initViews();
        setupTabListeners();
        requestAllPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void initViews() {
        rvContacts = findViewById(R.id.rvContacts);
        rvSmsList = findViewById(R.id.rvSmsList);
        rvCallLogList = findViewById(R.id.rvCallLogList);
        rvReceivedSms = findViewById(R.id.rvReceivedSms);

        tvEmptyContacts = findViewById(R.id.tvEmptyContacts);
        tvEmptySms = findViewById(R.id.tvEmptySms);
        tvEmptyCallLog = findViewById(R.id.tvEmptyCallLog);
        tvNoSms = findViewById(R.id.tvNoSms);

        tvContactsSectionTitle = findViewById(R.id.tvContactsSectionTitle);
        tvSmsSectionTitle = findViewById(R.id.tvSmsSectionTitle);
        tvCallLogSectionTitle = findViewById(R.id.tvCallLogSectionTitle);
        tvSmsReceivedTitle = findViewById(R.id.tvSmsReceivedTitle);

        tvMyNumber = findViewById(R.id.tvMyNumber);
        etSearchContacts = findViewById(R.id.etSearchContacts);
        etManualPhone = findViewById(R.id.etManualPhone);
        layoutManual = findViewById(R.id.layoutManual);

        btnTabContacts = findViewById(R.id.btnTabContacts);
        btnTabSms = findViewById(R.id.btnTabSms);
        btnTabCallLog = findViewById(R.id.btnTabCallLog);

        contactAdapter = new ContactAdapter(this);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(contactAdapter);

        smsListAdapter = new SmsListAdapter();
        rvSmsList.setLayoutManager(new LinearLayoutManager(this));
        rvSmsList.setAdapter(smsListAdapter);

        callLogAdapter = new CallLogAdapter();
        rvCallLogList.setLayoutManager(new LinearLayoutManager(this));
        rvCallLogList.setAdapter(callLogAdapter);

        receivedSmsAdapter = new ReceivedSmsAdapter();
        rvReceivedSms.setLayoutManager(new LinearLayoutManager(this));
        rvReceivedSms.setAdapter(receivedSmsAdapter);

        findViewById(R.id.btnManualCall).setOnClickListener(v -> {
            String phone = etManualPhone.getText().toString().trim();
            if (!phone.isEmpty()) {
                onCallClick(new Contact("Số gọi ngoai", phone));
            } else {
                Toast.makeText(this, "Vui long nhap so", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnManualSms).setOnClickListener(v -> {
            String phone = etManualPhone.getText().toString().trim();
            if (!phone.isEmpty()) {
                onSmsClick(new Contact("So goi ngoai", phone));
            } else {
                Toast.makeText(this, "Vui long nhap so", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etSearchContacts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                contactAdapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        callLogAdapter.setOnCallLogActionListener(new CallLogAdapter.OnCallLogActionListener() {
            @Override
            public void onCallLogClick(CallLogItem callLog) {
                Toast.makeText(ContactsActivity.this, 
                    callLog.getTypeString() + " - " + callLog.getDisplayName(), 
                    Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCallClick(CallLogItem callLog) {
                makeCall(callLog.getPhoneNumber());
            }
        });

        smsListAdapter.setOnSmsActionListener(sms -> {
            Toast.makeText(this, "Tin nhan tu: " + sms.getDisplayName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabListeners() {
        btnTabContacts.setOnClickListener(v -> switchTab(TAB_CONTACTS));
        btnTabSms.setOnClickListener(v -> switchTab(TAB_SMS));
        btnTabCallLog.setOnClickListener(v -> switchTab(TAB_CALL_LOG));
    }

    private void switchTab(int tab) {
        currentTab = tab;
        updateTabButtonStyles();

        if (tab == TAB_CONTACTS) {
            showContactsSection(true);
            showSmsSection(false);
            showCallLogSection(false);
            loadReceivedSms();
        } else if (tab == TAB_SMS) {
            showContactsSection(false);
            showSmsSection(true);
            showCallLogSection(false);
            loadSmsFromProvider();
        } else if (tab == TAB_CALL_LOG) {
            showContactsSection(false);
            showSmsSection(false);
            showCallLogSection(true);
            loadCallLogFromProvider();
        }
    }

    private void updateTabButtonStyles() {
        int primaryColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int accentColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);

        btnTabContacts.setBackgroundColor(currentTab == TAB_CONTACTS ? accentColor : primaryColor);
        btnTabSms.setBackgroundColor(currentTab == TAB_SMS ? accentColor : primaryColor);
        btnTabCallLog.setBackgroundColor(currentTab == TAB_CALL_LOG ? accentColor : primaryColor);
    }

    private void showContactsSection(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        tvMyNumber.setVisibility(visibility);
        etSearchContacts.setVisibility(visibility);
        layoutManual.setVisibility(visibility);
        tvContactsSectionTitle.setVisibility(visibility);
        rvContacts.setVisibility(visibility);
        tvEmptyContacts.setVisibility(View.GONE);
        tvSmsReceivedTitle.setVisibility(visibility);
        rvReceivedSms.setVisibility(visibility);
        tvNoSms.setVisibility(visibility);
    }

    private void showSmsSection(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        tvSmsSectionTitle.setVisibility(visibility);
        rvSmsList.setVisibility(visibility);
        tvEmptySms.setVisibility(View.GONE);
    }

    private void showCallLogSection(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        tvCallLogSectionTitle.setVisibility(visibility);
        rvCallLogList.setVisibility(visibility);
        tvEmptyCallLog.setVisibility(View.GONE);
    }

    private void requestAllPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (missingPermissions.isEmpty()) {
            onAllPermissionsGranted();
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
            onAllPermissionsGranted();
        } else if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCallNumber != null) {
                    makeCall(pendingCallNumber);
                    pendingCallNumber = null;
                }
            } else {
                Toast.makeText(this, "Can quyen goi dien", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSmsContact != null) {
                    showSmsDialog(pendingSmsContact);
                    pendingSmsContact = null;
                }
            } else {
                Toast.makeText(this, "Can quyen gui SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onAllPermissionsGranted() {
        loadContacts();
        loadMyPhoneNumber();
        switchTab(currentTab);
    }

    private void loadContacts() {
        List<Contact> contacts = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            loadDummyContacts();
            return;
        }

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

        if (contacts.isEmpty()) {
            loadDummyContacts();
        } else {
            tvEmptyContacts.setVisibility(View.GONE);
            rvContacts.setVisibility(View.VISIBLE);
            contactAdapter.setData(contacts);
        }
    }

    private void loadDummyContacts() {
        List<Contact> dummyContacts = new ArrayList<>();
        dummyContacts.add(new Contact("Emulator 5554", "5554"));
        dummyContacts.add(new Contact("Emulator 5556", "5556"));

        tvEmptyContacts.setVisibility(View.GONE);
        rvContacts.setVisibility(View.VISIBLE);
        contactAdapter.setData(dummyContacts);
    }

    private void loadSmsFromProvider() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            tvEmptySms.setVisibility(View.VISIBLE);
            rvSmsList.setVisibility(View.GONE);
            return;
        }

        executorService.execute(() -> {
            List<SmsItem> smsList = new ArrayList<>();

            Uri smsUri = Uri.parse("content://sms/inbox");
            String[] projection = {"_id", "address", "body", "date", "type", "read"};

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(
                        smsUri,
                        projection,
                        null, null,
                        "date DESC LIMIT 100"
                );

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                        int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                        int read = cursor.getInt(cursor.getColumnIndexOrThrow("read"));

                        String contactName = getContactNameFromNumber(address);
                        smsList.add(new SmsItem(id, address, contactName, body, date, type, read == 1));
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            final List<SmsItem> finalList = smsList;
            runOnUiThread(() -> {
                if (finalList.isEmpty()) {
                    tvEmptySms.setVisibility(View.VISIBLE);
                    rvSmsList.setVisibility(View.GONE);
                } else {
                    tvEmptySms.setVisibility(View.GONE);
                    rvSmsList.setVisibility(View.VISIBLE);
                    smsListAdapter.setData(finalList);
                }
            });
        });
    }

    private String getContactNameFromNumber(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(
                        ContactsContract.PhoneLookup.DISPLAY_NAME));
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return null;
    }

    private void loadCallLogFromProvider() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
                != PackageManager.PERMISSION_GRANTED) {
            tvEmptyCallLog.setVisibility(View.VISIBLE);
            rvCallLogList.setVisibility(View.GONE);
            return;
        }

        executorService.execute(() -> {
            List<CallLogItem> callLogList = new ArrayList<>();

            String[] projection = {
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
            };

            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(
                        CallLog.Calls.CONTENT_URI,
                        projection,
                        null, null,
                        CallLog.Calls.DATE + " DESC LIMIT 100"
                );

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
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            final List<CallLogItem> finalList = callLogList;
            runOnUiThread(() -> {
                if (finalList.isEmpty()) {
                    tvEmptyCallLog.setVisibility(View.VISIBLE);
                    rvCallLogList.setVisibility(View.GONE);
                } else {
                    tvEmptyCallLog.setVisibility(View.GONE);
                    rvCallLogList.setVisibility(View.VISIBLE);
                    callLogAdapter.setData(finalList);
                }
            });
        });
    }

    private void loadReceivedSms() {
        notificationRepository.getAllNotifications().observe(this, notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                boolean hasSms = false;
                for (AppNotification notif : notifications) {
                    if (notif.getTaskName() != null && notif.getTaskName().startsWith("SMS:")) {
                        hasSms = true;
                        break;
                    }
                }
                
                if (hasSms) {
                    receivedSmsAdapter.setData(notifications);
                    tvNoSms.setVisibility(View.GONE);
                    rvReceivedSms.setVisibility(View.VISIBLE);
                } else {
                    tvNoSms.setVisibility(View.VISIBLE);
                    rvReceivedSms.setVisibility(View.GONE);
                }
            } else {
                tvNoSms.setVisibility(View.VISIBLE);
                rvReceivedSms.setVisibility(View.GONE);
            }
        });
    }

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

    private void makeCall(String phone) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phone));
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Khong the goi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

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

    private void showSmsDialog(Contact contact) {
        EditText editText = new EditText(this);
        editText.setHint("Nhap noi dung...");
        editText.setMinLines(3);

        new AlertDialog.Builder(this)
                .setTitle("Gui SMS den " + contact.getName())
                .setMessage("So: " + contact.getPhone())
                .setView(editText)
                .setPositiveButton("Gui", (dialog, which) -> {
                    String message = editText.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(this, "Noi dung trong", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendSms(contact.getPhone(), message);
                })
                .setNegativeButton("Huy", null)
                .show();
    }

    private void sendSms(String phone, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
            Toast.makeText(this, "Da gui SMS!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gui that bai: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMyPhoneNumber() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    String myNumber = telephonyManager.getLine1Number();
                    if (myNumber != null && !myNumber.isEmpty()) {
                        tvMyNumber.setText("So may nay: " + myNumber);
                    } else {
                        tvMyNumber.setText("So may nay: Khong lay duoc");
                    }
                } catch (SecurityException e) {
                    tvMyNumber.setText("So may nay: Bi chan");
                }
            }
        } else {
            tvMyNumber.setText("So may nay: Chua cap quyen");
        }
    }
}
