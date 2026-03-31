package hcmute.edu.vn.nguyenthetan.media;

import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;

import hcmute.edu.vn.nguyenthetan.adapter.MediaAdapter;
import hcmute.edu.vn.nguyenthetan.service.MusicPlayerService;
import hcmute.edu.vn.nguyenthetan.util.DurationFormatter;

/**
 * Trách nhiệm duy nhất: quản lý UI now-playing + seekbar update loop.
 */
public class NowPlayingController {

    private final View layoutNowPlaying;
    private final TextView tvTitle;
    private final TextView tvArtist;
    private final TextView tvCurrentTime;
    private final TextView tvTotalTime;
    private final ImageView btnPlayPause;
    private final SeekBar seekBar;

    private final Handler handler;
    private Runnable updateRunnable;

    public NowPlayingController(
            View layoutNowPlaying,
            TextView tvTitle,
            TextView tvArtist,
            TextView tvCurrentTime,
            TextView tvTotalTime,
            ImageView btnPlayPause,
            SeekBar seekBar,
            Handler handler
    ) {
        this.layoutNowPlaying = layoutNowPlaying;
        this.tvTitle = tvTitle;
        this.tvArtist = tvArtist;
        this.tvCurrentTime = tvCurrentTime;
        this.tvTotalTime = tvTotalTime;
        this.btnPlayPause = btnPlayPause;
        this.seekBar = seekBar;
        this.handler = handler;
    }

    public void show(MediaAdapter.MusicItem item, boolean isPlaying) {
        layoutNowPlaying.setVisibility(View.VISIBLE);
        tvTitle.setText(item.title);
        tvArtist.setText(item.artist != null ? item.artist : "Không rõ");
        tvTotalTime.setText(DurationFormatter.formatMs(item.duration));
        seekBar.setMax((int) item.duration);
        setPlaying(isPlaying);
    }

    public void hideAndStopLoop() {
        layoutNowPlaying.setVisibility(View.GONE);
        stopLoop();
    }

    public void setPlaying(boolean isPlaying) {
        btnPlayPause.setImageResource(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play
        );
    }

    public void startLoop(MusicPlayerService service) {
        if (updateRunnable == null) {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    if (service != null) {
                        int currentPos = service.getCurrentPosition();
                        seekBar.setProgress(currentPos);
                        tvCurrentTime.setText(DurationFormatter.formatMs(currentPos));
                        setPlaying(service.isPlaying());
                    }
                    handler.postDelayed(this, 1000);
                }
            };
        }
        handler.postDelayed(updateRunnable, 0);
    }

    public void stopLoop() {
        if (updateRunnable != null) handler.removeCallbacks(updateRunnable);
    }
}

