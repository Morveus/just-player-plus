package com.brouken.player.subtitle;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ForwardingRenderer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RendererConfiguration;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.text.TextOutput;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Applies the user subtitle delay and speed factor at render time by transforming the playback
 * position seen by the text renderers. Shifting cue timestamps at parse time cannot move embedded
 * subtitles earlier: their parsed timestamps are relative to the container sample (start == 0), so
 * a negative shift gets clamped and only shortens the cue duration. Render-time transforming works
 * in both directions, for embedded, external and image-based subtitles alike.
 *
 * <p>The speed factor (in ppm, 1_000_000 == 1.0) corrects subtitles timestamped for a different
 * frame rate than the playback: the drift grows linearly with position, so a constant delay can
 * never compensate it. The factor must only scale the media-relative position, not the raw
 * renderer position: renderer positions include a large stream offset (captured from
 * {@link Renderer#enable} / {@link Renderer#replaceStream}) that would be scaled too otherwise.
 */
public final class SubtitleDelayRenderersFactory extends DefaultRenderersFactory {

    public static final int SPEED_PPM_NORMAL = 1_000_000;

    private final AtomicInteger subtitleDelayMs;
    private final AtomicInteger subtitleSpeedPpm;

    public SubtitleDelayRenderersFactory(Context context, AtomicInteger subtitleDelayMs, AtomicInteger subtitleSpeedPpm) {
        super(context);
        this.subtitleDelayMs = subtitleDelayMs;
        this.subtitleSpeedPpm = subtitleSpeedPpm;
    }

    @Override
    protected void buildTextRenderers(@NonNull Context context, @NonNull TextOutput output, @NonNull Looper outputLooper, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
        ArrayList<Renderer> textRenderers = new ArrayList<>();
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, textRenderers);
        for (Renderer renderer : textRenderers) {
            if (renderer.getTrackType() == C.TRACK_TYPE_TEXT) {
                out.add(new SubtitleSyncRenderer(renderer, subtitleDelayMs, subtitleSpeedPpm));
            } else {
                out.add(renderer);
            }
        }
    }

    private static final class SubtitleSyncRenderer extends ForwardingRenderer {

        private final AtomicInteger delayMs;
        private final AtomicInteger speedPpm;

        private volatile long streamOffsetUs;

        private SubtitleSyncRenderer(Renderer renderer, AtomicInteger delayMs, AtomicInteger speedPpm) {
            super(renderer);
            this.delayMs = delayMs;
            this.speedPpm = speedPpm;
        }

        @Override
        public void enable(RendererConfiguration configuration, Format[] formats, SampleStream stream, long positionUs, boolean joining, boolean mayRenderStartOfStream, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) throws ExoPlaybackException {
            streamOffsetUs = offsetUs;
            super.enable(configuration, formats, stream, positionUs, joining, mayRenderStartOfStream, startPositionUs, offsetUs, mediaPeriodId);
        }

        @Override
        public void replaceStream(Format[] formats, SampleStream stream, long startPositionUs, long offsetUs, MediaSource.MediaPeriodId mediaPeriodId) throws ExoPlaybackException {
            streamOffsetUs = offsetUs;
            super.replaceStream(formats, stream, startPositionUs, offsetUs, mediaPeriodId);
        }

        private long adjustedPositionUs(long positionUs) {
            long mediaPositionUs = positionUs - streamOffsetUs;
            long speed = speedPpm.get();
            if (speed > 0 && speed != SPEED_PPM_NORMAL) {
                mediaPositionUs = mediaPositionUs * speed / SPEED_PPM_NORMAL;
            }
            // Cues show when position >= cue start: presenting an earlier position delays
            // subtitles, a later one advances them.
            return streamOffsetUs + mediaPositionUs - delayMs.get() * 1000L;
        }

        @Override
        public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
            super.render(adjustedPositionUs(positionUs), elapsedRealtimeUs);
        }

        @Override
        public long getDurationToProgressUs(long positionUs, long elapsedRealtimeUs) {
            return super.getDurationToProgressUs(adjustedPositionUs(positionUs), elapsedRealtimeUs);
        }

    }

}
