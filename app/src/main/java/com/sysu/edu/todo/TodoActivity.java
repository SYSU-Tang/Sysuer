package com.sysu.edu.todo;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityTodoBinding;
import com.sysu.edu.databinding.DialogTodoBinding;
import com.sysu.edu.databinding.ItemEditTextBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.todo.info.TodoAdapter;
import com.sysu.edu.todo.info.TodoInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

public class TodoActivity extends AppCompatActivity {

    TodoList todoDB;
    int toAddCode = 0;
    TodoViewModel viewModel;
    TodoAdapter adp;
    DialogTodoBinding dialogBinding;
    ArrayList<String> types;
    ArrayList<String> subjects;
    AlertDialog todoDialog;
    TodoInfo todoInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTodoBinding binding = ActivityTodoBinding.inflate(getLayoutInflater());
        dialogBinding = DialogTodoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(TodoViewModel.class);
        todoInfo = new TodoInfo();
        adp = ((TodoFragment) binding.fragmentTodo.getFragment()).getAdapter(this);
        todoDB = new TodoList(this);
        todoInfo.getPriority().observe(this, it -> dialogBinding.priorityValue.setText(new String[]{"不重要且不紧急", "不重要但紧急", "重要但不紧急", "重要且紧急"}[it - 1]));
        binding.tool.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        dialogBinding.prioritySlider.addOnChangeListener((slider, value, fromUser) -> todoInfo.setPriority((int) value));
        todoDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.submit, (dialog1, which) -> {
                    todoInfo.setTitle(Objects.requireNonNull(dialogBinding.title.getText()).toString());
                    todoInfo.setDescription(Objects.requireNonNull(dialogBinding.description.getText()).toString());
                    todoDB.add(todoInfo);
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (dialog13, which) -> {

                })
                .create();
        ItemEditTextBinding itemEditTextBinding = ItemEditTextBinding.inflate(getLayoutInflater());
        itemEditTextBinding.getRoot().setHint(R.string.type);
        AlertDialog todo_add_dialog = new MaterialAlertDialogBuilder(this)
                .setView(itemEditTextBinding.getRoot())
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    Editable toAdd = itemEditTextBinding.edit.getText();
                    if (toAdd != null && !toAdd.toString().isEmpty()) {
                        ArrayList<String> array = List.of(types, subjects).get(toAddCode);
                        ChipGroup chipGroup = (new ChipGroup[]{dialogBinding.todoType, dialogBinding.subject}[toAddCode]);
                        if (!array.contains(toAdd.toString())) {
                            array.add(toAdd.toString());
                            createFilterChip(toAdd.toString(), chipGroup,toAddCode);
                        }
                        ContentValues values = new ContentValues();
                        values.put("name", toAdd.toString());
                        todoDB.getDatabase().insertOrThrow(new String[]{"types", "subjects"}[toAddCode], null, values);
                        ((Chip) chipGroup.getChildAt(array.indexOf(toAdd.toString()) + 1)).setChecked(true);
                        if (toAddCode == 0) {
                            todoInfo.setType(toAdd.toString());
                        } else {
                            todoInfo.setSubject(toAdd.toString());
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(59);
        AlertDialog remindDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.custom_remind_title)
                .setView(numberPicker)
                .setPositiveButton(R.string.submit, (dialog, which) -> todoInfo.setRemindTime(String.format(Locale.getDefault(), "%02d%s", numberPicker.getValue(), getString(R.string.minute))))
                .setNegativeButton(R.string.cancel, null)
                .create();
        for (AlertDialog dialog : new AlertDialog[]{todoDialog, todo_add_dialog, remindDialog}) {
            if (dialog != remindDialog) {
                dialog.setCanceledOnTouchOutside(false);
                ((FrameLayout) Objects.requireNonNull(dialog.findViewById(android.R.id.content))).setPadding(48, 48, 48, 0);
            }
            Window window = Objects.requireNonNull(dialog.getWindow());
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawableResource(R.drawable.top_capsule);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
        }
        binding.add.setOnClickListener(view -> todoDialog.show());
        //binding.fragment_todo.getFragment.setAdapter(new TodoAdapter(this));
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialDatePicker<Long> ddlPicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder().build();
        IntStream.range(0, 2).forEach(i -> List.of(dialogBinding.todoTypeAdd, dialogBinding.todoSubjectAdd).get(i).setOnClickListener(view -> {
            toAddCode = i;
            todo_add_dialog.show();
        }));
        ArrayList<PopupMenu> popupMenuArrayList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int i = 0; i < 4; i++) {
            ItemPreferenceBinding itemPreferenceBinding = ItemPreferenceBinding.inflate(getLayoutInflater(), dialogBinding.times, false);
            itemPreferenceBinding.itemTitle.setText(getString(new int[]{R.string.date, R.string.time, R.string.remind, R.string.ddl}[i]));
            itemPreferenceBinding.icon.setImageResource(new int[]{R.drawable.calendar, R.drawable.time, R.drawable.alarm, R.drawable.warning}[i]);
            itemPreferenceBinding.getRoot().updateAppearance(i, 4);
            PopupMenu popupMenu = new PopupMenu(this, itemPreferenceBinding.getRoot(), Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
            Menu menu = popupMenu.getMenu();
            MenuItem none = menu.add(0, Menu.NONE, Menu.NONE, R.string.none);
            none.setOnMenuItemClickListener(item -> {
                itemPreferenceBinding.itemContent.setText(item.getTitle());
                return true;
            });
            switch (i) {
                case 0:
                    todoInfo.getDueDate().observe(this, v -> itemPreferenceBinding.itemContent.setText(v == null ? "无" : v));
                    datePicker.addOnPositiveButtonClickListener(selection -> todoInfo.setDueDate(dateString.format(selection)));
                    for (int j = 0; j < 3; j++) {
                        int finalJ = j;
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.today, R.string.tomorrow, R.string.next_week}[j]).setOnMenuItemClickListener(item -> {
                            calendar.setTime(new Date());
                            calendar.add(Calendar.DAY_OF_MONTH, new int[]{0, 1, 7}[finalJ]);
                            todoInfo.setDueDate(dateString.format(calendar.getTime()));
                            return true;
                        });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        datePicker.show(getSupportFragmentManager(), "date_picker");
                        return true;
                    });
                    none.setOnMenuItemClickListener(item -> {
                        todoInfo.getDueDate().setValue(null);
                        return true;
                    });
                    break;
                case 3:
                    for (int j = 0; j < 3; j++) {
                        int finalJ = j;
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.today, R.string.tomorrow, R.string.next_week}[j]).setOnMenuItemClickListener(item -> {
                            calendar.setTime(new Date());
                            calendar.add(Calendar.DAY_OF_MONTH, new int[]{0, 1, 7}[finalJ]);
                            todoInfo.setDdlDate(dateString.format(calendar.getTime()));
                            return true;
                        });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        ddlPicker.show(getSupportFragmentManager(), "ddl_picker");
                        return true;
                    });
                    todoInfo.getDdlDate().observe(this, v -> itemPreferenceBinding.itemContent.setText(v == null ? "无" : v));
                    ddlPicker.addOnPositiveButtonClickListener(selection -> todoInfo.getDdlDate().setValue(dateString.format(selection)));
                    none.setOnMenuItemClickListener(item -> {
                        todoInfo.getDdlDate().setValue(null);
                        return true;
                    });
                    break;
                case 1:
                    todoInfo.getTime().observe(this, v -> itemPreferenceBinding.itemContent.setText(v == null ? "无" : v));
                    for (int j = 0; j < 4; j++) {
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.morning, R.string.noon, R.string.afternoon, R.string.evening}[j]).setOnMenuItemClickListener(item -> {
                            todoInfo.setTime(String.valueOf(item.getTitle()));
                            return true;
                        });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        timePicker.show(getSupportFragmentManager(), "time_picker");
                        return true;
                    });
                    timePicker.addOnPositiveButtonClickListener(selection -> todoInfo.setTime(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute())));
                    break;
                case 2:
                    for (int j = 0; j < 6; j++) {
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.on_time, R.string.five_mins, R.string.fiveteen_mins, R.string.half_hour, R.string.one_hour, R.string.one_day}[j]).setOnMenuItemClickListener(item -> {
                            itemPreferenceBinding.itemContent.setText(item.getTitle());
                            return true;
                        });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.custom).setOnMenuItemClickListener(item -> {
                        remindDialog.show();
                        return true;
                    });
                    todoInfo.getRemindTime().observe(this, v -> itemPreferenceBinding.itemContent.setText(v == null ? "无" : v + "前"));
                    break;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true);
            }
            popupMenuArrayList.add(popupMenu);
            int finalI1 = i;
            itemPreferenceBinding.getRoot().setOnClickListener(v -> popupMenuArrayList.get(finalI1).show());
            dialogBinding.times.addView(itemPreferenceBinding.getRoot());
        }//todoDB.add();
        refresh();
        todoDB.addType();
        getTypes();
        getSubjects();
        types.forEach(s -> createFilterChip(s, dialogBinding.todoType,0));
        subjects.forEach(s -> createFilterChip(s, dialogBinding.subject,1));
        initDialog(todoInfo);
    }

    private void refresh() {
        adp.clear();
        try {
            Cursor cursor = todoDB.getDatabase().query("todos", null, null, null, null, null, "due_date");
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                TodoInfo todoDetail = new TodoInfo();
                todoDetail.setTitle(title);
                todoDetail.setDescription(description);
                todoDetail.setDueDate(dueDate);
                todoDetail.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow("priority")));
                todoDetail.setType(cursor.getString(cursor.getColumnIndexOrThrow("todo_type")));
                todoDetail.setLocation(cursor.getString(cursor.getColumnIndexOrThrow("location")));
                todoDetail.setSubject(cursor.getString(cursor.getColumnIndexOrThrow("subject")));
                todoDetail.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
                todoDetail.setLabel(cursor.getString(cursor.getColumnIndexOrThrow("label")));
                todoDetail.setDdlDate(cursor.getString(cursor.getColumnIndexOrThrow("ddl")));
                todoDetail.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
                todoDetail.setSubtask(cursor.getString(cursor.getColumnIndexOrThrow("subtask")));
                todoDetail.setAttachment(cursor.getString(cursor.getColumnIndexOrThrow("attachment")));
                todoDetail.setDoneDate(cursor.getString(cursor.getColumnIndexOrThrow("done_date")));
                adp.add(todoDetail);
                //viewModel.setTodoItem(new HashMap<>(Map.of("title", title, "description", description, "dueDate", dueDate)));
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void getTypes(){
        types = new ArrayList<>();
         try {
            Cursor cursor = todoDB.getDatabase().query("types", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                types.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    void getSubjects(){
        subjects = new ArrayList<>();
        try {
            Cursor cursor = todoDB.getDatabase().query("subjects", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                subjects.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void createFilterChip(String s, ChipGroup view, int toAddCode) {
        ItemFilterChipBinding chip = ItemFilterChipBinding.inflate(getLayoutInflater(), view, false);
        chip.getRoot().setText(s);
        chip.getRoot().setOnLongClickListener(v -> {

            if (((Chip)v).isChecked()){
                switch (toAddCode){
                    case 0:
                        todoInfo.setType(null);
                        break;
                    case 1:
                        todoInfo.setSubject(null);
                        break;
                }
            }
            view.removeView(chip.getRoot());
            List.of(types, subjects).get(toAddCode).remove(s);
            todoDB.getDatabase().delete(new String[]{"types", "subjects"}[toAddCode], "name=?", new String[]{s});
            return true;
        });
        view.addView(chip.getRoot(), view.getChildCount() - 1);
    }
    public void initDialog(TodoInfo todoInfo){
        this.todoInfo = todoInfo;
        dialogBinding.title.setText(todoInfo.getTitle().getValue());
        dialogBinding.description.setText(todoInfo.getDescription().getValue());
        dialogBinding.prioritySlider.setValue(todoInfo.getPriority().getValue() == null ? 1 : todoInfo.getPriority().getValue());
        String type= todoInfo.getType().getValue();
        if (type != null) {
            if (!types.contains(type)) {
                createFilterChip(type, dialogBinding.todoType, 0);
                types.add(type);
            }
            ((Chip) dialogBinding.todoType.getChildAt(types.indexOf(todoInfo.getType().getValue()) + 1)).setChecked(true);
        }
        String subject= todoInfo.getSubject().getValue();
        if (subject != null) {
            if (!subjects.contains(subject)) {
                createFilterChip(subject, dialogBinding.subject, 1);
                subjects.add(subject);
            }
            ((Chip) dialogBinding.subject.getChildAt(subjects.indexOf(subject) + 1)).setChecked(true);
        }
        dialogBinding.check.setChecked(todoInfo.getStatus().getValue() != null && todoInfo.getStatus().getValue() == 1);
    }
    public void showDialog(){
        todoDialog.show();
    }
}

