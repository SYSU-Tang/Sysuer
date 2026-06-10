package com.sysu.edu.browser;

import static com.sysu.edu.api.FileManager.readAssets;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class BrowserHelper extends SQLiteOpenHelper {
    private final Context context;
    
    public BrowserHelper(Context context) {
        super(context, "browser.db", null, 12);
        this.context = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS ua (id INTEGER PRIMARY KEY AUTOINCREMENT, uaId INTEGER UNIQUE, position INTEGER, title TEXT, ua TEXT, description TEXT, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE IF NOT EXISTS js (id INTEGER PRIMARY KEY AUTOINCREMENT, jsId INTEGER UNIQUE, position INTEGER, title TEXT, script TEXT, description TEXT, matches TEXT, state INTEGER DEFAULT 1, run INTEGER DEFAULT 0,author TEXT, time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        JSONArray.parseArray(readAssets(getContext(), "js.json")).forEach((i) -> {
            JSONObject item = (JSONObject) i;
            db.execSQL("INSERT OR IGNORE INTO js (jsId, position, title, script, description, matches) VALUES (?, ?, ?, ?, ?, ?)", new Object[]{item.getInteger("jsId"), item.getInteger("position"), item.getString("title"), item.getString("script"), item.getString("description"), item.getJSONArray("matches").toJSONString()});
        });
        JSONArray.parse(readAssets(getContext(), "ua.json")).forEach((i) -> {
            JSONObject item = (JSONObject) i;
            db.execSQL("INSERT OR IGNORE INTO ua (uaId, position, title, ua, description) VALUES (?, ?, ?, ?, ?)", new Object[]{item.getInteger("uaId"), item.getInteger("position"), item.getString("title"), item.getString("ua"), item.getString("description")});
        });
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE js ADD COLUMN state INTEGER DEFAULT 1");
            ContentValues state = new ContentValues();
            state.put("state", 1);
            db.update("js", state, null, null);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE js ADD COLUMN run INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE js ADD COLUMN author TEXT");
        }
        if (oldVersion < 4) {
            ContentValues value = new ContentValues();
            value.put("state", 1);
            value.put("run", 0);
            value.put("author", "SYSU-Tang");
            value.put("title", "美化");
            value.put("description", "教务系统界面美化");
            value.put("matches", "[\"://jwxt.sysu.edu.cn/jwxt/mk/\"]");
            value.put("script", "['.sys-header','.sys-footer','.ant-breadcrumb'].forEach(function(v){if(document.querySelector(v)!=null)document.querySelector(v).style.display='none';});document.querySelectorAll('col').forEach(element=>{element.style.minWidth = \"0px\";});document.querySelector('.stu-con').style.padding='0px';");
            db.insert("js", null, value);
        }
        if (oldVersion < 10) {
            ContentValues value = new ContentValues();
            value.put("title", "美化");
            value.put("description", "教务系统主页去除无用元素，包括头部、底部、调查问卷");
            value.put("author", "SYSU-Tang");
            value.put("matches", "[\"://jwxt.sysu.edu.cn/jwxt/#/student\"]");
            value.put("script", "['.sys-header','.sys-footer','.invest2'].forEach(function(v){if(document.querySelector(v)!=null)document.querySelector(v).style.display='none';});document.querySelectorAll('.noPaddingTd col').forEach(element=>{element.style.minWidth = \"0px\";});document.querySelector('.ant-layout-content').style.paddingTop='0px';");
            db.insert("js", null, value);
        }
        if (oldVersion < 9) {
            ContentValues value = new ContentValues();
            value.put("script", "['.sys-header','.sys-footer','.ant-breadcrumb','.ant-tabs-bar'].forEach(function(v){if(document.querySelector(v)!=null)document.querySelector(v).style.display='none';});document.querySelectorAll('col').forEach(element=>{element.style.minWidth = \"0px\";});document.querySelector('.stu-con').style.padding='0px';");
            db.update("js", value, "matches LIKE  ?", new String[]{"%personalTrainingProgramView%"});
        }
        if (oldVersion < 11) {
            ContentValues value = new ContentValues();
            value.put("run", 0);
            value.put("author", "SYSU-Tang");
            value.put("title", "新心理健康视频速通");
            value.put("description", "在线教学平台，视频一键通关");
            value.put("matches", "[\"://lms.sysu.edu.cn/mod/fsresource/view.php\"]");
            value.put("script", "var sourceData = playerdata && playerdata.source ? JSON.parse(playerdata.source) : {};\nvar sources = {};\nif (sourceData?.FD) {\n    sources.FD = [{\n        src: sourceData.FD\n    }];\n}\nif (sourceData?.LD) {\n    sources.LD = [{\n        src: sourceData.LD\n    }];\n}\nif (sourceData?.SD) {\n    sources.SD = [{\n        src: sourceData.SD\n    }];\n    defaultRes = 'SD';\n}\nif (sourceData?.HD) {\n    sources.HD = [{\n        src: sourceData.HD\n    }];\n}\nif (sourceData?.OD) {\n    sources.FHD = [{\n        src: sourceData.OD\n    }];\n}\nvar playerWrapper = new TCPlayerWrapper(\n    \"fsplayer-container-id_html5_api\",\n    sources,\n    playerdata.siteUrl + \"/lib/ajax/service.php?sesskey=\" + playerdata.sesskey,\n    `fs_${playerdata.userid}_${playerdata.fsresourceid || 0}`,\n    15 * 1000,\n    playerdata.progress == 1\n);\nfor (let i = 0; i < playerWrapper.player.duration()/4+1 ; i++){setTimeout(()=>{playerWrapper.viewTotalTime = 4000 ;playerWrapper.ajaxOrder()},i*10);\n}");
            db.insert("js", null, value);
        }
        if (oldVersion < 12) {
            ContentValues value = new ContentValues();
//            value.put("run", 0);
//            value.put("author", "SYSU-Tang");
            value.put("title", "在线教学平台视频速通");
//            value.put("description", "在线教学平台，视频一键通关");
//            value.put("matches", "[\"://lms.sysu.edu.cn/mod/fsresource/view.php\"]");
//            value.put("script", "var sourceData = playerdata && playerdata.source ? JSON.parse(playerdata.source) : {};\nvar sources = {};\nif (sourceData?.FD) {\n    sources.FD = [{\n        src: sourceData.FD\n    }];\n}\nif (sourceData?.LD) {\n    sources.LD = [{\n        src: sourceData.LD\n    }];\n}\nif (sourceData?.SD) {\n    sources.SD = [{\n        src: sourceData.SD\n    }];\n    defaultRes = 'SD';\n}\nif (sourceData?.HD) {\n    sources.HD = [{\n        src: sourceData.HD\n    }];\n}\nif (sourceData?.OD) {\n    sources.FHD = [{\n        src: sourceData.OD\n    }];\n}\nvar playerWrapper = new TCPlayerWrapper(\n    \"fsplayer-container-id_html5_api\",\n    sources,\n    playerdata.siteUrl + \"/lib/ajax/service.php?sesskey=\" + playerdata.sesskey,\n    `fs_${playerdata.userid}_${playerdata.fsresourceid || 0}`,\n    15 * 1000,\n    playerdata.progress == 1\n);\nfor (let i = 0; i < playerWrapper.player.duration()/4+1 ; i++){setTimeout(()=>{playerWrapper.viewTotalTime = 4000 ;playerWrapper.ajaxOrder()},i*10);\n}");
            db.update("js", value, "title LIKE  ?", new String[]{"%新心理健康视频速通%"});
        }
        
    }
    
    public Context getContext() {
        return context;
    }
    
}
