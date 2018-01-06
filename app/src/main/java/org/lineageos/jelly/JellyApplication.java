package org.lineageos.jelly;

import android.app.Application;
import android.support.v7.app.AppCompatDelegate;

public class JellyApp extends Application {

    static {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
