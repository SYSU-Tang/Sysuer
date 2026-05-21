package com.sysu.edu.view;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.GridLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.databinding.DialogGridBinding;
import com.sysu.edu.databinding.ItemButtonGridBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class GridDialog {
    
    final ArrayList<Integer> referenceIds = new ArrayList<>();
    private final DialogGridBinding menuBinding;
    private final BottomSheetDialog menuDialog;
    private final FragmentActivity activity;
    private int selected = -1;
    private boolean selectable = false;
    private boolean multipleSelectable = false;
    private int iconGravity;
    private int gravity;
    
    public GridDialog(FragmentActivity activity) {
        this.activity = activity;
        menuDialog = new BottomSheetDialog(activity);
        menuBinding = DialogGridBinding.inflate(activity.getLayoutInflater());
        menuDialog.setContentView(menuBinding.getRoot());
    }
    
    
    public void setColumn(int column) {
        ((GridLayout.LayoutParams) menuBinding.handler.getLayoutParams()).columnSpec = GridLayout.spec(GridLayout.UNDEFINED, column, GridLayout.FILL, 1.0f);
        menuBinding.grid.setColumnCount(column);
    }
    
    public void setIconGravity(int gravity) {
        iconGravity = gravity;
        referenceIds.forEach(id -> ((MaterialButton) menuBinding.grid.findViewById(id)).setIconGravity(gravity));
    }
    
    public void setGravity(int gravity) {
        this.gravity = gravity;
        referenceIds.forEach(id -> ((MaterialButton) menuBinding.grid.findViewById(id)).setGravity(gravity));
    }
    
    public <T> void loadMenu(List<T> menuTitle, List<Integer> menuIcon, List<? extends Consumer<MaterialButton>> menuAction, Class<T> type) {
        referenceIds.clear();
        IntStream.range(0, menuTitle.size()).forEach(i -> {
            MaterialButton menu = ItemButtonGridBinding.inflate(activity.getLayoutInflater(), menuBinding.grid, false).getRoot();
            if (type == Integer.class)
                menu.setText((Integer) menuTitle.get(i));
            else
                menu.setText((String) menuTitle.get(i));
            if (menuIcon.size() > i && menuIcon.get(i) != 0)
                menu.setIconResource(menuIcon.get(i));
            int id = View.generateViewId();
            referenceIds.add(id);
            menu.setId(id);
            menu.addOnCheckedChangeListener((_, isChecked) -> menu.setStrokeWidth((isChecked && (selectable || multipleSelectable)) ? 3 : 0));
            menu.setOnClickListener(_ -> {
                if (menuAction.size() > i && menuAction.get(i) != null)
                    menuAction.get(i).accept(menu);
                if (multipleSelectable)
                    toggleMenu(i);
                else if (selectable)
                    selectMenu(i);
            });
            if (iconGravity != 0)
                menu.setIconGravity(iconGravity);
            if (gravity != 0)
                menu.setGravity(gravity);
            menuBinding.grid.addView(menu);
        });
    }
    
    public void show() {
        menuDialog.show();
        menuDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    
    public BottomSheetDialog getDialog() {
        return menuDialog;
    }
    
    public MaterialButton getMenu(int position) {
        return position >= referenceIds.size() || position < 0 ? null : menuBinding.grid.findViewById(referenceIds.get(position));
    }
    
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
    
    public void setMultipleSelectable(boolean multipleSelectable) {
        this.multipleSelectable = multipleSelectable;
    }
    
    public void selectMenu(int position) {
        MaterialButton menu = getMenu(position);
        if (menu != null && position != selected) {
            menu.setChecked(true);
            if (selected >= 0) {
                MaterialButton selectedMenu = getMenu(selected);
                if (selectedMenu != null)
                    selectedMenu.setChecked(false);
            }
            selected = position;
        }
    }
    
    public void toggleMenu(int position) {
        MaterialButton menu = getMenu(position);
        if (menu != null)
            menu.setChecked(!menu.isChecked());
    }
    
    public void toggleMenu(int position, boolean toggle) {
        MaterialButton menu = getMenu(position);
        if (menu != null)
            menu.setChecked(toggle);
    }
    
    public void clickMenu(int position) {
        MaterialButton menu = getMenu(position);
        if (menu != null)
            menu.performClick();
    }


    /*public void multipleSelectMenu(int[] positions) {
        IntStream.of(positions).forEach(this::selectMenu);
    }*/
    
    public void setTogglable(int[] positions, boolean togglable) {
        IntStream.of(positions).forEach(i -> setTogglable(i, togglable));
    }
    
    public void setTogglable(int position, boolean togglable) {
        MaterialButton menu = getMenu(position);
        if (menu != null) {
            ColorStateList colorStateList = AppCompatResources.getColorStateList(activity, R.color.toggle);
            menu.setToggleCheckedStateOnClick(togglable);
            menu.setCheckable(togglable);
            menu.setIconTint(togglable ? colorStateList : null);
            menu.setTextColor(togglable ? colorStateList : null);
        }
    }
    
    public void setPositiveButton(CharSequence text, DialogInterface.OnClickListener action) {
        menuBinding.positive.setText(text);
        menuBinding.positive.setOnClickListener(_ -> action.onClick(menuDialog, DialogInterface.BUTTON_POSITIVE));
        menuBinding.buttonGroup.setVisibility(View.VISIBLE);
    }
    
    public void setPositiveButton(int text, DialogInterface.OnClickListener action) {
        menuBinding.positive.setText(text);
        menuBinding.positive.setOnClickListener(_ -> action.onClick(menuDialog, DialogInterface.BUTTON_POSITIVE));
        menuBinding.buttonGroup.setVisibility(View.VISIBLE);
    }
    
    public void setNegativeButton(CharSequence text, DialogInterface.OnClickListener action) {
        menuBinding.negative.setText(text);
        menuBinding.negative.setOnClickListener(_ -> action.onClick(menuDialog, DialogInterface.BUTTON_NEGATIVE));
        menuBinding.buttonGroup.setVisibility(View.VISIBLE);
    }
    
    public void setNegativeButton(int text, DialogInterface.OnClickListener action) {
        menuBinding.negative.setText(text);
        menuBinding.negative.setOnClickListener(_ -> action.onClick(menuDialog, DialogInterface.BUTTON_NEGATIVE));
        menuBinding.buttonGroup.setVisibility(View.VISIBLE);
    }
    
    public void setNeutralButton(CharSequence text, DialogInterface.OnClickListener action) {
        menuBinding.neutral.setText(text);
        menuBinding.neutral.setOnClickListener(_ -> action.onClick(menuDialog, DialogInterface.BUTTON_NEUTRAL));
        menuBinding.buttonGroup.setVisibility(View.VISIBLE);
    }
    
    public void setNeutralButton(int text, DialogInterface.OnClickListener action) {
        menuBinding.neutral.setText(text);
        menuBinding.neutral.setOnClickListener(_ -> action.onClick(menuDialog, DialogInterface.BUTTON_NEUTRAL));
        menuBinding.buttonGroup.setVisibility(View.VISIBLE);
    }
    
    public void dismiss() {
        menuDialog.dismiss();
    }
}
