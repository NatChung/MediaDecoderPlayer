package nat.chung.mediadecoderplayer;

import android.media.MediaFormat;
import android.view.TextureView;
import java.io.IOException;

import nat.chung.mediadecoderplayer.decorator.SnapshotDecorator;

/**
 * Created by Nat on 2017/1/29.
 */

public class DemoPlayer {

    private SnapshotDecorator player;

    public DemoPlayer(TextureView textureView){
        player = new SnapshotDecorator(new DecodePlayer(textureView));
    }

    public void stop(){
        player.stop();
    }

    public void setup(String mineType, MediaFormat format) throws IOException {
        player.setup(mineType, format);
    }

    public void addVideoFrame(byte[] data, long timestamp){
        player.addVideoFrame(data, timestamp);
    }
}
