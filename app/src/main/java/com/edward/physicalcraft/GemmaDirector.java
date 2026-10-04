package com.edward.physicalcraft;
import android.content.Context;
import android.net.Uri;
import com.google.ai.edge.litertlm.*;
import java.io.*;
import java.util.Collections;
import java.security.MessageDigest;
import java.util.concurrent.*;

/** Single serialized inference/import queue. No network inference, analytics or remote APIs. */
public final class GemmaDirector {
 public interface Callback{void done(Physics.Mission mission,String error);}
 public interface ImportCallback{void done(String error);}
 private final Context context;private final ExecutorService executor=Executors.newSingleThreadExecutor();private Engine engine;public volatile boolean busy;private volatile boolean closed;
 private static GemmaDirector instance;
 public static synchronized GemmaDirector get(Context c){if(instance==null)instance=new GemmaDirector(c);return instance;}
 private GemmaDirector(Context c){context=c.getApplicationContext();}
 public String modelName(){return context.getSharedPreferences("local-ai",0).getString("name","Gemma local");}
 public File model(){return new File(context.getFilesDir(),"gemma.litertlm");}
 public boolean ready(){return model().isFile();}
 private Engine open(File f){return new Engine(new EngineConfig(f.getAbsolutePath(),new Backend.CPU(),null,null,1024,null,context.getCacheDir().getAbsolutePath()));}
 public synchronized void generate(Physics.Mission base,Callback cb){if(busy){cb.done(null,"O Gemma já está trabalhando.");return;}if(!ready()){cb.done(null,"Importe o Gemma em IA local primeiro.");return;}busy=true;executor.execute(()->{try{if(engine==null){engine=open(model());engine.initialize();}String output;try(Conversation conv=engine.createConversation(new ConversationConfig())){com.google.ai.edge.litertlm.Message reply=conv.sendMessage(Blueprint.prompt(base),Collections.emptyMap(),null,null,null,384);output=reply.getContents().toString();}Physics.Mission m=Blueprint.parse(output,base.index);m.source=modelName()+" · local";cb.done(m,null);}catch(Throwable e){android.util.Log.e("PhysicalCraft","Local inference failed",e);release();cb.done(null,"O modelo não gerou uma fase válida. Sua fase atual foi preservada. Tente gerar novamente. A campanha continua disponível offline.");}finally{busy=false;release();}});}
 public void importModel(Uri uri,ImportCallback cb){importModel(uri,"Gemma / modelo importado",null,0,cb);}
 public synchronized void importModel(Uri uri,String label,String expectedHash,long expectedBytes,ImportCallback cb){if(busy){cb.done("Aguarde a geração terminar.");return;}busy=true;executor.execute(()->{File temp=new File(context.getFilesDir(),"gemma-import.litertlm");try{release();try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(temp)){if(in==null)throw new IOException("arquivo");byte[] buffer=new byte[1024*1024];long bytes=0;MessageDigest hash=MessageDigest.getInstance("SHA-256");int n;while((n=in.read(buffer))!=-1){bytes+=n;if(bytes>3_000_000_000L)throw new IOException("tamanho");hash.update(buffer,0,n);out.write(buffer,0,n);}out.getFD().sync();if(expectedBytes>0&&bytes!=expectedBytes)throw new IOException("Download incompleto");if(expectedHash!=null){StringBuilder hex=new StringBuilder();for(byte v:hash.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",v&255));if(!hex.toString().equals(expectedHash))throw new IOException("Verificação SHA-256 falhou");}}if(temp.length()<100_000_000L)throw new IOException("formato");try(Engine test=open(temp)){test.initialize();}java.nio.file.Files.move(temp.toPath(),model().toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);context.getSharedPreferences("local-ai",0).edit().putString("name",label).apply();cb.done(null);}catch(Throwable e){android.util.Log.e("PhysicalCraft","Model import failed",e);temp.delete();cb.done("Não foi possível preparar a IA. Verifique o arquivo .litertlm e o espaço livre. Feche outros apps e tente novamente.");}finally{busy=false;}});}
 private void release(){if(engine!=null){try{engine.close();}catch(Throwable ignored){}engine=null;}}
 public void close(){closed=true;executor.execute(this::release);executor.shutdown();}
}
