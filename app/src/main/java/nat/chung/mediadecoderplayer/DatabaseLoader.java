package nat.chung.mediadecoderplayer;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.File;

import nat.chung.mediadecoderplayer.SQLCache.PlaybackFileSQLiteOpenHelper;

/**
 * Created by starvedia on 2017/1/5.
 */

public class DatabaseLoader {

    public interface OnDataUpdateListener {
        void onVideoRawData(byte[] data, long pts, int isKeyFrame);
        void onAudioRawData(byte[] data, long pts);
        void onFileFinish();
    }

    private final Context mContext;
    public final String SD_PLAYBACKFILE = "/sdcard/mediacodec/temp.db";
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

    public DatabaseLoader(Context context){
        this.mContext = context;
    }
    public void setDataUpdateListener(OnDataUpdateListener onDataUpdateListener){
        this.onDataUpdateListener = onDataUpdateListener;
    }


    public void getVideoDataFromDatabase(String path) {

        File s_decFile = new File(path);
        if (s_decFile.exists()) {
            pbFileHelper = new PlaybackFileSQLiteOpenHelper(mContext, path, null, version, PBtables, playBackfieldNames, playBackfieldTypes);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadSDCardPlayBackFromDB2Array();
                }
            }).start();
        }
        else {
            Log.i("ClementDebug", "getVideoDataFromDatabase: file don't exit.");
        }
    }

    private void loadSDCardPlayBackFromDB2Array()
    {
        if ((null != pbFileHelper))
        {
            String f[] = { "*" };
            Cursor c = pbFileHelper.select(PBtables[1], f, null, null, null, null, null);

            //  data type{ "INTEGER PRIMARY KEY", "INTEGER", "INTEGER", "INTEGER", "INTEGER", "Blob" }
            //  data field { "ID", "isKey", "isVideo", "ptsms", "frameLens", "frame" }
            int count = 0;
            while (c.moveToNext())
            {
                int isVideo = c.getInt(2);
                if (isVideo == 1)
                    onDataUpdateListener.onVideoRawData(c.getBlob(5), c.getLong(3), c.getInt(1));
                else
                    onDataUpdateListener.onAudioRawData(c.getBlob(5), c.getLong(3));
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: =========================");
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: id = "+c.getInt(0));
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: isKeyFrame = "+c.getInt(1));
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: isVideo = "+c.getInt(2));
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: pts = "+c.getLong(3));
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: data len = "+c.getInt(4));
//                Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: data = "+c.getBlob(5).length);

            }
            Log.i("ClementDebug", "loadSDCardPlayBackFromDB2Array: read source db done");
            onDataUpdateListener.onFileFinish();
        }

    }
}
