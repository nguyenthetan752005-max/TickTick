/**
 * MediaAdapter: Adapter hiển thị danh sách bài hát trong RecyclerView.
 * Chức năng: Hiển thị thông tin bài hát (tên, nghệ sĩ, thời lượng),
 * điều khiển phát/dừng nhạc, xóa nhạc đã tải lên.
 */
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

    /**
     * Data class chứa thông tin một bài hát.
     */
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

    /**
     * Interface callback xử lý sự kiện phát/dừng và xóa bài hát.
     */
    public interface OnMusicClickListener {
        void onPlayPauseClick(MusicItem item, int position);
        void onDeleteClick(MusicItem item, int position);
    }

    private List<MusicItem> items = new ArrayList<>();
    private OnMusicClickListener listener;
    private int currentPlayingPosition = -1;
    private boolean isActuallyPlaying = false;

    /**
     * Constructor khởi tạo adapter.
     * @param listener Callback khi người dùng tương tác với bài hát
     */
    public MediaAdapter(OnMusicClickListener listener) {
        this.listener = listener;
    }

    /**
     * Cập nhật danh sách bài hát và làm mới giao diện.
     * @param data Danh sách MusicItem mới
     */
    public void setData(List<MusicItem> data) {
        this.items = data;
        notifyDataSetChanged();
    }

    /**
     * Cập nhật trạng thái đang phát của một bài hát.
     * Gọi notifyItemChanged để cập nhật icon play/pause.
     * @param position Vị trí bài hát đang phát (-1 nếu không có)
     * @param isPlaying true nếu đang phát, false nếu tạm dừng
     */
    public void setCurrentPlaying(int position, boolean isPlaying) {
        int old = currentPlayingPosition;
        currentPlayingPosition = position;
        isActuallyPlaying = isPlaying;
        if (old >= 0 && old < items.size()) notifyItemChanged(old);
        if (position >= 0 && position < items.size()) notifyItemChanged(position);
    }

    /**
     * Tạo ViewHolder mới bằng cách inflate layout item_music.
     * @param parent ViewGroup cha
     * @param viewType Loại view (không sử dụng)
     * @return MusicViewHolder mới
     */
    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    /**
     * Gắn dữ liệu MusicItem vào ViewHolder.
     * Hiển thị tên, nghệ sĩ, thời lượng, cập nhật icon play/pause,
     * và điều chỉnh hiển thị nút xóa (chỉ cho nhạc đã tải lên).
     * @param holder ViewHolder cần gắn dữ liệu
     * @param position Vị trí trong danh sách
     */
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

    /**
     * Trả về tổng số bài hát trong danh sách.
     * @return Số lượng MusicItem
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Định dạng thời lượng từ milliseconds sang mm:ss.
     * @param ms Thời lượng tính bằng milliseconds
     * @return Chuỗi định dạng "mm:ss"
     */
    private String formatDuration(long ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    /**
     * ViewHolder chứa các thành phần UI của một item bài hát.
     * Bao gồm: icon nhạc/play/pause, tên bài, nghệ sĩ, thời lượng, nút xóa.
     */
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
