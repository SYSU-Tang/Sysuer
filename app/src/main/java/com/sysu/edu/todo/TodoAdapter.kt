package com.sysu.edu.todo;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.databinding.ItemTodoBinding;
import com.sysu.edu.view.RecyclerAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TodoAdapter extends RecyclerAdapter<TodoInfo> {
    private final TodoManager todoManager;
    
    public TodoAdapter(TodoManager todoManager) {
        this.todoManager = todoManager;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemTodoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemTodoBinding binding = ItemTodoBinding.bind(holder.itemView);
        Context context = binding.getRoot().getContext();
        Resources resource = context.getResources();
        TodoInfo item = get(position);
        binding.title.setText(item.getTitle().getValue());
        String description = item.getDescription().getValue();
        if (isEmpty(description)) binding.description.setVisibility(View.GONE);
        else binding.description.setText(description);
        
        String type = item.getType().getValue();
        if (isEmpty(type)) binding.type.setVisibility(View.GONE);
        else binding.type.setText(type);
        StringBuilder content = new StringBuilder();
        Map.of(R.string.location, item.getLocation().getValue(), R.string.subject, item.getSubject().getValue(), R.string.ddl, item.getDdlDate().getValue(), R.string.remind, item.getRemindTime().getValue()).forEach((key, value) -> {
            if (!CommonUtil.isEmpty(value))
                content.append(String.format("%s:%s|", resource.getString(key), value));
        });
        Integer priority = item.getPriority().getValue();
        if (priority != null && priority > 0)
            content.append(resource.getStringArray(R.array.priority)[priority]);
        if (!isEmpty(content))
            binding.detailContent.setText(content.toString().substring(0, content.length() - 1));
        else binding.detailContent.setVisibility(View.GONE);
        binding.check.setOnCheckedChangeListener((_, isChecked) -> item.setStatus(isChecked ? TodoInfo.DONE : TodoInfo.TODO));
        binding.title.setAlpha(TodoInfo.DONE.equals(item.getStatus().getValue()) ? 0.5f : 1.0f);
        binding.description.setAlpha(TodoInfo.DONE.equals(item.getStatus().getValue()) ? 0.5f : 1.0f);
        item.getStatus().observe((FragmentActivity) context, status -> {
            boolean isCheck = TodoInfo.DONE.equals(status);
            binding.title.setAlpha(isCheck ? 0.5f : 1.0f);
            binding.description.setAlpha(isCheck ? 0.5f : 1.0f);
            binding.check.setChecked(isCheck);
            binding.title.setPaintFlags(isCheck ? binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.description.setPaintFlags(isCheck ? binding.description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.description.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
//            binding.menu.setEnabled(!isCheck);
            //binding.check.setChecked(isChecked);
//            binding.title.setPaintFlags(isChecked ? binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
//            binding.description.setPaintFlags(isChecked ? binding.description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.description.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
//            binding.menu.setEnabled(!isChecked);
            item.setDoneDate(isCheck ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            todoManager.updateTodo(item);
        });
        super.onBindViewHolder(holder, position);
        //binding.dueDate.setText(item.get("due_date"));
    }
    
    @Override
    public int getItemViewType(int position) {
        return 1;
    }
}
