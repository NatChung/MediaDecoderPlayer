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
    Cursor videoCursor, audioCursor;

    @Override
    public CacheFrame popVideoFrame() {
        if (videoCursor.moveToNext()){
            return new CacheFrame(videoCursor.getBlob(3), videoCursor.getLong(2), videoCursor.getInt(4));
        }
        return null;
    }

    @Override
    public CacheFrame popAudioFrame() {
        if(audioCursor.moveToNext()){
            return new CacheFrame(audioCursor.getBlob(3), audioCursor.getLong(2), 0);
        }
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
    public void clear() { }

    @Override
    public void seekTo(float progress) {
        videoCursor.moveToPosition((int)(videoCursor.getCount() * progress));
        while (videoCursor.getInt(4)!=1 && videoCursor.moveToPrevious()){}

        long videoPts = videoCursor.getLong(2);

        audioCursor.moveToFirst();
        while (audioCursor.getLong(2) < videoPts && audioCursor.moveToNext()){}

        audioCursor.moveToPrevious();
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
            videoCursor = pbFileHelper.select2(PBtables[0], new String[]{"*"}, "IsVideo=1", null, "PtsTimeMs", null);
            audioCursor = pbFileHelper.select2(PBtables[0], new String[]{"*"}, "IsVideo=0", null, "PtsTimeMs", null);
        } else {
            Log.i("ClementDebug", "getVideoDataFromDatabase: file don't exit.");
        }
    }
}
