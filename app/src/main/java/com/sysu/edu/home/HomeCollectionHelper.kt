package com.sysu.edu.home;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HomeCollectionHelper extends SQLiteOpenHelper {
    public HomeCollectionHelper(Context context) {
        super(context, "service_collection.db", null, 5);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS service_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, serviceId INTEGER, serviceJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS dashboard_shortcut_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, shortcutId INTEGER, shortcutJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion <= 2) {
            db.execSQL("ALTER TABLE service_collection ADD COLUMN serviceId INTEGER");
        }
        if (newVersion <= 3) {
            db.execSQL("ALTER TABLE service_collection ADD COLUMN position INTEGER");
        }
        if (newVersion <= 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS dashboard_shortcut_collection (id INTEGER PRIMARY KEY AUTOINCREMENT, shortcutId INTEGER, shortcutJson TEXT, collectTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, position INTEGER)");
        }
    }
    
    public void addService(Integer id, String serviceJson, Integer position) {
        if (!isServiceCollected(id)) {
            ContentValues values = new ContentValues();
            values.put("serviceId", id);
            values.put("serviceJson", serviceJson);
            values.put("position", position);
            getWritableDatabase().insertOrThrow("service_collection", null, values);
        }
    }
    
    public void addDashboardShortcut(Integer id, String shortcutJson, Integer position) {
        if (!isDashboardShortcutCollected(id)) {
            ContentValues values = new ContentValues();
            values.put("shortcutId", id);
            values.put("shortcutJson", shortcutJson);
            if (position != null) values.put("position", position);
            getWritableDatabase().insertOrThrow("dashboard_shortcut_collection", null, values);
        }
    }
    
    public void updateServicePosition(Integer id, Integer position) {
        if (isServiceCollected(id)) {
            ContentValues values = new ContentValues();
            if (position != null) values.put("position", position);
            getWritableDatabase().update("service_collection", values, "serviceId = ?", new String[]{id.toString()});
        }
    }
    
    public void updateDashboardShortcutPosition(Integer id, Integer position) {
        if (isCollected("dashboard_shortcut_collection", "shortcutId", id)) {
            ContentValues values = new ContentValues();
            if (position != null) values.put("position", position);
            getWritableDatabase().update("dashboard_shortcut_collection", values, "shortcutId = ?", new String[]{id.toString()});
        }
    }
    
    public boolean isServiceCollected(Integer id) {
        return isCollected("service_collection", "serviceId", id);
    }
    
    public boolean isDashboardShortcutCollected(Integer id) {
        return isCollected("dashboard_shortcut_collection", "shortcutId", id);
    }
    
    public boolean isCollected(String table, String column, Integer id) {
        Cursor cursor = getReadableDatabase().query(table, null, column + " = ?", new String[]{id.toString()}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }
    
    public void deleteDashboardShortcut(Integer id) {
        getWritableDatabase().delete("dashboard_shortcut_collection", "shortcutId = ?", new String[]{id.toString()});
    }
    
    public void deleteService(Integer id) {
        getWritableDatabase().delete("service_collection", "serviceId = ?", new String[]{id.toString()});
    }
}
