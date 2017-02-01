package nat.chung.mediadecoderplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
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
    private static final long SHORT_SLEEP_TME_IN_MS = 10;

    private TextureView textureView;
    private MediaCodec decoder = null;
    private MediaCodec.BufferInfo bufferInfo = null;
    private PLAY_TASK_STATUS playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    private Queue<AVFrame> audioFrameQueue;
    private Queue<AVFrame> videoFrameQueue;
    private OnDecodePlayerPlaybackListener onDecodePlayerPlaybackListener;
    private boolean avFrameFinished = false;

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

    private void initCodec(String mineType, MediaFormat format) throws  IOException{
        bufferInfo = new MediaCodec.BufferInfo();
        decoder = MediaCodec.createDecoderByType(mineType);
        decoder.configure(format, new Surface(textureView.getSurfaceTexture()), null, 0 /* 0:decoder 1:encoder */);
        decoder.start();
    }

    private void releaseCodec(){

        if(decoder == null)
            return;

        decoder.flush();
        decoder.stop();
        decoder.release();
        decoder = null;
    }


    private void startPlayTask(){
        new Thread(new Runnable() {
            public void run() {
                playTask();
            }
        }).start();
    }

    private void startOnDidFinishTask(){

        if(onDecodePlayerPlaybackListener == null)
            return;

        new Thread(new Runnable() {
            public void run() {
                onDecodePlayerPlaybackListener.onDidFinishPlay();
            }
        }).start();
    }

    private void stopPlayTask(){
        if(playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING)
            playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPING;
    }

    private void cleanAVFrameQueue(){
        audioFrameQueue.clear();
        videoFrameQueue.clear();
    }

    private void waitForPlayTaskEnd(){

        while (playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_STOPPED){
            try {
                Thread.sleep(SHORT_SLEEP_TME_IN_MS);
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
            sleep(SHORT_SLEEP_TME_IN_MS);
        }
    }

    private void frameBufferEmptyHander(){

        if(avFrameFinished == true){
            playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPING;
            startOnDidFinishTask();
            return;
        }

        sleep(SHORT_SLEEP_TME_IN_MS);
    }

    private void videoDecoderEnqueueFrame(AVFrame videoFrame){
        int inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs);
        if (inputBufferIndex >= 0) {
            ByteBuffer buf[] = decoder.getInputBuffers();
            buf[inputBufferIndex].put(videoFrame.data);
            decoder.queueInputBuffer(inputBufferIndex, 0, videoFrame.data.length, videoFrame.timestampMS*1000, 0);
        }
    }

    private void videoDecoderDequeueFrame(){
        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        if (outputBufferIndex >= 0) {
            decoder.releaseOutputBuffer(outputBufferIndex, true);
            sleepForNextFrame(bufferInfo.presentationTimeUs/1000);
        }
    }

    private long baseTimestamp = 0;
    private void playTask()  {

        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_RUNNING;
        while (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING){
            AVFrame videoFrame = videoFrameQueue.poll();
            if(videoFrame == null){
                frameBufferEmptyHander();
                continue;
            }
            videoDecoderEnqueueFrame(videoFrame);
            videoDecoderDequeueFrame();
        }
        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    }

    @Override
    public void addAVFrame(AVFRAME_TYPE type, byte[] data, long timestampMS) {

        if(type == AVFRAME_TYPE.VIDEO){
            addVideoFrame(data, timestampMS);
        }
    }

    @Override
    public void finishAVFrame() {
        Log.i("NatDebug", "finishAVFrame");
        avFrameFinished = true;
    }

    @Override
    public void setup(String mineType, MediaFormat format) throws IOException{

        if(textureView.getSurfaceTexture() == null){
            throw new IOException("SurfaceTexture not ready yet");
        }

        if(playTaskStatus != PLAY_TASK_STATUS.PLAY_TASK_STOPPED){
            throw new IOException("Need stop before using setup, playTaskStatus:"+playTaskStatus);
        }

        if(decoder != null){
            throw new IOException("Need stop before using setup, decoder is not released");
        }

        initCodec(mineType, format);
        startPlayTask();
    }

    @Override
    public void stop() {

        stopPlayTask();
        waitForPlayTaskEnd();
        releaseCodec();
        cleanAVFrameQueue();

        baseTimestamp = 0;
        avFrameFinished = false;
    }

    @Override
    public TextureView getTextureView() {
        return textureView;
    }

    public void setOnDecodePlayerPlaybackListener(OnDecodePlayerPlaybackListener onDecodePlayerPlaybackListener){
        this.onDecodePlayerPlaybackListener = onDecodePlayerPlaybackListener;
    }


    private class AVFrame{
        public final byte[] data;
        public final long timestampMS;
        public AVFrame(byte[] data, long timestampMS){
            this.data = data;
            this.timestampMS = timestampMS;
        }
    }

    public interface OnDecodePlayerPlaybackListener{
        void onDidFinishPlay();
    }
}

