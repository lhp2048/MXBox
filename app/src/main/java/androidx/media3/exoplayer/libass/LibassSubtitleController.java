package androidx.media3.exoplayer.libass;

import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.text.SecondaryTextOutput;
import androidx.media3.exoplayer.trackselection.SecondaryTextTrackSelector;

public final class LibassSubtitleController {

    public LibassSubtitleController(ExoPlayer player, LibassPlaybackSession session, SecondaryTextTrackSelector.Factory trackSelectorFactory, SecondaryTextOutput secondaryTextOutput) {
    }

    public void close() {
    }

    public SubtitleSelectionState getSubtitleSelectionState() {
        return SubtitleSelectionState.EMPTY;
    }

    public void setSecondaryTextTrackSelectionOverride(TrackSelectionOverride selection) {
    }

    public void setSecondaryTextTrackAutoSelectionEnabled(boolean enabled) {
    }
}
