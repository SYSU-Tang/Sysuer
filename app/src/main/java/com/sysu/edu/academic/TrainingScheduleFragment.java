package com.sysu.edu.academic;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.transition.AutoTransition;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentTrainingScheduleBinding;

import java.util.ArrayList;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrainingScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrainingScheduleFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    FragmentTrainingScheduleBinding binding;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private View view;

    public TrainingScheduleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TrainingScheduleFragment.
     */
    public static TrainingScheduleFragment newInstance(String param1, String param2) {
        TrainingScheduleFragment fragment = new TrainingScheduleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(new AutoTransition());
        setSharedElementReturnTransition(new AutoTransition());
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view==null){
            binding=FragmentTrainingScheduleBinding.inflate(inflater);
            view=binding.getRoot();
            ViewCompat.setTransitionName(binding.query,"result");
            binding.college.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    ((TrainingSchedule)requireActivity()).getColleges(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
            binding.profession.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    ((TrainingSchedule)requireActivity()).getProfessions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
             binding.query.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Navigation.findNavController(requireActivity(),R.id.fragment).navigate(R.id.confirmationAction,
                           // new FragmentNavigator.Extras(Map.of(v,"result"))
                           null,null,new FragmentNavigator.Extras(Map.of(v,"result"))
                    );
                }
            });
        }
        // Inflate the layout for this fragment
        return view;
    }
    public void deal(int what, JSONObject data){
        switch (what){
            case 1:{
                ArrayList<String> list = new ArrayList<>();
                data.getJSONArray("data").forEach(e->{
                    list.add(((JSONObject)e).getString("departmentName"));
                });
                binding.college.setSimpleItems(list.toArray(new String[]{}));
                if(binding.college.hasFocus()){binding.college.showDropDown();};
                break;
            }
            case 2:{ArrayList<String> list = new ArrayList<>();
                data.getJSONArray("data").forEach(e->list.add(((JSONObject) e).getString("dataName")));
                binding.gradePicker.setMinValue(1);
                binding.gradePicker.setMaxValue(list.size());
                binding.gradePicker.setValue(list.size());
                binding.gradePicker.setDisplayedValues(list.toArray(new String[list.size()]));
                break;
            }
            //binding.college.setSimpleItems(list.toArray(new String[]{}));}
            case 3:{
                data.getJSONArray("data").forEach(e->{
                    Chip chip= (Chip) getLayoutInflater().inflate(R.layout.chip,binding.types,false);
                    chip.setText(((JSONObject) e).getString("dataName"));
                    binding.types.addView(chip);
                });
                ((Chip)binding.types.getChildAt(0)).setChecked(true);
                break;
            }
            case 4:{ArrayList<String> list = new ArrayList<>();
                data.getJSONArray("data").forEach(e->{
                    list.add(((JSONObject)e).getString("name"));
                });
                binding.profession.setSimpleItems(list.toArray(new String[]{}));
                if(binding.profession.hasFocus()){binding.profession.showDropDown();}
                break;
            }
        }

    }

}