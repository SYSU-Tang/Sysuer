package com.sysu.edu.api;

import android.content.Context;
import android.util.Base64;
import android.util.Pair;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AccountManager {
    
    private static volatile AccountManager INSTANCE;
    private final RxDataStore<Preferences> dataStore;
    private final Aead aead;
    
    private AccountManager(Context context) {
        try {
            AeadConfig.register();
            aead = new AndroidKeysetManager.Builder()
                    .withSharedPref(context, "master_keyset", "secure_keys")
                    .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                    .withMasterKeyUri("android-keystore://master_key")
                    .build()
                    .getKeysetHandle()
                    .getPrimitive(RegistryConfiguration.get(), Aead.class);
        } catch (Exception e) {
            throw new RuntimeException("Tink 初始化失败", e);
        }
        dataStore = new RxPreferenceDataStoreBuilder(context, "accounts").build();
    }
    
    public static AccountManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AccountManager.class) {
                if (INSTANCE == null)
                    INSTANCE = new AccountManager(context.getApplicationContext());
            }
        }
        return INSTANCE;
    }
    
    public Pair<String, String> getActiveAccountSync(String domain) {
        String username = dataStore.data().blockingFirst().get(PreferencesKeys.stringKey("active:" + domain));
        if (username == null) return null;
        String password = getPasswordSync(domain, username);
        return new Pair<>(username, password);
    }
    
    public String getPasswordSync(String domain, String username) {
        String encoded = dataStore.data().blockingFirst().get(PreferencesKeys.stringKey(domain + ":" + username));
        if (encoded == null) return null;
        try {
            return new String(aead.decrypt(Base64.decode(encoded, Base64.DEFAULT), null));
        } catch (Exception e) {
            return null;
        }
    }
    
    public Single<Pair<String, String>> getActiveAccountAsync(String domain) {
        return dataStore.data().firstOrError()
                .map(prefs -> {
                    String username = prefs.get(PreferencesKeys.stringKey("active:" + domain));
                    if (username == null) throw new Exception("No active account");
                    String encoded = prefs.get(PreferencesKeys.stringKey(domain + ":" + username));
                    if (encoded == null) throw new Exception("Password not found");
                    String password = new String(aead.decrypt(Base64.decode(encoded, Base64.DEFAULT), null));
                    System.out.println("Username " + username + " Decrypted password: " + password);
                    return new Pair<>(username, password);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    public Completable setAccountAsync(String domain, String username, String password) {
        return Completable.fromAction(() -> dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(PreferencesKeys.stringKey(domain + ":" + username), Base64.encodeToString(aead.encrypt(password.getBytes(), null), Base64.DEFAULT));
            return Single.just(mutable);
        })).subscribeOn(Schedulers.io());
    }
    
    public Completable setActiveAccountAsync(String domain, String username) {
        return Completable.fromAction(() -> dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(PreferencesKeys.stringKey("active:" + domain), username);
            return Single.just(mutable);
        })).subscribeOn(Schedulers.io());
    }
    
    public Completable removeAccountAsync(String domain, String username) {
        return Completable.fromAction(() -> dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.remove(PreferencesKeys.stringKey(domain + ":" + username));
            return Single.just(mutable);
        })).subscribeOn(Schedulers.io());
    }
    
    public Completable removeActiveAccountAsync(String domain) {
        return Completable.fromAction(() -> dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.remove(PreferencesKeys.stringKey("active:" + domain));
            return Single.just(mutable);
        })).subscribeOn(Schedulers.io());
    }
}
