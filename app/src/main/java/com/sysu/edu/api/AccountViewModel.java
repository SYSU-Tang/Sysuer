package com.sysu.edu.api;

import androidx.lifecycle.ViewModel;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class AccountViewModel extends ViewModel {
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    //    private final MutableLiveData<String> domainTrigger = new MutableLiveData<>();
//    private AccountManager accountManager;
//    private final LiveData<Pair<String, String>> activeAccount = Transformations.switchMap(
//            domainTrigger,
//            domain -> {
//                MutableLiveData<Pair<String, String>> result = new MutableLiveData<>();
//                disposables.add(
//                        accountManager.getActiveAccountAsync(domain)
//                                .subscribe(result::setValue,
//                                        _ -> result.setValue(null)
//                                ));
//                return result;
//            }
//    );
//
//    public AccountViewModel(AccountManager accountManager) {
//        this.accountManager = accountManager;
//    }
//
//    public void setDomain(String domain) {
//        if (!domain.equals(domainTrigger.getValue())) domainTrigger.setValue(domain);
//    }
//
//    public LiveData<Pair<String, String>> getActiveAccount() {
//        return activeAccount;
//    }
//
    public void add(Disposable disposable) {
        disposables.add(disposable);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}