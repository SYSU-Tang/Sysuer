package com.sysu.edu.browser;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;
import static com.sysu.edu.api.CommonUtil.trim;
import static com.sysu.edu.api.DownloadManager.downloadFile;
import static com.sysu.edu.api.DownloadManager.openFile;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.api.DownloadManager;
import com.sysu.edu.api.Config;
import com.sysu.edu.databinding.ActivityBrowserBinding;
import com.sysu.edu.databinding.DialogJsBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.EditTextDialog;
import com.sysu.edu.view.GridDialog;
import com.sysu.edu.view.RecyclerAdapter;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BrowserActivity extends BaseActivity {
    final MutableLiveData<Integer> progress = new MutableLiveData<>();
    final CompositeDisposable disposable = new CompositeDisposable();
    WebView web;
    ActivityBrowserBinding binding;
    WebSettings webSettings;
    CookieManager cookie;
    MaterialButton refreshButton;
    BrowserHelper db;
    JavaScript js;
    Config config;
    
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = new BrowserHelper(this);
        cookie = CookieManager.getInstance();
        binding.toolbar.setNavigationOnClickListener(_ -> finishAfterTransition());
        config = new Config(this);
        BrowserPreference preference = new BrowserPreference(this);
        String url = getIntent().getDataString() != null ? getIntent().getDataString() : "https://www.sysu.edu.cn/";
        js = new JavaScript();
        getJSList();
        Cursor cursor;
        web = binding.web;
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
                if (url1.startsWith("https://") || url1.startsWith("http://")) view.loadUrl(url1);
                else
                    startActivity(new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setData(Uri.parse(url1)));
                return true;
            }
            
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/system-manage/infoRelease/downloadFile", Pattern.DOTALL).matcher(url1).find()) {
                    try {
                        Response response = new OkHttpClient().newCall(new Request.Builder().url(url1)
                                .header("Cookie", cookie.getCookie(url1))
                                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/")
                                .build()).execute();
                        MediaType mediaType = response.body().contentType();
                        return new WebResourceResponse(mediaType == null ? "application/octet-stream" : mediaType.type(), "utf-8", response.body().byteStream());
                    } catch (IOException _) {
                    
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
            
            @Override
            public void onPageFinished(WebView view, String link) {
                if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(link).find())
                    disposable.add(config.getContextUtil().getAccountManager().getActiveAccountAsync("sysu.edu.cn").subscribe(account ->
                    {
                        if (!isEmpty(account.first) && !isEmpty(account.second))
                            web.evaluateJavascript(String.format("""
                                    javascript:(function(){\
                                    function waitElement(selector, callback) {\
                                    const element = document.querySelector(selector);\
                                    if (element) {callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}\
                                    waitElement('.para-widget-account-psw', () => {\
                                    var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()""", account.first, account.second), _ -> {
                            });
                    }));
                else if (Pattern.compile("://appgw.sysu.edu.cn/").matcher(link).find()) {
                    web.stopLoading();
                    web.loadUrl(url.replace(".sysu.edu.cn/", "-443.webvpn.sysu.edu.cn/"));
                } else if (Pattern.compile("://cas.*?sysu.edu.cn/login/mfaLogin.html").matcher(link).find()) {
                    cookie.setCookie("https://cas.sysu.edu.cn", "device_trust_Cookie=true; Path=/esc-sso; Domain=cas.sysu.edu.cn;");
                    web.loadUrl(toStringOrDefault(Uri.parse(URLDecoder.decode(link, StandardCharsets.UTF_8)).getQueryParameter("appUrl")));
                } else if (preference.isPC())
                    view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
                js.searchJS(link, true).forEach(a -> {
                    System.out.println(a);
                    view.evaluateJavascript(a.getString("script"), null);
                });
                super.onPageFinished(view, link);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(BrowserActivity.this).setMessage(message).setPositiveButton(R.string.confirm, (_, _) -> result.confirm()).create().show();
                return true;
            }
            
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(BrowserActivity.this).setMessage(message).setPositiveButton(R.string.confirm, (_, _) -> result.confirm()).create().show();
                return true;
            }
            
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(BrowserActivity.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        web.loadUrl(String.valueOf(request.getUrl()));
                        newWebView.destroy();
                        return super.shouldOverrideUrlLoading(view, request);
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
            
            @Override
            public void onReceivedTitle(WebView view, String title) {
                binding.toolbar.setTitle(title);
                binding.toolbar.setSubtitle(view.getUrl());
                super.onReceivedTitle(view, title);
            }
            
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                binding.toolbar.setLogo(new BitmapDrawable(getResources(), icon));
                binding.toolbar.setLogoAdjustViewBounds(true);
                binding.toolbar.setLogoScaleType(ImageView.ScaleType.FIT_CENTER);
                super.onReceivedIcon(view, icon);
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progress.postValue(newProgress);
            }
        });
        
        /*
         * 下载弹窗
         * */
        
        GridDialog downloadDialog = new GridDialog(this);
        downloadDialog.setColumn(1);
        downloadDialog.loadMenu(List.of(R.string.link, R.string.location), List.of(R.drawable.link, R.drawable.save), List.of(
                (_) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadDialog.getMenu(0).getText().toString())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                _ -> downloadDialog.getDialog().dismiss()
        ), Integer.class);
        downloadDialog.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        downloadDialog.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        downloadDialog.getMenu(0).setMaxLines(Integer.MAX_VALUE);
        downloadDialog.getMenu(0).setOnLongClickListener(_ -> {
            config.copy("link", downloadDialog.getMenu(0).getText().toString());
            config.toast(R.string.copy_successfully);
            return true;
        });
        downloadDialog.setNegativeButton(R.string.cancel, (_, _) -> {
        });
        
        web.setDownloadListener((url1, _, _, _, _) -> {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + getFileName(url1);
            downloadDialog.getMenu(0).setText(url1);
            downloadDialog.getMenu(1).setText(path);
            downloadDialog.setPositiveButton(R.string.download, (_, _) -> {
                if (url1.contains("jwxt.sysu.edu.cn"))
                    downloadFile(this, new Request.Builder()
                            .url(url1)
                            .header("Cookie", cookie.getCookie(url1))
                            .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/").build(), path);
                else downloadFile(this, url1, path);
            });
            downloadDialog.show();
        });
        webSettings = web.getSettings();
        webSettings.supportZoom();
        webSettings.setJavaScriptEnabled(preference.isJSEnabled());
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(preference.isSaveMobileDataMode() ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(preference.isImageBlocked());
        webSettings.setDefaultTextEncodingName("utf-8");
        setPrivacyMode(preference.isPrivacyMode());
        cookie.setAcceptCookie(preference.isCookieAccept());
        cookie.setAcceptThirdPartyCookies(web, preference.isThirdPartyCookieAccept());
        
        /*
         * 长按菜单
         * */
        final View anchorView = new View(this);
        anchorView.setLayoutParams(new FrameLayout.LayoutParams(1, 1));
        ((FrameLayout) web.getParent()).addView(anchorView);
        GestureDetector gesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
//                int[] webViewLocation = new int[2];
//                web.getLocationOnScreen(webViewLocation);
//                System.out.println("webViewLocation:" + Arrays.toString(webViewLocation));
                anchorView.setX(/*webViewLocation[0] +*/ (int) e.getX());
                anchorView.setY(/*webViewLocation[1] + */(int) e.getY());
                PopupMenu pop = new PopupMenu(BrowserActivity.this, anchorView);
                WebView.HitTestResult result = web.getHitTestResult();
                int type = result.getType();
                String extra = result.getExtra();
                switch (type) {
                    case WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                        if (!isEmpty(extra)) showLinkMenu(extra, pop);
                    }
                    case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                        if (!isEmpty(extra)) showImageMenu(extra, pop);
                    }
                }
            }
        });
        web.setOnTouchListener((_, event) -> {
            gesture.onTouchEvent(event);
            return false;
        });
        /*
         * 脚本弹窗
         * */
        BottomSheetDialog jsDialog = new BottomSheetDialog(this);
        DialogJsBinding JSBinding = DialogJsBinding.inflate(getLayoutInflater());
        jsDialog.setContentView(JSBinding.getRoot());
        jsDialog.setTitle(R.string.js);
        JSAdapter jsAdapter = new JSAdapter();
        
        jsAdapter.setListener(new AdapterListener() {
            @Override
            public void onBind(@NonNull RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, @NonNull RecyclerView.ViewHolder holder, int position) {
                JSONObject item = jsAdapter.get(position);
                holder.itemView.setOnClickListener(_ -> web.evaluateJavascript(item.getString("script"), null));
                holder.itemView.setOnLongClickListener(v -> {
                    PopupMenu pop = new PopupMenu(BrowserActivity.this, v);
                    pop.getMenuInflater().inflate(R.menu.js_item_menu, pop.getMenu());
                    pop.getMenu().add(0, R.id.run, 0, R.string.run);
                    pop.show();
                    pop.getMenu().findItem(R.id.ban).setTitle(item.getInteger("state") == 1 ? R.string.disable : R.string.enable);
                    pop.setOnMenuItemClickListener(menuItem -> {
                        int itemId = menuItem.getItemId();
                        if (itemId == R.id.edit) {
                            v.setTransitionName("script");
                            Bundle bundle = new Bundle();
                            bundle.putString("item", item.toString());
                            startActivity(new Intent(BrowserActivity.this, JSActivity.class).putExtras(bundle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            return true;
                        } else if (itemId == R.id.delete) {
                            db.getWritableDatabase().delete("js", "id=?", new String[]{String.valueOf(item.getLong("id"))});
                            jsAdapter.remove(position);
                            return true;
                        } else if (itemId == R.id.ban) {
                            ContentValues value = new ContentValues();
                            int state = 1 - item.getInteger("state");
                            value.put("state", state);
                            db.getWritableDatabase().update("js", value, "id=?", new String[]{String.valueOf(item.getLong("id"))});
                            item.fluentPut("state", state);
                            jsAdapter.notifyItemChanged(position);
                            return true;
                        } else if (itemId == R.id.run) {
                            v.performClick();
                            return true;
                        }
                        return false;
                    });
                    return false;
                });
            }
            
            @Override
            public void onCreate(@NonNull RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            }
        });
        
        RecyclerView jsList = JSBinding.recyclerView.getRoot();
        jsList.setLayoutManager(new LinearLayoutManager(this));
        jsList.setAdapter(jsAdapter);
        JSBinding.manage.setOnClickListener(v -> startActivity(new Intent(this, JSActivity.class),
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "miniapp").toBundle()));
        JSBinding.add.setOnClickListener(v -> startActivity(new Intent(this, JSActivity.class).putExtra("operation", "add"),
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "miniapp").toBundle()));
        
        /*
         * 菜单弹窗
         * */
        GridDialog menuDialog = new GridDialog(this);
        menuDialog.loadMenu(List.of(R.string.back, R.string.forward, R.string.refresh, R.string.exit, R.string.page_up, R.string.page_down, R.string.zoom_in, R.string.zoom_out, R.string.find_text),
                List.of(R.drawable.left, R.drawable.right, R.drawable.refresh, R.drawable.exit, R.drawable.up, R.drawable.down, R.drawable.zoom_in, R.drawable.zoom_out, R.drawable.search),
                List.of(_ -> goBack(), _ -> goForward(), _ -> refresh(), _ -> supportFinishAfterTransition(), _ -> pageUp(), _ -> pageDown()
                        , _ -> web.zoomIn(), _ -> web.zoomOut(), _ -> {
                            binding.searchContainer.setVisibility(View.VISIBLE);
                            web.findAllAsync(binding.keyword.getText().toString());
                            menuDialog.dismiss();
                        }), Integer.class);
        refreshButton = menuDialog.getMenu(2);
        
        /*
         * UA 弹窗
         * */
        GridDialog uaDialog = new GridDialog(this);
        uaDialog.setColumn(2);
        uaDialog.setSelectable(true);
        cursor = db.getReadableDatabase().query("ua", null, null, null, null, null, null);
        ArrayList<String> uaNames = new ArrayList<>();
        ArrayList<Integer> uaIcons = new ArrayList<>();
        ArrayList<Consumer<MaterialButton>> uaAction = new ArrayList<>();
        uaNames.add(getString(R.string.follow_system));
        uaIcons.add(R.drawable.setting);
        uaAction.add(_ -> {
            webSettings.setUserAgentString(WebSettings.getDefaultUserAgent(this));
            preference.setUA(-1);
            web.reload();
        });
        if (cursor.moveToFirst()) {
            do {
                uaNames.add(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                int uaId = cursor.getInt(cursor.getColumnIndexOrThrow("uaId"));
                uaIcons.add(List.of(R.drawable.laptop, R.drawable.laptop, R.drawable.laptop, R.drawable.mac, R.drawable.android, R.drawable.tablet, R.drawable.iphone, R.drawable.ipad, R.drawable.ua, R.drawable.laptop, R.drawable.laptop, R.drawable.android).get(uaId));
                String ua = cursor.getString(cursor.getColumnIndexOrThrow("ua"));
                uaAction.add(_ -> {
                    webSettings.setUserAgentString(ua);
                    preference.setUA(uaId);
                    web.reload();
                });
            } while (cursor.moveToNext());
        }
        cursor.close();
        uaDialog.loadMenu(uaNames, uaIcons, uaAction, String.class);
        uaDialog.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        uaDialog.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        uaDialog.selectMenu(preference.getUA() + 1);
        uaAction.get(preference.getUA() + 1).accept(null);
        /*
         * 主题弹窗
         * */
        GridDialog themeDialog = new GridDialog(this);
        themeDialog.setColumn(1);
        themeDialog.setSelectable(true);
        List<Integer> themeTitle = List.of(R.string.follow_system, R.string.dark_mode, R.string.light_mode);
        List<Integer> themeIcon = List.of(R.drawable.setting, R.drawable.dark, R.drawable.light);
        List<Consumer<MaterialButton>> themeAction = List.of(_ -> preference.setTheme(0), _ -> preference.setTheme(1), _ -> preference.setTheme(2));
        themeDialog.loadMenu(themeTitle, themeIcon, themeAction, Integer.class);
        themeDialog.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        themeDialog.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        themeDialog.selectMenu(preference.getTheme());
        
        /*
         * Cookie 弹窗
         * */
        GridDialog cookieModeDialog = new GridDialog(this);
        cookieModeDialog.setColumn(1);
        cookieModeDialog.setMultipleSelectable(true);
        cookieModeDialog.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        cookieModeDialog.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        cookieModeDialog.loadMenu(
                List.of(R.string.cookie, R.string.third_party_cookie),
                List.of(R.drawable.cookie, R.drawable.cookie),
                List.of(_ -> {
                    boolean accept = !preference.isCookieAccept();
                    preference.setCookieAccept(accept);
                    cookie.setAcceptCookie(accept);
                }, _ -> {
                    boolean accept = !preference.isThirdPartyCookieAccept();
                    preference.setThirdPartyCookieAccept(accept);
                    cookie.setAcceptThirdPartyCookies(web, accept);
                }),
                Integer.class
        );
        cookieModeDialog.toggleMenu(0, preference.isCookieAccept());
        cookieModeDialog.toggleMenu(1, preference.isThirdPartyCookieAccept());
        
        /*
         * 网页弹窗
         * */
        GridDialog browserDialog = new GridDialog(this);
        List<Integer> webTitle = List.of(R.string.ua, preference.isPC() ? R.string.pc_mode : R.string.mobile_mode, preference.isImageBlocked() ? R.string.image_blocked : R.string.image, R.string.javascript, R.string.save_mobile_data_mode, R.string.theme, R.string.privacy_mode, R.string.cookie);
        List<Integer> webIcon = List.of(R.drawable.ua, preference.isPC() ? R.drawable.laptop : R.drawable.phone, preference.isImageBlocked() ? R.drawable.image_block : R.drawable.image, R.drawable.js, R.drawable.wifi, R.drawable.light, R.drawable.privacy, R.drawable.cookie);
        List<Consumer<MaterialButton>> webAction = List.of(_ -> uaDialog.show(), v -> {
                    boolean pc = !preference.isPC();
                    preference.setPC(pc);
                    v.setText(pc ? R.string.pc_mode : R.string.mobile_mode);
                    v.setIconResource(pc ? R.drawable.laptop : R.drawable.phone);
                    web.reload();
                },
                v -> {
                    boolean imageBlocked = !preference.isImageBlocked();
                    preference.setImageBlocked(imageBlocked);
                    v.setText(imageBlocked ? R.string.image_blocked : R.string.image);
                    v.setIconResource(imageBlocked ? R.drawable.image_block : R.drawable.image);
                    webSettings.setBlockNetworkImage(imageBlocked);
                },
                _ -> {
                    boolean jsEnabled = !preference.isJSEnabled();
                    preference.setJSEnabled(jsEnabled);
                    webSettings.setJavaScriptEnabled(jsEnabled);
                }, v -> {
                    boolean saveMobileDataMode = !preference.isSaveMobileDataMode();
                    preference.setSaveMobileDataMode(saveMobileDataMode);
                    v.setIconResource(saveMobileDataMode ? R.drawable.no_wifi : R.drawable.wifi);
                    webSettings.setCacheMode(saveMobileDataMode ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
                }, _ -> {
                    themeDialog.show();
//                    String css = """
//                            body { background-color: #121212 !important; color: #e0e0e0 !important; }\
//                            a { color: #80cbc4 !important; }\
//                            img { filter: brightness(0.8) contrast(1.2); }""";
//                    web.evaluateJavascript("var style = document.createElement('style'); style.innerHTML = '" + css + "'; document.head.appendChild(style);", null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        webSettings.setForceDark(WebSettings.FORCE_DARK_ON);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        webSettings.setAlgorithmicDarkeningAllowed(true);
                }, _ -> {
                    boolean privacyMode = !preference.isPrivacyMode();
                    setPrivacyMode(privacyMode);
                    preference.setPrivacyMode(privacyMode);
                }, _ -> cookieModeDialog.show());
        browserDialog.loadMenu(webTitle, webIcon, webAction, Integer.class);
        browserDialog.setTogglable(new int[]{3, 4, 6}, true);
        browserDialog.setColumn(4);
        browserDialog.toggleMenu(3, preference.isJSEnabled());
        browserDialog.toggleMenu(4, preference.isSaveMobileDataMode());
        browserDialog.toggleMenu(6, preference.isPrivacyMode());
        
        /*
         * Cookie 弹窗
         * */
        var cookieDialog = new EditTextDialog(this);
        cookieDialog.setTitle(R.string.cookie);
        
        /*
         * 网站弹窗
         * */
        var websiteDialog = new GridDialog(this);
        websiteDialog.loadMenu(List.of(R.string.copy, R.string.share, R.string.open_in_browser, R.string.cookie, R.string.webpage_source),
                List.of(R.drawable.copy, R.drawable.share, R.drawable.export, R.drawable.cookie, R.drawable.version),
                List.of(_ -> config.copy("url:", web.getUrl()),
                        _ -> startActivity(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, trim(web.getUrl()))),
                        _ -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(trim(web.getUrl())))),
                        _ -> {
                            String targetUrl = trim(web.getUrl());
                            cookieDialog.setValue(cookie.getCookie(targetUrl));
                            cookieDialog.getDialog().setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.save), (_, _) -> cookie.setCookie(targetUrl, cookieDialog.getText()));
                            cookieDialog.getDialog().setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), (_, _) -> web.evaluateJavascript(BrowserCookieManager.clearAllCookies, null));
                            cookieDialog.getDialog().setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.copy), (_, _) -> config.copy("Cookie:", cookieDialog.getText()));
                            cookieDialog.show();
                        },
                        _ -> web.loadUrl("view-source:" + web.getUrl())), Integer.class);
        websiteDialog.setColumn(4);
        binding.js.setOnClickListener(_ -> {
            jsAdapter.set(js.searchJS(web.getUrl()));
            jsDialog.show();
        });
        binding.menu.setOnClickListener(_ -> menuDialog.show());
        binding.browser.setOnClickListener(_ -> browserDialog.show());
        binding.website.setOnClickListener(_ -> websiteDialog.show());
        binding.close.setOnClickListener(_ -> {
            binding.searchContainer.setVisibility(View.GONE);
            web.clearMatches();
        });
        binding.keyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                web.findAllAsync(s.toString());
            }
        });
        binding.next.setOnClickListener(_ -> web.findNext(true));
        binding.last.setOnClickListener(_ -> web.findNext(false));
        binding.number.setOnClickListener(_ -> web.findAllAsync(binding.keyword.getText().toString()));
        web.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting)
                binding.number.setText(String.format("%s/%s", activeMatchOrdinal == 0 ? 0 : activeMatchOrdinal + 1, numberOfMatches));
        });
        
        progress.observe(this, p -> {
            refreshButton.setIconResource(p == 100 ? R.drawable.refresh : R.drawable.close);
            refreshButton.setText(p == 100 ? R.string.refresh : R.string.stop);
        });
        binding.back.setOnClickListener(_ -> {
            if (web.canGoBack()) web.goBack();
        });
        binding.forward.setOnClickListener(_ -> {
            if (web.canGoForward()) web.goForward();
        });
        if (getIntent().hasExtra("data") && getIntent().getStringExtra("data") != null) {
            webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36");
            web.loadDataWithBaseURL("https://jwxt.sysu.edu.cn", Objects.requireNonNull(getIntent().getStringExtra("data")), "text/html", "utf-8", "https://jwxt.sysu.edu.cn");
        } else web.loadUrl(url);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (progress.getValue() != null && progress.getValue() != 100) web.stopLoading();
                else if (web.canGoBack()) web.goBack();
                else supportFinishAfterTransition();
            }
        });
    }
    
    private void getJSList() {
        Cursor cursor = db.getReadableDatabase().query("js", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                JSONObject item = (new JSONObject().fluentPut("title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                        .fluentPut("description", cursor.getString(cursor.getColumnIndexOrThrow("description")))
                        .fluentPut("matches", JSONArray.parse(cursor.getString(cursor.getColumnIndexOrThrow("matches"))))
                        .fluentPut("state", cursor.getInt(cursor.getColumnIndexOrThrow("state")))
                        .fluentPut("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")))
                        .fluentPut("run", cursor.getInt(cursor.getColumnIndexOrThrow("run")))
                        .fluentPut("script", cursor.getString(cursor.getColumnIndexOrThrow("script"))));
                js.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
    
    void setPrivacyMode(boolean enabled) {
//        webSettings.setSaveFormData(!enabled);
        webSettings.setDomStorageEnabled(!enabled);
        webSettings.setDatabaseEnabled(!enabled);
        webSettings.setAllowFileAccess(!enabled);
        webSettings.setAllowContentAccess(!enabled);
    }
    
    protected void onDestroy() {
        disposable.dispose();
        if (web != null) {
            web.stopLoading();
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
    
    void goBack() {
        if (web.canGoBack()) web.goBack();
    }
    
    void goForward() {
        if (web.canGoForward()) web.goForward();
    }
    
    void refresh() {
        if (progress.getValue() != null && progress.getValue() == 100) web.reload();
        else web.stopLoading();
    }
    
    void pageUp() {
        web.pageUp(true);
    }
    
    void pageDown() {
        web.pageDown(true);
    }
    
    
    private void showLinkMenu(final String url, PopupMenu popup) {
        popup.getMenu().add(R.string.open_in_browser).setOnMenuItemClickListener(_ -> {
            web.loadUrl(url);
            return true;
        });
        popup.getMenu().add(R.string.copy).setOnMenuItemClickListener(_ -> {
            config.copy("link", url);
            return true;
        });
        popup.getMenu().add(R.string.share).setOnMenuItemClickListener(_ -> {
            shareText(url);
            return true;
        });
        popup.show();
    }
    
    private void showImageMenu(final String imageUrl, PopupMenu popup) {
        popup.getMenu().add(R.string.download).setOnMenuItemClickListener(_ -> {
//            System.out.println(imageUrl);
//            System.out.println(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + getString(R.string.app_name) + "/" + getFileName(imageUrl));
            downloadFile(this, imageUrl, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + getString(R.string.app_name) + "/" + getFileName(imageUrl), new DownloadManager.DownloadListener() {
                @Override
                public void onDownloadProgress(long progress, long total) {
                    System.out.println(progress + " " + total);
                }
                
                @Override
                public void onDownloadComplete(String path) {
                    Snackbar.make(web, String.format("下载完成，保存到：%s", path), Snackbar.LENGTH_LONG).setAction(R.string.open, _ -> openFile(BrowserActivity.this, path)).show();
                }
                
                @Override
                public void onDownloadError(int code, String message) {
                    System.out.println(code + " " + message);
                }
            });
            return true;
        });
        popup.getMenu().add(R.string.open_in_browser).setOnMenuItemClickListener(_ -> {
            web.loadUrl(imageUrl);
            return true;
        });
        popup.getMenu().add(R.string.copy).setOnMenuItemClickListener(_ -> {
            config.copy("image", imageUrl);
            return true;
        });
        popup.getMenu().add(R.string.share).setOnMenuItemClickListener(_ -> {
            shareText(imageUrl);
            return true;
        });
        popup.show();
    }
    
    private void shareText(String text) {
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), getString(R.string.share)));
    }
    
    String getFileName(String url) {
        String path = URLDecoder.decode(URI.create(url).getPath(), StandardCharsets.UTF_8);
        int i = path.lastIndexOf("/");
        return i >= 0 ? path.substring(i + 1) : path;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        js.clear();
        getJSList();
    }
    
    static class JSAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preference, parent, false)) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
            JSONObject item = get(position);
            binding.itemTitle.setText(item.getString("title"));
            binding.itemContent.setText(item.getString("description"));
            binding.itemIcon.setImageResource(R.drawable.js);
            binding.getRoot().updateAppearance(position, getItemCount());
            super.onBindViewHolder(holder, position);
        }
    }
}