package com.example.mediadecoderplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;

import java.io.IOException;
import java.nio.ByteBuffer;

import nat.chung.mediadecoderplayer.IPlayer;
import nat.chung.mediadecoderplayer.R;

public class MainActivity extends AppCompatActivity implements DatabaseLoader.OnDataUpdateListener, SeekBar.OnSeekBarChangeListener{

    private static final String TAG = "MainActivity";
    DemoPlayer player = null;
    boolean endOfExtraFile = true;
    private final String DB_PATH = "/sdcard/mediacodec/localplayback1min.db";
    SeekBar seekBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = new DemoPlayer(this, (TextureView)findViewById(R.id.video_view));
        MainActivity.verifyStoragePermissions(this);


        setSeekBar();
    }

    private void setSeekBar() {
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onPlayClicked(@SuppressWarnings("unused") View unused){

        if(endOfExtraFile == false)
            return;

        endOfExtraFile = false;
        new Thread(new Runnable() {
            public void run() {
                try {
                    extraMP4File();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void onSQLPlayCLicked(View view){
        setDataBaseLoader();
    }

    public void onStopClicked(@SuppressWarnings("unused") View unused){
        endOfExtraFile = true;
        player.stop();
    }

    public void onSnapshotClicked(@SuppressWarnings("unused") View unused){
        //player.snapshot("/sdcard/out.png");
//        player.seekTo(0f);
        seekBar.setProgress(0);
    }

    public void onVideoResume(View unused){
        player.resume();
    }

    public void onVideoPause(View unused){
        player.pause();
    }


    private void extraMP4File() throws IOException {

        AssetFileDescriptor afd = this.getResources().openRawResourceFd(R.raw.clipcanvas_14348_h264_640x360);
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
        int numTracks = mediaExtractor.getTrackCount();
        String mine_type = null;
        MediaFormat format = null;
        for (int i = 0; i < numTracks; ++i) {
            format = mediaExtractor.getTrackFormat(i);
            mine_type = format.getString(MediaFormat.KEY_MIME);
            if (mine_type.startsWith("video/")) {
                mediaExtractor.selectTrack(i);
                format.setInteger(MediaFormat.KEY_CAPTURE_RATE, 24);
                format.setInteger(MediaFormat.KEY_PUSH_BLANK_BUFFERS_ON_STOP, 1);
                break;
            }
        }
        player.setup(mine_type, format);
        player.setupPCM(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.MODE_STREAM);

        ByteBuffer inputBuffer = ByteBuffer.allocate(640*380*3);
        while (endOfExtraFile == false) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if(sampleSize > 0){
                byte[] data = new byte[sampleSize];
                inputBuffer.get(data);
                player.addAVFrame(IPlayer.AVFRAME_TYPE.VIDEO, data, mediaExtractor.getSampleTime()/1000, -1);
                mediaExtractor.advance();
                continue;
            }
            break;
        }

        endOfExtraFile = true;
        mediaExtractor.release();
        player.dataFinish();
    }


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    private void setDataBaseLoader() {

        player.setupCache(new DatabaseLoader(this, DB_PATH));
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 800);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        try {
            player.setup("video/avc", format);
            player.setupPCM(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.MODE_STREAM);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onVideoRawData(byte[] data, long pts, int isKeyFrame) {
        player.addAVFrame(IPlayer.AVFRAME_TYPE.VIDEO, data, pts, isKeyFrame);
    }

    @Override
    public void onAudioRawData(byte[] data, long pts) {
        player.addAVFrame(IPlayer.AVFRAME_TYPE.AUDIO, data, pts, -1);
    }

    @Override
    public void onFileFinish() {
        player.dataFinish();
    }
    
    // seekbar listener
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        player.seekTo((float)i / 100f);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i("ClementDebug", "onStartTrackingTouch: ");
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        Log.i("ClementDebug", "onStopTrackingTouch: ");
        new Handler().postDelayed(new Runnable(){
            public void run(){
                player.seekTo((float)seekBar.getProgress() / 100f);
            }
        }, 500);
        
    }
    //==========
}
