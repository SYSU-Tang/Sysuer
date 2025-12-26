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
import com.alibaba.fastjson2.JSONReader;
import com.sysu.edu.R;
import com.sysu.edu.academic.AcademyNotification;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.BrowserActivity;
import com.sysu.edu.academic.CETActivity;
import com.sysu.edu.academic.CalendarActivity;
import com.sysu.edu.academic.ClassroomQueryActivity;
import com.sysu.edu.academic.CourseCompletion;
import com.sysu.edu.academic.EvaluationActivity;
import com.sysu.edu.academic.ExamActivity;
import com.sysu.edu.academic.Grade;
import com.sysu.edu.academic.MajorInfo;
import com.sysu.edu.academic.RegisterInfo;
import com.sysu.edu.academic.SchoolRoll;
import com.sysu.edu.academic.SchoolWorkWarning;
import com.sysu.edu.academic.TrainingSchedule;
import com.sysu.edu.databinding.FragmentServiceBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;
import com.sysu.edu.databinding.ItemServiceBoxBinding;
import com.sysu.edu.extra.LaunchMiniProgram;
import com.sysu.edu.life.Pay;
import com.sysu.edu.life.SchoolBus;
import com.sysu.edu.news.News;
import com.sysu.edu.todo.TodoActivity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.IntStream;

public class ServiceFragment extends Fragment {
    FragmentServiceBinding binding;
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {}
    );
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentServiceBinding.inflate(inflater);

            JSONReader reader = JSONReader.of(getResources().openRawResource(R.raw.service), StandardCharsets.UTF_8);
            JSONArray array = reader.readJSONArray();

            View.OnClickListener[][] actions = new View.OnClickListener[][]{
                    {
                            newActivity(SchoolRoll.class),
                            newActivity(CETActivity.class),
                            newActivity(RegisterInfo.class),
                            newActivity(SchoolWorkWarning.class),
                            newActivity(CourseCompletion.class)
                    },
                    {
                            newActivity(TodoActivity.class),
                    },//学习
                    /*{

                    },//学工*/
                    {
                            newActivity(News.class),
                            v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.comingx.zanao")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                            newActivity(AcademyNotification.class),
                    },//信息
                    {//newActivity(PEPreservation.class),
                            browse("https://gym-443.webvpn.sysu.edu.cn/#/"),
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
                            browse("https://bwgxsg.sysu.edu.cn/"),
                            browse("https://library.sysu.edu.cn/"),
                            browse("https://alumni.sysu.edu.cn/"),
                            browse("https://mail.sysu.edu.cn/"),
                    },//官网
                    {
                            v -> {
                                String linking = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("qrcode", "");
                                if (linking.isEmpty()) {
                                    new LaunchMiniProgram(requireActivity()).launchMiniProgram("gh_85575b9f544e");
                                } else {
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(linking)));
                                    } catch (ActivityNotFoundException e) {
                                        // Toast.makeText(requireContext(), R.string.no_app, Toast.LENGTH_LONG).show();
                                    }
                                }
                            },
                            v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                            v -> startActivity(Objects.requireNonNull(requireActivity().getPackageManager().getLaunchIntentForPackage("com.tencent.wework")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                    },//官媒
                    {newActivity(EvaluationActivity.class),
                            /* newActivity(CourseSelection.class),*/null,
                            newActivity(AgendaActivity.class),
                            newActivity(ExamActivity.class),
                            newActivity(CalendarActivity.class),
                            newActivity(ClassroomQueryActivity.class),
                            newActivity(Grade.class),
                            null,
                            browse("https://jwxt.sysu.edu.cn/jwxt/mk/#/personalTrainingProgramView"),
                            newActivity(TrainingSchedule.class),
                            newActivity(MajorInfo.class)
                    },//教务
                    {

                            browse("https://www.seelight.net/"),
                            browse("https://www.yuketang.cn/web"),
                            browse("https://www.ketangpai.com/"),
                            browse("https://lms.sysu.edu.cn/"),
                            browse("https://www.icourse163.org/"),
                            browse("https://welearn.sflep.com/index.aspx")
                    },//学习
                    {null,
                            newActivity(SchoolBus.class),
                            null,
                            null,
                            null,
                            browse("https://zhny.sysu.edu.cn/h5/#/"),
                            newActivity(Pay.class),
                    },//生活
                    {
                            browse("https://chat.sysu.edu.cn/zntgc/agent"),
                            browse("https://chat.sysu.edu.cn/znt/chat/empty"),
                            browse("https://xgxw.sysu.edu.cn/aicounsellor/agents/outlink/sunyatsenuniversity"),
                    }//AI
            };
            IntStream.range(0, array.size()).forEach(i ->
                    initBox(inflater, (array.getJSONObject(i)).getString("name"), array.getJSONObject(i).getJSONArray("items"), actions[i])
            );
        }
        return binding.getRoot();
    }

    public void initBox(LayoutInflater inflater, String box_title, JSONArray items, View.OnClickListener[] actions) {
        ItemServiceBoxBinding box = ItemServiceBoxBinding.inflate(inflater);
        box.serviceBoxTitle.setText(box_title);
        IntStream.range(0, items.size()).forEach(index -> {
            ItemActionChipBinding chip = ItemActionChipBinding.inflate(inflater, box.serviceBoxItems, false);
            chip.getRoot().setOnClickListener(
                    (index < actions.length && actions[index] != null) ? actions[index] : v -> Toast.makeText(v.getContext(), "未开发", Toast.LENGTH_LONG).show()
            );
            chip.getRoot().setOnLongClickListener(v -> {
                Toast.makeText(requireContext(),items.getJSONObject(index).getString("description"),Toast.LENGTH_LONG).show();
                return true;
            });
            chip.getRoot().setText(items.getJSONObject(index).getString("name"));
            box.serviceBoxItems.addView(chip.getRoot());
        });
        binding.serviceContainer.addView(box.getRoot());
    }
    public View.OnClickListener browse(String url) {
        return view -> startActivity(new Intent(view.getContext(), BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle());
    }

    public View.OnClickListener newActivity(Class<?> activity_class) {
        return view -> launcher.launch(new Intent(view.getContext(), activity_class), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp"));
    }
}