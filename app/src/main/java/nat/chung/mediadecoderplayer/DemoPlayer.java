package nat.chung.mediadecoderplayer;

import android.content.Context;
import android.media.MediaFormat;
import android.view.TextureView;
import java.io.IOException;

import nat.chung.mediadecoderplayer.decorator.SnapshotDecorator;
import nat.chung.mediadecoderplayer.decorator.ZoomDecorator;

/**
 * Created by Nat on 2017/1/29.
 */

public class DemoPlayer {

    private SnapshotDecorator player;

    public DemoPlayer(Context context, TextureView textureView){
        DecodePlayer decodePlayer = new DecodePlayer(textureView);
        ZoomDecorator zoomDecorator = new ZoomDecorator(context, decodePlayer);
        player = new SnapshotDecorator(zoomDecorator);
    }

    public void stop(){
        player.stop();
    }

    public void setup(String mineType, MediaFormat format) throws IOException {
        player.setup(mineType, format);
    }

    public void addAVFrame(IPlayer.AVFRAME_TYPE type, byte[] data, long timestamp){
        player.addAVFrame(type, data, timestamp);
    }

    public void snapshot(String savedPath){
        player.snapshot(savedPath);
    }
}
