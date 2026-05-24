package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentCourseQueryResultBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;

public class RoomQueryResultFragment extends StaggeredFragment {
    FragmentCourseQueryResultBinding courseQueryResultBinding;
    JwxtModel model;
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        courseQueryResultBinding = FragmentCourseQueryResultBinding.inflate(inflater, container, false);
        courseQueryResultBinding.getRoot().addView(super.onCreateView(inflater, courseQueryResultBinding.getRoot(), savedInstanceState), -1, -1);
        courseQueryResultBinding.fab.setOnClickListener(_ -> export(courseQueryResultBinding.fab, getString(R.string.course)));
        model = new JwxtModel(requireContext());
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.getSecond();
            Integer code = response.getInteger("code");
            if (code == 200) response.getJSONObject("data").getJSONArray("data").forEach(e -> {
                JSONObject item = (JSONObject) e;
                ArrayList<String> values = extractValue(item, new String[]{"yearTerm", "date", "week", "dayWeek", "date", "campus", "teachingBuild", "teachingBuildNum", "classroomNum", "floor", "classroomID", "seatCount"});
                for (String i : new String[]{"oneSection", "twoSection", "threeSection", "fourSection", "fiveSection", "sixSection", "sevenSection", "eightSection", "nineSection", "tenSection", "elevenSection", "twelveSection", "thirteenSection", "fourteenSection", "fifteenSection", "sixteenSection"}) {
                    JSONObject section = item.getJSONObject(i);
                    values.add(toStringOrDefault(section.getString("occupyReason"), "") + "-" + toStringOrDefault(section.getString("occupyUseDepartment"), ""));
                }
                add(item.getString("classroomNum"), List.of(getString(R.string.year_term), getString(R.string.date), getString(R.string.week_range), getString(R.string.week), getString(R.string.date), getString(R.string.campus), getString(R.string.office), getString(R.string.teaching_building_number), getString(R.string.classroom_number), getString(R.string.floor), getString(R.string.classroom_id), getString(R.string.seat_count),
                        getString(R.string.first_section), getString(R.string.second_section), getString(R.string.third_section), getString(R.string.fourth_section), getString(R.string.fifth_section), getString(R.string.sixth_section), getString(R.string.seventh_section), getString(R.string.eighth_section), getString(R.string.ninth_section), getString(R.string.tenth_section), getString(R.string.eleventh_section), getString(R.string.twelfth_section), getString(R.string.thirteenth_section), getString(R.string.fourteenth_section), getString(R.string.fifteenth_section), getString(R.string.sixteenth_section)), values);
            });
        });
        getRooms();
        return courseQueryResultBinding.getRoot();
    }
    
    void getRooms() {
        model.addAndNext("jwxt/schedule/agg/classroomOccupy/pageCheckList", String.format("{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":%s}", requireArguments().getString("params")), 0);
    }
}
