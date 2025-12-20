package com.sysu.edu.todo.info;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sysu.edu.databinding.ItemTodoBinding;
import com.sysu.edu.todo.TodoActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TodoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    Context context;
    ArrayList<TodoInfo> data = new ArrayList<>();

    public TodoAdapter(Context context) {
        super();
        this.context = context;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTodoBinding binding = ItemTodoBinding.inflate(LayoutInflater.from(context), parent, false);
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemTodoBinding binding = ItemTodoBinding.bind(holder.itemView);
        TodoInfo item = data.get(position);
        binding.title.setText(item.getTitle().getValue());
        binding.description.setText(item.getDescription().getValue());
       //binding.dueDate.setText(item.getDueDate());
        binding.getRoot().setOnClickListener(v -> {
            ((TodoActivity)context).initDialog(item);
            ((TodoActivity)context).showDialog();
        });
        boolean isCheck = item.getStatus().getValue() != null && item.getStatus().getValue() == 1;
        binding.check.setChecked(isCheck);
        binding.getRoot().setAlpha(isCheck ? 0.5f : 1f);
        binding.check.setOnCheckedChangeListener((buttonView, isChecked) -> item.setStatus(isChecked ? TodoInfo.DONE : TodoInfo.TODO));
        item.getStatus().observe((TodoActivity)context, status -> {
            boolean isChecked = status != null && status.equals(TodoInfo.DONE);
            binding.check.setChecked(isChecked);
            binding.title.setPaintFlags(isChecked ? binding.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.description.setPaintFlags(isChecked ? binding.description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : binding.description.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.menu.setEnabled(!isChecked);
            item.setDoneDate(isChecked ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) : null);
            ((TodoActivity) context).updateTodo(item);
        });
        //binding.dueDate.setText(item.get("due_date"));
    }

   /* public TodoInfo getTodoInfoAt(int position){
        return data.get(position);
    }*/
    public void add(TodoInfo item){
        data.add(item);
        notifyItemInserted(data.size()-1);
    }
    public void clear(){
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, tmp);
    }

    public void refreshAt(int position){
        notifyItemChanged(position);
    }
    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }
}
