package com.example.mediadecoderplayer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by starvedia on 2017/2/11.
 */

public class SQLCacheDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "cache.db";
    public static final int VERSION = 1;
    private static final String TAG = "SQLCacheDBHelper";
    private static SQLiteDatabase database;

    public SQLCacheDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        int i = Log.i(TAG, "SQLCacheDBHelper onCreate");
        sqLiteDatabase.execSQL(SQLCache.CREATE_VIDEO_TABLE);
        sqLiteDatabase.execSQL(SQLCache.CREATE_AUDIO_TABLE);
    }

    public static SQLiteDatabase getDatabase(Context context) {
        if (database == null || !database.isOpen()) {
            database = new SQLCacheDBHelper(context, DATABASE_NAME, null, VERSION).getWritableDatabase();
        }

        return database;
    }

    public static Cursor select(String table)
    {
        Cursor cursor = database.query(table, null, null, null, null, null, null);
        return cursor;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.i(TAG,"SQLCacheDBHelper");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SQLCache.TABLE_VIDEO);
        // 呼叫onCreate建立新版的表格
        onCreate(sqLiteDatabase);
    }

    public static void delete(){
        database.execSQL("DROP TABLE IF EXISTS " + SQLCache.TABLE_VIDEO);
    }
}
