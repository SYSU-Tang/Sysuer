package com.sysu.edu.api;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileManager {
    
    public static String readAssets(Context context, String file) {
        StringBuilder jsJSON = new StringBuilder();
        try {
            InputStreamReader input = new InputStreamReader(context.getAssets().open(file));
            BufferedReader buffer = new BufferedReader(input);
            String line;
            while ((line = buffer.readLine()) != null) jsJSON.append(line);
            input.close();
            buffer.close();
        } catch (IOException _) {
        }
        return jsJSON.toString();
    }
}
