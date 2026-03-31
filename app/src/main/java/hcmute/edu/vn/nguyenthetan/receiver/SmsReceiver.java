package hcmute.edu.vn.nguyenthetan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;
import hcmute.edu.vn.nguyenthetan.util.AppExecutors;

/**
 * BroadcastReceiver nhận SMS đến.
 * Trích xuất số điện thoại người gửi và nội dung tin nhắn,
 * sau đó lưu vào CSDL thông qua AppNotificationRepository trên luồng nền.
 */
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        // Trích xuất SMS từ PDUs
        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");

        if (pdus == null || pdus.length == 0) return;

        StringBuilder messageBody = new StringBuilder();
        String senderPhone = null;

        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);

            // Lấy số điện thoại người gửi (chỉ lấy từ message đầu tiên)
            if (senderPhone == null) {
                senderPhone = smsMessage.getOriginatingAddress();
            }

            // Ghép nội dung tin nhắn (SMS dài có thể chia thành nhiều PDU)
            messageBody.append(smsMessage.getMessageBody());
        }

        if (senderPhone == null) return;

        Log.d(TAG, "SMS nhận từ: " + senderPhone + " | Nội dung: " + messageBody);

        // Lưu vào CSDL trên luồng nền
        final String sender = senderPhone;
        final String body = messageBody.toString();

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppNotificationRepository repository = new AppNotificationRepository(context);
                AppNotification notification = new AppNotification(
                        0,                          // taskId = 0 (không liên quan đến task)
                        "SMS: " + sender,           // taskName = số điện thoại người gửi
                        body,                       // message = nội dung tin nhắn
                        System.currentTimeMillis()  // timestamp
                );
                repository.addNotification(notification);
                Log.d(TAG, "Đã lưu SMS vào DB thành công");
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi lưu SMS vào DB: " + e.getMessage(), e);
            }
        });
    }
}
