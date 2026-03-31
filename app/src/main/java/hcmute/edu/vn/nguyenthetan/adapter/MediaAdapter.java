package hcmute.edu.vn.nguyenthetan.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.nguyenthetan.R;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MusicViewHolder> {

    public static class MusicItem {
        public String title;
        public String artist;
        public long duration; // milliseconds
        public Uri uri;

        public MusicItem(String title, String artist, long duration, Uri uri) {
            this.title = title != null ? title : "Không tiêu đề";
            this.artist = artist != null ? artist : "Không rõ";
            this.duration = duration;
            this.uri = uri;
        }
    }

    public interface OnMusicClickListener {
        void onPlayPauseClick(MusicItem item, int position);
        void onDeleteClick(MusicItem item, int position);
    }

    private List<MusicItem> items = new ArrayList<>();
    private OnMusicClickListener listener;
    private int currentPlayingPosition = -1;
    private boolean isActuallyPlaying = false;

    public MediaAdapter(OnMusicClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<MusicItem> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    public void setCurrentPlaying(int position, boolean isPlaying) {
        int old = currentPlayingPosition;
        currentPlayingPosition = position;
        isActuallyPlaying = isPlaying;
        if (old >= 0 && old < items.size()) notifyItemChanged(old);
        if (position >= 0 && position < items.size()) notifyItemChanged(position);
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicItem item = items.get(position);

        holder.tvSongTitle.setText(item.title);
        holder.tvSongArtist.setText(item.artist);
        holder.tvDuration.setText(formatDuration(item.duration));

        boolean isCurrent = (position == currentPlayingPosition);
        
        // Cập nhật icon dựa trên trạng thái phát thực tế
        if (isCurrent && isActuallyPlaying) {
            holder.ivMusicIcon.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            holder.ivMusicIcon.setImageResource(android.R.drawable.ic_media_play);
        }

        // Hiển thị nút xóa cho nhạc đã tải lên (file://)
        boolean isUserUploadedMusic = false;
        if (item.uri != null) {
            String scheme = item.uri.getScheme();
            String path = item.uri.getPath();
            isUserUploadedMusic = "file".equals(scheme) || (path != null && path.contains("uploaded_music"));
        }
        
        holder.btnDeleteMusic.setVisibility(isUserUploadedMusic ? View.VISIBLE : View.GONE);



        holder.btnDeleteMusic.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item, holder.getAdapterPosition());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlayPauseClick(item, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDuration(long ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMusicIcon, btnDeleteMusic;
        TextView tvSongTitle, tvSongArtist, tvDuration;

        MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMusicIcon = itemView.findViewById(R.id.ivMusicIcon);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);

            btnDeleteMusic = itemView.findViewById(R.id.btnDeleteMusic);
        }
    }
}
