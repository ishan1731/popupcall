package com.example.popupcall

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CallDatabase.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "calls"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NUMBER = "number"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_DURATION = "duration"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NUMBER TEXT NOT NULL,
                $COLUMN_STATUS TEXT,
                $COLUMN_NOTES TEXT,
                $COLUMN_DURATION TEXT
            )
        """
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertCall(number: String, status: String, notes: String, duration: String, date: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NUMBER, number)
            put(COLUMN_STATUS, status)
            put(COLUMN_NOTES, notes)
            put(COLUMN_DURATION, duration)
            put(COLUMN_DURATION, date)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getCallByNumber(number: String): CallRecord? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_NUMBER = ?",
            arrayOf(number),
            null,
            null,
            null
        )

        var callRecord: CallRecord? = null
        if (cursor.moveToFirst()) {
            callRecord = CallRecord(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                number = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMBER)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
                duration = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION))
            )
        }
        cursor.close()
        db.close()
        return callRecord
    }
}
