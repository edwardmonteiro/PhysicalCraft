package com.edward.physicalcraft;

import android.app.*;
import android.content.Intent;
import android.os.*;

/** Runs the actual Activity and GL thread; a compiling APK alone is not a pass. */
public final class StartupTest extends Instrumentation {
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){Bundle result=new Bundle();
  try{
   Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   MainActivity activity=(MainActivity)startActivitySync(launch);waitForIdleSync();
   require(activity.world!=null&&activity.hud!=null,"Activity failed to initialize");
   long deadline=SystemClock.elapsedRealtime()+30000;
   while(activity.world.renderedFrames<30&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
   require(activity.world.graphicsReady&&activity.world.renderedFrames>=30,"World did not render 30 frames");
   runOnMainSync(()->{activity.start();activity.openExperiment();});waitForIdleSync();
   runOnMainSync(()->{activity.dismiss();activity.gemmaMenu();});waitForIdleSync();
   runOnMainSync(()->{activity.dismiss();activity.save();activity.finish();});waitForIdleSync();
   MainActivity reopened=(MainActivity)startActivitySync(launch);waitForIdleSync();
   require(reopened.world!=null&&reopened.hud!=null,"Activity failed to reopen");
   deadline=SystemClock.elapsedRealtime()+30000;
   while(reopened.world.renderedFrames<10&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
   require(reopened.world.renderedFrames>=10,"World failed after reopening");
   result.putString("stream","PHYSICALCRAFT_STARTUP_PASS: launch, GL rendering, experiment, Gemma menu, save and reopen\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
 }
 private void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
