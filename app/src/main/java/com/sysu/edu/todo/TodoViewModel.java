package com.sysu.edu.todo;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.sysu.edu.todo.info.TodoInfo;

public class TodoViewModel extends ViewModel {
    MutableLiveData<TodoInfo> todoItem = new MutableLiveData<>();

    public MutableLiveData<TodoInfo> getTodoItem() {
        return todoItem;
    }

    public void setTodoItem(TodoInfo todoItem) {
        getTodoItem().setValue(todoItem);
    }
}
