package nat.chung.mediadecoderplayer;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.os.SystemClock.sleep;

/**
 * Created by Nat on 2017/1/29.
 */

public class DecodePlayer implements IPlayer, TextureView.SurfaceTextureListener {

    private static final String TAG = "DecodePlayer";
    private static final int timeoutUs = 1000000;

    private TextureView textureView;
    private MediaCodec decoder;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private PLAY_TASK_STATUS playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;

    enum PLAY_TASK_STATUS {
        PLAY_TASK_RUNNING,
        PLAY_TASK_STOPING,
        PLAY_TASK_STOPPED
    }

    public DecodePlayer(TextureView textureView){
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
    }

    private void addVideoFrame(byte[] data, long timestampMS){

        if((playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_RUNNING)||(decoder == null ))
            return;

        synchronized (this){
            int inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs);
            if (inputBufferIndex >= 0) {
                ByteBuffer buf[] = decoder.getInputBuffers();
                buf[inputBufferIndex].put(data);
                decoder.queueInputBuffer(inputBufferIndex, 0, data.length, timestampMS*1000, 0);
            }
        }
    }


    @Override
    public void addAVFrame(AVFRAME_TYPE type, byte[] data, long timestampMS) {

        if(type == AVFRAME_TYPE.VIDEO){
            addVideoFrame(data, timestampMS);
        }
    }

    @Override
    public void setup(String mineType, MediaFormat format) throws IOException{

        if(textureView.getSurfaceTexture() == null){
            throw new IOException("SurfaceTexture not ready yet");
        }

        if(playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_STOPPED){
            throw new IOException("Need stop before using setup, playTaskStatus:"+playTaskStatus);
        }

        initCodec(mineType, format);
        startPlayTask();
    }

    @Override
    public void stop() {

        stopPlayTask();
        waitForPlayTaskEnd();
        releaseCodec();
        baseTimestamp = 0;
    }

    @Override
    public TextureView getTextureView() {
        return textureView;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}


    private long baseTimestamp = 0;
    private void playTask() throws IOException, InterruptedException {

        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_RUNNING;

        while (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING){
            int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
            if (outputBufferIndex >= 0) {
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                sleepForNextFrame(bufferInfo.presentationTimeUs/1000);
            }
        }
        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    }

    private void initCodec(String mineType, MediaFormat format) throws  IOException{
        decoder = MediaCodec.createDecoderByType(mineType);
        decoder.configure(format, new Surface(textureView.getSurfaceTexture()), null, 0 /* 0:decoder 1:encoder */);
        decoder.start();
    }

    private void releaseCodec(){

        if(decoder == null)
            return;

        synchronized (this){
            decoder.flush();
            decoder.stop();
            decoder.release();
            decoder = null;
        }
    }


    private void startPlayTask(){

        new Thread(new Runnable() {
            public void run() {
                try {
                    playTask();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void stopPlayTask(){

        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPING;
    }

    private void waitForPlayTaskEnd(){

        while (playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_STOPPED){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sleepForNextFrame(long timestamp){

        if(baseTimestamp == 0){
            baseTimestamp = System.currentTimeMillis() - timestamp;
        }

        while((baseTimestamp + timestamp) > System.currentTimeMillis() && (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING)) {
            sleep(10);
        }
    }
}

