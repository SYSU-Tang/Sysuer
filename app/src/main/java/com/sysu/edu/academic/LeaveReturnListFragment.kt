package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.model.XgxtModel;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class LeaveReturnListFragment extends StaggeredFragment {
    XgxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        model = new XgxtModel(requireContext());
        LeaveReturnRegistrationViewModel viewModel = new ViewModelProvider(requireActivity()).get(LeaveReturnRegistrationViewModel.class);
        viewModel.year.observe(getViewLifecycleOwner(), this::getList);
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.second;
            if (response.getInteger("code") == 200) {
                clear();
                response.getJSONArray("data").forEach(e -> add(((JSONObject) e).getString("gzmc"), ((JSONObject) e).getInteger("gzztm") == 1 ? R.drawable.uncheck : R.drawable.check, List.of(getResources().getStringArray(R.array.registration_keys)),
                        extractValue((JSONObject) e, new String[]{"blxn", "lxdjsj", "gzsm", "jjrmc", "jjrrq", "gzzt", "zt"})));
                setListener(new AdapterListener() {
                    @Override
                    public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                        boolean isRegistering = response.getJSONArray("data").getJSONObject(position).getInteger("gzztm") == 1;
                        String status = response.getJSONArray("data").getJSONObject(position).getString("zt");
                        MaterialButton button = holder.itemView.findViewById(R.id.button);
                        button.setText(isRegistering ? "registering".equals(status) ? R.string.start_registration : R.string.modify_registration : R.string.view_detail);
                        button.setOnClickListener(_ -> {
                            if (isRegistering) {
                                Bundle arg = new Bundle();
                                arg.putString("Id", response.getJSONArray("data").getJSONObject(position).getString("cjlfxgzId"));
                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.leave_return_list_fragment, LeaveReturnRegistrationFragment.class, arg)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        });
                    }
                    
                    @Override
                    public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
                        MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                        button.setId(R.id.button);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.gravity = Gravity.END;
                        lp.setMargins(0, 0, model.getContextUtil().dpToPx(16), model.getContextUtil().dpToPx(16));
                        button.setLayoutParams(lp);
                        ((ItemCardBinding) binding).getRoot().addView(button);
                    }
                });
            }
        });
        return view;
    }
    
    void getList(String year) {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/work-list?blxn=" + year, 0);
    }
    
}