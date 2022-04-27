package br.alimsoft.customcrash;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import br.alimsoft.customcrash.CrashActivity;

public final class CrashHandler {

    public static final UncaughtExceptionHandler DEFAULT_UNCAUGHT_EXCEPTION_HANDLER = Thread.getDefaultUncaughtExceptionHandler();

    public static void init(Application app) {
        init(app, null);
    }

    public static void init(final Application app, final String crashDir) {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){

                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    try {
                        tryUncaughtException(thread, throwable);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        if (DEFAULT_UNCAUGHT_EXCEPTION_HANDLER != null)
                            DEFAULT_UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(thread, throwable);
                    }
                }

                private void tryUncaughtException(Thread thread, Throwable throwable) {
                    final String time = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy").format(new Date());
                    File crashFile = new File(TextUtils.isEmpty(crashDir) ? new File(app.getExternalFilesDir(null), "crash")
                                              : new File(crashDir), "crash_" + time + ".txt");

                    String versionName = "unknown";
                    long versionCode = 0;
                    try { 
                        PackageInfo packageInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                        versionName = packageInfo.versionName;
                        versionCode = Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode()
                            : packageInfo.versionCode;
                    } catch (PackageManager.NameNotFoundException ignored) {}

                    String fullStackTrace; {
                        StringWriter sw = new StringWriter(); 
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        fullStackTrace = sw.toString();
                        pw.close();
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("**************** Erro do App *******************\n");
                    sb.append("Hora do Crash             : ").append(time).append("\n");
                    sb.append("**************** Info do Aplicativo ************\n");
                    sb.append("Nome da Versão            : ").append(versionName).append("\n");
                    sb.append("Versão do Código          : ").append(versionCode).append("\n");
                    sb.append("**************** Info do Dispositivo ************\n");
                    //sb.append("Brand                     : ").append(Build.BRAND).append("\n");
                    sb.append("Fabricante                : ").append(Build.MANUFACTURER).append("\n");
                    sb.append("Modelo                    : ").append(Build.MODEL).append("\n");
                    sb.append("Id                        : ").append(Build.ID).append("\n");
                    sb.append("Produto                   : ").append(Build.PRODUCT).append("\n");
                    sb.append("**************** FIRMWARE *******************\n");
                    sb.append("SDK do Android            : ").append(Build.VERSION.SDK_INT).append("\n");
                    sb.append("Versão do Android         : ").append(Build.VERSION.RELEASE).append("\n");
                    sb.append("Incremento                : ").append(Build.VERSION.INCREMENTAL).append("\n");              
                    sb.append("**************** ======== *******************\n");
                    sb.append("\n").append(fullStackTrace);
                  

                    String errorLog = sb.toString();

                    try {
                        writeFile(crashFile, errorLog);
                    } catch (IOException ignored) {}

                    gotoCrashActiviy: {
                        Intent intent = new Intent(app, CrashActivity.class);
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );
                        intent.putExtra(CrashActivity.EXTRA_CRASH_INFO, errorLog);
                        try {
                            app.startActivity(intent);
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(0);
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                            if (DEFAULT_UNCAUGHT_EXCEPTION_HANDLER != null)
                                DEFAULT_UNCAUGHT_EXCEPTION_HANDLER.uncaughtException(thread, throwable);
                        }
                    }

                }

                private void writeFile(File file, String content) throws IOException {
                    File parentFile = file.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(content.getBytes());
                    try {
                        fos.close();
                    } catch (IOException e) {}
                }

            });
    }

}

