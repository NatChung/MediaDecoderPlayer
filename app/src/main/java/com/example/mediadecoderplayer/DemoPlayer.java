package com.example.mediadecoderplayer;

import android.content.Context;
import android.media.MediaFormat;
import android.view.TextureView;
import java.io.IOException;

import nat.chung.mediadecoderplayer.DecodePlayer;
import nat.chung.mediadecoderplayer.G711UCodec;
import nat.chung.mediadecoderplayer.IPlayer;
import nat.chung.mediadecoderplayer.IDataCache;
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

    public void setupCache(IDataCache cache){
        player.setupCache(cache);
    }
    public void stop(){
        player.stop();
    }
    public void dataFinish(){
        player.finishAddAVFrame();
    }

    public void setup(String mineType, MediaFormat format) throws IOException {
        player.setupVideoDecoder(mineType, format);
    }

    public void pause(){ player.pause();}

    public void resume(){ player.resume();}

    public void setupPCM(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode){
        player.setupPCM(streamType, sampleRateInHz, channelConfig, audioFormat, mode);
    }

    public void setupPCM(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int mode, G711UCodec g711){
        player.setupPCM(streamType, sampleRateInHz, channelConfig, audioFormat, mode, g711);
    }

    public void addAVFrame(IPlayer.AVFRAME_TYPE type, byte[] data, long timestampMS, int isKeyFrame){
        player.addAVFrame(type, data, timestampMS, isKeyFrame);
    }

    public void snapshot(String savedPath){
        player.snapshot(savedPath);
    }

    public void seekTo(float progress){
        player.seekTo(progress);
    }

    @Override
    public void onDidFinishPlay() {
        player.stop();
    }

    @Override
    public void onDidPlay() {

    }
}
