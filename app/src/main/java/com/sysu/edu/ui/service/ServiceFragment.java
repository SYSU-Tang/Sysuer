import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import com.sysu.edu.R;
import com.sysu.edu.activity.AgendaActivity;
import com.sysu.edu.activity.BrowseActivity;
import com.sysu.edu.activity.ClassroomQueryActivity;
import com.sysu.edu.activity.Evaluation;

public class ServiceFragment extends Fragment {
    LinearLayout service_container;
    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment=inflater.inflate(R.layout.fragment_service,container,false);
        service_container=fragment.findViewById(R.id.service_container);
        String[] titles = new String[]{"系统", "官网", "官媒","教务","学习", "出行", "宿舍","查询"};
        String[][] items = new String[][]{{"学工系统","教务系统","中山大学统一门户","大学服务中心","财务信息系统"},
                {"中山大学官网","本科招生","研究生招生","人才招聘","百年校庆","公务电子邮件系统","博物馆","图书馆","校友会"},
                {"逸仙码","企业微信","中大招生","wps教育版"},
                {"评教","选课","课程表","考试","校历","自习室/研讨室","成绩","课程详情"},
                {"SeeLight","雨课堂","课堂派","在线教学平台","中国大学（慕课）"},
                {"校园地图","校车","出行证","校医院"},
                {"宿舍报修","缴纳水电费"},
                {"空教室查询"}
        };
        View.OnClickListener[][] actions = new View.OnClickListener[][]{
                {
                        browse("https://xgxt-443.webvpn.sysu.edu.cn/main/#/index"),
                        browse("https://jwxt.sysu.edu.cn/jwxt/#/student"),
                        browse("https://portal.sysu.edu.cn/newClient/#/newPortal/index"),
                        browse("https://usc.sysu.edu.cn/taskcenter-v4/workflow/index"),
                        browse("https://cwxt-443.webvpn.sysu.edu.cn/#/home/index"),
                },//系统
                {
                        browse("https://www.sysu.edu.cn/"),
                        browse("https://admission.sysu.edu.cn/"),
                        browse("https://graduate.sysu.edu.cn/zsw/"),
                        browse("https://rcb.sysu.edu.cn/"),
                        browse("https://sysu100.sysu.edu.cn/"),
                        browse("https://mail.sysu.edu.cn/"),
                        browse("https://bwgxsg.sysu.edu.cn/"),
                        browse("https://library.sysu.edu.cn/"),
                        browse("https://alumni.sysu.edu.cn/"),
                },//官网
                {

                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                if(v.getContext().getPackageManager().getPackageInfo("com.tencent.wework",0)!= null) {
                                    startActivity(new Intent(Intent.ACTION_MAIN).setPackage("com.tencent.wework"));
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                Toast.makeText(v.getContext(),"未安装企业微信",Toast.LENGTH_SHORT).show();
                                throw new RuntimeException(e);
                            }
                        }
                    },
                },//官媒
                {   newActivity(Evaluation.class),
null,
                    newActivity(AgendaActivity.class),
                        null,
                        null,
                        newActivity(ClassroomQueryActivity.class),
                },//教务
                {

                },//学习
                {

                },//出行
                {

                },//宿舍
                {

                },//查询
        };
        for (int i=0;i<titles.length;i++){
            initBox(inflater,titles[i],items[i],actions[i]);
        }
        return fragment;
    }
    public void initBox(LayoutInflater inflater, String box_title, String[] items, View.OnClickListener[] actions)
    {
        LinearLayout box= (LinearLayout) inflater.inflate(R.layout.service_box,service_container,false);
        TextView title=box.findViewById(R.id.service_box_title);
        RelativeLayout items_container=box.findViewById(R.id.service_box_items);
        title.setText(box_title);
        title.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                   }

    });
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        int length=36;
        for(int i=0;i<items.length;i++){
            MaterialButton item = (MaterialButton) inflater.inflate(R.layout.service_item,items_container,false);
            item.setOnClickListener(
                       (i<actions.length&&actions[i]!=null)?actions[i]:new View.OnClickListener() {
                           @Override
                           public void onClick(View v) {
                               Toast.makeText(v.getContext(),"未开发",Toast.LENGTH_LONG).show();
                           }
                       }
                );
            item.setId(i+1);
            item.setText(items[i]);
            item.measure(View.MEASURED_SIZE_MASK,View.MEASURED_SIZE_MASK);
            RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) item.getLayoutParams();
            length+=item.getMeasuredWidth()+24;
            if (length-12<metrics.widthPixels)
            {
                lp.addRule(RelativeLayout.RIGHT_OF,i);
                lp.addRule(RelativeLayout.ALIGN_TOP,i);
            }else{
                lp.topMargin=12;
                lp.bottomMargin=12;
                lp.addRule(RelativeLayout.BELOW,i);
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                length=item.getMeasuredWidth()+36;
            }
            items_container.addView(item);
        }
        service_container.addView(box);
    }
    public View.OnClickListener browse(String url){
        return new View.OnClickListener(){
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), BrowseActivity.class).setData(Uri.parse(url)));
            }
        };
    }
    public View.OnClickListener newActivity(Class activity_class){
        return new View.OnClickListener(){
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(),activity_class));
            }
        };
    }
}

