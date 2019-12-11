package com.guichaguri.trackplayer.service.player;

import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.source.ForwardingTimeline;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;

import java.io.IOException;

/**
 * This custom media source is intended to allow setting a custom start position for a track media source.
 * 
 * It is based on the example given by the ExoPlayer team in this thread:
 * https://github.com/google/ExoPlayer/issues/2403#issuecomment-330955125
 * 
 * Portions were also copied from:
 * com.google.android.exoplayer2.source.BaseMediaPlayer
 * In particular, the event listener related code.
 */

public class CustomStartPositionMediaSource implements MediaSource {

	private final MediaSourceEventListener.EventDispatcher eventDispatcher;

	private final MediaSource wrappedSource;
	private final long startPositionUs;

	public CustomStartPositionMediaSource(MediaSource wrappedSource, long startPositionMs) {
		this.eventDispatcher = new MediaSourceEventListener.EventDispatcher();
		this.wrappedSource = wrappedSource;
		this.startPositionUs = C.msToUs(startPositionMs);
	}

	@Override
	public final void prepareSource(SourceInfoRefreshListener listener, @Nullable TransferListener mediaTransferListener) {
		wrappedSource.prepareSource(new SourceInfoRefreshListener() {
			@Override
			public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, @Nullable Object manifest) {
				listener.onSourceInfoRefreshed(source, new CustomStartPositionTimeline(timeline, startPositionUs), manifest);
			}
		}, mediaTransferListener);
	}

	@Override
	public void maybeThrowSourceInfoRefreshError() throws IOException {
		wrappedSource.maybeThrowSourceInfoRefreshError();
	}

	@Override
	public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
		return wrappedSource.createPeriod(id, allocator, startPositionUs);
	}

	@Override
	public void releasePeriod(MediaPeriod mediaPeriod) {
		wrappedSource.releasePeriod(mediaPeriod);
	}

	@Override
	public void releaseSource(SourceInfoRefreshListener listener) {
		wrappedSource.releaseSource(listener);
	}

	private static final class CustomStartPositionTimeline extends ForwardingTimeline {

		private final long defaultPositionUs;

		public CustomStartPositionTimeline(Timeline wrapped, long defaultPositionUs) {
			super(wrapped);
			this.defaultPositionUs = defaultPositionUs;
		}

		@Override
		public Window getWindow(int windowIndex, Window window, boolean setIds, long defaultPositionProjectionUs) {
			super.getWindow(windowIndex, window, setIds, defaultPositionProjectionUs);
			window.defaultPositionUs = defaultPositionUs;
			return window;
		}
	}

	// Event Listeners ...

	/**
   * Returns a {@link MediaSourceEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified media period id.
   *
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events. May be null, if
   *     the events do not belong to a specific media period.
   * @return An event dispatcher with pre-configured media period id.
   */
  protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(
      @Nullable MediaPeriodId mediaPeriodId) {
    return eventDispatcher.withParameters(
        /* windowIndex= */ 0, mediaPeriodId, /* mediaTimeOffsetMs= */ 0);
  }

  /**
   * Returns a {@link MediaSourceEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified media period id and time offset.
   *
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events.
   * @param mediaTimeOffsetMs The offset to be added to all media times, in milliseconds.
   * @return An event dispatcher with pre-configured media period id and time offset.
   */
  protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(
      MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
    Assertions.checkArgument(mediaPeriodId != null);
    return eventDispatcher.withParameters(/* windowIndex= */ 0, mediaPeriodId, mediaTimeOffsetMs);
  }

  /**
   * Returns a {@link MediaSourceEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified window index, media period id and time offset.
   *
   * @param windowIndex The timeline window index to be reported with the events.
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events. May be null, if
   *     the events do not belong to a specific media period.
   * @param mediaTimeOffsetMs The offset to be added to all media times, in milliseconds.
   * @return An event dispatcher with pre-configured media period id and time offset.
   */
  protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(
      int windowIndex, @Nullable MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
    return eventDispatcher.withParameters(windowIndex, mediaPeriodId, mediaTimeOffsetMs);
  }

  @Override
  public final void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
    eventDispatcher.addEventListener(handler, eventListener);
  }

  @Override
  public final void removeEventListener(MediaSourceEventListener eventListener) {
    eventDispatcher.removeEventListener(eventListener);
  }
}