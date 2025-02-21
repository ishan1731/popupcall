package com.example.popupcall

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "CallLogs.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE calls (id INTEGER PRIMARY KEY, number TEXT, type TEXT, note TEXT)""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS calls")
        onCreate(db)
    }

    fun saveCall(number: String, type: String, note: String) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("number", number)
                put("type", type)
                put("note", note)
            }
            db.insert("calls", null, values)
        }
    }

    fun isNumberSaved(number: String): Boolean = readableDatabase.use { db ->
        db.query("calls", null, "number=?", arrayOf(number), null, null, null).use { cursor ->
            cursor.moveToFirst()
        }
    }
}

