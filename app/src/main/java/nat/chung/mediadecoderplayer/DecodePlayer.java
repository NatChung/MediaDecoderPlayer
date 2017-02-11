package nat.chung.mediadecoderplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
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

public class DecodePlayer implements IPlayer, TextureView.SurfaceTextureListener {

    private static final String TAG = "DecodePlayer";
    private static final int timeoutUs = 1000000;
    private static final long SHORT_SLEEP_TME_IN_MS = 10;

    private TextureView textureView;
    private MediaCodec decoder = null;
    private MediaCodec.BufferInfo bufferInfo = null;
    private PLAY_TASK_STATUS playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    private Queue<AVFrame> videoFrameQueue;
    private Queue<AVFrame> audioFrameQueue;
    private OnDecodePlayerPlaybackListener onDecodePlayerPlaybackListener;
    private boolean avFrameFinished = false;
    private Surface surface;
    private AudioTrack audioTrack;
    private IDataCache mDataCache;

    enum PLAY_TASK_STATUS {
        PLAY_TASK_RUNNING,
        PLAY_TASK_STOPING,
        PLAY_TASK_STOPPED
    }

    public DecodePlayer(TextureView textureView){
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
        videoFrameQueue = new LinkedBlockingQueue<>();
        audioFrameQueue = new LinkedBlockingQueue<>();
    }

    // TODO: 2017/2/11 waiting sql cache 
    public DecodePlayer(TextureView textureView, IDataCache dataCache){
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
        this.mDataCache = dataCache;
        
    }
    

    private void addVideoFrame(byte[] data, long timestampMS){
        videoFrameQueue.offer(new AVFrame(data, timestampMS));
    }

    private void addAudioFrame(byte[] data) {
        if(audioTrack != null && baseTimestamp > 0)
            audioFrameQueue.offer(new AVFrame(data, -1));
    }

    private void initCodec(String mineType, MediaFormat format) throws  IOException{
        bufferInfo = new MediaCodec.BufferInfo();
        decoder = MediaCodec.createDecoderByType(mineType);
        decoder.configure(format, surface, null, 0 /* 0:decoder 1:encoder */);
        decoder.start();
    }

    private void initAudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode){
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        audioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, minBufferSize << 2, mode);
        audioTrack.play();
    }

    private void releaseCodec(){

        if(decoder == null)
            return;

        decoder.flush();
        decoder.stop();
        decoder.release();
        decoder = null;
    }

    private void releaseAudio(){
        if(audioTrack == null)
            return;

        audioTrack.flush();
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }

    private void cleanAVFrameQueue(){
        audioFrameQueue.clear();
        videoFrameQueue.clear();
    }

    private void startVideoTask(){
        new Thread(new Runnable() {
            public void run() {
                videoTask();
            }
        }).start();
    }

    private void startAudioTask(){
        new Thread(new Runnable() {
            public void run() {
                audioTask();
            }
        }).start();
    }

    private void startOnDidFinishTask(){

        if(onDecodePlayerPlaybackListener == null)
            return;

        new Thread(new Runnable() {
            public void run() { onDecodePlayerPlaybackListener.onDidFinishPlay();}
        }).start();
    }

    private void startOnDidPlay(){

        if(baseTimestamp != 0 || onDecodePlayerPlaybackListener == null)
            return;

        new Thread(new Runnable() {
            public void run() { onDecodePlayerPlaybackListener.onDidPlay();}
        }).start();
    }

    private void stopPlayTask(){
        if(playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING)
            playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPING;
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
            startOnDidPlay();
            sleepForNextFrame(bufferInfo.presentationTimeUs/1000);
        }
    }


    private long baseTimestamp = 0;
    private void videoTask()  {

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
    private void audioTask() {

        while (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING){
            AVFrame audioFrame = audioFrameQueue.poll();
            if(audioFrame == null){
                sleep(SHORT_SLEEP_TME_IN_MS);
                continue;
            }
            audioTrack.write(audioFrame.data, 0, audioFrame.data.length);
        }

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.surface = new Surface(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopPlayTask();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void addAVFrame(AVFRAME_TYPE type, byte[] data, long timestampMS) {

        if(type == AVFRAME_TYPE.VIDEO){
            addVideoFrame(data, timestampMS);
        }else {
            addAudioFrame(data);
        }
    }

    @Override
    public void finishAddAVFrame() { avFrameFinished = true; }

    @Override
    public void setupVideoDecoder(String mineType, MediaFormat format) throws IOException {

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
        startVideoTask();
    }

    @Override
    public void setupPCM(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode) {
        initAudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, mode);
        startAudioTask();
    }



    @Override
    public void stop() {

        stopPlayTask();
        waitForPlayTaskEnd();
        releaseCodec();
        releaseAudio();
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
        void onDidPlay();
    }
}

