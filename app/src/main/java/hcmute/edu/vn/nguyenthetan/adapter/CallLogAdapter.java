package hcmute.edu.vn.nguyenthetan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.CallLogItem;

/**
 * Adapter hiển thị danh sách Call Log (lịch sử cuộc gọi)
 * Lấy dữ liệu từ Content Provider content://call_log/calls
 */
public class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder> {

    private List<CallLogItem> callLogs = new ArrayList<>();
    private OnCallLogActionListener listener;

    public interface OnCallLogActionListener {
        void onCallLogClick(CallLogItem callLog);
        void onCallClick(CallLogItem callLog);
    }

    public void setOnCallLogActionListener(OnCallLogActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<CallLogItem> callLogs) {
        this.callLogs = callLogs != null ? callLogs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CallLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_call_log, parent, false);
        return new CallLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CallLogViewHolder holder, int position) {
        CallLogItem item = callLogs.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    class CallLogViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCallType;
        private final TextView tvContactName;
        private final TextView tvPhoneNumber;
        private final TextView tvCallTime;
        private final TextView tvCallDuration;
        private final ImageView btnCall;

        public CallLogViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCallType = itemView.findViewById(R.id.ivCallType);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvCallTime = itemView.findViewById(R.id.tvCallTime);
            tvCallDuration = itemView.findViewById(R.id.tvCallDuration);
            btnCall = itemView.findViewById(R.id.btnCall);
        }

        public void bind(CallLogItem item) {
            // Set tên hoặc số điện thoại
            tvContactName.setText(item.getDisplayName());
            
            // Set số điện thoại (luôn hiển thị dưới tên)
            tvPhoneNumber.setText(item.getPhoneNumber());
            
            // Format thời gian
            tvCallTime.setText(formatTime(item.getTimestamp()));
            
            // Hiển thị thời lượng (chỉ với cuộc gọi không nhỡ)
            if (!item.isMissed() && item.getDuration() > 0) {
                tvCallDuration.setText(item.getFormattedDuration());
                tvCallDuration.setVisibility(View.VISIBLE);
            } else {
                tvCallDuration.setVisibility(View.GONE);
            }
            
            // Set icon và màu theo loại cuộc gọi
            ivCallType.setImageResource(item.getTypeIcon());
            if (item.isMissed()) {
                ivCallType.setColorFilter(itemView.getContext().getColor(android.R.color.holo_red_light));
            } else if (item.isIncoming()) {
                ivCallType.setColorFilter(itemView.getContext().getColor(android.R.color.holo_green_light));
            } else {
                ivCallType.setColorFilter(itemView.getContext().getColor(android.R.color.holo_blue_light));
            }
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCallLogClick(item);
                }
            });
            
            btnCall.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCallClick(item);
                }
            });
        }
        
        private String formatTime(long timestamp) {
            Date date = new Date(timestamp);
            Date now = new Date();
            
            // Nếu là hôm nay, chỉ hiển thị giờ:phút
            if (isSameDay(date, now)) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return "Hôm nay " + sdf.format(date);
            }
            
            // Nếu là hôm qua
            Date yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
            if (isSameDay(date, yesterday)) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return "Hôm qua " + sdf.format(date);
            }
            
            // Nếu là tuần này, hiển thị thứ
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, HH:mm", Locale.getDefault());
            return sdf.format(date);
        }
        
        private boolean isSameDay(Date date1, Date date2) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            return sdf.format(date1).equals(sdf.format(date2));
        }
    }
}
