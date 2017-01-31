package nat.chung.mediadecoderplayer.decorator;

import android.media.MediaFormat;
import android.view.TextureView;

import java.io.IOException;

import nat.chung.mediadecoderplayer.IPlayer;

/**
 * Created by Nat on 2017/1/29.
 */

public class PlayerDecorator implements IPlayer {

    private IPlayer iPlayer;

    public PlayerDecorator(IPlayer iPlayer) {
        this.iPlayer = iPlayer;
    }

    @Override
    public void addAVFrame(AVFRAME_TYPE type, byte[] data, long timestamp) {
        iPlayer.addAVFrame(type, data, timestamp);
    }

    @Override
    public void setup(String mineType, MediaFormat format) throws IOException {
        iPlayer.setup(mineType, format) ;
    }

    @Override
    public void stop() {
        iPlayer.stop();
    }

    @Override
    public TextureView getTextureView() {
        return iPlayer.getTextureView();
    }
}
