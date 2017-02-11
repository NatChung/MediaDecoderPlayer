package nat.chung.mediadecoderplayer.SQLCache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import nat.chung.mediadecoderplayer.CacheFrame;

/**
 * Created by starvedia on 2017/2/12.
 */

public class SQLCache implements IDataCache {

    public static final String TABLE_NAME = "FrameQueue";
    public static final String KEY_ID = "_id";
    // TODO: 2017/2/12 db field init
    public static final String FRAME_TYPE = "FrameType";
    public static final String FRAME_DATA = "FrameData";
    public static final String IS_KEYFRAME = "isKeyFrame";
    public static final String PTS = "Pts";
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FRAME_TYPE + " INTEGER NOT NULL, " +
                    FRAME_DATA + " Blob NOT NULL, " +
                    IS_KEYFRAME + " INTEGER NOT NULL, " +
                    PTS +  " INTEGER)";

    private SQLiteDatabase db;

    public SQLCache(Context context){
        db = SQLCacheDBHelper.getDatabase(context);
    }

    public int getCount() {
        int result = 0;
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

        if (cursor.moveToNext()) {
            result = cursor.getInt(0);
        }

        return result;
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
        ContentValues cv = new ContentValues();
        cv.put(FRAME_TYPE, 1);
        cv.put(FRAME_DATA, videoFrame.data);
        cv.put(IS_KEYFRAME, videoFrame.isKeyFrame);
        cv.put(PTS, videoFrame.timestampMS);
        long id = db.insert(TABLE_NAME, null, cv);
        return (id != -1) ? true : false;
    }

    @Override
    public boolean pushAudioFrame(CacheFrame audioFrame) {
        ContentValues cv = new ContentValues();
        cv.put(FRAME_TYPE, 0);
        cv.put(FRAME_DATA, audioFrame.data);
        cv.put(IS_KEYFRAME, audioFrame.isKeyFrame);
        cv.put(PTS, audioFrame.timestampMS);
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
        // TODO: 2017/2/12  
    }
}
