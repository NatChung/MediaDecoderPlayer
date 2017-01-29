package com.example.mediadecoderplayer;

import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import nat.chung.mediadecoderplayer.DemoPlayer;
import nat.chung.mediadecoderplayer.R;
import uzb.uz.PanZoomPlayer.pan.zoom.ZoomableTextureLayout;

public class MainActivity extends AppCompatActivity implements  TextureView.SurfaceTextureListener{

    private static final String TAG = "MainActivity";
    DemoPlayer player = null;
    ZoomableTextureLayout zoomableTextureLayout;

    private DisplayMetrics displayMetrics;
    private int displayWidth;
    private int displayHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;

        zoomableTextureLayout = (ZoomableTextureLayout)findViewById(R.id.video_view);
        zoomableTextureLayout.zoomableTextureView.setSurfaceTextureListener(this);
        zoomableTextureLayout.zoomableTextureView.setDisplayMetrics(displayWidth, displayHeight);
        player = new DemoPlayer();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public void onClick(@SuppressWarnings("unused") View unused){
        player.play(this, new Surface(zoomableTextureLayout.zoomableTextureView.getSurfaceTexture()));
    }

}
