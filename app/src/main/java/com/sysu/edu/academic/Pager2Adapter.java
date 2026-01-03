package com.sysu.edu.academic;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class Pager2Adapter extends FragmentStateAdapter {

    final ArrayList<Fragment> fragments=new ArrayList<>();

    public Pager2Adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    public Pager2Adapter add(Fragment e){
        fragments.add(e);
        notifyItemInserted(getItemCount());
        return this;
    }
    public Fragment getItem(int position){
        return fragments.get(position);
    }
    @Override
    public int getItemCount() {
        return fragments.size();
    }
}
