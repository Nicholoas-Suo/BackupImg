package tech.xiaosuo.com.bmob;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

/**
 * Created by wangshumin on 2017/12/19.
 */

public class BackUpApplication extends Application{

    private static final String TAG = "BackUpApplication";
    private static boolean mLoginSstatus = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG," wangsm BackUpApplication onCreate mLoginSstatus  " + mLoginSstatus);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG," wangsm BackUpApplication onTerminate");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG," wangsm BackUpApplication onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG," wangsm BackUpApplication onTrimMemory");
    }

    public static boolean ismLoginSstatus() {
        return mLoginSstatus;
    }

    public static void setmLoginSstatus(boolean mLoginSstatus) {
        BackUpApplication.mLoginSstatus = mLoginSstatus;
    }
}
