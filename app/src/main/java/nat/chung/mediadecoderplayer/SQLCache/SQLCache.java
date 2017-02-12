package nat.chung.mediadecoderplayer.SQLCache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.util.Log;

import nat.chung.mediadecoderplayer.CacheFrame;
import nat.chung.mediadecoderplayer.IPlayer;

/**
 * Created by starvedia on 2017/2/12.
 */

public class SQLCache implements IDataCache {

    public static final String TABLE_NAME = "FrameQueue";
    public static final String KEY_ID = "_id";
    public static final String IS_VIDEO = "isVideo";
    public static final String FRAME_DATA = "FrameData";
    public static final String IS_KEYFRAME = "isKeyFrame";
    public static final String PTS = "Pts";
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    IS_VIDEO + " INTEGER NOT NULL, " +
                    FRAME_DATA + " Blob NOT NULL, " +
                    IS_KEYFRAME + " INTEGER NOT NULL, " +
                    PTS +  " INTEGER)";
    enum FRAME_TYPE{
        VIDEO, AUDIO
    }
    private SQLiteDatabase db;
    private int playIndex = 0;
    public SQLCache(Context context){
        db = SQLCacheDBHelper.getDatabase(context);
    }

    @Override
    public int getCacheCount() {
        Cursor cursor = getTableCursor(TABLE_NAME);
        return cursor.getCount();
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
        Cursor cursor = getTableCursor(TABLE_NAME);
        cursor.moveToPosition(playIndex);
        CacheFrame result = null;
        while (cursor.moveToNext()){
            playIndex++;
            int isVideo = cursor.getInt(1);
            if (isVideo == type.ordinal()) {
                result = new CacheFrame(cursor.getBlob(2), cursor.getInt(4), cursor.getInt(3));
            }
        }
        return result;
    }

    @Override
    public boolean pushVideoFrame(CacheFrame videoFrame) {
        return putFrameToDB(FRAME_TYPE.VIDEO, videoFrame);
    }

    @Override
    public boolean pushAudioFrame(CacheFrame audioFrame) {
        return putFrameToDB(FRAME_TYPE.AUDIO, audioFrame);
    }

    private boolean putFrameToDB(FRAME_TYPE type, CacheFrame inputFrame){

        ContentValues cv = new ContentValues();
        cv.put(IS_VIDEO, type.ordinal());
        cv.put(FRAME_DATA, inputFrame.data);
        cv.put(IS_KEYFRAME, inputFrame.isKeyFrame);
        cv.put(PTS, inputFrame.timestampMS);

        long id = db.insert(TABLE_NAME, null, cv);
        return (id == -1) ? true : false;
    }

    private Cursor getTableCursor(String tableName){
        return db.query(TABLE_NAME, null, null, null, null, null, null);
    }

    @Override
    public void clear() {
        db.delete(TABLE_NAME, null, null);
    }

    @Override
    public void seekTo(float progress) {
        Cursor cursor = getTableCursor(TABLE_NAME);
        int progressIndex = (int) Math.abs(cursor.getCount()*(progress/100));
        playIndex = findKeyFrameIndex(cursor, progressIndex);
    }

    private int findKeyFrameIndex(Cursor cursor, int progressIndex) {
        cursor.moveToPosition(progressIndex);
        while (cursor.moveToNext()){
            progressIndex++;
            int isVideo = cursor.getInt(1);
            int isKeyFrame = cursor.getInt(3);
            if (isVideo == 1 && isKeyFrame == 1)
                break;
        }
        return progressIndex;
    }
}
