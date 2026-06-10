package com.sysu.edu.api;

import androidx.annotation.NonNull;

import java.util.LinkedList;

public class RequestQueue {
    final LinkedList<Runnable> queue = new LinkedList<>();
    Runnable lastRequest;
    
    public void add(@NonNull Runnable runnable) {
        queue.add(runnable);
    }
    
    public void next() {
        lastRequest = queue.poll();
        if (lastRequest != null) lastRequest.run();
    }
    
    public void addAndNext(@NonNull Runnable runnable) {
        add(runnable);
        next();
    }
    
    public void retry() {
        lastRequest.run();
    }
}
