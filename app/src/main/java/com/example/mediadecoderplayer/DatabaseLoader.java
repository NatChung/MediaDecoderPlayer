package com.example.mediadecoderplayer;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.File;

import com.example.mediadecoderplayer.PlaybackFileSQLiteOpenHelper;

import nat.chung.mediadecoderplayer.CacheFrame;
import nat.chung.mediadecoderplayer.IDataCache;

/**
 * Created by starvedia on 2017/1/5.
 */

public class DatabaseLoader implements IDataCache {

    private static final String TAG = "DatabaseLoader";
    Cursor videoCursor;

    @Override
    public CacheFrame popVideoFrame() {
        if (videoCursor.moveToNext()){
            return new CacheFrame(videoCursor.getBlob(5), videoCursor.getLong(3), videoCursor.getInt(1));
        }
        return null;
    }

    @Override
    public CacheFrame popAudioFrame() {
        return null;
    }

    @Override
    public boolean pushVideoFrame(CacheFrame videoFrame) {
        return false;
    }

    @Override
    public boolean pushAudioFrame(CacheFrame audioFrame) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public void seekTo(float progress) {

        videoCursor.moveToFirst();
        videoCursor.moveToPrevious();
    }

    @Override
    public int getCacheCount() {
        return 0;
    }

    public interface OnDataUpdateListener {
        void onVideoRawData(byte[] data, long pts, int isKeyFrame);
        void onAudioRawData(byte[] data, long pts);
        void onFileFinish();
    }

    public final String SD_PLAYBACKFILE = "/sdcard/mediacodec/temp1280.db";
    public static PlaybackFileSQLiteOpenHelper pbFileHelper = null;
    private final String PBtables[] = { "Recording", "ESFrame"};
    private final int version = 1;
    private final String playBackfieldNames[][] =
            {
                    { "ID", "IsVideo", "PtsTimeMs", "Data", "isKeyFrame", "DataLen", "FrameNo" }
            };
    private final String playBackfieldTypes[][] =
            {
                    { "INTEGER PRIMARY KEY", "INTEGER", "INTEGER", "Blob", "INTEGER", "INTEGER", "INTEGER" }
            };
    private final String sdPlayBackfieldNames[][] =
            {
                    { "ID", "isKey", "isVideo", "ptsms", "frameLens", "frame" }
            };
    private final String sdPlayBackfieldTypes[][] =
            {
                    { "INTEGER PRIMARY KEY", "INTEGER", "INTEGER", "INTEGER", "INTEGER", "Blob" }
            };
    OnDataUpdateListener onDataUpdateListener;

    public DatabaseLoader(Context context, String path){

        File s_decFile = new File(path);
        if (s_decFile.exists()) {
            pbFileHelper = new PlaybackFileSQLiteOpenHelper(context, path, null, version, PBtables, playBackfieldNames, playBackfieldTypes);
            videoCursor = pbFileHelper.select2(PBtables[1], new String[]{"*"}, "isVideo=1", null, "ptsms", null);
        }
        else {
            Log.i("ClementDebug", "getVideoDataFromDatabase: file don't exit.");
        }
    }
    public void setDataUpdateListener(OnDataUpdateListener onDataUpdateListener){
        this.onDataUpdateListener = onDataUpdateListener;
    }

    private void loadSDCardPlayBackFromDB2Array()
    {
        if (null != pbFileHelper)
        {
            String f[] = { "*" };
            Cursor c = pbFileHelper.select(PBtables[1], f, null, null, null, null, null);
            while (c.moveToNext())
            {
                int isVideo = c.getInt(2);
                if (isVideo == 1)
                    onDataUpdateListener.onVideoRawData(c.getBlob(5), c.getLong(3), c.getInt(1));
                else
                    onDataUpdateListener.onAudioRawData(c.getBlob(5), c.getLong(3));
            }
            Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: read source db done");
            onDataUpdateListener.onFileFinish();
        }

    }
}
