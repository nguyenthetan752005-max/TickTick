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
import hcmute.edu.vn.nguyenthetan.model.SmsItem;

/**
 * Adapter hiển thị danh sách SMS từ Content Provider
 * URI: content://sms/inbox
 */
public class SmsListAdapter extends RecyclerView.Adapter<SmsListAdapter.SmsViewHolder> {

    private List<SmsItem> smsList = new ArrayList<>();
    private OnSmsActionListener listener;

    public interface OnSmsActionListener {
        void onSmsClick(SmsItem sms);
    }

    public void setOnSmsActionListener(OnSmsActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<SmsItem> smsList) {
        this.smsList = smsList != null ? smsList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsItem item = smsList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

    class SmsViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSender;
        private final TextView tvSmsBody;
        private final TextView tvSmsTime;
        private final View unreadIndicator;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvSmsBody = itemView.findViewById(R.id.tvSmsBody);
            tvSmsTime = itemView.findViewById(R.id.tvSmsTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        public void bind(SmsItem item) {
            // Set tên người gửi (hoặc số điện thoại)
            tvSender.setText(item.getDisplayName());
            
            // Set nội dung tin nhắn (cắt ngắn nếu quá dài)
            tvSmsBody.setText(item.getBodyPreview(100));
            
            // Format thời gian
            tvSmsTime.setText(formatTime(item.getTimestamp()));
            
            // Hiển thị indicator nếu chưa đọc
            unreadIndicator.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSmsClick(item);
                }
            });
        }
        
        private String formatTime(long timestamp) {
            Date date = new Date(timestamp);
            Date now = new Date();
            
            // Nếu là hôm nay, chỉ hiển thị giờ:phút
            if (isSameDay(date, now)) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return sdf.format(date);
            }
            
            // Nếu là hôm qua
            Date yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
            if (isSameDay(date, yesterday)) {
                return "Hôm qua";
            }
            
            // Nếu là tuần này
            long diffDays = (now.getTime() - date.getTime()) / (24 * 60 * 60 * 1000);
            if (diffDays < 7) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
                return sdf.format(date);
            }
            
            // Còn lại hiển thị ngày/tháng
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            return sdf.format(date);
        }
        
        private boolean isSameDay(Date date1, Date date2) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            return sdf.format(date1).equals(sdf.format(date2));
        }
    }
}
