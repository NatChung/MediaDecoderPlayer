package nat.chung.mediadecoderplayer;

/**
 * Created by starvedia on 2017/2/11.
 */

public interface IDataCache {
    void seekTo();
    void popVideoFrame();
    void popAUdioFrame();
    void pushVideoFrame();
    void pushAudioFrame();
}
