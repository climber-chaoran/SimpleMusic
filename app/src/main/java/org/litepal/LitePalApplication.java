package org.litepal;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.litepal.exceptions.GlobalException;

/* loaded from: classes.dex */
public class LitePalApplication extends Application {
    public static Context sContext;

    public LitePalApplication() {
        Log.e("com.example.simplemusic","llllllllllllllll");
        sContext = this;
    }

    public static Context getContext() {

        Log.e("com.example.simplemusic","llllllllllllllll111111111111111111");
        Context context = sContext;
        if (context != null) {
            return context;
        }
        throw new GlobalException(GlobalException.APPLICATION_CONTEXT_IS_NULL);
    }
}
