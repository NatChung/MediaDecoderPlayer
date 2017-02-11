package nat.chung.mediadecoderplayer;

/**
 * Created by Nat on 2017/2/11.
 */

public class CacheFrame {
    public final byte[] data;
    public final long timestampMS;
    public CacheFrame(byte[] data, long timestampMS){
        this.data = data;
        this.timestampMS = timestampMS;
    }
}
