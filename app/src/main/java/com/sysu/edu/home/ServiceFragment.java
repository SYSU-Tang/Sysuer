package com.sysu.edu.home;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.sysu.edu.R;
import com.sysu.edu.academic.AcademyNotification;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.BrowserActivity;
import com.sysu.edu.academic.CETActivity;
import com.sysu.edu.academic.CalendarActivity;
import com.sysu.edu.academic.ClassroomQueryActivity;
import com.sysu.edu.academic.CourseCompletion;
import com.sysu.edu.academic.CourseSelectedActivity;
import com.sysu.edu.academic.CourseSelection;
import com.sysu.edu.academic.EvaluationActivity;
import com.sysu.edu.academic.ExamActivity;
import com.sysu.edu.academic.Grade;
import com.sysu.edu.academic.MajorInfo;
import com.sysu.edu.academic.RegisterInfo;
import com.sysu.edu.academic.SchoolRoll;
import com.sysu.edu.academic.SchoolWorkWarning;
import com.sysu.edu.academic.TrainingSchedule;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentServiceBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;
import com.sysu.edu.databinding.ItemServiceBoxBinding;
import com.sysu.edu.life.Pay;
import com.sysu.edu.life.SchoolBus;
import com.sysu.edu.news.News;
import com.sysu.edu.todo.TodoActivity;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public class ServiceFragment extends Fragment {
    // 创建HashMap来存储actions，使用id作为key
    private final Map<Integer, View.OnClickListener> actionMap = new HashMap<>();
    FragmentServiceBinding binding;
    final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
            }
    );
    Params params;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentServiceBinding.inflate(inflater);
            params = new Params(requireActivity());
            // 初始化actions HashMap
            initializeActionMap();

            JSONReader reader = JSONReader.of(getResources().openRawResource(R.raw.service), StandardCharsets.UTF_8);
            JSONArray array = reader.readJSONArray();

            // 使用HashMap替代原来的二维数组
            IntStream.range(0, array.size()).forEach(i -> {
                JSONObject serviceGroup = array.getJSONObject(i);
                initBoxWithHashMap(inflater, serviceGroup.getString("name"), serviceGroup.getJSONArray("items"));
            });
        }
        return binding.getRoot();
    }

    // 初始化actions HashMap
    private void initializeActionMap() {
        // 学术服务 (id: 1xx)
        actionMap.put(101, newActivity(SchoolRoll.class));           // 学籍
        actionMap.put(102, newActivity(CETActivity.class));          // 四六级
        actionMap.put(103, newActivity(RegisterInfo.class));         // 注册
        actionMap.put(104, newActivity(SchoolWorkWarning.class));    // 学业预警
        actionMap.put(105, newActivity(CourseCompletion.class));     // 课程完成情况

        // 学习服务 (id: 2xx)
        actionMap.put(201, newActivity(TodoActivity.class));         // 待办

        // 资讯门户 (id: 3xx)
        actionMap.put(301, newActivity(News.class));                 // 资讯门户
        actionMap.put(302, v -> {
            try {
                startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.comingx.zanao")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException e) {
                params.toast(R.string.no_app);
            }
        }); // 校园集市
        actionMap.put(303, newActivity(AcademyNotification.class));  // 教务通知

        // 系统服务 (id: 4xx)
        actionMap.put(401, browse("https://gym-443.webvpn.sysu.edu.cn/#/"));                   // 体育场馆预定系统
        actionMap.put(402, browse("https://xgxt-443.webvpn.sysu.edu.cn/main/#/index"));        // 学工系统
        actionMap.put(403, browse("https://jwxt.sysu.edu.cn/jwxt/yd/index/#/Home"));           // 本科教务系统
        actionMap.put(404, browse("https://portal.sysu.edu.cn/newClient/#/newPortal/index"));  // 中山大学统一门户
        actionMap.put(405, browse("https://usc.sysu.edu.cn/taskcenter-v4/workflow/index"));    // 大学服务中心
        actionMap.put(406, browse("https://cwxt-443.webvpn.sysu.edu.cn/#/home/index"));        // 财务信息系统

        // 官网服务 (id: 5xx)
        actionMap.put(501, browse("https://www.sysu.edu.cn/"));              // 中山大学官网
        actionMap.put(502, browse("https://admission.sysu.edu.cn/"));        // 本科招生
        actionMap.put(503, browse("https://graduate.sysu.edu.cn/zsw/"));     // 研究生招生
        actionMap.put(504, browse("https://rcb.sysu.edu.cn/"));              // 人才招聘
        actionMap.put(505, browse("https://sysu100.sysu.edu.cn/"));          // 百年校庆
        actionMap.put(506, browse("https://bwgxsg.sysu.edu.cn/"));           // 博物馆
        actionMap.put(507, browse("https://library.sysu.edu.cn/"));          // 图书馆
        actionMap.put(508, browse("https://alumni.sysu.edu.cn/"));           // 校友会
        actionMap.put(509, browse("https://mail.sysu.edu.cn/"));             // 公务电子邮件系统

        // 官方服务 (id: 6xx)
        actionMap.put(601, v -> {    // 二维码
            String linking = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("qrcode", "");
            if (!linking.isEmpty()) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linking)));
                } catch (ActivityNotFoundException e) {
                    params.toast(R.string.no_app);
                }
            }/* else {
                //new LaunchMiniProgram(requireActivity()).launchMiniProgram("gh_85575b9f544e");
            }*/
        });
        actionMap.put(602, v -> {
            try {
                startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                //(requireContext(), R.string.no_app, Toast.LENGTH_LONG).show();
            }
        }); // 企业微信
        //actionMap.put(603, v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))); // 中大招生

        // 教务服务 (id: 7xx)
        actionMap.put(701, newActivity(EvaluationActivity.class));           // 评教
        actionMap.put(702, newActivity(CourseSelection.class));
        actionMap.put(703, newActivity(AgendaActivity.class));               // 课程表
        actionMap.put(704, newActivity(ExamActivity.class));                 // 考试
        actionMap.put(705, newActivity(CalendarActivity.class));             // 校历
        actionMap.put(706, newActivity(ClassroomQueryActivity.class));       // 自习室
        actionMap.put(707, newActivity(Grade.class));                        // 成绩
        actionMap.put(709, browse("https://jwxt.sysu.edu.cn/jwxt/mk/#/personalTrainingProgramView")); // 个人培养方案
        actionMap.put(710, newActivity(TrainingSchedule.class));             // 培养方案
        actionMap.put(711, newActivity(MajorInfo.class));                    // 专业
        actionMap.put(712, newActivity(CourseSelectedActivity.class));                  // 已选课程


        // 学习平台 (id: 8xx)
        actionMap.put(801, browse("https://www.seelight.net/"));             // SeeLight
        actionMap.put(802, browse("https://www.yuketang.cn/web"));           // 雨课堂
        actionMap.put(803, browse("https://www.ketangpai.com/"));            // 课堂派
        actionMap.put(804, browse("https://lms.sysu.edu.cn/"));              // 在线教学平台
        actionMap.put(805, browse("https://www.icourse163.org/"));           // 中国大学（慕课）
        actionMap.put(806, browse("https://welearn.sflep.com/index.aspx"));  // WeLearn

        // 生活服务 (id: 9xx)
        actionMap.put(902, newActivity(SchoolBus.class));                    // 校车
        actionMap.put(906, browse("https://zhny.sysu.edu.cn/h5/#/"));        // 水电费
        actionMap.put(907, newActivity(Pay.class));                          // 缴费大厅

        // 人工智能服务 (id: 10xx)
        actionMap.put(1001, browse("https://chat.sysu.edu.cn/zntgc/agent"));     // Deepseek
        actionMap.put(1002, browse("https://chat.sysu.edu.cn/znt/chat/empty"));  // 逸闻
        actionMap.put(1003, browse("https://xgxw.sysu.edu.cn/aicounsellor/agents/outlink/sunyatsenuniversity")); // 学工君
    }

    // 新的initBox方法，使用HashMap来获取对应的action
    public void initBoxWithHashMap(LayoutInflater inflater, String box_title, JSONArray items) {
        ItemServiceBoxBinding b = ItemServiceBoxBinding.inflate(inflater);
        b.serviceBoxTitle.setText(box_title);
        IntStream.range(0, items.size()).forEach(index -> {
            JSONObject item = items.getJSONObject(index);
            int itemId = item.getIntValue("id");
            ItemActionChipBinding chip = ItemActionChipBinding.inflate(inflater, b.serviceBoxItems, false);

            // 从HashMap中获取对应的action，如果没有则显示"未开发"
            View.OnClickListener action = actionMap.get(itemId);
            chip.getRoot().setOnClickListener(
                    action != null ? action : v -> Toast.makeText(v.getContext(), "未开发", Toast.LENGTH_LONG).show()
            );

            chip.getRoot().setText(item.getString("name"));
            b.serviceBoxItems.addView(chip.getRoot());
        });
        binding.serviceContainer.addView(b.getRoot());
    }

    public View.OnClickListener browse(String url) {
        return view -> startActivity(new Intent(view.getContext(), BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle());
    }

    public View.OnClickListener newActivity(Class<?> activity_class) {
        return view -> launcher.launch(new Intent(view.getContext(), activity_class), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp"));
    }
}