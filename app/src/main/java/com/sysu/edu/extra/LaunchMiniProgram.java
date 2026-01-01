package com.sysu.edu.extra;

/*import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class LaunchMiniProgram {
    public static final String APP_ID = "wxc6f733eaa4df672a";
    private final IWXAPI api;

    public LaunchMiniProgram(Context context){
        api = WXAPIFactory.createWXAPI(context, APP_ID, true);

        // 将应用的appId注册到微信
        api.registerApp(APP_ID);

        //建议动态监听微信启动广播进行注册到微信
        registerReceiver(context, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                api.registerApp(APP_ID);
            }
        }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP), ContextCompat.RECEIVER_NOT_EXPORTED);
    }
    public void launchMiniProgram(String gh) {
        WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
        req.userName = gh;
        req.miniprogramType = WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE;
        api.sendReq(req);
    }
}*/
