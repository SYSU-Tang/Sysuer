package com.sysu.edu.todo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.sysu.edu.R;
import com.sysu.edu.databinding.DialogEditTextBinding;
import com.sysu.edu.databinding.DialogTodoBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.todo.info.TitleAdapter;
import com.sysu.edu.todo.info.TodoAdapter;
import com.sysu.edu.todo.info.TodoInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InitTodo {

    final TodoList todoDB;
    int toAddCode = 0;
    final DialogTodoBinding dialogBinding;
    ArrayList<String> types;
    ArrayList<String> subjects;
    ArrayList<String> tags;
    final AlertDialog todoDetailDialog;
    final FragmentActivity activity;
    TodoInfo todoInfo = new TodoInfo();
    int count;

    public InitTodo(FragmentActivity activity, TodoFragment todoFragment) {
        this.activity = activity;
        todoDB = new TodoList(activity, 7);
        dialogBinding = DialogTodoBinding.inflate(activity.getLayoutInflater());
        dialogBinding.prioritySlider.addOnChangeListener((slider, value, fromUser) -> todoInfo.setPriority((int) value));
        DialogEditTextBinding itemEditTextBinding = DialogEditTextBinding.inflate(activity.getLayoutInflater());

        todoDetailDialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.confirm, (dialog1, which) -> {
                    todoInfo.setTitle(Objects.requireNonNull(dialogBinding.title.getText()).toString());
                    todoInfo.setDescription(Objects.requireNonNull(dialogBinding.description.getText()).toString());
                    if (todoInfo.getFunction() == TodoInfo.ADD) {
                        todoDB.addTodo(todoInfo);
                    } else if (todoInfo.getFunction() == TodoInfo.VIEW) {
                        todoDB.updateTodo(todoInfo);
                    }
                    refresh(todoFragment);
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (dialog13, which) -> {
                    if (todoInfo.getFunction() == TodoInfo.VIEW) {
                        todoDB.deleteTodo(todoInfo);
                        refresh(todoFragment);
                    }
                })
                .create();
        AlertDialog todoAddDialog = new MaterialAlertDialogBuilder(activity)
                .setView(itemEditTextBinding.getRoot())
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    Editable toAdd = itemEditTextBinding.edit.getText();
                    if (toAdd != null && !toAdd.toString().isEmpty()) {
                        SQLiteDatabase db = todoDB.getWritableDatabase();
                        ArrayList<String> array = Arrays.asList(types, subjects, tags).get(toAddCode);
                        ChipGroup chipGroup = new ChipGroup[]{dialogBinding.todoType, dialogBinding.subject, dialogBinding.tag}[toAddCode];
                        String name = toAdd.toString();
                        if (!array.contains(name)) {
                            array.add(name);
                            createFilterChip(name, chipGroup, toAddCode);
                        }
                        ContentValues values = new ContentValues();
                        values.put("name", name);
                        db.insert(new String[]{"types", "subjects", "tags"}[toAddCode], null, values);
                        selectChipIfPresent(chipGroup, array, name);
                        if (toAddCode == 0) {
                            todoInfo.setType(name);
                        } else if (toAddCode == 1) {
                            todoInfo.setSubject(name);
                        } else if (toAddCode == 2) {
                            todoInfo.setTag(name);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        NumberPicker numberPicker = new NumberPicker(activity);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(59);
        AlertDialog remindDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.custom_remind_title)
                .setView(numberPicker)
                .setPositiveButton(R.string.confirm, (dialog, which) -> todoInfo.setRemindTime(String.format(Locale.getDefault(), "%02d%s", numberPicker.getValue(), activity.getString(R.string.minute))))
                .setNegativeButton(R.string.cancel, null)
                .create();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialDatePicker<Long> ddlPicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder().build();

        // 共用对话框外观设置
        for (AlertDialog dialog : new AlertDialog[]{todoDetailDialog, todoAddDialog, remindDialog}) {
            setupDialogWindow(dialog, dialog != remindDialog);
        }

        for (int i = 0; i < 3; i++) {
            int finalI = i;
            List.of(dialogBinding.todoTypeAdd, dialogBinding.todoSubjectAdd, dialogBinding.todoTagAdd).get(i).setOnClickListener(view -> {
                toAddCode = finalI;
                itemEditTextBinding.getRoot().setHint(new int[]{R.string.type, R.string.subject, R.string.tag}[finalI]);
                todoAddDialog.show();
            });
        }

        ArrayList<PopupMenu> popupMenuArrayList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 4; i++) {
            ItemPreferenceBinding itemPreferenceBinding = ItemPreferenceBinding.inflate(activity.getLayoutInflater(), dialogBinding.times, false);
            itemPreferenceBinding.itemTitle.setText(activity.getString(new int[]{R.string.date, R.string.time, R.string.remind, R.string.ddl}[i]));
            itemPreferenceBinding.icon.setImageResource(new int[]{R.drawable.calendar, R.drawable.time, R.drawable.alarm, R.drawable.warning}[i]);
            itemPreferenceBinding.getRoot().updateAppearance(i, 4);

            PopupMenu popupMenu = new PopupMenu(activity, itemPreferenceBinding.getRoot(), Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
            Menu menu = popupMenu.getMenu();

            MenuItem none = menu.add(0, Menu.NONE, Menu.NONE, R.string.none);

            switch (i) {
                case 0: // due date
                    datePicker.addOnPositiveButtonClickListener(selection -> todoInfo.setDueDate(dateString.format(selection)));
                    for (int j = 0; j < 3; j++) {
                        int finalJ = j;
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.today, R.string.tomorrow, R.string.next_week}[j])
                                .setOnMenuItemClickListener(item -> {
                                    calendar.setTime(new Date());
                                    calendar.add(Calendar.DAY_OF_MONTH, new int[]{0, 1, 7}[finalJ]);
                                    todoInfo.setDueDate(dateString.format(calendar.getTime()));
                                    return true;
                                });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        datePicker.show(activity.getSupportFragmentManager(), "date_picker");
                        return true;
                    });
                    none.setOnMenuItemClickListener(item -> {
                        todoInfo.getDueDate().setValue(null);
                        return true;
                    });
                    break;
                case 3: // ddl
                    for (int j = 0; j < 3; j++) {
                        int finalJ = j;
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.today, R.string.tomorrow, R.string.next_week}[j])
                                .setOnMenuItemClickListener(item -> {
                                    calendar.setTime(new Date());
                                    calendar.add(Calendar.DAY_OF_MONTH, new int[]{0, 1, 7}[finalJ]);
                                    todoInfo.setDdlDate(dateString.format(calendar.getTime()));
                                    return true;
                                });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        ddlPicker.show(activity.getSupportFragmentManager(), "ddl_picker");
                        return true;
                    });
                    ddlPicker.addOnPositiveButtonClickListener(selection -> todoInfo.getDdlDate().setValue(dateString.format(selection)));
                    none.setOnMenuItemClickListener(item -> {
                        todoInfo.getDdlDate().setValue(null);
                        return true;
                    });
                    break;
                case 1: // time
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        timePicker.show(activity.getSupportFragmentManager(), "time_picker");
                        return true;
                    });
                    timePicker.addOnPositiveButtonClickListener(selection -> todoInfo.setDueTime(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute())));
                    break;
                case 2: // remind
                    for (int j = 0; j < 6; j++) {
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.on_time, R.string.five_mins, R.string.fifteen_mins, R.string.half_hour, R.string.one_hour, R.string.one_day}[j])
                                .setOnMenuItemClickListener(item -> {
                                    itemPreferenceBinding.itemContent.setText(item.getTitle());
                                    return true;
                                });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.custom).setOnMenuItemClickListener(item -> {
                        remindDialog.show();
                        return true;
                    });
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true);
            }
            popupMenuArrayList.add(popupMenu);
            int finalI1 = i;
            itemPreferenceBinding.getRoot().setOnClickListener(v -> popupMenuArrayList.get(finalI1).show());
            dialogBinding.times.addView(itemPreferenceBinding.getRoot());
        }

        dialogBinding.check.setOnCheckedChangeListener((buttonView, isChecked) -> todoInfo.setStatus(isChecked ? TodoInfo.DONE : TodoInfo.TODO));

        loadListFromTable("types", types = new ArrayList<>());
        loadListFromTable("subjects", subjects = new ArrayList<>());
        loadListFromTable("tags", tags = new ArrayList<>());

        types.forEach(s -> createFilterChip(s, dialogBinding.todoType, 0));
        subjects.forEach(s -> createFilterChip(s, dialogBinding.subject, 1));
        tags.forEach(s -> createFilterChip(s, dialogBinding.tag, 2));

        initDialog();
        refresh(todoFragment);
    }

    public void filterStatus(TodoFragment f, int status) {
        refresh(f, "status = ?", new String[]{String.valueOf(status)});
    }

    public void refresh(TodoFragment f) {
        refresh(f, null, null);
    }

    public void refresh(TodoFragment f, String selection, String[] args) {
        // 清除已有 adapters
        f.getConcatAdapter().getAdapters().forEach(adp -> f.getConcatAdapter().removeAdapter(adp));
        try {
            TodoAdapter todoAdapter = new TodoAdapter(activity, this);
            TitleAdapter titleAdapter = new TitleAdapter(activity);
            SQLiteDatabase db = todoDB.getWritableDatabase();
            try (Cursor cursor = db.query("todos", null, selection, args, null, null, "due_date")) {
                count = cursor.getCount();
                while (cursor.moveToNext()) {
                    TodoInfo todoDetail = new TodoInfo();
                    int priority = cursor.getInt(cursor.getColumnIndexOrThrow("priority"));
                    String dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                    todoDetail.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    todoDetail.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    todoDetail.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    todoDetail.setDueDate(dueDate);
                    todoDetail.setPriority(priority);
                    todoDetail.setType(cursor.getString(cursor.getColumnIndexOrThrow("todo_type")));
                    todoDetail.setLocation(cursor.getString(cursor.getColumnIndexOrThrow("location")));
                    todoDetail.setSubject(cursor.getString(cursor.getColumnIndexOrThrow("subject")));
                    todoDetail.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
                    todoDetail.setTag(cursor.getString(cursor.getColumnIndexOrThrow("label")));
                    todoDetail.setDdlDate(cursor.getString(cursor.getColumnIndexOrThrow("ddl")));
                    todoDetail.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow("status")));
                    todoDetail.setSubtask(cursor.getString(cursor.getColumnIndexOrThrow("subtask")));
                    todoDetail.setAttachment(cursor.getString(cursor.getColumnIndexOrThrow("attachment")));
                    todoDetail.setDoneDate(cursor.getString(cursor.getColumnIndexOrThrow("done_datetime")));
                    todoDetail.setFunction(TodoInfo.VIEW);

                    if (!titleAdapter.getTitle().equals(dueDate)) {
                        titleAdapter = new TitleAdapter(activity);
                        titleAdapter.setTitle(dueDate);
                        f.getConcatAdapter().addAdapter(titleAdapter);
                        todoAdapter = new TodoAdapter(activity, this);
                        f.getConcatAdapter().addAdapter(todoAdapter);
                    }
                    todoAdapter.add(todoDetail);
                }
            }
        } catch (Exception e) {
            Log.e("TodoActivity", "refresh: ", e);
        }
    }

    void loadListFromTable(String table, List<String> target) {
        SQLiteDatabase db = todoDB.getWritableDatabase();
        try (Cursor cursor = db.query(table, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                target.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
        } catch (Exception ignored) {
        }
    }

    private void createFilterChip(String s, ChipGroup view, int toAddCode) {
        Chip chip = ItemFilterChipBinding.inflate(activity.getLayoutInflater(), view, false).getRoot();
        chip.setText(s);

        chip.setOnLongClickListener(v -> {
            if (chip.isChecked()) {
                switch (toAddCode) {
                    case 0:
                        todoInfo.setType(null);
                        break;
                    case 1:
                        todoInfo.setSubject(null);
                        break;
                    case 2:
                        todoInfo.setTag(null);
                        break;
                }
            }
            view.removeView(chip);
            Arrays.asList(types, subjects, tags).get(toAddCode).remove(s);
            SQLiteDatabase db = todoDB.getWritableDatabase();
            db.delete(new String[]{"types", "subjects", "tags"}[toAddCode], "name=?", new String[]{s});
            return true;
        });

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) return;
            switch (toAddCode) {
                case 0:
                    todoInfo.setType(chip.getText().toString());
                    break;
                case 1:
                    todoInfo.setSubject(chip.getText().toString());
                    break;
                case 2:
                    todoInfo.setTag(chip.getText().toString());
                    break;
            }
        });
        view.addView(chip, view.getChildCount() - 1);
    }

    public void initDialog(TodoInfo todoInfo) {
        if (todoInfo == null) return;
        this.todoInfo.copyFrom(todoInfo);
    }

    public void initDialog() {
        if (todoInfo == null) todoInfo = new TodoInfo();

        todoInfo.getTitle().observe(activity, dialogBinding.title::setText);
        todoInfo.getDescription().observe(activity, dialogBinding.description::setText);
        todoInfo.getPriority().observe(activity, integer -> {
            int priority = checkValid(integer) ? integer : 0;
            dialogBinding.prioritySlider.setValue(priority);
            dialogBinding.priorityValue.setText(new String[]{"无", "不重要且不紧急", "不重要但紧急", "重要但不紧急", "重要且紧急"}[priority]);
        });
        todoInfo.getType().observe(activity, s -> {
            if (checkValid(s)) {
                if (!types.contains(s)) {
                    createFilterChip(s, dialogBinding.todoType, 0);
                    types.add(s);
                }
                selectChipIfPresent(dialogBinding.todoType, types, s);
            }
        });
        todoInfo.getSubject().observe(activity, s -> {
            if (checkValid(s)) {
                if (!subjects.contains(s)) {
                    createFilterChip(s, dialogBinding.subject, 1);
                    subjects.add(s);
                }
                selectChipIfPresent(dialogBinding.subject, subjects, s);
            }
        });
        todoInfo.getTag().observe(activity, s -> {
            if (checkValid(s)) {
                if (!tags.contains(s)) {
                    createFilterChip(s, dialogBinding.tag, 2);
                    tags.add(s);
                }
                selectChipIfPresent(dialogBinding.tag, tags, s);
            }
        });
        todoInfo.getStatus().observe(activity, integer -> dialogBinding.check.setChecked(integer != null && Objects.equals(integer, TodoInfo.DONE)));
        todoInfo.getDueDate().observe(activity, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(0)).itemContent.setText(checkValid(i) ? i : activity.getString(R.string.none)));
        todoInfo.getDueTime().observe(activity, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(1)).itemContent.setText(checkValid(i) ? i : activity.getString(R.string.none)));
        todoInfo.getRemindTime().observe(activity, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(2)).itemContent.setText(checkValid(i) ? i : activity.getString(R.string.none)));
        todoInfo.getDdlDate().observe(activity, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(3)).itemContent.setText(checkValid(i) ? i : activity.getString(R.string.none)));
    }

    public void showDialog() {
        todoDetailDialog.show();
    }

    public void showTodoAddDialog() {
        todoInfo.reset();
        todoInfo.setFunction(TodoInfo.ADD);
        showDialog();
    }

    boolean checkValid(String string) {
        return string != null && !string.isEmpty();
    }

    boolean checkValid(Integer integer) {
        return integer != null && integer != -1;
    }

    private void setupDialogWindow(AlertDialog dialog, boolean applyPadding) {
        if (dialog == null) return;
        dialog.setCanceledOnTouchOutside(false);
        Window window = Objects.requireNonNull(dialog.getWindow());
        if (applyPadding) {
            FrameLayout content = dialog.findViewById(android.R.id.content);
            if (content != null) content.setPadding(48, 48, 48, 0);
        }
        window.setGravity(Gravity.BOTTOM);
        window.setBackgroundDrawableResource(R.drawable.top_capsule);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
       // View decorView = window.getDecorView();

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        /*int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;*/
//        decorView.setSystemUiVisibility(flags);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private void selectChipIfPresent(ChipGroup group, List<String> list, String value) {
        if (value == null || list == null) return;
        int idx = list.indexOf(value);
        int childIndex = idx + 1; // 因为最后一个是添加按钮
        if (idx >= 0 && childIndex < group.getChildCount()) {
            Chip c = (Chip) group.getChildAt(childIndex);
            if (c != null) c.setChecked(true);
        }
    }

    public void updateTodo(TodoInfo item) {
        todoDB.updateTodo(item);
    }

    public int getCount() {
        return count;
    }
}
