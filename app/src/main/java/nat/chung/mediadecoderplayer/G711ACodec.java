package nat.chung.mediadecoderplayer;

/**
 * Created by Nat on 2017/2/15.
 */

public class G711ACodec  {
    // s0000000wxyz...s000wxyz
    // s0000001wxyz...s001wxyz
    // s000001wxyza...s010wxyz
    // s00001wxyzab...s011wxyz
    // s0001wxyzabc...s100wxyz
    // s001wxyzabcd...s101wxyz
    // s01wxyzabcde...s110wxyz
    // s1wxyzabcdef...s111wxyz
    private static byte[] table12to8 = new byte[4096];
    private static short[] table8to16 = new short[256];
    static {
        // b12 --> b8
        for (int m = 0; m < 16; m++) {
            int v = m ^ 0x55;
            table12to8[m] = (byte) v;
            table12to8[4095 - m] = (byte) (v + 128);
        }
        for (int p = 1, q = 0x10; p <= 0x40; p <<= 1, q+=0x10) {
            for (int i = 0, j = (p << 4); i < 16; i++, j += p) {
                int v = (i + q) ^ 0x55;
                byte value1 = (byte) v;
                byte value2 = (byte) (v + 128);
                for (int m = j, e = j + p; m < e; m++) {
                    table12to8[m] = value1;
                    table12to8[4095 - m] = value2;
                }
            }
        }
        // b8 --> b16
        for (int m = 0; m < 16; m++) {
            int v = m << 4;
            table8to16[m ^ 0x55] = (short) v;
            table8to16[(m + 128) ^ 0x55] = (short) (65536 - v);
        }
        for (int q = 1; q <= 7; q++) {
            for (int i = 0, m = (q << 4); i < 16; i++, m++) {
                int v = (i + 0x10) << (q + 3);
                table8to16[m ^ 0x55] = (short) v;
                table8to16[(m + 128) ^ 0x55] = (short) (65536 - v);
            }
        }
    }
    public int decode(short[] b16, byte[] b8, int count, int offset) {
        for (int i = 0, j = offset; i < count; i++, j++) {
            b16[i] = table8to16[b8[j] & 0xff];
        }
        return count;
    }
    public int encode(short[] b16, int count, byte[] b8, int offset) {
        for (int i = 0, j = offset; i < count; i++, j++) {
            b8[j] = table12to8[(b16[i] & 0xffff) >> 4];
        }
        return count;
    }
    public int getSampleCount(int frameSize) {
        return frameSize;
    }
}