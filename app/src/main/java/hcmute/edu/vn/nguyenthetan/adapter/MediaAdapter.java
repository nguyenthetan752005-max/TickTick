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
            this.title = title;
            this.artist = artist;
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

    public MediaAdapter(OnMusicClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<MusicItem> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    public void setCurrentPlaying(int position) {
        int old = currentPlayingPosition;
        currentPlayingPosition = position;
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
        holder.tvSongArtist.setText(item.artist != null ? item.artist : "Không rõ");
        holder.tvDuration.setText(formatDuration(item.duration));

        boolean isPlaying = position == currentPlayingPosition;
        holder.btnPlayPause.setImageResource(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        holder.ivMusicIcon.setImageResource(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

        // Hiển thị nút thùng rác nếu là nhạc tải lên (từ file nội bộ)
        if (item.uri != null && "file".equals(item.uri.getScheme())) {
            holder.btnDeleteMusic.setVisibility(View.VISIBLE);
        } else {
            holder.btnDeleteMusic.setVisibility(View.GONE);
        }

        holder.btnPlayPause.setOnClickListener(v -> {
            if (listener != null) listener.onPlayPauseClick(item, holder.getAdapterPosition());
        });

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
        ImageView ivMusicIcon, btnPlayPause, btnDeleteMusic;
        TextView tvSongTitle, tvSongArtist, tvDuration;

        MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMusicIcon = itemView.findViewById(R.id.ivMusicIcon);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            btnDeleteMusic = itemView.findViewById(R.id.btnDeleteMusic);
        }
    }
}
