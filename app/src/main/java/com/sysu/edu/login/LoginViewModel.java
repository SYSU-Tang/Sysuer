package com.sysu.edu.login;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    final MutableLiveData<String> password = new MutableLiveData<>();
    final MutableLiveData<String> account = new MutableLiveData<>();
    final MutableLiveData<String> url = new MutableLiveData<>();
    final MutableLiveData<Boolean> login = new MutableLiveData<>();
    final MutableLiveData<String> sessionId = new MutableLiveData<>();
    final MutableLiveData<String> cookie = new MutableLiveData<>();
    final MutableLiveData<String> target = new MutableLiveData<>();
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
