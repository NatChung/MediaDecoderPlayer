package nat.chung.mediadecoderplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.os.SystemClock.sleep;

/**
 * Created by Nat on 2017/1/29.
 */

public class DemoPlayer {

    private Surface surface;
    private Context context;
    private DecodePlayer decodePlayer;

    public DemoPlayer(TextureView textureView){
        decodePlayer = new DecodePlayer(textureView);
    }

    public void stop(){
        decodePlayer.stop();
    }

    public void setup(String mineType, MediaFormat format) throws IOException {
        decodePlayer.setup(mineType, format);
    }

    public void addVideoFrame(byte[] data, long timestamp){
        decodePlayer.addVideoFrame(data, timestamp);
    }
}
