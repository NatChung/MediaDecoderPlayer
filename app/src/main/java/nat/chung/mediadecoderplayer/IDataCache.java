package nat.chung.mediadecoderplayer;

/**
 * Created by starvedia on 2017/2/11.
 */

public interface IDataCache {
    void seekTo();
    void popVideoFrame();
    void popAudioFrame();
    void pushVideoFrame();
    void pushAudioFrame();
}
