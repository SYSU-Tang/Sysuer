package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Config;
import com.sysu.edu.databinding.FragmentComplaintMainBinding;
import com.sysu.edu.databinding.ItemTodoBinding;
import com.sysu.edu.view.RecyclerAdapter;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

public class ComplaintMainFragment extends Fragment {
    
    HttpManager http;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentComplaintMainBinding binding = FragmentComplaintMainBinding.inflate(inflater, container, false);
        binding.captchaImage.setOnClickListener(_ -> loadCaptcha(binding.captchaImage));
        ActivityResultLauncher<Intent> fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Uri uri = null;
                if (result.getData() != null) uri = result.getData().getData();
                if (uri != null)
                    uploadAttachment(uri);
            }
        });
        binding.uploadAttachment.setOnClickListener(_ -> pickAttachment(fileLauncher));
        loadCaptcha(binding.captchaImage);
        Config config = new Config(this);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1 -> config.toast(R.string.no_net_connected);
                    case 0, 1 -> System.out.println(msg.obj);
                }
            }
        });
        http.setParams(config);
        return binding.getRoot();
    }
    
    /*
     * 上传附件
     * */
    void pickAttachment(ActivityResultLauncher<? super Intent> fileLauncher) {
        fileLauncher.launch(new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"));
    }
    
    void uploadAttachment(Uri uri) {
        ContentResolver resolver = requireContext().getContentResolver();
        String type = resolver.getType(uri);
        try {
            InputStream inputStream = resolver.openInputStream(uri);
            RequestBody requestBody = new RequestBody() {
                @Override
                public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {
                    if (inputStream != null) bufferedSink.writeAll(Okio.source(inputStream));
                }
                
                @Nullable
                @Override
                public MediaType contentType() {
                    return type != null ? MediaType.parse(type) : null;
                }
            };
            if (inputStream != null) inputStream.close();
            http.sendRequest(http.generateRequest("https://xinfang.sysu.edu.cn/jsp_api/upload", null, null).post(new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", uri.getLastPathSegment(), requestBody).build()).build(), 1);
            
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
//        if (cursor == null) {
//            if (uri.getPath() != null)
//                file = new File(uri.getPath());
//        } else {
//            cursor.moveToFirst();
//            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
//            if (nameIndex != -1) {
//                String name = cursor.getString(nameIndex);
//            }
//            System.out.println(cursor.getString(0));
//            file = new File(cursor.getString(0));
//            cursor.close();
//        }
//        if (file != null) {
//            System.out.println(file);
//        }
//        if (file != null && type != null)
    }
    
    
    void loadCaptcha(ShapeableImageView imageView) {
        Glide.with(requireContext()).load(Uri.parse("https://xinfang.sysu.edu.cn/servlet/checkcode")).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).override(300, 120).into(imageView);
    }
    
    static class FileAdapter extends RecyclerAdapter<JSONObject> {
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false)) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            JSONObject item = get(position);
            ItemTodoBinding binding = ItemTodoBinding.bind(holder.itemView);
            binding.title.setText(toStringOrDefault(item.getString("title")));
            super.onBindViewHolder(holder, position);
        }
    }
}