package nat.chung.mediadecoderplayer;
import android.media.MediaFormat;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Created by Nat on 2017/1/29.
 */



public interface IPlayer {

    public enum AVFRAME_TYPE{
        AUDIO,
        VIDEO
    }

    void addAVFrame(AVFRAME_TYPE type, byte[] data, long timestampMS, int isKeyFrame);
    void finishAddAVFrame();
    void setupVideoDecoder(String mineType, MediaFormat format) throws IOException;
    void setupPCM(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode);
    void stop();
    TextureView getTextureView();
}
