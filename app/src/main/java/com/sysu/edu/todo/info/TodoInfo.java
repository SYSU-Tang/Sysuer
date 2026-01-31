package com.sysu.edu.todo.info;

import androidx.lifecycle.MutableLiveData;

public class TodoInfo {
    public final static Integer ADD = 0;
    public final static Integer VIEW = 1;
    public final static Integer TODO = 0;
    public final static Integer DONE = 1;
    public final static Integer DELETE = 2;
    final MutableLiveData<String> title;
    final MutableLiveData<String> description;
    final MutableLiveData<String> dueDate;
    final MutableLiveData<String> ddlDate;
    final MutableLiveData<String> dueTime;
    final MutableLiveData<String> remindTime;
    final MutableLiveData<String> type;
    final MutableLiveData<String> location;
    final MutableLiveData<String> subject;
    final MutableLiveData<Integer> priority;
    final MutableLiveData<String> subtask;
    final MutableLiveData<String> attachment;
    final MutableLiveData<String> doneDate;
    final MutableLiveData<Integer> status;
    final MutableLiveData<String> color;
    final MutableLiveData<String> tag;
    final MutableLiveData<Integer> id;
    int function = ADD;

    public TodoInfo() {
        title = new MutableLiveData<>();
        description = new MutableLiveData<>();
        dueDate = new MutableLiveData<>();
        ddlDate = new MutableLiveData<>();
        dueTime = new MutableLiveData<>();
        priority = new MutableLiveData<>(0);
        remindTime = new MutableLiveData<>();
        type = new MutableLiveData<>();
        location = new MutableLiveData<>();
        subject = new MutableLiveData<>();
        subtask = new MutableLiveData<>();
        attachment = new MutableLiveData<>();
        doneDate = new MutableLiveData<>();
        status = new MutableLiveData<>(0);
        color = new MutableLiveData<>();
        tag = new MutableLiveData<>();
        id = new MutableLiveData<>(0);
        reset();
    }

    public int getFunction() {
        return function;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public MutableLiveData<String> getDdlDate() {
        return ddlDate;
    }

    public void setDdlDate(String ddlDate) {
        this.ddlDate.setValue(ddlDate);
    }

    public MutableLiveData<String> getDueTime() {
        return dueTime;
    }

    public void setDueTime(String dueTime) {
        this.dueTime.setValue(dueTime);
    }

    public MutableLiveData<String> getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime.setValue(remindTime);
    }

    public MutableLiveData<String> getType() {
        return type;
    }

    public void setType(String type) {
        this.type.setValue(type);
    }

    public MutableLiveData<String> getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location.setValue(location);
    }

    public MutableLiveData<String> getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title.setValue(title);
    }

    public MutableLiveData<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description.setValue(description);
    }

    public MutableLiveData<String> getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate.setValue(dueDate);
    }

    public MutableLiveData<Integer> getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority.setValue(priority);
    }

    public MutableLiveData<String> getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject.setValue(subject);
    }

    public MutableLiveData<String> getSubtask() {
        return subtask;
    }

    public void setSubtask(String subtask) {
        this.subtask.setValue(subtask);
    }

    public MutableLiveData<String> getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment.setValue(attachment);
    }

    public MutableLiveData<Integer> getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status.setValue(status);
    }

    public MutableLiveData<String> getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color.setValue(color);
    }

    public MutableLiveData<String> getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag.setValue(tag);
    }

    public MutableLiveData<String> getDoneDate() {
        return doneDate;
    }

    public void setDoneDate(String doneDate) {
        this.doneDate.setValue(doneDate);
    }

    public MutableLiveData<Integer> getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id.setValue(id);
    }

    public void reset() {
        setTitle("");
        setDescription("");
        setDueDate("");
        setDdlDate("");
        setDueTime("");
        setPriority(0);
        setRemindTime("");
        setType("");
        setLocation("");
        setSubject("");
        setSubtask("");
        setAttachment("");
        setDoneDate("");
        setStatus(0);
        setColor("");
        setTag("");
        function = ADD;
    }

    public void copyFrom(TodoInfo todoInfo) {
        title.setValue(todoInfo.getTitle().getValue());
        description.setValue(todoInfo.getDescription().getValue());
        dueDate.setValue(todoInfo.getDueDate().getValue());
        ddlDate.setValue(todoInfo.getDdlDate().getValue());
        dueTime.setValue(todoInfo.getDueTime().getValue());
        priority.setValue(todoInfo.getPriority().getValue());
        remindTime.setValue(todoInfo.getRemindTime().getValue());
        type.setValue(todoInfo.getType().getValue());
        location.setValue(todoInfo.getLocation().getValue());
        subject.setValue(todoInfo.getSubject().getValue());
        subtask.setValue(todoInfo.getSubtask().getValue());
        attachment.setValue(todoInfo.getAttachment().getValue());
        doneDate.setValue(todoInfo.getDoneDate().getValue());
        status.setValue(todoInfo.getStatus().getValue());
        color.setValue(todoInfo.getColor().getValue());
        tag.setValue(todoInfo.getTag().getValue());
        id.setValue(todoInfo.getId().getValue());
        function = todoInfo.getFunction();
    }
}