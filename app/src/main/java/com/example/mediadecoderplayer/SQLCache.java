package com.example.mediadecoderplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import nat.chung.mediadecoderplayer.CacheFrame;
import nat.chung.mediadecoderplayer.IDataCache;

/**
 * Created by starvedia on 2017/2/12.
 */

public class SQLCache implements IDataCache {

    public static final String TABLE_VIDEO = "Video";
    public static final String TABLE_AUDIO = "Audio";
    public static final String KEY_ID = "_id";
    public static final String IS_VIDEO = "isVideo";
    public static final String FRAME_DATA = "FrameData";
    public static final String IS_KEYFRAME = "isKeyFrame";
    public static final String PTS = "Pts";
    public static final String CREATE_VIDEO_TABLE =
            "CREATE TABLE " + TABLE_VIDEO + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FRAME_DATA + " Blob NOT NULL, " +
                    IS_KEYFRAME + " INTEGER NOT NULL, " +
                    PTS +  " INTEGER)";
    public static final String CREATE_AUDIO_TABLE =
            "CREATE TABLE " + TABLE_AUDIO + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FRAME_DATA + " Blob NOT NULL, " +
                    IS_KEYFRAME + " INTEGER NOT NULL, " +
                    PTS +  " INTEGER)";
    private static final String TAG = "SQLCache";

    enum FRAME_TYPE{
        AUDIO, VIDEO
    }
    private SQLiteDatabase db;
    private int playVideoIndex = 0;
    private int playAudioIndex = 0;
    public SQLCache(Context context){
        db = SQLCacheDBHelper.getDatabase(context);
        clear();
    }

    @Override
    public int getCacheCount() {
        Cursor videoCursor = getTableCursor(FRAME_TYPE.VIDEO);
        Cursor audioCursor = getTableCursor(FRAME_TYPE.AUDIO);
        return videoCursor.getCount()+audioCursor.getCount();
    }

    @Override
    public CacheFrame popVideoFrame() {
        return getCacheFrame(FRAME_TYPE.VIDEO);
    }

    @Override
    public CacheFrame popAudioFrame() {
        return getCacheFrame(FRAME_TYPE.AUDIO);
    }

    @Nullable
    private CacheFrame getCacheFrame(FRAME_TYPE type) {
        Cursor cursor = db.query(getTableName(type), null, null, null, null, null, null);
        int index = (type == FRAME_TYPE.VIDEO) ? playVideoIndex:playAudioIndex;
        cursor.moveToPosition(index);
        CacheFrame result;

        try{
            result = new CacheFrame(cursor.getBlob(1), cursor.getInt(3), cursor.getInt(2));
        }catch (CursorIndexOutOfBoundsException e){
            return null;
        }
        cursor.close();
        increaseIndex(type);
        return result;
    }

    public void increaseIndex(FRAME_TYPE type){
        if(type == FRAME_TYPE.VIDEO)
            playVideoIndex++;
        else
            playAudioIndex++;
    }

    @Override
    public boolean pushVideoFrame(CacheFrame videoFrame) {
        return putFrameToDB(FRAME_TYPE.VIDEO, videoFrame);
    }

    @Override
    public boolean pushAudioFrame(CacheFrame audioFrame) {
        return true;//putFrameToDB(FRAME_TYPE.AUDIO, audioFrame);
    }

    private boolean putFrameToDB(FRAME_TYPE type, CacheFrame inputFrame){

        ContentValues cv = new ContentValues();

        cv.put(FRAME_DATA, inputFrame.data);
        cv.put(IS_KEYFRAME, inputFrame.isKeyFrame);
        cv.put(PTS, inputFrame.timestampMS);

        long id = db.insert(getTableName(type), null, cv);
        return (id == -1) ? true : false;
    }

    @NonNull
    private String getTableName(FRAME_TYPE type) {
        return (type== FRAME_TYPE.VIDEO)? TABLE_VIDEO : TABLE_AUDIO;
    }

    private Cursor getTableCursor(FRAME_TYPE type){
        return db.query(getTableName(type), null, null, null, null, null, null);
    }

    @Override
    public void clear() {
        db.delete(TABLE_VIDEO, null, null);
        db.delete(TABLE_AUDIO, null, null);
    }

    @Override
    public void seekTo(float progress) {
        Cursor videoCursor = db.query(getTableName(FRAME_TYPE.VIDEO), null, null, null, null, null, null);
        int totalCount = videoCursor.getCount();

        if (totalCount == 0){
            return;
        }

        int seekToIndex = (int) Math.abs((float)totalCount*progress);
        playVideoIndex = findKeyFrameIndex(videoCursor, seekToIndex);
        Log.i(TAG,"playVideoIndex:"+playVideoIndex);
        videoCursor.moveToPosition(playVideoIndex);
        playAudioIndex = findAudioIndex(videoCursor.getInt(3));
        videoCursor.close();
    }

    private int findKeyFrameIndex(Cursor cursor, int progressIndex) {
        cursor.moveToPosition(progressIndex);
        int result = 0;
        while (cursor.moveToNext()){
            result = cursor.getInt(0);
            int isKeyFrame = cursor.getInt(2);
            if (isKeyFrame == 1)
                break;
        }
        return result;
    }

    private int findAudioIndex(int videoPts) {
        int result = 0;
        Cursor audioCursor = getTableCursor(FRAME_TYPE.AUDIO);
        while (audioCursor.moveToNext()){
            int pts = audioCursor.getInt(3);
            if(pts == videoPts){
                result = audioCursor.getInt(0);
            }
        }
        audioCursor.close();
        return result;
    }
}
