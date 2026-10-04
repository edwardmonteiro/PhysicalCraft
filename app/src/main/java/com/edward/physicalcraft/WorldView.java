package com.edward.physicalcraft;
import android.content.Context;
import android.opengl.*;
import android.os.SystemClock;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Small native renderer, without a third-party game engine. All vertices are generated locally. */
public final class WorldView extends GLSurfaceView implements GLSurfaceView.Renderer {
 public final Equipment equipment=new Equipment(); public volatile boolean thirdPerson=true; public volatile float cameraDistance=6.2f; public volatile String combatMessage="Espada e picareta equipadas. Os sentinelas dourados são alvos de treino."; private long swingAt=-1000,hitAt=-1000; private int hitTarget=-1; private float walk;
 public volatile double px=4,pz=9,py=10; public volatile float yaw=1.9f,pitch=-.1f,moveX,moveZ; public volatile boolean paused=true;
 public volatile int missionIndex=0,fps=0,environment=0; public volatile float jump=0; public final ConcurrentHashMap<String,Integer> edits=new ConcurrentHashMap<>();
 private final LinkedHashMap<Long,Mesh> chunks=new LinkedHashMap<>(); private final Set<Long> pending=new HashSet<>();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final ConcurrentLinkedQueue<Result> ready=new ConcurrentLinkedQueue<>();private final Object editLock=new Object();
 private float vertical;private long last,clock;private int frames,program,mvpLoc,originLoc,posLoc,colLoc,modelLoc,tintLoc;private final float[] proj=new float[16],view=new float[16],mvp=new float[16];private Mesh beacon,cube;private final float[] model=new float[16],rig=new float[16];
 private int generation=0;private volatile boolean destroyed;public volatile boolean graphicsReady;public volatile int renderedFrames;
 private static final class Mesh{FloatBuffer data;int count;Mesh(float[] f){data=ByteBuffer.allocateDirect(f.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();data.put(f).position(0);count=f.length/6;}}
 private static final class Result{long key;int generation;Mesh mesh;Result(long k,int g,Mesh m){key=k;generation=g;mesh=m;}}
 private static final class Builder{float[] f=new float[8192];int n;void v(float x,float y,float z,float r,float g,float b){if(n+6>=f.length)f=Arrays.copyOf(f,f.length*2);f[n++]=x;f[n++]=y;f[n++]=z;f[n++]=r;f[n++]=g;f[n++]=b;}void quad(float[] p,float r,float g,float b){int[] ix={0,1,2,0,2,3};for(int i:ix)v(p[i*3],p[i*3+1],p[i*3+2],r,g,b);}Mesh done(){return new Mesh(Arrays.copyOf(f,n));}}
 public WorldView(Context c){super(c);setEGLContextClientVersion(2);setEGLConfigChooser((egl,display)->{
  // Minimum channel sizes accept both RGB and RGBA surfaces. Some phone drivers
  // expose only RGBA8888, which the previous exact alpha=0 chooser rejected.
  for(int bits:new int[]{8,5}){int[] attrs={0x3024,bits,0x3023,bits==5?6:8,0x3022,bits,0x3025,16,0x3040,4,0x3033,4,0x3038};int[] count={0};
   if(egl.eglChooseConfig(display,attrs,null,0,count)&&count[0]>0){EGLConfig[] configs=new EGLConfig[count[0]];if(egl.eglChooseConfig(display,attrs,configs,configs.length,count))return configs[0];}}
  throw new IllegalStateException("Nenhuma configuração OpenGL ES 2 compatível.");
 });setRenderer(this);setPreserveEGLContextOnPause(true);}
 private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
 public double floor(double x,double z){int ix=(int)Math.floor(x),iz=(int)Math.floor(z);return Physics.height(ix,iz)+edits.getOrDefault(ix+","+iz,0);}
 public void restore(double x,double z,float a,float p){px=x;pz=z;yaw=a;pitch=p;py=floor(x,z)+1.7;}
 public void camp(){queueEvent(()->{double x=Physics.stationX(missionIndex),z=Physics.stationZ(missionIndex);px=x-9;pz=z+8;py=floor(px,pz)+1.7;yaw=(float)Math.atan2(x-px,-(z-pz));pitch=-.06f;vertical=0;moveX=moveZ=0;});}
 public double distance(){return Math.hypot(px-Physics.stationX(missionIndex),pz-Physics.stationZ(missionIndex));}
 public void edit(boolean place){queueEvent(()->{if(thirdPerson){int ix=(int)Math.floor(px+Math.sin(yaw)*2.2),iz=(int)Math.floor(pz-Math.cos(yaw)*2.2);String k=ix+","+iz;int v=Math.max(-4,Math.min(6,edits.getOrDefault(k,0)+(place?1:-1)));edits.put(k,v);generation++;chunks.clear();pending.clear();return;}double dx=Math.sin(yaw)*Math.cos(pitch),dy=Math.sin(pitch),dz=-Math.cos(yaw)*Math.cos(pitch);for(double t=.6;t<7;t+=.08){double x=px+t*dx,y=py+t*dy,z=pz+t*dz;if(y<=floor(x,z)){int ix=(int)Math.floor(x),iz=(int)Math.floor(z);if(Math.hypot(ix+.5-px,iz+.5-pz)<1.1)return;String k=ix+","+iz;int old=edits.getOrDefault(k,0);int v=Math.max(-4,Math.min(6,old+(place?1:-1)));edits.put(k,v);generation++;chunks.clear();pending.clear();return;}}});}
 @Override public void onSurfaceCreated(GL10 gl,EGLConfig conf){
  graphicsReady=false;
  try{
  String vertex="uniform mat4 uMVP; uniform mat4 uModel; uniform vec3 uOrigin; attribute vec3 aPosition; attribute vec3 aColor; varying mediump vec3 vColor; varying mediump float vDistance; void main(){vec3 p=(uModel*vec4(aPosition,1.0)).xyz+uOrigin;gl_Position=uMVP*vec4(p,1.0);vColor=aColor;vDistance=length(p);}";
  String fragment="precision mediump float;varying mediump vec3 vColor;varying mediump float vDistance;uniform vec3 uTint;void main(){float fog=smoothstep(30.0,66.0,vDistance);gl_FragColor=vec4(mix(vColor*uTint,vec3(0.55,0.73,0.77),fog),1.0);}";
  int vs=shader(GLES20.GL_VERTEX_SHADER,vertex),fs=shader(GLES20.GL_FRAGMENT_SHADER,fragment);program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vs);GLES20.glAttachShader(program,fs);GLES20.glLinkProgram(program);int[] ok={0};GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));GLES20.glDeleteShader(vs);GLES20.glDeleteShader(fs);
  tintLoc=GLES20.glGetUniformLocation(program,"uTint");modelLoc=GLES20.glGetUniformLocation(program,"uModel");mvpLoc=GLES20.glGetUniformLocation(program,"uMVP");originLoc=GLES20.glGetUniformLocation(program,"uOrigin");posLoc=GLES20.glGetAttribLocation(program,"aPosition");colLoc=GLES20.glGetAttribLocation(program,"aColor");GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDisable(GLES20.GL_CULL_FACE);GLES20.glClearColor(.55f,.73f,.77f,1);last=SystemClock.elapsedRealtime();clock=last;beacon=beacon();Builder unit=new Builder();box(unit,-.5f,-.5f,-.5f,1,1,1,1,1,1);cube=unit.done();
  graphicsReady=true;
  }catch(RuntimeException error){android.util.Log.e("PhysicalCraft","Graphics startup failed",error);post(()->((MainActivity)getContext()).startupFailure(error));}
 }
 private int shader(int kind,String source){int s=GLES20.glCreateShader(kind);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[] ok={0};GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
 @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,64,(float)w/h,.08f,100);}
 @Override public void onDrawFrame(GL10 gl){
  if(!graphicsReady||destroyed)return;
  long now=SystemClock.elapsedRealtime();float dt=Math.min(.045f,(now-last)/1000f);last=now;frames++;if(now-clock>1000){fps=frames;frames=0;clock=now;}
  if(!paused){walk+=dt*Math.min(1,Math.hypot(moveX,moveZ))*10;double speed=4.3*dt;double nx=px+(Math.sin(yaw)*moveZ+Math.cos(yaw)*moveX)*speed,nz=pz+(-Math.cos(yaw)*moveZ+Math.sin(yaw)*moveX)*speed;double feet=py-1.65;
   if(floor(nx,pz)<=feet+1.05)px=nx;if(floor(px,nz)<=feet+1.05)pz=nz;
   double ground=floor(px,pz)+1.65;if(jump>0&&py<=ground+.12){vertical=6.4f;}jump=0;vertical-=9.81f*dt;py+=vertical*dt;if(py<ground){py=ground;vertical=0;}
  }
  int cx=(int)Math.floor(px/16),cz=(int)Math.floor(pz/16);Result result;while((result=ready.poll())!=null){pending.remove(result.key);if(result.generation==generation)chunks.put(result.key,result.mesh);}
  Iterator<Map.Entry<Long,Mesh>> it=chunks.entrySet().iterator();while(it.hasNext()){long k=it.next().getKey();int x=(int)(k>>32),z=(int)k;if(Math.abs(x-cx)>4||Math.abs(z-cz)>4)it.remove();}
  if(!destroyed&&pending.size()<3){outer:for(int r=0;r<=3;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;int xx=cx+x,zz=cz+z;long k=key(xx,zz);if(!chunks.containsKey(k)&&!pending.contains(k)){pending.add(k);int gen=generation;worker.execute(()->{if(!destroyed)ready.add(new Result(k,gen,terrain(xx,zz)));});break outer;}}}
  GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);if(thirdPerson){
   float elevation=Math.max(.20f,Math.min(1.15f,.48f-pitch*.65f));
   float ex=(float)(-Math.sin(yaw)*Math.cos(elevation)*cameraDistance),ez=(float)(Math.cos(yaw)*Math.cos(elevation)*cameraDistance),ey=(float)(Math.sin(elevation)*cameraDistance);
   // Pull the camera towards the player before it enters a hillside.
   for(float t=.12f;t<=1;t+=.04f){if(py+ey*t<floor(px+ex*t,pz+ez*t)+.45){float clear=Math.max(.12f,t-.06f);ex*=clear;ey*=clear;ez*=clear;break;}}
   ey=Math.max(ey,(float)(floor(px+ex,pz+ez)+.45-py));
   Matrix.setLookAtM(view,0,ex,ey,ez,0,-.35f,0,0,1,0);
  }else{Matrix.setLookAtM(view,0,0,0,0,(float)(Math.sin(yaw)*Math.cos(pitch)),(float)Math.sin(pitch),(float)(-Math.cos(yaw)*Math.cos(pitch)),0,1,0);}Matrix.multiplyMM(mvp,0,proj,0,view,0);GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0);
  GLES20.glEnableVertexAttribArray(posLoc);GLES20.glEnableVertexAttribArray(colLoc);
  for(Map.Entry<Long,Mesh> e:chunks.entrySet()){int x=(int)(e.getKey()>>32),z=(int)(long)e.getKey();double dx=x*16.0+8-px,dz=z*16.0+8-pz;double forward=dx*Math.sin(yaw)-dz*Math.cos(yaw);if(forward < -18)continue;draw(e.getValue(),(float)(x*16.0-px),(float)-py,(float)(z*16.0-pz));}
  double sx=Physics.stationX(missionIndex),sz=Physics.stationZ(missionIndex);draw(beacon,(float)(sx-px),(float)(floor(sx,sz)-py),(float)(sz-pz));
  drawTargets(now);if(thirdPerson)drawAvatar(now);
  GLES20.glDisableVertexAttribArray(posLoc);GLES20.glDisableVertexAttribArray(colLoc);
  renderedFrames++;
 }
 private void draw(Mesh mesh,float x,float y,float z){Matrix.setIdentityM(model,0);GLES20.glUniformMatrix4fv(modelLoc,1,false,model,0);GLES20.glUniform3f(tintLoc,1,1,1);drawRaw(mesh,x,y,z);}
 private void drawRaw(Mesh mesh,float x,float y,float z){GLES20.glUniform3f(originLoc,x,y,z);mesh.data.position(0);GLES20.glVertexAttribPointer(posLoc,3,GLES20.GL_FLOAT,false,24,mesh.data);mesh.data.position(3);GLES20.glVertexAttribPointer(colLoc,3,GLES20.GL_FLOAT,false,24,mesh.data);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,mesh.count);}
 private Mesh terrain(int cx,int cz){Builder b=new Builder();for(int x=0;x<16;x++)for(int z=0;z<16;z++){int wx=cx*16+x,wz=cz*16+z;float h=(float)floor(wx,wz);double biome=Math.sin(wx*.012)+Math.cos(wz*.015);if(environment==1)biome=-1;float tint=(float)(.92+.08*Math.sin(wx*12.8+wz*73.1));float r,g,bl;if(biome<-.35){r=.73f;g=.64f;bl=.43f;}else if(biome>1.25){r=.32f;g=.48f;bl=.40f;}else{r=.39f;g=.58f;bl=.39f;}
  if(environment==2){r=.77f;g=.86f;bl=.85f;}
  if(edits.getOrDefault(wx+","+wz,0)>0){r=.65f;g=.69f;bl=.63f;}
  b.quad(new float[]{x,h,z,x+1,h,z,x+1,h,z+1,x,h,z+1},r*tint,g*tint,bl*tint);
  float h1=(float)floor(wx-1,wz);if(h>h1)b.quad(new float[]{x,h1,z,x,h,z,x,h,z+1,x,h1,z+1},r*.61f,g*.63f,bl*.62f);
  h1=(float)floor(wx+1,wz);if(h>h1)b.quad(new float[]{x+1,h1,z,x+1,h1,z+1,x+1,h,z+1,x+1,h,z},r*.74f,g*.74f,bl*.7f);
  h1=(float)floor(wx,wz-1);if(h>h1)b.quad(new float[]{x,h1,z,x+1,h1,z,x+1,h,z,x,h,z},r*.66f,g*.67f,bl*.65f);
  h1=(float)floor(wx,wz+1);if(h>h1)b.quad(new float[]{x,h1,z+1,x,h,z+1,x+1,h,z+1,x+1,h1,z+1},r*.85f,g*.85f,bl*.8f);
  long hash=((wx*73856093L)^(wz*19349663L))&0xffff;
  if(hash%113==0&&biome>-.35&&Math.abs(wx%28-14)>4){box(b,x+.36f,h,z+.36f,.28f,2.3f,.28f,.31f,.25f,.17f);box(b,x-.4f,h+1.8f,z-.4f,1.8f,1.4f,1.8f,.20f,.39f,.32f);box(b,x-.1f,h+3,z-.1f,1.2f,.8f,1.2f,.29f,.49f,.37f);}
  if(hash%599==0){box(b,x,h,z,.8f,1.6f,.8f,.46f,.55f,.55f);box(b,x+.2f,h+1.6f,z+.2f,.4f,.45f,.4f,.52f,.89f,.8f);}
 }
 return b.done();}
 private Mesh beacon(){Builder b=new Builder();box(b,-2,0,-2,4,.3f,4,.32f,.40f,.43f);for(int i=-1;i<=1;i+=2){box(b,i*1.5f-.25f,.3f,-.3f,.5f,4,.6f,.61f,.69f,.66f);box(b,i*1.5f-.12f,.6f,-.35f,.24f,3.3f,.1f,.50f,.95f,.79f);}box(b,-1.8f,4.3f,-.4f,3.6f,.5f,.8f,.72f,.78f,.70f);box(b,-.45f,1,-.45f,.9f,.9f,.9f,.56f,1,.83f);box(b,-.035f,2,-.035f,.07f,35,.07f,.74f,.95f,.80f);return b.done();}
 private static void box(Builder b,float x,float y,float z,float w,float h,float d,float r,float g,float bl){float X=x+w,Y=y+h,Z=z+d;
 b.quad(new float[]{x,Y,z,X,Y,z,X,Y,Z,x,Y,Z},r,g,bl);b.quad(new float[]{x,y,z,x,Y,z,x,Y,Z,x,y,Z},r*.64f,g*.64f,bl*.64f);b.quad(new float[]{X,y,z,X,y,Z,X,Y,Z,X,Y,z},r*.83f,g*.83f,bl*.83f);b.quad(new float[]{x,y,z,X,y,z,X,Y,z,x,Y,z},r*.71f,g*.71f,bl*.71f);b.quad(new float[]{x,y,Z,x,Y,Z,X,Y,Z,X,y,Z},r*.91f,g*.91f,bl*.91f);}

 public void attack(){queueEvent(()->{
  if(paused)return;long now=SystemClock.elapsedRealtime();
  if(equipment.selected==Equipment.PICKAXE){swingAt=now;edit(false);combatMessage="Picareta · retire blocos perto de você";return;}
  int target=equipment.strike(missionIndex,px,pz,yaw,now);if(target==-2)return;swingAt=now;
  if(target>=0){hitAt=now;hitTarget=target;combatMessage=equipment.health(missionIndex,target)==0?"Sentinela desativado · +1 cristal":"Acertou! "+equipment.health(missionIndex,target)+" golpes restantes";
   post(()->{((MainActivity)getContext()).play(true);((MainActivity)getContext()).save();});
  }else combatMessage="Aproxime-se do sentinela dourado e olhe para ele.";
 });}
 private void part(float x,float y,float z,float w,float h,float d,float rx,float red,float green,float blue){
  System.arraycopy(rig,0,model,0,16);Matrix.translateM(model,0,x,y,z);Matrix.rotateM(model,0,rx,1,0,0);Matrix.scaleM(model,0,w,h,d);
  GLES20.glUniformMatrix4fv(modelLoc,1,false,model,0);GLES20.glUniform3f(tintLoc,red,green,blue);drawRaw(cube,0,0,0);
 }
 private void drawAvatar(long now){
  Matrix.setIdentityM(rig,0);Matrix.translateM(rig,0,0,-1.65f,0);Matrix.rotateM(rig,0,-yaw*57.29578f,0,1,0);
  float stride=(float)Math.sin(walk)*Math.min(1,(float)Math.hypot(moveX,moveZ))*25;
  part(-.19f,.40f,0,.27f,.72f,.31f,stride,.12f,.22f,.28f);part(.19f,.40f,0,.27f,.72f,.31f,-stride,.12f,.22f,.28f);
  part(-.19f,.08f,-.07f,.31f,.17f,.44f,0,.16f,.19f,.22f);part(.19f,.08f,-.07f,.31f,.17f,.44f,0,.16f,.19f,.22f);
  part(0,1.02f,0,.70f,.62f,.40f,0,.13f,.45f,.43f);part(0,.77f,0,.74f,.12f,.44f,0,.74f,.53f,.27f);
  part(0,1.48f,0,.46f,.46f,.44f,0,.87f,.66f,.46f);part(0,1.72f,.01f,.51f,.15f,.47f,0,.15f,.18f,.21f);
  part(-.13f,1.49f,-.23f,.08f,.065f,.02f,0,.08f,.13f,.15f);part(.13f,1.49f,-.23f,.08f,.065f,.02f,0,.08f,.13f,.15f);
  part(0,1.06f,.31f,.49f,.47f,.25f,0,.45f,.32f,.20f);part(0,1.20f,.45f,.20f,.07f,.04f,0,.85f,.71f,.37f);
  part(-.47f,1.02f,0,.22f,.62f,.25f,-stride,.17f,.58f,.52f);
  float progress=Math.max(0,Math.min(1,(now-swingAt)/420f)),swing=(float)Math.sin(progress*Math.PI);
  // Right arm and weapon share a shoulder pivot.
  Matrix.translateM(rig,0,.47f,1.28f,0);Matrix.rotateM(rig,0,20+swing*105,1,0,0);
  part(0,-.26f,0,.22f,.56f,.25f,0,.17f,.58f,.52f);part(0,-.53f,0,.24f,.22f,.25f,0,.87f,.66f,.46f);
  part(0,-.51f,-.20f,.10f,.11f,.45f,0,.38f,.25f,.14f);
  if(equipment.selected==Equipment.SWORD){part(0,-.51f,-.47f,.43f,.10f,.10f,0,.95f,.70f,.24f);part(0,-.51f,-.95f,.15f,.08f,.94f,0,.69f,.96f,.92f);part(0,-.51f,-1.45f,.08f,.06f,.13f,0,.88f,1,.99f);}
  else{part(0,-.51f,-.75f,.11f,.10f,.60f,0,.46f,.29f,.13f);part(0,-.51f,-1.06f,.75f,.14f,.15f,0,.61f,.70f,.73f);}
 }
 private void drawTargets(long now){
  for(int i=0;i<3;i++){
   int hp=equipment.health(missionIndex,i);double tx=Equipment.targetX(missionIndex,i),tz=Equipment.targetZ(missionIndex,i);
   Matrix.setIdentityM(rig,0);Matrix.translateM(rig,0,(float)(tx-px),(float)(floor(tx,tz)-py),(float)(tz-pz));
   part(0,.08f,0,1.3f,.16f,1.3f,0,.31f,.37f,.38f);
   if(hp==0){part(0,.25f,0,.50f,.30f,.50f,0,.23f,.43f,.42f);continue;}
   float recoil=i==hitTarget?Math.max(0,1-(now-hitAt)/420f):0;
   Matrix.translateM(rig,0,0,.12f*(float)Math.sin(now*.002+i),recoil*.4f);
   part(0,.60f,0,.42f,1.02f,.42f,0,.47f,.39f,.25f);
   part(0,1.13f,0,1.10f,.70f,.65f,recoil*25,1,.73f+recoil*.2f,.26f+recoil*.5f);
   part(0,1.20f,-.35f,.50f,.13f,.06f,0,.20f,.94f,.87f);
   part(0,1.82f,0,1.1f,.09f,.10f,0,.20f,.26f,.27f);
   part(-.55f+(hp/3f)*.55f,1.83f,-.01f,1.1f*hp/3f,.10f,.12f,0,.54f,.94f,.71f);
  }
 }
 public void refreshTerrain(){queueEvent(()->{generation++;chunks.clear();pending.clear();});}
 public void close(){destroyed=true;worker.shutdownNow();}
}
