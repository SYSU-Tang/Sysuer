package com.sysu.edu.todo.info;

import androidx.lifecycle.MutableLiveData;

public class TodoInfo {
    MutableLiveData<String> title;
    MutableLiveData<String> description;
    MutableLiveData<String> dueDate;
    MutableLiveData<String> ddlDate;
    MutableLiveData<String> time;
    MutableLiveData<String> remindTime;
    MutableLiveData<String> type;
    MutableLiveData<String> location;
    MutableLiveData<String> subject;
    MutableLiveData<Integer> priority;
    MutableLiveData<String> subtask;
    MutableLiveData<String> attachment;
    MutableLiveData<String> doneDate;
    MutableLiveData<Integer> status;
    MutableLiveData<String> color;
    MutableLiveData<String> label;

    public TodoInfo() {
        title = new MutableLiveData<>();
        description = new MutableLiveData<>();
        dueDate = new MutableLiveData<>();
        ddlDate = new MutableLiveData<>();
        time = new MutableLiveData<>();
        priority = new MutableLiveData<>();
        remindTime = new MutableLiveData<>();
        type = new MutableLiveData<>();
        location = new MutableLiveData<>();
        subject = new MutableLiveData<>();
        subtask = new MutableLiveData<>();
        attachment = new MutableLiveData<>();
        doneDate = new MutableLiveData<>();
        status = new MutableLiveData<>();
        color = new MutableLiveData<>();
        label = new MutableLiveData<>();
    }

    public MutableLiveData<String> getDdlDate() {
        return ddlDate;
    }

    public void setDdlDate(String ddlDate) {
        this.ddlDate.setValue(ddlDate);
    }

    public MutableLiveData<String> getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time.setValue(time);
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

    public MutableLiveData<String> getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label.setValue(label);
    }

    public MutableLiveData<String> getDoneDate() {
        return doneDate;
    }

    public void setDoneDate(String doneDate) {
        this.doneDate.setValue(doneDate);
    }
}
