package com.edward.physicalcraft;

import android.app.DownloadManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import java.io.File;

/** Android owns the transfer, including background progress and network retries. */
public final class ModelDownload {
 public static final String NAME="Qwen 2.5 1.5B";
 public static final long BYTES=1597931520L;
 public static final String SHA256="faa60663b333290c1496c499828b21d3e3254a788cacd8cce917ce0f761a2dc9";
 public static final String URL="https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/19edb84c69a0212f29a6ef17ba0d6f278b6a1614/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm";
 private final Context context;private final DownloadManager manager;private final SharedPreferences prefs;
 public volatile String status="";private volatile boolean installing;
 private static ModelDownload instance;
 public static synchronized ModelDownload get(Context c){if(instance==null)instance=new ModelDownload(c);return instance;}
 private ModelDownload(Context c){context=c.getApplicationContext();manager=(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);prefs=context.getSharedPreferences("model-download",0);}
 public boolean active(){return prefs.getLong("id",-1)>=0;}
 public synchronized void start(){
  if(active())return;
  File folder=context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
  if(folder==null||folder.getUsableSpace()<BYTES*2+200_000_000L)throw new IllegalStateException("Reserve 3,4 GB livres para baixar e instalar a IA.");
  DownloadManager.Request request=new DownloadManager.Request(Uri.parse(URL)).setTitle("PhysicalCraft · IA local").setDescription("Qwen 2.5 1.5B · 1,60 GB").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE).setAllowedOverRoaming(false).setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,"qwen-"+System.currentTimeMillis()+".litertlm");
  long id=manager.enqueue(request);prefs.edit().putLong("id",id).apply();status="Iniciando download…";
 }
 public synchronized void cancel(){long id=prefs.getLong("id",-1);if(id>=0&&!installing){manager.remove(id);prefs.edit().remove("id").apply();status="Download cancelado. A IA instalada foi preservada.";}}
 public synchronized void poll(GemmaDirector director,Runnable changed){
  long id=prefs.getLong("id",-1);if(id<0||installing)return;
  try(Cursor c=manager.query(new DownloadManager.Query().setFilterById(id))){
   if(c==null||!c.moveToFirst()){prefs.edit().remove("id").apply();status="Download não encontrado. Toque em baixar para tentar novamente.";return;}
   int state=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));long bytes=c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
   if(state==DownloadManager.STATUS_SUCCESSFUL){
    if(director.busy){status="Download completo. Aguardando a IA terminar…";return;}
    Uri uri=manager.getUriForDownloadedFile(id);if(uri==null)throw new IllegalStateException("Arquivo indisponível");
    installing=true;status="Download completo · verificando e preparando a IA…";
    director.importModel(uri,NAME,SHA256,BYTES,error->{installing=false;manager.remove(id);prefs.edit().remove("id").apply();status=error==null?"IA pronta. Toque em Gerar mistério.":error;changed.run();});
   }else if(state==DownloadManager.STATUS_FAILED){int reason=c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));manager.remove(id);prefs.edit().remove("id").apply();status="Download falhou ("+reason+"). Verifique a conexão e o espaço e tente novamente.";}
   else status=(state==DownloadManager.STATUS_PAUSED?"Aguardando conexão · ":"Baixando IA · ")+Math.max(0,bytes)*100/BYTES+"% · "+Math.max(0,bytes)/1_000_000+" / 1598 MB";
  }catch(RuntimeException error){status="Não foi possível consultar o download. Tente novamente.";}
 }
}
