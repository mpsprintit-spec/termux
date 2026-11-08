package com.termux.app.ra;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;

/**
 * NASA SDO video layer for the EXISTING RA Terminal Activity and its real PTY.
 *
 * This view never handles input or draws terminal cells. Media is offline-only:
 * $HOME/.ra-terminal/solar.mp4 (preferred), then solar.webm.
 * It stays transparent until the first decoded frame, allowing the existing
 * RaSolarBackdropView to be the fully functional fallback.
 *
 * Two TextureViews crossfade beneath three smooth luminance pulses. The frame
 * stays centered and never pans or scales in response to the transition.
 */
public final class RaNasaSolarView extends FrameLayout {
    public interface ReadyListener { void onReady(boolean ready); }
    private static final String TAG = "RA-NASA";
    private static final long THREE_WAVE_MS = 7800L;
    private static final float[] DEPTH = {0.23f, 0.42f, 0.28f};
    private final TextureView[] textures = new TextureView[2];
    private final MediaPlayer[] players = new MediaPlayer[2];
    private final boolean[] prepared = new boolean[2];
    private final FrameLayout disk;
    private final View glow;
    private File media;
    private ReadyListener listener;
    private boolean resumed, hasPicture, transitioning, nextStarted, released;
    private int active = 0;
    private long transitionStart = 0;
    private String mode = "eco";
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!resumed || released) return;
            updateTransition();
            postDelayed(this, "flow".equals(mode) ? 32L : 50L);
        }
    };

    public RaNasaSolarView(Context context) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        glow = new GlowView(context);
        addView(glow, new LayoutParams(-1, -1));
        glow.setVisibility(INVISIBLE);
        disk = new FrameLayout(context);
        disk.setBackgroundColor(Color.TRANSPARENT);
        addView(disk, new LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));

        for (int i = 0; i < 2; i++) {
            final int slot = i;
            TextureView view = new TextureView(context);
            view.setOpaque(false);
            view.setAlpha(0f);
            view.setClickable(false);
            view.setFocusable(false);
            view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (media != null && !released) prepare(slot, surface);
                }
                @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
                @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    releasePlayer(slot);
                    return true;
                }
                @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
            textures[i] = view;
            disk.addView(view, new LayoutParams(-1, -1));
        }
        disk.addView(new EdgeFadeView(context), new LayoutParams(-1, -1));
    }

    public void setReadyListener(ReadyListener callback) {
        listener = callback;
        if (callback != null) callback.onReady(hasPicture);
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        // NASA square frames have a dark margin; the solar disk appears ~45% of screen width.
        int side = Math.max(1, Math.min(Math.round(w * .55f), Math.round(h * .35f)));
        LayoutParams p = (LayoutParams) disk.getLayoutParams();
        p.width = side; p.height = side;
        p.leftMargin = Math.round((w - side) * .5f);
        p.topMargin = Math.round(h * .41f - side * .5f);
        disk.setLayoutParams(p);
    }

    private File findMedia() {
        File dir = new File(getContext().getFilesDir(), "home/.ra-terminal");
        for (String filename : new String[]{"solar.mp4", "solar.webm"}) {
            File file = new File(dir, filename);
            if (file.isFile() && file.canRead() && file.length() > 100_000L) return file;
        }
        return null;
    }

    private void prepare(int slot, SurfaceTexture texture) {
        if (players[slot] != null || released || media == null) return;
        MediaPlayer player = new MediaPlayer();
        players[slot] = player;
        final Surface surface = new Surface(texture);
        try {
            player.setDataSource(media.getAbsolutePath());
            player.setSurface(surface);
            player.setVolume(0f, 0f);
            player.setLooping(false);
            player.setOnPreparedListener(p -> {
                if (released) return;
                prepared[slot] = true;
                if (slot == active && resumed && !"off".equals(mode)) {
                    try { p.start(); } catch (IllegalStateException e) { fail(e); }
                }
            });
            player.setOnInfoListener((p, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && slot == active) {
                    showVideo();
                }
                return false;
            });
            player.setOnErrorListener((p, what, extra) -> {
                Log.w(TAG, "Decoder failed: " + what + "/" + extra);
                fail(new IOException("video decoder " + what + "/" + extra));
                return true;
            });
            player.setOnCompletionListener(p -> {
                if (!transitioning && slot == active && resumed) {
                    // Fallback: when a second decoder is unavailable, still keep video alive.
                    try { p.seekTo(0); p.start(); } catch (IllegalStateException e) { fail(e); }
                }
            });
            player.prepareAsync();
        } catch (IOException | IllegalStateException e) {
            fail(e);
        } finally {
            surface.release();
        }
    }

    private void showVideo() {
        if (hasPicture || released) return;
        hasPicture = true;
        glow.setVisibility(VISIBLE);
        textures[active].setAlpha(1f);
        if (listener != null) listener.onReady(true);
    }

    private void fail(Exception error) {
        Log.w(TAG, "NASA file unavailable; using original RA background", error);
        if (hasPicture) {
            hasPicture = false;
            if (listener != null) listener.onReady(false);
        }
        glow.setVisibility(INVISIBLE);
        for (TextureView v : textures) v.setAlpha(0f);
        removeCallbacks(ticker);
        for (int i = 0; i < 2; i++) releasePlayer(i);
        media = null;
        transitioning = false;
    }

    public void setAnimationMode(String next) {
        if (!"flow".equals(next) && !"off".equals(next)) next = "eco";
        mode = next;
        if ("off".equals(next)) {
            for (MediaPlayer player : players) if (player != null && player.isPlaying()) player.pause();
        } else if (resumed && prepared[active]) {
            try { players[active].start(); } catch (IllegalStateException e) { fail(e); }
        }
    }

    public void resume() {
        if (released) return;
        resumed = true;
        if (media == null) {
            media = findMedia();
            if (media == null) {
                Log.i(TAG, "Put solar.mp4 in ~/.ra-terminal/ to activate NASA video");
            } else {
                for (int i = 0; i < 2; i++) {
                    if (textures[i].isAvailable()) prepare(i, textures[i].getSurfaceTexture());
                }
            }
        }
        if (!"off".equals(mode) && prepared[active] && players[active] != null) {
            try { players[active].start(); } catch (IllegalStateException e) { fail(e); }
        }
        removeCallbacks(ticker);
        post(ticker);
    }

    public void pause() {
        resumed = false;
        removeCallbacks(ticker);
        for (MediaPlayer player : players) {
            try { if (player != null && player.isPlaying()) player.pause(); } catch (IllegalStateException ignored) {}
        }
    }

    private static float smooth(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    private void updateTransition() {
        if (!hasPicture || "off".equals(mode)) return;
        MediaPlayer primary = players[active];
        int secondarySlot = 1 - active;
        MediaPlayer secondary = players[secondarySlot];
        if (primary == null || !prepared[active]) return;
        if (!transitioning) {
            int duration;
            int position;
            try { duration = primary.getDuration(); position = primary.getCurrentPosition(); }
            catch (IllegalStateException e) { return; }
            // Short recordings fall back to normal looping.
            if (duration < 11000 || !prepared[secondarySlot] || secondary == null) return;
            int lead = (int) Math.min(THREE_WAVE_MS + 300, duration * .39f);
            if (position >= duration - lead) {
                transitioning = true;
                transitionStart = SystemClock.uptimeMillis();
                nextStarted = false;
            }
            return;
        }
        float u = Math.min(1f, (SystemClock.uptimeMillis() - transitionStart) / (float) THREE_WAVE_MS);
        int wave = Math.min(2, (int) (u * 3f));
        float local = u * 3f - wave;
        float oscillation = (float) Math.sin(Math.PI * local);
        float brightness = 1f - DEPTH[wave] * oscillation * oscillation;
        // The crossfade is hidden inside the SECOND dimming wave, not a hard seek.
        float cross = smooth((u - .405f) / .19f);
        if (!nextStarted && u >= .39f) {
            try {
                secondary.seekTo(0);
                secondary.start();
                nextStarted = true;
            } catch (IllegalStateException e) {
                transitioning = false;
                textures[active].setAlpha(1f);
                return;
            }
        }
        textures[active].setAlpha(brightness * (1f - cross));
        textures[secondarySlot].setAlpha(nextStarted ? brightness * cross : 0f);
        if (u >= 1f) {
            try { primary.pause(); primary.seekTo(0); } catch (IllegalStateException ignored) {}
            active = secondarySlot;
            transitioning = false;
            textures[1 - active].setAlpha(0f);
            textures[active].setAlpha(1f);
        }
    }

    private void releasePlayer(int slot) {
        MediaPlayer player = players[slot];
        players[slot] = null;
        prepared[slot] = false;
        if (player != null) {
            try { player.reset(); player.release(); } catch (IllegalStateException ignored) {}
        }
    }

    public void release() {
        released = true;
        pause();
        for (int i = 0; i < 2; i++) releasePlayer(i);
        media = null;
        if (hasPicture && listener != null) listener.onReady(false);
        hasPicture = false;
    }

    private static final class EdgeFadeView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        EdgeFadeView(Context context) { super(context); setClickable(false); }
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w,h,oldW,oldH);
            paint.setShader(new RadialGradient(w*.5f, h*.5f, Math.max(1, w*.72f),
                new int[]{Color.TRANSPARENT, Color.TRANSPARENT, 0x9a000000, Color.BLACK},
                new float[]{0f, .54f, .84f, 1f}, Shader.TileMode.CLAMP));
        }
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    private static final class GlowView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        GlowView(Context context) { super(context); setClickable(false); }
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w,h,oldW,oldH);
            float radius = Math.max(10f, Math.min(w*.8f,h*.57f));
            paint.setShader(new RadialGradient(w*.5f,h*.41f,radius,
                new int[]{0x09004bff,0x13003edc,0x09001a75,Color.TRANSPARENT},
                new float[]{0f,.21f,.49f,1f}, Shader.TileMode.CLAMP));
        }
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawCircle(getWidth()*.5f,getHeight()*.41f,
                Math.max(10f,Math.min(getWidth()*.8f,getHeight()*.57f)), paint);
        }
    }
}
