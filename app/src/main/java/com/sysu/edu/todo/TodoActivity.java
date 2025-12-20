package com.sysu.edu.todo;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
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
import com.sysu.edu.databinding.DialogEditTextBinding;
import com.sysu.edu.databinding.DialogTodoBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.todo.info.TitleAdapter;
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
    DialogTodoBinding dialogBinding;
    ArrayList<String> types;
    ArrayList<String> subjects;
    ArrayList<String> tags;
    AlertDialog todoDialog;
    TodoInfo todoInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTodoBinding binding = ActivityTodoBinding.inflate(getLayoutInflater());
        dialogBinding = DialogTodoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(TodoViewModel.class);
        todoDB = new TodoList(this, 5);
        binding.tool.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        dialogBinding.prioritySlider.addOnChangeListener((slider, value, fromUser) -> todoInfo.setPriority((int) value));
        todoDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.confirm, (dialog1, which) -> {
                    todoInfo.setTitle(Objects.requireNonNull(dialogBinding.title.getText()).toString());
                    todoInfo.setDescription(Objects.requireNonNull(dialogBinding.description.getText()).toString());
                    if (todoInfo.getFunction() == TodoInfo.ADD) {
                        todoDB.addTodo(todoInfo);
                    } else if (todoInfo.getFunction() == TodoInfo.VIEW) {
                        todoDB.updateTodo(todoInfo);
                    }
                    refresh(binding.fragmentTodo.getFragment());
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (dialog13, which) -> {
                })
                .create();
        DialogEditTextBinding itemEditTextBinding = DialogEditTextBinding.inflate(getLayoutInflater());
        itemEditTextBinding.getRoot().setHint(R.string.type);
        AlertDialog todo_add_dialog = new MaterialAlertDialogBuilder(this)
                .setView(itemEditTextBinding.getRoot())
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    Editable toAdd = itemEditTextBinding.edit.getText();
                    if (toAdd != null && !toAdd.toString().isEmpty()) {
                        ArrayList<String> array = List.of(types, subjects, tags).get(toAddCode);
                        ChipGroup chipGroup = (new ChipGroup[]{dialogBinding.todoType, dialogBinding.subject, dialogBinding.tag}[toAddCode]);
                        if (!array.contains(toAdd.toString())) {
                            array.add(toAdd.toString());
                            createFilterChip(toAdd.toString(), chipGroup, toAddCode);
                        }
                        ContentValues values = new ContentValues();
                        values.put("name", toAdd.toString());
                        todoDB.getWritableDatabase().insertOrThrow(new String[]{"types", "subjects", "tags"}[toAddCode], null, values);
                        ((Chip) chipGroup.getChildAt(array.indexOf(toAdd.toString()) + 1)).setChecked(true);
                        if (toAddCode == 0) {
                            todoInfo.setType(toAdd.toString());
                        } else if (toAddCode == 1) {
                            todoInfo.setSubject(toAdd.toString());
                        } else if (toAddCode == 2) {
                            todoInfo.setTag(toAdd.toString());
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
                .setPositiveButton(R.string.confirm, (dialog, which) -> todoInfo.setRemindTime(String.format(Locale.getDefault(), "%02d%s", numberPicker.getValue(), getString(R.string.minute))))
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
        binding.add.setOnClickListener(view -> {
            todoInfo.reset();
            todoInfo.setFunction(TodoInfo.ADD);
            todoDialog.show();
        });
        //binding.fragment_todo.getFragment.setAdapter(new TodoAdapter(this));
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialDatePicker<Long> ddlPicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder().build();
        IntStream.range(0, 3).forEach(i -> List.of(dialogBinding.todoTypeAdd, dialogBinding.todoSubjectAdd, dialogBinding.todoTagAdd).get(i).setOnClickListener(view -> {
            toAddCode = i;
            itemEditTextBinding.getRoot().setHint(new int[]{R.string.type, R.string.subject, R.string.tag}[i]);
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
                    ddlPicker.addOnPositiveButtonClickListener(selection -> todoInfo.getDdlDate().setValue(dateString.format(selection)));
                    none.setOnMenuItemClickListener(item -> {
                        todoInfo.getDdlDate().setValue(null);
                        return true;
                    });
                    break;
                case 1:
                   /* for (int j = 0; j < 4; j++) {
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{}[j]).setOnMenuItemClickListener(item -> {
                            todoInfo.setTime(String.valueOf(item.getTitle()));
                            return true;
                        });
                    }*/
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(item -> {
                        timePicker.show(getSupportFragmentManager(), "time_picker");
                        return true;
                    });
                    timePicker.addOnPositiveButtonClickListener(selection -> todoInfo.setDueTime(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute())));
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
        getTypes();
        getSubjects();
        getTags();
        types.forEach(s -> createFilterChip(s, dialogBinding.todoType, 0));
        subjects.forEach(s -> createFilterChip(s, dialogBinding.subject, 1));
        tags.forEach(s -> createFilterChip(s, dialogBinding.tag, 2));
        initDialog();
        refresh(binding.fragmentTodo.getFragment());
    }
    public void updateTodo(TodoInfo todoInfo) {
        todoDB.updateTodo(todoInfo);
    }

    private void refresh(TodoFragment f) {
        f.getConcatAdapter().getAdapters().forEach(adp ->
        {
           //System.out.println(adp);
           f.getConcatAdapter().removeAdapter(adp);
        });
        // f.getConcatAdapter().
        // titleAdp.addTitle("0");
        try {
            TodoAdapter todoAdapter = new TodoAdapter(this);
            TitleAdapter titleAdapter = new TitleAdapter(this);
            Cursor cursor = todoDB.getWritableDatabase().query("todos", null, null, null, null, null, "due_date");
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
                //todoDetail.setDueTime(cursor.getString(cursor.getColumnIndexOrThrow("due_time")));
                //todoDetail.getStatus().observe(this, status -> {todoDB.updateTodo(todoDetail);});

                if (!titleAdapter.getTitle().equals(dueDate)) {
                    titleAdapter = new TitleAdapter(this);
                    titleAdapter.setTitle(dueDate);
                    f.getConcatAdapter().addAdapter(titleAdapter);
                    todoAdapter = new TodoAdapter(this);
                    f.getConcatAdapter().addAdapter(todoAdapter);
                }
                todoAdapter.add(todoDetail);
                //f.getConcatAdapter().notifyItemRangeInserted(f.getConcatAdapter().getItemCount() - 1, 1);
            }
            cursor.close();
            todoDB.close();
        } catch (Exception e) {
            Log.e("TodoActivity", "refresh: ", e);
        }
    }

    void getTypes() {
        types = new ArrayList<>();
        try {
            Cursor cursor = todoDB.getWritableDatabase().query("types", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                types.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        } catch (Exception ignored) {}
    }

    void getSubjects() {
        subjects = new ArrayList<>();
        try {
            Cursor cursor = todoDB.getWritableDatabase().query("subjects", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                subjects.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        } catch (Exception ignored) {
        }
    }

    private void createFilterChip(String s, ChipGroup view, int toAddCode) {
        ItemFilterChipBinding chip = ItemFilterChipBinding.inflate(getLayoutInflater(), view, false);
        chip.getRoot().setText(s);
        chip.getRoot().setOnLongClickListener(v -> {
            if (((Chip) v).isChecked()) {
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
            view.removeView(chip.getRoot());
            List.of(types, subjects, tags).get(toAddCode).remove(s);
            todoDB.getWritableDatabase().delete(new String[]{"types", "subjects", "tags"}[toAddCode], "name=?", new String[]{s});
            return true;
        });
        view.addView(chip.getRoot(), view.getChildCount() - 1);
    }

    void getTags() {
        tags = new ArrayList<>();
        try {
            Cursor cursor = todoDB.getWritableDatabase().query("tags", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                tags.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
            cursor.close();
        } catch (Exception ignored) {
        }
    }

    public void initDialog(TodoInfo todoInfo) {
        this.todoInfo.copyFrom(todoInfo);
    }

    public void initDialog() {
        todoInfo = todoInfo == null ? new TodoInfo() : todoInfo;
        todoInfo.getTitle().observe(this, dialogBinding.title::setText);
        todoInfo.getDescription().observe(this, dialogBinding.description::setText);
        todoInfo.getPriority().observe(this, integer -> {
            int priority = checkValid(integer) ? integer : 0;
            dialogBinding.prioritySlider.setValue(priority);
            dialogBinding.priorityValue.setText(new String[]{"无", "不重要且不紧急", "不重要但紧急", "重要但不紧急", "重要且紧急"}[priority]);
        });
        todoInfo.getType().observe(this, s -> {
            if (checkValid(s)) {
                if (!types.contains(s)) {
                    createFilterChip(s, dialogBinding.todoType, 0);
                    types.add(s);
                }
            } else {
                return;
            }
            ((Chip) dialogBinding.todoType.getChildAt(types.indexOf(todoInfo.getType().getValue()) + 1)).setChecked(true);
        });
        todoInfo.getSubject().observe(this, s -> {
            if (checkValid(s)) {
                if (!subjects.contains(s)) {
                    createFilterChip(s, dialogBinding.subject, 1);
                    subjects.add(s);
                }
            } else {
                return;
            }
            ((Chip) dialogBinding.subject.getChildAt(subjects.indexOf(todoInfo.getSubject().getValue()) + 1)).setChecked(true);
        });
        todoInfo.getTag().observe(this, s -> {
            if (checkValid(s)) {
                if (!tags.contains(s)) {
                    createFilterChip(s, dialogBinding.tag, 2);
                    tags.add(s);
                }
            }
            ((Chip) dialogBinding.tag.getChildAt(tags.indexOf(s) + 1)).setChecked(true);
        });
        todoInfo.getStatus().observe(this, integer -> dialogBinding.check.setChecked(integer != null && Objects.equals(integer, TodoInfo.DONE)));
        todoInfo.getDueDate().observe(this, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(0)).itemContent.setText(checkValid(i) ? i : getString(R.string.none)));
        todoInfo.getDueTime().observe(this, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(1)).itemContent.setText(checkValid(i) ? i : getString(R.string.none)));
        todoInfo.getRemindTime().observe(this, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(2)).itemContent.setText(checkValid(i) ? i : getString(R.string.none)));
        todoInfo.getDdlDate().observe(this, i -> ItemPreferenceBinding.bind(dialogBinding.times.getChildAt(3)).itemContent.setText(checkValid(i) ? i : getString(R.string.none)));
    }

    public void showDialog() {
        todoDialog.show();
    }

    boolean checkValid(String string) {
        return string != null && !string.isEmpty();
    }

    boolean checkValid(Integer integer) {
        return integer != null && integer != -1;
    }
}

