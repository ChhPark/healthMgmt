package com.house.healthMgmt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "HealthMgmt.db";
    private static final int DATABASE_VERSION = 1;

    // 테이블 이름에 접두어 적용
    public static final String TABLE_NAME = "health_daily_log";
    
    public static final String COL_ID = "id";
    public static final String COL_DATE = "record_date";
    // 각 항목별 컬럼
    public static final String COL_PROTEIN = "protein_val";
    public static final String COL_SODIUM = "sodium_val";
    public static final String COL_WATER = "water_val";
    public static final String COL_NO_SODA = "no_soda";
    public static final String COL_NO_ALCOHOL = "no_alcohol";
    public static final String COL_SLEEP = "sleep_time";
    public static final String COL_EXERCISE = "exercise_time";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DATE + " TEXT, " +
                COL_PROTEIN + " INTEGER DEFAULT 0, " +
                COL_SODIUM + " INTEGER DEFAULT 0, " +
                COL_WATER + " INTEGER DEFAULT 0, " +
                COL_NO_SODA + " INTEGER DEFAULT 0, " + // 0:Fail, 1:Success
                COL_NO_ALCOHOL + " INTEGER DEFAULT 0, " +
                COL_SLEEP + " INTEGER DEFAULT 0, " +
                COL_EXERCISE + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
