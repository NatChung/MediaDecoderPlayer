package com.example.mediadecoderplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;
import java.nio.ByteBuffer;

import nat.chung.mediadecoderplayer.IPlayer;
import nat.chung.mediadecoderplayer.R;
import nat.chung.mediadecoderplayer.SQLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    DemoPlayer player = null;
    boolean endOfExtraFile = true;
    private String DB_PATH = "/sdcard/mediacodec/temp.db";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        MainActivity.verifyStoragePermissions(this);
    }


    public void onPlayClicked(@SuppressWarnings("unused") View unused){

        if(endOfExtraFile == false)
            return;
        
        endOfExtraFile = false;
        
        player = new DemoPlayer(this, (TextureView)findViewById(R.id.video_view));
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

    @SuppressWarnings("unused")
    public void onSQLPlayClicked(View unused){
        try {
            player = new DemoPlayer(this, (TextureView)findViewById(R.id.video_view), DB_PATH);
        } catch (IOException e) {
            Log.i("ClementDebug", "onSQLPlayClicked: DB file don't exit. ");
        }

    }

    public void onStopClicked(@SuppressWarnings("unused") View unused){
        endOfExtraFile = true;
        player.stop();
    }

    public void onSnapshotClicked(@SuppressWarnings("unused") View unused){
        player.snapshot("/sdcard/out.png");
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

        ByteBuffer inputBuffer = ByteBuffer.allocate(640*380*3);
        while (endOfExtraFile == false) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if(sampleSize > 0){
                byte[] data = new byte[sampleSize];
                inputBuffer.get(data);
                player.addAVFrame(IPlayer.AVFRAME_TYPE.VIDEO,data, mediaExtractor.getSampleTime()/1000);
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



}
