package nat.chung.mediadecoderplayer;

/**
 * Created by Nat on 2017/1/29.
 */

public class PlayerDecorator implements IPlayer {

    private IPlayer iPlayer;


    public PlayerDecorator(IPlayer iPlayer) {
        this.iPlayer = iPlayer;
    }

    @Override
    public void addVideoFrameWithTimestamp(byte[] data, int timestamp, boolean isKeyFrame) {

    }
}
