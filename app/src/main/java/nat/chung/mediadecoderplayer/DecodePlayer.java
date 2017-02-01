package nat.chung.mediadecoderplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.SystemClock.sleep;

/**
 * Created by Nat on 2017/1/29.
 */

public class DecodePlayer implements IPlayer {

    private static final String TAG = "DecodePlayer";
    private static final int timeoutUs = 1000000;
    private static final long SHOT_SLEEP_TIME_MS = 10;

    private TextureView textureView;
    private MediaCodec decoder = null;
    private MediaCodec.BufferInfo bufferInfo = null;
    private PLAY_TASK_STATUS playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    private Queue<AVFrame> audioFrameQueue;
    private Queue<AVFrame> videoFrameQueue;

    enum PLAY_TASK_STATUS {
        PLAY_TASK_RUNNING,
        PLAY_TASK_STOPING,
        PLAY_TASK_STOPPED
    }

    public DecodePlayer(TextureView textureView){

        this.textureView = textureView;
        audioFrameQueue = new LinkedBlockingQueue<>();
        videoFrameQueue = new LinkedBlockingQueue<>();
    }

    private void addVideoFrame(byte[] data, long timestampMS){

        if((playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_RUNNING)||(decoder == null ))
            return;

        videoFrameQueue.offer(new AVFrame(data, timestampMS));
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
        cleanAVFrameQueue();
    }

    @Override
    public TextureView getTextureView() {
        return textureView;
    }

    private long baseTimestamp = 0;
    private void playTask() throws IOException, InterruptedException {

        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_RUNNING;

        while (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING){

            if(videoFrameQueue.size() == 0){
                sleep(SHOT_SLEEP_TIME_MS);continue;
            }

            AVFrame videoFrame = videoFrameQueue.poll();
            int inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs);
            if (inputBufferIndex >= 0) {
                ByteBuffer buf[] = decoder.getInputBuffers();
                buf[inputBufferIndex].put(videoFrame.data);
                decoder.queueInputBuffer(inputBufferIndex, 0, videoFrame.data.length, videoFrame.timestampMS*1000, 0);
            }

            int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
            if (outputBufferIndex >= 0) {
                decoder.releaseOutputBuffer(outputBufferIndex, true);
                sleepForNextFrame(bufferInfo.presentationTimeUs/1000);
            }
        }
        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    }

    private void initCodec(String mineType, MediaFormat format) throws  IOException{
        bufferInfo = new MediaCodec.BufferInfo();
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

    private void cleanAVFrameQueue(){
        audioFrameQueue.clear();
        videoFrameQueue.clear();
    }

    private void waitForPlayTaskEnd(){

        while (playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_STOPPED){
            try {
                Thread.sleep(SHOT_SLEEP_TIME_MS);
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
            sleep(SHOT_SLEEP_TIME_MS);
        }
    }



    private class AVFrame{
        public final byte[] data;
        public final long timestampMS;
        public AVFrame(byte[] data, long timestampMS){
            this.data = data;
            this.timestampMS = timestampMS;
        }

    }
}

