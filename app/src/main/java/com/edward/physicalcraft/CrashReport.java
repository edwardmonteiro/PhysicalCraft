package com.edward.physicalcraft;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Local diagnostic only: never uploads logs or reads personal files. */
public final class CrashReport extends Application {
 @Override public void onCreate(){super.onCreate();
  Thread.UncaughtExceptionHandler system=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
   try(FileOutputStream out=openFileOutput("last-crash.txt",MODE_PRIVATE)){
    out.write(describe(error).getBytes(StandardCharsets.UTF_8));out.getFD().sync();
   }catch(Exception ignored){}
   if(system!=null)system.uncaughtException(thread,error);
   else{android.os.Process.killProcess(android.os.Process.myPid());System.exit(1);}
  });
 }
 static String describe(Throwable error){
  StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));
  String text="PhysicalCraft 0.2.0\n"+Build.MANUFACTURER+" "+Build.MODEL+" · Android "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\n"+trace;
  return text.substring(0,Math.min(12000,text.length()));
 }
 static String take(Context context){
  File file=new File(context.getFilesDir(),"last-crash.txt");if(!file.isFile())return null;
  try{String report=new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);file.delete();return report;}catch(IOException ignored){return null;}
 }
}
