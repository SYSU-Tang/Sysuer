package com.sysu.edu.academic;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.transition.TransitionInflater;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentTrainingScheduleBinding;

import java.util.ArrayList;
import java.util.Map;

public class TrainingScheduleFragment extends Fragment {

    FragmentTrainingScheduleBinding binding;
   View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));

    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(view==null){
            binding=FragmentTrainingScheduleBinding.inflate(inflater);
            view=binding.getRoot();
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
             binding.query.setOnClickListener(v -> Navigation.findNavController(requireActivity(),R.id.fragment).navigate(R.id.confirmationAction,
                    // new FragmentNavigator.Extras(Map.of(v,"result"))
                    null,null
//                            new NavOptions.Builder().setEnterAnim(android.R.anim.fade_in)
//                                    .setExitAnim(android.R.anim.fade_out)
//                                    .setPopEnterAnim(android.R.anim.fade_in)
//                                    .setPopExitAnim(android.R.anim.fade_out).build()
                     ,new FragmentNavigator.Extras(Map.of(v,"result"))
             ));
        }
        // Inflate the layout for this fragment
        return view;
    }
    public void deal(int what, JSONObject data){
        switch (what){
            case 1:{
                ArrayList<String> list = new ArrayList<>();
                data.getJSONArray("data").forEach(e-> list.add(((JSONObject)e).getString("departmentName")));
                binding.college.setSimpleItems(list.toArray(new String[]{}));
                if(binding.college.hasFocus()){binding.college.showDropDown();}
                break;
            }
            case 2:{ArrayList<String> list = new ArrayList<>();
                data.getJSONArray("data").forEach(e->list.add(((JSONObject) e).getString("dataName")));
                binding.gradePicker.setMinValue(1);
                binding.gradePicker.setMaxValue(list.size());
                binding.gradePicker.setValue(list.size());
                binding.gradePicker.setDisplayedValues(list.toArray(new String[0]));
                break;
            }
            //binding.college.setSimpleItems(list.toArray(new String[]{}));}
            case 3:{
                data.getJSONArray("data").forEach(e->{
                    Chip chip= (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip,binding.types,false);
                    chip.setText(((JSONObject) e).getString("dataName"));
                    binding.types.addView(chip);
                });
                ((Chip)binding.types.getChildAt(0)).setChecked(true);
                break;
            }
            case 4:{ArrayList<String> list = new ArrayList<>();
                data.getJSONArray("data").forEach(e-> list.add(((JSONObject)e).getString("name")));
                binding.profession.setSimpleItems(list.toArray(new String[]{}));
                if(binding.profession.hasFocus()){binding.profession.showDropDown();}
                break;
            }
        }

    }

}