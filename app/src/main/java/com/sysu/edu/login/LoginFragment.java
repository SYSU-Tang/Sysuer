package com.sysu.edu.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.sysu.edu.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {
    FragmentLoginBinding binding;
    LoginViewModel model;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        model = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        model.getPassword().observe(getViewLifecycleOwner(), binding.password::setText);
        model.getAccount().observe(getViewLifecycleOwner(), binding.username::setText);
        model.getLogin().observe(getViewLifecycleOwner(),binding.loginButton::setEnabled);
//        model.getLogin().observe(getViewLifecycleOwner(), a -> {
//            if(!a){
////                binding.loginButton.setEnabled(true);
////                Glide.with(requireContext()).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie", Objects.requireNonNull(model.getSessionId().getValue())).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).override(92*3,34*3).into(binding.capImg);
//            }
//        });
//        binding.capImg.setOnClickListener(v -> {
//            binding.loginButton.setEnabled(true);
//            Glide.with(requireContext()).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp", new LazyHeaders.Builder().addHeader("Cookie", Objects.requireNonNull(model.getSessionId().getValue())).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into((ImageView) v);
//        });
        binding.loginButton.setOnClickListener(v -> {
            String username = String.valueOf(binding.username.getText());
            String password = String.valueOf(binding.password.getText());
            if (!username.isEmpty() && !password.isEmpty()) {
                //binding.loginButton.setEnabled(false);
               model.setUrl(String.format("javascript:(function(){var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click()})()", username, password));
            }
        });
        return binding.getRoot();
    }
}
