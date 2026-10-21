package androidx.media3.exoplayer.iso;

import android.content.Context;

import androidx.media3.common.DiscMenuController;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;

public final class IsoNavigationPlayerController implements DiscMenuController {

    private final ExoPlayer player;

    public IsoNavigationPlayerController(ExoPlayer player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public MediaSource prepareMediaSource(Context context, MediaSource source, DataSource.Factory dataSourceFactory) {
        return source;
    }

    public boolean isStarted() {
        return false;
    }

    public void close() {
    }
}
