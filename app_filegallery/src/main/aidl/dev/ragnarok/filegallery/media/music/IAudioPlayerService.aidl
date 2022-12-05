package dev.ragnarok.filegallery.media.music;

import dev.ragnarok.filegallery.model.Audio;
import java.util.List;

interface IAudioPlayerService {
    void openFile(in Audio audio);
    void open(in List<Audio> list, int position);
    void playAfterCurrent(in Audio audio);
    boolean canPlayAfterCurrent(in Audio audio);
    void skip(int position);
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    void closeMiniPlayer();
    boolean getMiniplayerVisibility();
    void setShuffleMode(int shufflemode);
    void setRepeatMode(int repeatmode);
    void refresh();
    boolean isPlaying();
    boolean isPreparing();
    boolean isInitialized();
    List<Audio> getQueue();
    long duration();
    long position();
    long seek(long pos);
    Audio getCurrentAudio();
    int getCurrentAudioPos();
    String getArtistName();
    String getTrackName();
    String getAlbumName();
    String getPath();
    String getAlbumCover();
    int getQueuePosition();
    int getShuffleMode();
    int getRepeatMode();
    int getAudioSessionId();
    int getBufferPercent();
    long getBufferPosition();
}

