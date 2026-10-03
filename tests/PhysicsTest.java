package com.edward.physicalcraft;
import org.json.*;
public final class PhysicsTest {
 static int checks=0;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}static void near(double a,double b,String s){check(Math.abs(a-b)<1e-8,s);}
 public static void main(String[] args)throws Exception{
  near(Physics.evaluate(0,12,3),4,"Newton F/m");near(Physics.evaluate(1,19.62,1),2,"free fall");near(Physics.evaluate(2,9.81,1),9.81,"45 degree ballistic range");near(Physics.evaluate(3,5,1),Math.sqrt(98.1),"energy conservation");near(Physics.evaluate(4,3,1),60,"torque");near(Physics.evaluate(5,10,1),98.1,"buoyancy litres conversion");near(Physics.evaluate(6,12,6),2,"Ohm");near(Physics.evaluate(7,4.18,1),1,"thermal kJ conversion");near(Physics.evaluate(8,170,1),2,"wave");near(Physics.evaluate(9,20,1),20,"thin lens");near(Physics.evaluate(10,7000,1),Math.sqrt(398600./7000),"circular orbit");near(Physics.evaluate(11,.6,1),1.25,"Lorentz");near(Physics.evaluate(12,100,1),0,"photoelectric threshold");
  for(int i=0;i<260;i++)for(int seed=0;seed<20;seed++){Physics.Mission m=Physics.create(i,seed);check(m.type>=0&&m.type<13,"topic");check(m.success(m.solution),"reachable solution");check(!m.success(Double.NaN),"NaN");check(!m.success(m.max+1),"outside domain");check(Double.isFinite(m.value(m.min))&&Double.isFinite(m.value(m.max)),"finite endpoints");Physics.Mission restored=Blueprint.parse(Blueprint.json(m).toString(),i);near(m.target,restored.target,"save roundtrip");}
  Physics.Mission m=Physics.create(0,3);JSONObject j=Blueprint.json(m);j.put("title","A plataforma perdida").put("story","Uma plataforma antiga bloqueia a entrada da câmara. Ajuste seu motor para recuperar o arquivo.");Physics.Mission g=Blueprint.parse(j.toString(),0);check(g.source.equals("Gemma local")&&g.success(g.solution),"valid AI level");
  String[] bad={"{}","{\"version\":99}",j.toString().replace("\"type\":0","\"type\":12"),j.toString().replace("\"parameter\":5","\"parameter\":0")};
  for(int i=0;i<3;i++){boolean rejected=false;try{Blueprint.parse(bad[i],0);}catch(Exception e){rejected=true;}check(rejected,"reject malformed or locked topic");}
  j.put("solution_step",101);boolean rejected=false;try{Blueprint.parse(j.toString(),0);}catch(Exception e){rejected=true;}check(rejected,"unreachable solution rejected");
  j.put("solution_step",50).put("parameter",Double.MAX_VALUE);rejected=false;try{Blueprint.parse(j.toString(),0);}catch(Exception e){rejected=true;}check(rejected,"unbounded parameter rejected");
  for(int x=-128;x<128;x++)for(int z=-128;z<128;z++){double h=Physics.height(x,z);check(Double.isFinite(h)&&h>=3&&h<=14,"bounded continuous terrain");check(Math.abs(h-Physics.height(x+1,z))<=2,"walkable terrain slope");}
  System.out.println("PASS: "+checks+" checks; 5,200 reachable levels, persistence, invalid blueprint rejection, 13 reference equations, terrain boundaries.");
 }
}
