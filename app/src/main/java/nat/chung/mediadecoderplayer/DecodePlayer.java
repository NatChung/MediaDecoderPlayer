package nat.chung.mediadecoderplayer;

import android.graphics.SurfaceTexture;
import android.media.AudioTrack;
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

public class DecodePlayer implements IPlayer, TextureView.SurfaceTextureListener {

    private static final String TAG = "DecodePlayer";
    private static final int timeoutUs = 10000;
    private static final long SHORT_SLEEP_TME_IN_MS = 10;

    private TextureView textureView;
    private MediaCodec decoder = null;
    private MediaCodec.BufferInfo bufferInfo = null;
    private PLAY_TASK_STATUS playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    private OnDecodePlayerPlaybackListener onDecodePlayerPlaybackListener;
    private boolean avFrameFinished = false;
    private Surface surface = null;
    private AudioTrack audioTrack = null;
    private IDataCache dataCache = null;
    private long lastSeekToTimestamp = System.currentTimeMillis();
    private final long SEEKTO_DURATION_MS = 500;

    private Queue<CacheFrame> audioQueue = new LinkedBlockingQueue<>();
    private Queue<CacheFrame> videoQueue = new LinkedBlockingQueue<>();


    enum PLAY_TASK_STATUS {
        PLAY_TASK_RUNNING,
        PLAY_TASK_STOPING,
        PLAY_TASK_STOPPED,
        PLAY_TASK_PAUSED
    }

    public DecodePlayer(TextureView textureView){
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(this);
    }

    private void addVideoFrame(byte[] data, long timestampMS, int isKeyFrame){

        if(dataCache == null){
            videoQueue.offer(new CacheFrame(data, timestampMS, isKeyFrame));
        }else{
            dataCache.pushVideoFrame(new CacheFrame(data, timestampMS, isKeyFrame));
        }

    }

    private void addAudioFrame(byte[] data, long timestampMS, int isKeyFrame) {
        if(dataCache == null){
            audioQueue.offer(new CacheFrame(data, timestampMS, isKeyFrame));
        }else{
            dataCache.pushAudioFrame(new CacheFrame(data, timestampMS, isKeyFrame));
        }

    }

    private void initCodec(String mineType, MediaFormat format) throws  IOException{
        bufferInfo = new MediaCodec.BufferInfo();
        decoder = MediaCodec.createDecoderByType(mineType);
        decoder.configure(format, surface, null, 0 /* 0:decoder 1:encoder */);
        decoder.start();
    }

    private void initAudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode){
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        audioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, minBufferSize * 10, mode);
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

        if(dataCache == null){
            audioQueue.clear();
            videoQueue.clear();
        }else{
            dataCache.clear();
        }
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
            audioTrack.play();
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

    private void videoDecoderEnqueueFrame(CacheFrame videoFrame){

        while(playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING){
            int inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs);
            if (inputBufferIndex >= 0) {
                ByteBuffer buf[] = decoder.getInputBuffers();
                buf[inputBufferIndex].put(videoFrame.data);
                decoder.queueInputBuffer(inputBufferIndex, 0, videoFrame.data.length, videoFrame.timestampMS*1000, 0);
                break;
            }
        }
    }

    private void videoDecoderDequeueFrameToResumeTimestamp(){

        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        if (outputBufferIndex >= 0) {
            if((bufferInfo.presentationTimeUs/1000) == resumeTimestamp){
                baseTimestamp = 0;
                resumeTimestamp = 0;
                decoder.releaseOutputBuffer(outputBufferIndex, true);
            }else{
                decoder.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    private void videoDecoderDequeueFrame(){

        int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
        if (outputBufferIndex >= 0) {
            sleepForNextFrame(bufferInfo.presentationTimeUs/1000);
            decoder.releaseOutputBuffer(outputBufferIndex, true);
            startOnDidPlay();
        }
    }

    private CacheFrame popVideoFrame() {
        return (dataCache == null) ? videoQueue.poll() : dataCache.popVideoFrame();
    }


    private long baseTimestamp = 0;
    private long resumeTimestamp = 0;
    private void videoTask()  {

        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_RUNNING;
        while (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING || playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_PAUSED){

            waitForPause();
            synchronized(this){
                CacheFrame videoFrame = popVideoFrame();
                if(videoFrame == null){
                    frameBufferEmptyHander();
                    continue;
                }
                videoDecoderEnqueueFrame(videoFrame);
            }

            if(resumeTimestamp > 0){
                videoDecoderDequeueFrameToResumeTimestamp();
            }else{
                videoDecoderDequeueFrame();
            }

        }
        playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_STOPPED;
    }

    private CacheFrame popAudioFrame() {
        return (dataCache == null) ? audioQueue.poll() : dataCache.popAudioFrame();
    }

    G711UCodec G711 = new G711UCodec();
    private void audioTask() {

        while (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_RUNNING || playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_PAUSED){
            waitForPause();

            CacheFrame audioFrame = null;
            synchronized(this) {
                audioFrame = popAudioFrame();
            }

            if(audioFrame == null ){
                sleep(SHORT_SLEEP_TME_IN_MS);
                continue;
            }

            if(dataCache == null){
                audioTrack.write(audioFrame.data, 0, audioFrame.data.length);
            }else{
                short[] audioData = new short [audioFrame.data.length ];
                G711.decode(audioData, audioFrame.data, audioFrame.data.length , 0);
                audioTrack.write(audioData, 0, audioData.length);
            }

            sleep(SHORT_SLEEP_TME_IN_MS*9);
        }
    }

    private void waitForPause(){
        while (playTaskStatus== PLAY_TASK_STATUS.PLAY_TASK_PAUSED) {
            sleep(SHORT_SLEEP_TME_IN_MS*5);
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
    public void addAVFrame(AVFRAME_TYPE type, byte[] data, long timestampMS, int isKeyFrame) {
        if(type == AVFRAME_TYPE.VIDEO){
            addVideoFrame(data, timestampMS, isKeyFrame);
        }else {
            addAudioFrame(data, timestampMS, isKeyFrame);
        }
    }

    @Override
    public void finishAddAVFrame() {
        avFrameFinished = true;
    }

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
    public void seekTo(final float progress) {

        if(dataCache == null) return;

        long diff = System.currentTimeMillis() - lastSeekToTimestamp;
        if(diff < SEEKTO_DURATION_MS){
            return;
        }
        lastSeekToTimestamp = System.currentTimeMillis();



        new Thread(new Runnable() {
            public void run() {
                synchronized (this){
                    dataCache.seekTo(progress);

                    CacheFrame videoFrame = dataCache.popVideoFrame();
                    videoDecoderEnqueueFrame(videoFrame);
                    int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
                    if (outputBufferIndex >= 0) {
                        decoder.releaseOutputBuffer(outputBufferIndex, false);
                    }
                    resumeTimestamp = videoFrame.timestampMS;

                    audioTrack.flush();
                    audioTrack.stop();
                    audioTrack.play();
                }
            }
        }).start();
    }

    @Override
    public void resume() {
        if (playTaskStatus == PLAY_TASK_STATUS.PLAY_TASK_PAUSED)
            playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_RUNNING;
    }

    @Override
    public void pause() {
        if (playTaskStatus== PLAY_TASK_STATUS.PLAY_TASK_RUNNING) {
            playTaskStatus = PLAY_TASK_STATUS.PLAY_TASK_PAUSED;
        }
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
    public void setupCache(IDataCache cache) {
        dataCache = cache;
    }

    @Override
    public TextureView getTextureView() {
        return textureView;
    }

    public void setOnDecodePlayerPlaybackListener(OnDecodePlayerPlaybackListener onDecodePlayerPlaybackListener){
        this.onDecodePlayerPlaybackListener = onDecodePlayerPlaybackListener;
    }

    public interface OnDecodePlayerPlaybackListener{
        void onDidFinishPlay();
        void onDidPlay();
    }
}

