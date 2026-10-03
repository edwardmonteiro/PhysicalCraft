package com.edward.physicalcraft;
import android.content.Context;
import android.net.Uri;
import com.google.ai.edge.litertlm.*;
import java.io.*;
import java.util.Collections;
import java.util.concurrent.*;

/** Single serialized inference/import queue. No network inference, analytics or remote APIs. */
public final class GemmaDirector {
 public interface Callback{void done(Physics.Mission mission,String error);}
 public interface ImportCallback{void done(String error);}
 private final Context context;private final ExecutorService executor=Executors.newSingleThreadExecutor();private Engine engine;public volatile boolean busy;private volatile boolean closed;
 public GemmaDirector(Context c){context=c.getApplicationContext();}
 public File model(){return new File(context.getFilesDir(),"gemma.litertlm");}
 public boolean ready(){return model().isFile();}
 private Engine open(File f){return new Engine(new EngineConfig(f.getAbsolutePath(),new Backend.CPU(),null,null,4096,null,context.getCacheDir().getAbsolutePath()));}
 public void generate(Physics.Mission base,Callback cb){if(busy){cb.done(null,"O Gemma já está trabalhando.");return;}if(!ready()){cb.done(null,"Importe o Gemma em IA local primeiro.");return;}busy=true;executor.execute(()->{try{if(engine==null){engine=open(model());engine.initialize();}String output;try(Conversation conv=engine.createConversation(new ConversationConfig())){com.google.ai.edge.litertlm.Message reply=conv.sendMessage(Blueprint.prompt(base),Collections.emptyMap(),null,null,null,600);output=reply.getContents().toString();}Physics.Mission m=Blueprint.parse(output,base.index);cb.done(m,null);}catch(Throwable e){release();cb.done(null,"O modelo não gerou uma fase válida. Sua fase atual foi preservada. Tente novamente ou use Gemma 3 1B .litertlm.");}finally{busy=false;release();}});}
 public void importModel(Uri uri,ImportCallback cb){if(busy){cb.done("Aguarde a geração terminar.");return;}busy=true;executor.execute(()->{File temp=new File(context.getFilesDir(),"gemma-import.tmp");try{release();try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(temp)){if(in==null)throw new IOException("arquivo");byte[] buffer=new byte[1024*1024];long bytes=0;int n;while((n=in.read(buffer))!=-1){bytes+=n;if(bytes>3_000_000_000L)throw new IOException("tamanho");out.write(buffer,0,n);}out.getFD().sync();}if(temp.length()<100_000_000L)throw new IOException("formato");try(Engine test=open(temp)){test.initialize();}java.nio.file.Files.move(temp.toPath(),model().toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);cb.done(null);}catch(Throwable e){temp.delete();cb.done("Não foi possível importar. Escolha Gemma 3 1B em formato .litertlm, com espaço livre para copiar o arquivo.");}finally{busy=false;}});}
 private void release(){if(engine!=null){try{engine.close();}catch(Throwable ignored){}engine=null;}}
 public void close(){closed=true;executor.execute(this::release);executor.shutdown();}
}
