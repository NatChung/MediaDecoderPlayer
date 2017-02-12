package nat.chung.mediadecoderplayer.SQLCache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import nat.chung.mediadecoderplayer.CacheFrame;
import nat.chung.mediadecoderplayer.IPlayer;

/**
 * Created by starvedia on 2017/2/12.
 */

public class SQLCache implements IDataCache {

    public static final String TABLE_NAME = "FrameQueue";
    public static final String KEY_ID = "_id";
    // TODO: 2017/2/12 db field init
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

    private SQLiteDatabase db;
    private int playIndex = 0;
    public SQLCache(Context context){
        db = SQLCacheDBHelper.getDatabase(context);
    }

    @Override
    public int getCacheCount() {
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        return cursor.getCount();
    }

    @Override
    public CacheFrame popVideoFrame() {
        // TODO: 2017/2/12  
        return null;
    }

    @Override
    public CacheFrame popAudioFrame() {
        // TODO: 2017/2/12  
        return null;
    }

    @Override
    public boolean pushVideoFrame(CacheFrame videoFrame) {
        return putFrameToDB(IPlayer.AVFRAME_TYPE.VIDEO, videoFrame);
    }

    @Override
    public boolean pushAudioFrame(CacheFrame audioFrame) {
        return putFrameToDB(IPlayer.AVFRAME_TYPE.AUDIO, audioFrame);
    }

    private boolean putFrameToDB(IPlayer.AVFRAME_TYPE type, CacheFrame inputFrame){
        ContentValues cv = new ContentValues();

        if (type == IPlayer.AVFRAME_TYPE.VIDEO)
            cv.put(IS_VIDEO, 1);
        else
            cv.put(IS_VIDEO, 0);

        cv.put(FRAME_DATA, inputFrame.data);
        cv.put(IS_KEYFRAME, inputFrame.isKeyFrame);
        cv.put(PTS, inputFrame.timestampMS);
        long id = db.insert(TABLE_NAME, null, cv);
        return (id == -1) ? true : false;
    }

    @Override
    public void clear() {
        Log.i("ClementDebug", "clear cache");
        db.delete(TABLE_NAME, null, null);
    }

    @Override
    public void seekTo(float progress) {
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        playIndex = (int) Math.abs(cursor.getCount()*(progress/100));
        Log.i("ClementDebug", "seekTo: playIndex = "+playIndex);
    }
}
