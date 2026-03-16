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

    public interface OnInboxItemClickListener {
        void onTaskClick(Task task);
        void onTaskCompleteClick(Task task);
        void onNotificationClick(AppNotification notification);
        void onNotificationDeleteClick(AppNotification notification);
    }

    public InboxAdapter(OnInboxItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<InboxItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == InboxItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == InboxItem.TYPE_TASK) {
            View view = inflater.inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        InboxItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            HeaderItem header = (HeaderItem) item;
            ((HeaderViewHolder) holder).tvHeaderTitle.setText(header.getTitle());
        } else if (holder instanceof TaskViewHolder) {
            TaskItemWrapper taskWrapper = (TaskItemWrapper) item;
            Task task = taskWrapper.getTask();
            TaskViewHolder taskHolder = (TaskViewHolder) holder;

            taskHolder.tvName.setText(task.getName());
            taskHolder.tvDesc.setText(task.getDescription());
            taskHolder.tvDate.setText("Không có deadline");

            if (task.isCompleted()) {
                taskHolder.ivCheckbox.setVisibility(View.GONE);
            } else {
                taskHolder.ivCheckbox.setVisibility(View.VISIBLE);
                taskHolder.ivCheckbox.setImageResource(android.R.drawable.checkbox_off_background);
            }

            taskHolder.ivSelected.setVisibility(View.GONE);

            taskHolder.ivCheckbox.setOnClickListener(v -> listener.onTaskCompleteClick(task));
            taskHolder.itemView.setOnClickListener(v -> listener.onTaskClick(task));

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

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolders
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvDate;
        ImageView ivCheckbox, ivSelected;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvDate = itemView.findViewById(R.id.tvTaskDate);
            ivCheckbox = itemView.findViewById(R.id.ivCheckbox);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }

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
