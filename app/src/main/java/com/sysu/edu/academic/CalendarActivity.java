package com.sysu.edu.academic;

import static com.sysu.edu.api.DownloadManager.openFile;

import android.content.ContentValues;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.tabs.TabLayout;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityCalendarBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CalendarActivity extends BaseActivity {
    
    int top = 0;
    
    public boolean saveImage(String url, String fileName) {
        return saveImage(url, Environment.DIRECTORY_PICTURES + "/SYSUER", fileName, true);
    }
    
    public boolean saveImage(String url, String parentDir, String fileName, boolean defaultDir) {
        try {
            
            Uri fileUri;
            if (defaultDir) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, parentDir);
                fileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                fileUri = Uri.fromFile(new File(parentDir, fileName));
            }
            
            if (fileUri == null) {
                return false;
            }
            
            OutputStream outStream = getContentResolver().openOutputStream(fileUri);
            Glide.with(this)
                    .asFile()
                    .load(url)
                    .into(new CustomTarget<File>() {
                        @Override
                        public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                            if (outStream != null) {
                                try {
                                    FileInputStream fileInputStream = new FileInputStream(resource);
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        FileUtils.copy(fileInputStream, outStream);
                                    } else {
                                        byte[] buffer = new byte[1024 * 4];
                                        int bytesRead;
                                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                            outStream.write(buffer, 0, bytesRead);
                                        }
                                    }
                                    outStream.flush();
                                    outStream.close();
                                    fileInputStream.close();
                                } catch (IOException _) {
                                }
                            }
                        }
                        
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        
                        }
                    });
//            sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", fileUri));
            
            return true;
            
        } catch (IOException _) {
        }
        return false;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCalendarBinding binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        binding.scroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (_, _, scrollY, _, oldScrollY) -> {
            if (top > scrollY && binding.tabs.getSelectedTabPosition() == 1 && scrollY < oldScrollY) {
                Objects.requireNonNull(binding.tabs.getTabAt(0)).select();
            } else if (top <= scrollY && binding.tabs.getSelectedTabPosition() == 0 && scrollY > oldScrollY) {
                Objects.requireNonNull(binding.tabs.getTabAt(1)).select();
            }
        });
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (binding.content.getChildCount() > 2) {
                    top = binding.content.getChildAt(2).getTop();
                }
                switch (binding.tabs.getSelectedTabPosition()) {
                    case 0:
                        if (binding.scroll.getScrollY() >= top) {
                            binding.scroll.smoothScrollTo(0, 0);
                        }
                        break;
                    case 1:
                        if (binding.scroll.getScrollY() <= top) {
                            binding.scroll.smoothScrollTo(0, top);
                        }
                        break;
                }
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            
            }
        });
        Handler handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1:
                        params.toast(R.string.no_net_connected);
                        break;
                    case 1:
                        Matcher matcher = Pattern.compile("(<strong>.+?)(?=<strong>)").matcher(msg.obj + "<strong>");
                        while (matcher.find()) {
                            Matcher m = Pattern.compile("<strong>(.+?)<").matcher(Objects.requireNonNull(matcher.group(1)));
                            if (m.find()) {
                                binding.tabs.addTab(binding.tabs.newTab().setText(m.group(1)));
                            }
                            Matcher n = Pattern.compile("src=\"(.+?)\"").matcher(Objects.requireNonNull(matcher.group(1)));
                            while (n.find()) {
                                ImageView image = new ImageView(CalendarActivity.this);
                                String url = "https://jwb.sysu.edu.cn/" + n.group(1);
                                Glide.with(CalendarActivity.this).load(url).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(image);
                                image.setOnLongClickListener(_ -> {
                                    PopupMenu pop = new PopupMenu(CalendarActivity.this, image, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                                    Menu menu = pop.getMenu();
                                    menu.add(R.string.save).setOnMenuItemClickListener(_ -> {
                                        params.toast(saveImage(url, System.currentTimeMillis() + ".jpg") ? R.string.save_successful : R.string.save_fail);
                                        return true;
                                    });
                                    menu.add(R.string.share).setOnMenuItemClickListener(_ -> {
                                        String fileName = System.currentTimeMillis() + ".jpg";
                                        saveImage(url, Objects.requireNonNull(getExternalCacheDir()).getPath(), fileName, false);
                                        openFile(CalendarActivity.this, getExternalCacheDir().getPath() + "/" + fileName);
//                                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(CalendarActivity.this, "com.sysu.edu.fileProvider", new File(Objects.requireNonNull(getExternalCacheDir()).getPath() + "/" + fileName))), getString(R.string.share)));
                                        return true;
                                    });
                                    pop.show();
                                    return true;
                                });
                                binding.content.addView(image);
                            }
                            binding.progressBar.setVisibility(View.GONE);
                        }
                        break;
                }
            }
        };
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        okHttpClient.newCall(new Request.Builder().url("https://jwb.sysu.edu.cn/school-calendar").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handler.sendEmptyMessage(-1);
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                Matcher matcher = Pattern.compile("block-region-left.+?>([\\s\\S]+?)<.+?block-region-left-below").matcher(response.body().string());//
                if (matcher.find()) {
                    msg.obj = Pattern.compile("</?(?!img|strong).+?>|\\s+").matcher(Objects.requireNonNull(matcher.group(1))).replaceAll("");
                    handler.sendMessage(msg);
                }
            }
        });
    }
}