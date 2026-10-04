package com.edward.physicalcraft;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.util.concurrent.*;
import java.io.*;
import android.graphics.Bitmap;

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
   require(activity.world.thirdPerson,"Third-person camera must be default");
   runOnMainSync(()->{activity.start();activity.equip(Equipment.SWORD);});
   CountDownLatch positioned=new CountDownLatch(1);activity.world.queueEvent(()->{activity.world.px=Equipment.targetX(0,0);activity.world.pz=Equipment.targetZ(0,0)+2;activity.world.py=activity.world.floor(activity.world.px,activity.world.pz)+1.65;activity.world.yaw=0;positioned.countDown();});require(positioned.await(5,TimeUnit.SECONDS),"GL positioning");
   SystemClock.sleep(1500);
   Bitmap screen=getUiAutomation().takeScreenshot();try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"preview.png"))){require(screen!=null,"Screenshot");screen.compress(Bitmap.CompressFormat.PNG,100,out);}screen.recycle();
   for(int i=0;i<3;i++){runOnMainSync(()->activity.world.attack());SystemClock.sleep(550);}
   require(activity.world.equipment.health(0,0)==0&&activity.world.equipment.crystals==1,"Sword must hit target and reward exactly one crystal");
   runOnMainSync(()->{activity.inventory();activity.dismiss();activity.equip(Equipment.PICKAXE);activity.openExperiment();});waitForIdleSync();
   runOnMainSync(()->{activity.dismiss();activity.gemmaMenu();});waitForIdleSync();
   runOnMainSync(()->{activity.dismiss();activity.save();activity.finish();});waitForIdleSync();
   MainActivity reopened=(MainActivity)startActivitySync(launch);waitForIdleSync();
   require(reopened.world!=null&&reopened.hud!=null,"Activity failed to reopen");
   deadline=SystemClock.elapsedRealtime()+30000;
   while(reopened.world.renderedFrames<10&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
   require(reopened.world.renderedFrames>=10,"World failed after reopening");
   require(reopened.world.equipment.crystals==1&&reopened.world.equipment.selected==Equipment.PICKAXE&&reopened.world.equipment.health(0,0)==0,"Inventory and targets must survive restart");
   // Exercise Android's real downloader, checksum verification, runtime load and inference.
   reopened.download.start();deadline=SystemClock.elapsedRealtime()+720000;
   while(reopened.download.active()&&SystemClock.elapsedRealtime()<deadline){reopened.download.poll(reopened.director,()->{});SystemClock.sleep(1000);}
   require(reopened.director.ready(),"Model install failed: "+reopened.download.status);
   CountDownLatch generated=new CountDownLatch(1);String[] error={null};Physics.Mission[] output={null};
   while(reopened.director.busy)SystemClock.sleep(100);
   reopened.director.generate(reopened.mission,(m,e)->{output[0]=m;error[0]=e;generated.countDown();});
   require(generated.await(240,TimeUnit.SECONDS),"Local inference timed out");
   require(error[0]==null&&output[0]!=null&&output[0].success(output[0].solution),"Local AI blueprint invalid: "+error[0]);
   result.putString("stream","PHYSICALCRAFT_STARTUP_PASS: third-person rendering, sword hits, loot persistence, DownloadManager, SHA256, local Qwen inference, valid mission\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
 }
 private void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
