package hcmute.edu.vn.nguyenthetan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;

/**
 * ReceivedSmsAdapter: Adapter hiển thị danh sách SMS đã nhận trong RecyclerView.
 * Chức năng: Hiển thị số người gửi, nội dung tin nhắn và thời gian nhận.
 */
public class ReceivedSmsAdapter extends RecyclerView.Adapter<ReceivedSmsAdapter.SmsViewHolder> {

    private List<AppNotification> smsList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());

    /**
     * Cập nhật danh sách SMS từ AppNotification (lọc chỉ lấy SMS).
     * @param notifications Danh sách tất cả notifications từ database
     */
    public void setData(List<AppNotification> notifications) {
        smsList.clear();
        if (notifications != null) {
            for (AppNotification notif : notifications) {
                if (notif.getTaskName() != null && notif.getTaskName().startsWith("SMS:")) {
                    smsList.add(notif);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_received_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        AppNotification sms = smsList.get(position);

        // Hiển thị số người gửi (bỏ prefix "SMS: ")
        String sender = sms.getTaskName().replace("SMS: ", "");
        holder.tvSender.setText("Từ: " + sender);

        // Hiển thị nội dung tin nhắn
        holder.tvMessage.setText(sms.getMessage());

        // Hiển thị thời gian
        holder.tvTime.setText(dateFormat.format(new Date(sms.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

    /**
     * ViewHolder chứa các thành phần UI của một item SMS.
     * Bao gồm: số người gửi, nội dung, thời gian.
     */
    static class SmsViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTime;

        SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
