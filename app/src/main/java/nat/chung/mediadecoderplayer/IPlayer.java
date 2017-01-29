package nat.chung.mediadecoderplayer;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * Created by Nat on 2017/1/29.
 */

public interface IPlayer {

    void addVideoFrame(byte[] data, long timestamp);
    void setup(String mineType, MediaFormat format) throws IOException;
    void stop();
}
