package com.smartcampus.manouba.adapters;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.model.ChatMessage;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessage> messages;

    // Shared Media Player state for inline voice playback
    private android.media.MediaPlayer mediaPlayer;
    private String playingUrl = null;
    private boolean isPaused = false;
    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable progressRunnable;

    private SeekBar activeSeekBar;
    private TextView activeDurationTv;
    private ImageButton activePlayButton;

    public MessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByMe() ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        boolean isSent = holder instanceof SentViewHolder;

        TextView tvMsg = isSent ? ((SentViewHolder) holder).tvMessage : ((ReceivedViewHolder) holder).tvMessage;
        TextView tvTime = isSent ? ((SentViewHolder) holder).tvTime : ((ReceivedViewHolder) holder).tvTime;
        ImageView ivImg = isSent ? ((SentViewHolder) holder).ivImage : ((ReceivedViewHolder) holder).ivImage;
        LinearLayout llFile = isSent ? ((SentViewHolder) holder).llFileContainer : ((ReceivedViewHolder) holder).llFileContainer;
        TextView tvFile = isSent ? ((SentViewHolder) holder).tvFileName : ((ReceivedViewHolder) holder).tvFileName;
        LinearLayout llAudio = isSent ? ((SentViewHolder) holder).llAudioContainer : ((ReceivedViewHolder) holder).llAudioContainer;
        ImageButton btnPlay = isSent ? ((SentViewHolder) holder).btnPlayAudio : ((ReceivedViewHolder) holder).btnPlayAudio;
        SeekBar sbProgress = isSent ? ((SentViewHolder) holder).sbAudioProgress : ((ReceivedViewHolder) holder).sbAudioProgress;
        TextView tvDuration = isSent ? ((SentViewHolder) holder).tvAudioDuration : ((ReceivedViewHolder) holder).tvAudioDuration;

        // Content Text
        tvMsg.setText(msg.getContent());
        tvMsg.setVisibility(TextUtils.isEmpty(msg.getContent()) ? View.GONE : View.VISIBLE);
        tvTime.setText(msg.getTime());

        // Reset Visibilities
        ivImg.setVisibility(View.GONE);
        if (llFile != null) llFile.setVisibility(View.GONE);
        if (llAudio != null) llAudio.setVisibility(View.GONE);

        String imageUrl = msg.getImageUrl();
        String fileUrl = msg.getFileUrl();
        String fileType = msg.getFileType();
        String fileName = msg.getFileName();

        if (!TextUtils.isEmpty(imageUrl)) {
            ivImg.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivImg);
        } else if (!TextUtils.isEmpty(fileUrl)) {
            if (isImage(fileUrl, fileType)) {
                ivImg.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(fileUrl)
                        .placeholder(android.R.color.darker_gray)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivImg);
            } else if (isAudio(fileUrl, fileType)) {
                setupAudioPlayer(fileUrl, llAudio, btnPlay, sbProgress, tvDuration);
            } else {
                setupFileDownload(fileUrl, fileName, llFile, tvFile);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopAudio();
    }

    // ── Helper Checkers ──────────────────────────────────────────────────────

    private boolean isImage(String url, String mediaType) {
        if (mediaType != null && mediaType.toLowerCase().startsWith("image/")) return true;
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif");
    }

    private boolean isAudio(String url, String mediaType) {
        if (mediaType != null && mediaType.toLowerCase().startsWith("audio/")) return true;
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.contains("/audio");
    }

    private String formatDuration(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / 60000) % 60;
        return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    // ── File downloads/viewing ────────────────────────────────────────────────

    private void setupFileDownload(String url, String providedFileName, View fileContainer, TextView fileNameTv) {
        if (fileContainer == null) return;
        fileContainer.setVisibility(View.VISIBLE);

        String fileName = !TextUtils.isEmpty(providedFileName) ? providedFileName : "Attachment";
        if (TextUtils.isEmpty(providedFileName) && url != null && url.contains("/")) {
            fileName = url.substring(url.lastIndexOf("/") + 1);
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }
            try {
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {}
        }
        if (fileNameTv != null) fileNameTv.setText(fileName);

        fileContainer.setOnClickListener(v -> {
            try {
                android.content.Context ctx = v.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                ctx.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Cannot open file link", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Audio player controls ─────────────────────────────────────────────────

    private void setupAudioPlayer(String url, View audioContainer, ImageButton playBtn, SeekBar seekBar, TextView durationTv) {
        if (audioContainer == null) return;
        audioContainer.setVisibility(View.VISIBLE);

        seekBar.setProgress(0);
        durationTv.setText("0:00");

        if (url.equals(playingUrl)) {
            activeSeekBar = seekBar;
            activeDurationTv = durationTv;
            activePlayButton = playBtn;

            if (mediaPlayer != null) {
                seekBar.setMax(mediaPlayer.getDuration());
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                durationTv.setText(formatDuration(mediaPlayer.getCurrentPosition()));

                if (mediaPlayer.isPlaying()) {
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    startProgressUpdate(seekBar, durationTv);
                } else {
                    playBtn.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        } else {
            playBtn.setImageResource(android.R.drawable.ic_media_play);
        }

        playBtn.setOnClickListener(v -> toggleAudio(url, seekBar, durationTv, playBtn));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && url.equals(playingUrl) && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    durationTv.setText(formatDuration(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {}
            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void toggleAudio(String url, SeekBar seekBar, TextView durationTv, ImageButton playBtn) {
        if (url == null || url.isEmpty()) return;

        if (url.equals(playingUrl)) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPaused = true;
                    playBtn.setImageResource(android.R.drawable.ic_media_play);
                    progressHandler.removeCallbacks(progressRunnable);
                } else {
                    mediaPlayer.start();
                    isPaused = false;
                    playBtn.setImageResource(android.R.drawable.ic_media_pause);
                    startProgressUpdate(seekBar, durationTv);
                }
            }
            return;
        }

        stopAudio();

        activeSeekBar = seekBar;
        activeDurationTv = durationTv;
        activePlayButton = playBtn;
        playingUrl = url;
        isPaused = false;

        playBtn.setImageResource(android.R.drawable.ic_media_pause);

        mediaPlayer = new android.media.MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (url.equals(playingUrl)) {
                    mp.start();
                    seekBar.setMax(mp.getDuration());
                    startProgressUpdate(seekBar, durationTv);
                }
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (url.equals(playingUrl)) {
                    stopAudio();
                    notifyDataSetChanged();
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopAudio();
                notifyDataSetChanged();
                return true;
            });
        } catch (Exception e) {
            stopAudio();
        }
    }

    private void startProgressUpdate(SeekBar seekBar, TextView durationTv) {
        progressHandler.removeCallbacks(progressRunnable);
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && seekBar == activeSeekBar) {
                    int current = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(current);
                    durationTv.setText(formatDuration(current));
                    progressHandler.postDelayed(this, 250);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopAudio() {
        progressHandler.removeCallbacks(progressRunnable);
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        playingUrl = null;
        isPaused = false;
        if (activeSeekBar != null) {
            activeSeekBar.setProgress(0);
        }
        if (activePlayButton != null) {
            activePlayButton.setImageResource(android.R.drawable.ic_media_play);
        }
        activeSeekBar = null;
        activeDurationTv = null;
        activePlayButton = null;
    }

    // ── ViewHolders ──────────────────────────────────────────────────────────

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;

        LinearLayout llFileContainer;
        TextView tvFileName;

        LinearLayout llAudioContainer;
        ImageButton btnPlayAudio;
        SeekBar sbAudioProgress;
        TextView tvAudioDuration;

        SentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_message_sent);
            tvTime = v.findViewById(R.id.tv_message_time_sent);
            ivImage = v.findViewById(R.id.iv_message_image_sent);

            llFileContainer = v.findViewById(R.id.ll_file_container_sent);
            tvFileName = v.findViewById(R.id.tv_file_name_sent);

            llAudioContainer = v.findViewById(R.id.ll_audio_container_sent);
            btnPlayAudio = v.findViewById(R.id.btn_play_audio_sent);
            sbAudioProgress = v.findViewById(R.id.sb_audio_progress_sent);
            tvAudioDuration = v.findViewById(R.id.tv_audio_duration_sent);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;

        LinearLayout llFileContainer;
        TextView tvFileName;

        LinearLayout llAudioContainer;
        ImageButton btnPlayAudio;
        SeekBar sbAudioProgress;
        TextView tvAudioDuration;

        ReceivedViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tv_message_received);
            tvTime = v.findViewById(R.id.tv_message_time_received);
            ivImage = v.findViewById(R.id.iv_message_image_received);

            llFileContainer = v.findViewById(R.id.ll_file_container_received);
            tvFileName = v.findViewById(R.id.tv_file_name_received);

            llAudioContainer = v.findViewById(R.id.ll_audio_container_received);
            btnPlayAudio = v.findViewById(R.id.btn_play_audio_received);
            sbAudioProgress = v.findViewById(R.id.sb_audio_progress_received);
            tvAudioDuration = v.findViewById(R.id.tv_audio_duration_received);
        }
    }
}
