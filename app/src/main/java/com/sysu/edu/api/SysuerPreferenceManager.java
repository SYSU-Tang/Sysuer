package com.sysu.edu.api;

import android.content.SharedPreferences;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SysuerPreferenceManager extends ViewModel {
    private final static String Theme = "theme";
    private final static String Home = "home";
    private final static String Language = "language";
    private final static String Qrcode = "qrcode";
    private final static String Update = "update";
    private final static String IsFirstLaunch = "launch";
    private final static String IsAgree = "agree";
    private final MutableLiveData<Boolean> isAgreeLiveData = new MutableLiveData<>();
    private SharedPreferences pm;

    public void setPM(SharedPreferences oldPM) {
        this.pm = oldPM;
    }
    public String getString(String key, String defValue) {
        return pm.getString(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return pm.getBoolean(key, defValue);
    }
    public SharedPreferences getPm() {
        return pm;
    }
    public String getTheme() {
        return getString(Theme, "2");
    }

    public String getHome() {
        return getString(Home, "2");
    }

    public String getLanguage() {
        return getString(Language, "2");
    }
    public String getQrcode() {
        return getString(Qrcode, "");
    }
    public boolean getIsAgree(){
        return getBoolean(IsAgree, false);
    }

    public void setIsAgree(boolean isAgree){
        pm.edit().putBoolean(IsAgree, isAgree).apply();
    }

    public boolean getIsFirstLaunch(){
        return getBoolean(IsFirstLaunch, false);
    }

    public void setIsFirstLaunch(boolean isFirstLaunch){
        pm.edit().putBoolean(IsFirstLaunch, isFirstLaunch).apply();
    }

    public boolean getUpdate(){
        return getBoolean(Update, true);
    }

    public MutableLiveData<Boolean> getIsAgreeLiveData() {
        return isAgreeLiveData;
    }
     public void setIsAgreeLiveData(boolean isAgree){
        isAgreeLiveData.setValue(isAgree);
    }

}
