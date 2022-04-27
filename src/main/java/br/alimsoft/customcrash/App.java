package br.alimsoft.customcrash;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class App extends Application {
    
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    
	@Override
	public void onCreate() {
       CrashHandler.init(this);
	}
    
    public static Context getContext() {
        if (mContext == null) {
            mContext = new App();
        }
        return mContext;
    }

}
