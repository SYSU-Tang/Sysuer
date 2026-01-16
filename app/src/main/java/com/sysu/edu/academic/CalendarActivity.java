package com.sysu.edu.academic;

import android.content.ContentValues;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.tabs.TabLayout;
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

public class CalendarActivity extends AppCompatActivity {

    Handler handler;
    int top = 0;
    Params params;

    /* public static boolean save(ImageView view) {
         try {

             ContentValues values = new ContentValues();
             values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

             Uri fileUri = view.getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

             if (fileUri == null) {
                 return false;
             }

             OutputStream outStream = view.getContext().getContentResolver().openOutputStream(fileUri);

             Bitmap bitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
             if (outStream != null) {
                 bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                 outStream.flush();
                 outStream.close();
             }
             view.getContext().sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", fileUri));

             return true;

         } catch (IOException ignored) {
         }
         return false;
     }*/
    public boolean save(String url, String fileName) {
        return save(url, Environment.DIRECTORY_PICTURES + "/SYSUER", fileName, true);
    }

    public boolean save(String url, String parentDir, String fileName, boolean defaultDir) {
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
                                } catch (IOException ignored) {
                                }
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
//            sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", fileUri));

            return true;

        } catch (IOException ignored) {
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCalendarBinding binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        binding.toolbar.setNavigationOnClickListener(view -> finishAfterTransition());
        binding.scroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
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
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1:
                        params.toast(R.string.no_wifi_warning);
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
                                image.setOnLongClickListener(view -> {
                                    PopupMenu pop = new PopupMenu(CalendarActivity.this, image, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                                    Menu menu = pop.getMenu();
                                    menu.add(R.string.save).setOnMenuItemClickListener(item -> {
                                        params.toast(save(url, System.currentTimeMillis() + ".jpg") ? R.string.save_successfully : R.string.save_fail);
                                        return true;
                                    });
                                    menu.add(R.string.share).setOnMenuItemClickListener(item -> {
                                        String fileName = System.currentTimeMillis() + ".jpg";
                                        save(url, Objects.requireNonNull(getExternalCacheDir()).getPath(), fileName, false);
                                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(CalendarActivity.this, "com.sysu.edu.fileProvider", new File(Objects.requireNonNull(getExternalCacheDir()).getPath() + "/" + fileName))), getString(R.string.share)));
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
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwb.sysu.edu.cn/school-calendar").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
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