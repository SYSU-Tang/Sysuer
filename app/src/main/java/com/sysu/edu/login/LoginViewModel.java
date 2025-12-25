package com.sysu.edu.login;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    MutableLiveData<String> password = new MutableLiveData<>();
    MutableLiveData<String> account = new MutableLiveData<>();
    MutableLiveData<String> url = new MutableLiveData<>();
    MutableLiveData<Boolean> login = new MutableLiveData<>();
    MutableLiveData<String> sessionId = new MutableLiveData<>();
    MutableLiveData<String> cookie = new MutableLiveData<>();
    MutableLiveData<String> target = new MutableLiveData<>();
    public MutableLiveData<String> getAccount(){
        return account;
    }

    public void setAccount(String acc){
        account.setValue(acc);
    }

    public MutableLiveData<String> getPassword(){
        return password;
    }

    public void setPassword(String pw){
        password.setValue(pw);
    }

    public MutableLiveData<String> getUrl(){
        return url;
    }

    public MutableLiveData<String> getTarget(){
        return target;
    }
    public void setUrl(String linking){
        url.setValue(linking);
    }

    public MutableLiveData<Boolean> getLogin(){
        return login;
    }

    public void setLogin(boolean isLogin){
        login.setValue(isLogin);
    }

    public MutableLiveData<String> getSessionId(){
        return sessionId;
    }

    public MutableLiveData<String> getCookie() {
        return cookie;
    }

    public void setCookie(String cookies) {
        cookie.setValue(cookies);
    }

    public void setSessionID(String session){
        sessionId.setValue(session);
    }

    public void setTarget(String targetLinking) {
        target.setValue(targetLinking);
    }
}
