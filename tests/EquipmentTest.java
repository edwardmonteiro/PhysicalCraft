package com.edward.physicalcraft;
public final class EquipmentTest {
 public static void main(String[] args){
  Equipment e=new Equipment();double x=Equipment.targetX(0,0),z=Equipment.targetZ(0,0);
  check(e.selected==Equipment.SWORD,"starter sword");
  check(e.strike(0,x,z+8,0,1000)==-1,"out of reach");
  check(e.strike(0,x,z+2,(float)Math.PI,1500)==-1,"behind player");
  check(e.strike(0,x,z+2,0,2000)==0&&e.health(0,0)==2,"first hit");
  check(e.strike(0,x,z+2,0,2100)==-2&&e.health(0,0)==2,"cooldown");
  e.strike(0,x,z+2,0,2500);e.strike(0,x,z+2,0,3000);
  check(e.health(0,0)==0&&e.crystals==1,"reward");
  e.strike(0,x,z+2,0,3500);check(e.crystals==1,"no repeat reward");
  check(!e.upgrade(),"upgrade costs crystals");e.crystals=3;check(e.upgrade()&&e.crystals==0,"upgrade");
  e.selected=Equipment.PICKAXE;check(e.strike(1,Equipment.targetX(1,0),Equipment.targetZ(1,0)+2,0,4000)==-2,"pickaxe is not sword");
  e.selected=Equipment.SWORD;e.strike(1,Equipment.targetX(1,0),Equipment.targetZ(1,0)+2,0,4500);check(e.health(1,0)==1,"upgraded damage");
  System.out.println("PASS: sword reach, facing, cooldown, loot, tool selection and upgrades");
 }
 private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
