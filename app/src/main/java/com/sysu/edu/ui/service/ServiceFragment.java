package com.sysu.edu.ui.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.BrowseActivity;
import com.sysu.edu.academic.CalendarActivity;
import com.sysu.edu.academic.ClassroomQueryActivity;
import com.sysu.edu.academic.Grade;
import com.sysu.edu.academic.TrainingSchedule;
import com.sysu.edu.databinding.FragmentServiceBinding;
import com.sysu.edu.databinding.ServiceBoxBinding;
import com.sysu.edu.news.News;
import com.sysu.edu.system.PEPreservation;

import java.util.Objects;

public class ServiceFragment extends Fragment {
    LinearLayout service_container;
    private NestedScrollView fragment;

    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(fragment==null){
        FragmentServiceBinding binding = FragmentServiceBinding.inflate(inflater);
        fragment=binding.getRoot();
        service_container=binding.serviceContainer;
        String[] titles = new String[]{a(R.string.news),a(R.string.system), a(R.string.official_website), a(R.string.official_media),a(R.string.academy),a(R.string.study), a(R.string.traffic), a(R.string.dorm)};
        String[][] items = new String[][]{{"资讯门户"//,"学校活动","校园集市"
        },
                {"体育场馆预定系统","学工系统","教务系统","中山大学统一门户","大学服务中心","财务信息系统"},
                {"中山大学官网","本科招生","研究生招生","人才招聘","百年校庆","公务电子邮件系统","博物馆","图书馆","校友会"},
                {"逸仙码","企业微信","中大招生"},
                {a(R.string.evaluation),"选课",a(R.string.agenda),a(R.string.exam),a(R.string.calendar),a(R.string.selfStudyRoom),a(R.string.score),a(R.string.course),a(R.string.trainType)},
                {"SeeLight","雨课堂","课堂派","在线教学平台","中国大学（慕课）"},
                {"校园地图","校车","出行证","校医院"},
                {"宿舍报修","缴纳水电费"},
        };
        View.OnClickListener[][] actions = new View.OnClickListener[][]{
                {
                        newActivity(News.class),
                },//信息
                {newActivity(PEPreservation.class),
                        browse("https://xgxt-443.webvpn.sysu.edu.cn/main/#/index"),
                        browse("https://jwxt.sysu.edu.cn/jwxt/yd/index/#/Home"),
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
                        v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                        v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                        v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                        v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                },//官媒
                {//newActivity(Evaluation.class),
                    null,
                        null,
                        newActivity(AgendaActivity.class),
                        null,
                        newActivity(CalendarActivity.class),
                        newActivity(ClassroomQueryActivity.class),
                        newActivity(Grade.class),null,
                        newActivity(TrainingSchedule.class),
                },//教务
                {

                        browse("https://www.seelight.net/"),
                        browse("https://www.yuketang.cn/web"),
                        browse("https://www.ketangpai.com/"),
                        browse("https://lms.sysu.edu.cn/"),
                        browse("https://www.icourse163.org/"),
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
        }
        return fragment;
    }
    public void initBox(LayoutInflater inflater, String box_title, String[] items, View.OnClickListener[] actions)
    {
        ServiceBoxBinding b=ServiceBoxBinding.inflate(inflater);
        LinearLayout box= b.getRoot();
        TextView title=b.serviceBoxTitle;
        ChipGroup items_container=b.serviceBoxItems;
        title.setText(box_title);
//        ViewCompat.setOnApplyWindowInsetsListener(box, (v, insets) -> {
//            // Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(48, 24, 48, 24);
//            return insets;
//        });
        for(int i=0;i<items.length;i++){
            Chip item = (Chip) inflater.inflate(R.layout.service_item,items_container,false);
            item.setOnClickListener(
                    (i<actions.length&&actions[i]!=null)?actions[i]: v -> Toast.makeText(v.getContext(),"未开发",Toast.LENGTH_LONG).show()
            );
            item.setText(items[i]);
            items_container.addView(item);
        }
        service_container.addView(box);
    }
    public View.OnClickListener browse(String url){
        return view -> startActivity(new Intent(view.getContext(), BrowseActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),view,"miniapp").toBundle());
    }
    public View.OnClickListener newActivity(Class activity_class){
        return view -> startActivity(new Intent(view.getContext(),activity_class),ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),view,"miniapp").toBundle());
    }
    String a(int i){return getString(i);}
}

