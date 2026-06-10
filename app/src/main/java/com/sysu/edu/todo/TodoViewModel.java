package com.sysu.edu.todo;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TodoViewModel extends ViewModel {
    final MutableLiveData<TodoInfo> todoItem = new MutableLiveData<>();
    
    public MutableLiveData<TodoInfo> getTodoItem() {
        return todoItem;
    }
    
    public void setTodoItem(TodoInfo todoItem) {
        getTodoItem().setValue(todoItem);
    }
}
