package nat.chung.mediadecoderplayer;

/**
 * Created by Nat on 2017/1/29.
 */

public interface IPlayer {

    public void addVideoFrameWithTimestamp(byte[] data, int timestamp, boolean isKeyFrame);
}
