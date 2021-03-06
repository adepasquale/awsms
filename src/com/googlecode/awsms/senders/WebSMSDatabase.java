/*
 * Copyright 2010-2011 Andrea De Pasquale
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.awsms.senders;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

/**
 * Object to manage database, useful e.g. to keep track of sent messages number
 * 
 * @author Andrea De Pasquale
 */
public class WebSMSDatabase {

  static final String TAG = "WebSenderDatabase";

  static final String DB_NAME = "awsms.db";
  static final int DB_VERSION = 2;
  static final String TABLE_NAME = "sms";

  public static final String _ID = "_id";
  public static final String DATE = "date";
  public static final String SENDER = "sender";
  public static final String COUNT = "count";

  private static class DBOpenHelper extends SQLiteOpenHelper {

    DBOpenHelper(Context context) {
      super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID
          + " INTEGER PRIMARY KEY," + DATE + " TEXT," + SENDER + " TEXT,"
          + COUNT + " INTEGER" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.execSQL("DROP TABLE IF EXISTS smslog;");
      onCreate(db);
    }
  }

  DBOpenHelper dbOpenHelper;
  SimpleDateFormat dateFormat;

  public WebSMSDatabase(Context context) {
    dbOpenHelper = new DBOpenHelper(context);
    dateFormat = new SimpleDateFormat("yyyyMMdd");
    deleteOld(); // keep the database clean
  }

  public int queryToday(String sender) {
    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    queryBuilder.setTables(TABLE_NAME);

    String[] projection = { _ID, DATE, SENDER, COUNT };
    String selection = String.format("%s=\'%s\' AND %s=\'%s\'", DATE,
        dateFormat.format(new Date()), SENDER, sender);

    SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
    Cursor cursor = queryBuilder.query(database, projection, selection, null,
        null, null, null);

    int size = 0;
    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      size += cursor.getInt(cursor.getColumnIndex(COUNT));
      cursor.moveToNext();
    }

    cursor.close();
    return size;
  }

  public void insertNew(String sender, int size) {
    ContentValues values = new ContentValues();
    values.put(DATE, dateFormat.format(new Date()));
    values.put(SENDER, sender);
    values.put(COUNT, size);

    SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
    database.insert(TABLE_NAME, SENDER, values);
  }

  public void deleteOld() {
    SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
    String whereClause = String.format("%s<>\'%s\'", DATE,
        dateFormat.format(new Date()));
    database.delete(TABLE_NAME, whereClause, null);
  }

}
