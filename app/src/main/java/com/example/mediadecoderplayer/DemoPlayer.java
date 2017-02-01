package com.example.mediadecoderplayer;

import android.content.Context;
import android.media.MediaFormat;
import android.util.Log;
import android.view.TextureView;
import java.io.IOException;

import nat.chung.mediadecoderplayer.DecodePlayer;
import nat.chung.mediadecoderplayer.IPlayer;
import nat.chung.mediadecoderplayer.decorator.SnapshotDecorator;
import nat.chung.mediadecoderplayer.decorator.Zoom.ZoomDecorator;


/**
 * Created by Nat on 2017/1/29.
 */

public class DemoPlayer implements DecodePlayer.OnDecodePlayerPlaybackListener {

    private static final String TAG = "DemoPlayer";
    private SnapshotDecorator player;

    public DemoPlayer(Context context, TextureView textureView){
        DecodePlayer decodePlayer = new DecodePlayer(textureView);
        decodePlayer.setOnDecodePlayerPlaybackListener(this);
        ZoomDecorator zoomDecorator = new ZoomDecorator(context, decodePlayer);
        player = new SnapshotDecorator(zoomDecorator);
    }

    public void stop(){
        player.stop();
    }

    public void dataFinish(){
        player.finishAVFrame();
    }

    public void setup(String mineType, MediaFormat format) throws IOException {
        player.setup(mineType, format);
    }

    public void addAVFrame(IPlayer.AVFRAME_TYPE type, byte[] data, long timestampMS){
        player.addAVFrame(type, data, timestampMS);
    }

    public void snapshot(String savedPath){
        player.snapshot(savedPath);
    }

    @Override
    public void onDidFinishPlay() {
        player.stop();
    }

    @Override
    public void onDidPlay() {
        Log.i(TAG,"onDidPlay~~~~~~~~~~~~~");
    }
}
