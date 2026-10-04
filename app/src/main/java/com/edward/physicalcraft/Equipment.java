package com.edward.physicalcraft;

import java.util.concurrent.ConcurrentHashMap;

/** Starter equipment and non-hostile training targets. No network or model required. */
public final class Equipment {
 public static final int SWORD=0,PICKAXE=1;
 public volatile int selected=SWORD,crystals,swordLevel;
 public final ConcurrentHashMap<String,Integer> damage=new ConcurrentHashMap<>();
 private long lastSwing=-1000;
 public static double targetX(int mission,int i){return Physics.stationX(mission)+new double[]{-4,3,5}[i];}
 public static double targetZ(int mission,int i){return Physics.stationZ(mission)+new double[]{5,5,-3}[i];}
 public int health(int mission,int i){return Math.max(0,3-damage.getOrDefault(mission+":"+i,0));}
 public synchronized boolean upgrade(){if(crystals<3||swordLevel>=2)return false;crystals-=3;swordLevel++;return true;}
 public synchronized int strike(int mission,double x,double z,float yaw,long now){
  if(selected!=SWORD||now-lastSwing<450)return -2;lastSwing=now;
  int target=-1;double closest=3.3;
  for(int i=0;i<3;i++){double dx=targetX(mission,i)-x,dz=targetZ(mission,i)-z,d=Math.hypot(dx,dz);
   if(health(mission,i)>0&&d<closest&&(d<.5||(dx*Math.sin(yaw)-dz*Math.cos(yaw))/d>.25)){closest=d;target=i;}}
  if(target>=0){String key=mission+":"+target;int hits=Math.min(3,damage.getOrDefault(key,0)+1+swordLevel);damage.put(key,hits);if(hits==3)crystals++;}
  return target;
 }
}
