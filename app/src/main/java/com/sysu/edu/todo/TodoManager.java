package com.sysu.edu.todo;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Build;
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
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.sysu.edu.R;
import com.sysu.edu.databinding.DialogTodoBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.databinding.ItemTodoBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.EditTextDialog;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TodoManager {

    final TodoHelper todoDB;
    final DialogTodoBinding dialogTodoBinding;
    final AlertDialog todoDetailDialog;
    final FragmentActivity activity;
    final ConcatAdapter concatAdapter;
    int toAddCode = 0;
    ArrayList<String> types;
    ArrayList<String> subjects;
    ArrayList<String> tags;
    TodoInfo todoInfo = new TodoInfo();
    int count;
    private OnRefreshListener listener;

    public TodoManager(FragmentActivity activity, ConcatAdapter concatAdapter) {
        this.concatAdapter = concatAdapter;
        this.activity = activity;
        todoDB = new TodoHelper(activity, 7);
        dialogTodoBinding = DialogTodoBinding.inflate(activity.getLayoutInflater());
        dialogTodoBinding.prioritySlider.addOnChangeListener((_, value, _) -> todoInfo.setPriority((int) value));
        dialogTodoBinding.priorityContainer.updateAppearance(0, 1);
        dialogTodoBinding.todoType.setOnCheckedStateChangeListener((_, checkedIds) -> todoInfo.setType(checkedIds.isEmpty() ? "" : types.get(checkedIds.indexOf(checkedIds.get(0)))));
        dialogTodoBinding.subject.setOnCheckedStateChangeListener((_, checkedIds) -> todoInfo.setSubject(checkedIds.isEmpty() ? "" : subjects.get(checkedIds.indexOf(checkedIds.get(0)))));
        dialogTodoBinding.tag.setOnCheckedStateChangeListener((_, checkedIds) -> todoInfo.setTag(checkedIds.isEmpty() ? "" : tags.get(checkedIds.indexOf(checkedIds.get(0)))));
        todoDetailDialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogTodoBinding.getRoot())
                .setPositiveButton(R.string.confirm, (_, _) -> {
                    todoInfo.setTitle(toStringOrDefault(dialogTodoBinding.title.getText()));
                    todoInfo.setDescription(toStringOrDefault(dialogTodoBinding.description.getText()));
                    if (todoInfo.getFunction() == TodoInfo.ADD) todoDB.addTodo(todoInfo);
                    else if (todoInfo.getFunction() == TodoInfo.VIEW) todoDB.updateTodo(todoInfo);
                    performRefresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, (_, _) -> {
                    if (todoInfo.getFunction() == TodoInfo.VIEW) {
                        todoDB.deleteTodo(todoInfo);
                        performRefresh();
                    }
                })
                .create();

        EditTextDialog todoItemAddDialog = new EditTextDialog(activity);
        todoItemAddDialog.getDialog().setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(R.string.confirm), (_, _) -> {
            String value = todoItemAddDialog.getValue();
            if (!isEmpty(value)) {
                ArrayList<String> array = List.of(types, subjects, tags).get(toAddCode);
                ChipGroup chipGroup = new ChipGroup[]{dialogTodoBinding.todoType, dialogTodoBinding.subject, dialogTodoBinding.tag}[toAddCode];
                if (!array.contains(value)) {
                    array.add(value);
                    createFilterChip(value, chipGroup, toAddCode);
                }
                ContentValues values = new ContentValues();
                values.put("name", value);
                todoDB.getWritableDatabase().insert(new String[]{"types", "subjects", "tags"}[toAddCode], null, values);
                selectChipIfPresent(chipGroup, array, value);
                switch (toAddCode) {
                    case 0 -> todoInfo.setType(value);
                    case 1 -> todoInfo.setSubject(value);
                    case 2 -> todoInfo.setTag(value);
                }
            }
        });

        NumberPicker numberPicker = new NumberPicker(activity);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(59);
        AlertDialog remindDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.custom_remind_title)
                .setView(numberPicker)
                .setPositiveButton(R.string.confirm, (_, _) -> todoInfo.setRemindTime(String.format(Locale.getDefault(), "%02d%s", numberPicker.getValue(), activity.getString(R.string.minute))))
                .setNegativeButton(R.string.cancel, null)
                .create();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialDatePicker<Long> ddlPicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build();
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder().build();

        // 共用对话框外观设置
        setupDialogWindow(todoDetailDialog);

        for (int i = 0; i < 3; i++) {
            int finalI = i;
            List.of(dialogTodoBinding.todoTypeAdd, dialogTodoBinding.todoSubjectAdd, dialogTodoBinding.todoTagAdd).get(i).setOnClickListener(_ -> {
                toAddCode = finalI;
                int hint = new int[]{R.string.type, R.string.subject, R.string.tag}[finalI];
                todoItemAddDialog.setHint(hint);
                todoItemAddDialog.setTitle(hint);
                todoItemAddDialog.show();
            });
        }

        ArrayList<PopupMenu> popupMenuArrayList = new ArrayList<>();
        SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 4; i++) {
            ItemPreferenceBinding itemPreferenceBinding = ItemPreferenceBinding.inflate(activity.getLayoutInflater(), dialogTodoBinding.times, false);
            itemPreferenceBinding.itemTitle.setText(activity.getString(new int[]{R.string.date, R.string.time, R.string.remind, R.string.ddl}[i]));
            itemPreferenceBinding.itemIcon.setImageResource(new int[]{R.drawable.calendar, R.drawable.time, R.drawable.alarm, R.drawable.warning}[i]);
            itemPreferenceBinding.getRoot().updateAppearance(i, 4);

            PopupMenu popupMenu = new PopupMenu(activity, itemPreferenceBinding.getRoot(), Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
            Menu menu = popupMenu.getMenu();

            MenuItem none = menu.add(0, Menu.NONE, Menu.NONE, R.string.none);

            switch (i) {
                case 0: // due date
                    datePicker.addOnPositiveButtonClickListener(selection -> todoInfo.setDueDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(selection), ZoneId.systemDefault()).format(formatter)));
                    for (int j = 0; j < 3; j++) {
                        int finalJ = j;
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.today, R.string.tomorrow, R.string.next_week}[j])
                                .setOnMenuItemClickListener(_ -> {
                                    todoInfo.setDueDate(LocalDate.now().plusDays(new int[]{0, 1, 7}[finalJ]).format(formatter));
                                    return true;
                                });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(_ -> {
                        datePicker.show(activity.getSupportFragmentManager(), "date_picker");
                        return true;
                    });
                    none.setOnMenuItemClickListener(_ -> {
                        todoInfo.getDueDate().setValue(null);
                        return true;
                    });
                    break;
                case 3: // ddl
                    for (int j = 0; j < 3; j++) {
                        int finalJ = j;
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.today, R.string.tomorrow, R.string.next_week}[j])
                                .setOnMenuItemClickListener(_ -> {
                                    todoInfo.setDdlDate(LocalDate.now().plusDays(new int[]{0, 1, 7}[finalJ]).format(formatter));
                                    return true;
                                });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(_ -> {
                        ddlPicker.show(activity.getSupportFragmentManager(), "ddl_picker");
                        return true;
                    });
                    ddlPicker.addOnPositiveButtonClickListener(selection -> todoInfo.getDdlDate().setValue(dateString.format(selection)));
                    none.setOnMenuItemClickListener(_ -> {
                        todoInfo.getDdlDate().setValue(null);
                        return true;
                    });
                    break;
                case 1: // time
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener(_ -> {
                        timePicker.show(activity.getSupportFragmentManager(), "time_picker");
                        return true;
                    });
                    timePicker.addOnPositiveButtonClickListener(_ -> todoInfo.setDueTime(String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute())));
                    break;
                case 2: // remind
                    for (int j = 0; j < 6; j++) {
                        menu.add(0, Menu.NONE, Menu.NONE, new int[]{R.string.on_time, R.string.five_mins, R.string.fifteen_mins, R.string.half_hour, R.string.one_hour, R.string.one_day}[j])
                                .setOnMenuItemClickListener(item -> {
                                    itemPreferenceBinding.itemContent.setText(item.getTitle());
                                    return true;
                                });
                    }
                    menu.add(1, Menu.NONE, Menu.NONE, R.string.custom).setOnMenuItemClickListener(_ -> {
                        remindDialog.show();
                        return true;
                    });
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) menu.setGroupDividerEnabled(true);
            popupMenuArrayList.add(popupMenu);
            int finalI1 = i;
            itemPreferenceBinding.getRoot().setOnClickListener(_ -> popupMenuArrayList.get(finalI1).show());
            dialogTodoBinding.times.addView(itemPreferenceBinding.getRoot());
        }

        dialogTodoBinding.check.setOnCheckedChangeListener((_, isChecked) -> todoInfo.setStatus(isChecked ? TodoInfo.DONE : TodoInfo.TODO));

        loadItemList("types", types = new ArrayList<>());
        loadItemList("subjects", subjects = new ArrayList<>());
        loadItemList("tags", tags = new ArrayList<>());

        types.forEach(s -> createFilterChip(s, dialogTodoBinding.todoType, 0));
        subjects.forEach(s -> createFilterChip(s, dialogTodoBinding.subject, 1));
        tags.forEach(s -> createFilterChip(s, dialogTodoBinding.tag, 2));

        initDialog();
        performRefresh();

    }

    public void performRefresh() {
        if (listener != null) listener.onRefresh();
    }

    /**
     * 过滤 TodoFragment 中的数据，根据状态
     *
     * @param status 状态值，TodoInfo.TODO 或 TodoInfo.DONE
     *
     */
    public void filterByStatus(int status) {
        refresh("status = ?", new String[]{String.valueOf(status)});
    }

    /**
     * 刷新 TodoFragment 中的所有数据
     *
     *
     */
    public void refresh() {
        refresh(null, null);
    }

    /**
     * 刷新 TodoFragment 中的所有数据
     *
     * @param where SQL 语句中的 WHERE 子句
     * @param args  WHERE 子句中的参数
     *
     */
    public void refresh(String where, String[] args) {
        // 清除已有 adapters
        concatAdapter.getAdapters().forEach(concatAdapter::removeAdapter);
        try {
//            TodoAdapter todoAdapter = new TodoAdapter(this);
//            TitleAdapter titleAdapter = new TitleAdapter();
            HashMap<String, TodoAdapter> dueMap = new HashMap<>();
            try (Cursor cursor = todoDB.getWritableDatabase().query("todos", null, where, args, null, null, "due_date")) {
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
//                    if (!titleAdapter.getTitle().equals(dueDate)) {
//                        titleAdapter = new TitleAdapter();
//                        titleAdapter.setTitle(dueDate);
//                        concatAdapter.addAdapter(titleAdapter);
//                        todoAdapter = new TodoAdapter(this);
//                        concatAdapter.addAdapter(todoAdapter);
//                    }

                    TodoAdapter todoAdapter = dueMap.getOrDefault(dueDate, null);
                    if (todoAdapter == null) {
                        concatAdapter.addAdapter(new TitleAdapter(isEmpty(dueDate) ? "无预定日期" : dueDate));
                        dueMap.put(dueDate, todoAdapter = new TodoAdapter(this));
                        concatAdapter.addAdapter(todoAdapter);
                    }
                    todoAdapter.add(todoDetail);
                    TodoAdapter finalTodoAdapter = todoAdapter;
                    todoAdapter.setListener(new AdapterListener() {
                        @Override
                        public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                            ItemTodoBinding binding = ItemTodoBinding.bind(holder.itemView);
                            binding.delete.setOnClickListener(_ -> {
                                todoDB.getWritableDatabase().delete("todos", "id=?", new String[]{String.valueOf(todoDetail.getId().getValue())});
                                performRefresh();
                            });
                            binding.getRoot().setOnClickListener(_ -> {
                                initDialog(finalTodoAdapter.get(position));
                                showDialog();
                            });
                            binding.copy.setOnClickListener(_ -> {
                                initDialog(finalTodoAdapter.get(position));
                                todoInfo.setFunction(TodoInfo.ADD);
                                showDialog();
                            });
                        }

                        @Override
                        public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {

                        }
                    });
                }
            }
        } catch (Exception _) {
        }
    }


    void loadItemList(String table, List<? super String> target) {
        try (Cursor cursor = todoDB.getWritableDatabase().query(table, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                target.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
        } catch (Exception _) {
        }
    }

    private void createFilterChip(String s, ChipGroup view, int toAddCode) {
        Chip chip = ItemFilterChipBinding.inflate(activity.getLayoutInflater(), view, false).getRoot();
        chip.setText(s);
        chip.setOnLongClickListener(v -> {
            Snackbar.make(v, R.string.delete_warning, Snackbar.LENGTH_LONG).setAction("删除", _ -> {
                if (chip.isChecked()) {
                    switch (toAddCode) {
                        case 0 -> todoInfo.setType(null);
                        case 1 -> todoInfo.setSubject(null);
                        case 2 -> todoInfo.setTag(null);
                    }
                }
                view.removeView(chip);
                List.of(types, subjects, tags).get(toAddCode).remove(s);
                todoDB.getWritableDatabase().delete(new String[]{"types", "subjects", "tags"}[toAddCode], "name=?", new String[]{s});
            }).show();
            return true;
        });

        chip.setOnCheckedChangeListener((_, isChecked) -> {
            if (isChecked) {
                switch (toAddCode) {
                    case 0 -> todoInfo.setType(chip.getText().toString());
                    case 1 -> todoInfo.setSubject(chip.getText().toString());
                    case 2 -> todoInfo.setTag(chip.getText().toString());
                }
            }
        });
        view.addView(chip, view.getChildCount() - 1);
    }

    public void initDialog(TodoInfo todoInfo) {
        if (todoInfo != null) this.todoInfo.copyFrom(todoInfo);
    }

    public void initDialog() {
        if (todoInfo == null) todoInfo = new TodoInfo();

        todoInfo.getTitle().observe(activity, dialogTodoBinding.title::setText);
        todoInfo.getDescription().observe(activity, dialogTodoBinding.description::setText);
        todoInfo.getPriority().observe(activity, integer -> {
            int priority = integer != null && integer != -1 ? integer : 0;
            dialogTodoBinding.prioritySlider.setValue(priority);
            dialogTodoBinding.priorityValue.setText(activity.getResources().getStringArray(R.array.priority)[priority]);
        });
        todoInfo.getType().observe(activity, s -> {
            if (!isEmpty(s)) {
                if (!types.contains(s)) {
                    createFilterChip(s, dialogTodoBinding.todoType, 0);
                    types.add(s);
                }
                selectChipIfPresent(dialogTodoBinding.todoType, types, s);
            }else{
                dialogTodoBinding.todoType.clearCheck();
            }
        });
        todoInfo.getSubject().observe(activity, s -> {
            if (!isEmpty(s)) {
                if (!subjects.contains(s)) {
                    createFilterChip(s, dialogTodoBinding.subject, 1);
                    subjects.add(s);
                }
                selectChipIfPresent(dialogTodoBinding.subject, subjects, s);
            }else{
                dialogTodoBinding.subject.clearCheck();
            }
        });
        todoInfo.getTag().observe(activity, s -> {
            if (!isEmpty(s)) {
                if (!tags.contains(s)) {
                    createFilterChip(s, dialogTodoBinding.tag, 2);
                    tags.add(s);
                }
                selectChipIfPresent(dialogTodoBinding.tag, tags, s);
            }else{
                dialogTodoBinding.tag.clearCheck();
            }
        });
//        todoInfo.getSubtask().observe(activity,s -> {
//            if (!isEmpty(s)) {
//                if (!tags.contains(s)) {
//                    createFilterChip(s, dialogTodoBinding.tag, 2);
//                    tags.add(s);
//                }
//                selectChipIfPresent(dialogTodoBinding.tag, tags, s);
//            }
//        } );
        todoInfo.getStatus().observe(activity, integer -> dialogTodoBinding.check.setChecked(integer != null && Objects.equals(integer, TodoInfo.DONE)));
        todoInfo.getDueDate().observe(activity, i -> ItemPreferenceBinding.bind(dialogTodoBinding.times.getChildAt(0)).itemContent.setText(!isEmpty(i) ? i : activity.getString(R.string.none)));
        todoInfo.getDueTime().observe(activity, i -> ItemPreferenceBinding.bind(dialogTodoBinding.times.getChildAt(1)).itemContent.setText(!isEmpty(i) ? i : activity.getString(R.string.none)));
        todoInfo.getRemindTime().observe(activity, i -> ItemPreferenceBinding.bind(dialogTodoBinding.times.getChildAt(2)).itemContent.setText(!isEmpty(i) ? i : activity.getString(R.string.none)));
        todoInfo.getDdlDate().observe(activity, i -> ItemPreferenceBinding.bind(dialogTodoBinding.times.getChildAt(3)).itemContent.setText(!isEmpty(i) ? i : activity.getString(R.string.none)));
    }

    public void showDialog() {
        todoDetailDialog.show();
    }

    public void showTodoAddDialog() {
        todoInfo.reset();
        todoInfo.setFunction(TodoInfo.ADD);
        showDialog();
    }

    public TodoInfo getTodoInfo() {
        return todoInfo;
    }


    private void setupDialogWindow(AlertDialog dialog) {
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false);
            Window window = Objects.requireNonNull(dialog.getWindow());
            FrameLayout content = dialog.findViewById(android.R.id.content);
            if (content != null) content.setPadding(48, 48, 48, 0);
//        window.setGravity(Gravity.BOTTOM);
//        window.setBackgroundDrawableResource(R.drawable.top_capsule);
//            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void selectChipIfPresent(ChipGroup group, List<String> list, String value) {
        if (value == null || list == null) return;
        int idx = list.indexOf(value);
        int childIndex = idx + 1; // 因为最后一个是添加按钮
        if (idx >= 0 && childIndex < group.getChildCount() && group.getChildAt(childIndex) instanceof Chip c)
            c.setChecked(true);
    }

    public void updateTodo(TodoInfo item) {
        todoDB.updateTodo(item);
    }

    public int getCount() {
        return count;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.listener = listener;
        listener.onRefresh();
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

}
