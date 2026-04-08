/**
 * InboxAdapter: Multi-type adapter hiển thị Hộp thư thông báo.
 * Hỗ trợ 2 loại item: Header (tiêu đề section) và Notification (thông báo).
 * Sử dụng ViewType để phân biệt và inflate layout khác nhau cho từng loại.
 */
package hcmute.edu.vn.nguyenthetan.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Task;

public class InboxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<InboxItem> items = new ArrayList<>();
    private OnInboxItemClickListener listener;

    /**
     * Interface callback xử lý sự kiện click vào thông báo hoặc xóa thông báo.
     */
    public interface OnInboxItemClickListener {
        void onNotificationClick(AppNotification notification);
        void onNotificationDeleteClick(AppNotification notification);
    }

    /**
     * Constructor khởi tạo adapter.
     * @param listener Callback khi người dùng tương tác với thông báo
     */
    public InboxAdapter(OnInboxItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách InboxItem và làm mới giao diện.
     * @param newItems Danh sách InboxItem mới (Header hoặc Notification)
     */
    public void setData(List<InboxItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Xác định loại view tại vị trí cụ thể.
     * Dùng để RecyclerView biết cần tạo loại ViewHolder nào.
     * @param position Vị trí trong danh sách
     * @return Loại view (TYPE_HEADER hoặc TYPE_NOTIFICATION)
     */
    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    /**
     * Tạo ViewHolder phù hợp dựa trên viewType.
     * TYPE_HEADER → HeaderViewHolder (layout item_header)
     * TYPE_NOTIFICATION → NotificationViewHolder (layout item_notification)
     * @param parent ViewGroup cha
     * @param viewType Loại view để quyết định ViewHolder
     * @return ViewHolder tương ứng
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == InboxItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    /**
     * Gắn dữ liệu vào ViewHolder dựa trên loại item.
     * Header: Hiển thị tiêu đề section
     * Notification: Hiển thị nội dung, thời gian, trạng thái đọc và nút xóa
     * @param holder ViewHolder cần gắn dữ liệu
     * @param position Vị trí trong danh sách
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        InboxItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            HeaderItem header = (HeaderItem) item;
            ((HeaderViewHolder) holder).tvHeaderTitle.setText(header.getTitle());
        } else if (holder instanceof NotificationViewHolder) {
            NotificationItemWrapper notifWrapper = (NotificationItemWrapper) item;
            AppNotification notif = notifWrapper.getNotification();
            NotificationViewHolder notifHolder = (NotificationViewHolder) holder;

            notifHolder.tvMessage.setText(notif.getMessage());
            
            // Format time as "X min ago"
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notif.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            notifHolder.tvTime.setText(timeAgo);

            if (notif.isRead()) {
                notifHolder.vUnreadDot.setVisibility(View.GONE);
                notifHolder.tvMessage.setTextColor(0xFF8E8E93); // text_secondary color
            } else {
                notifHolder.vUnreadDot.setVisibility(View.VISIBLE);
                notifHolder.tvMessage.setTextColor(0xFFFFFFFF); // white color
            }

            notifHolder.itemView.setOnClickListener(v -> listener.onNotificationClick(notif));
            notifHolder.btnDelete.setOnClickListener(v -> listener.onNotificationDeleteClick(notif));
        }
    }

    /**
     * Trả về tổng số item trong danh sách.
     * @return Số lượng InboxItem
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolders
    /**
     * ViewHolder cho Header item - hiển thị tiêu đề section.
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
        }
    }

    /**
     * ViewHolder cho Notification item - hiển thị nội dung thông báo.
     * Bao gồm: message, timestamp, indicator chưa đọc, nút xóa.
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View vUnreadDot;
        ImageView btnDelete;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            vUnreadDot = itemView.findViewById(R.id.vUnreadDot);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
        }
    }
}
