package com.edward.physicalcraft;
import org.json.*;
/** Strict bounded data contract. No scripts, URLs, class names or generated equations are executed. */
public final class Blueprint {
 public static Physics.Mission parse(String text,int index) throws JSONException {
  if(text==null||text.length()>12000)throw new JSONException("Resposta longa ou vazia");int a=text.indexOf('{'),b=text.lastIndexOf('}');if(a<0||b<=a)throw new JSONException("Sem JSON");JSONObject j=new JSONObject(text.substring(a,b+1));
  if(j.getInt("version")!=1)throw new JSONException("Versão inválida");int expected=Physics.create(index,1).type;if(j.getInt("type")!=expected)throw new JSONException("Tema fora da progressão");
  long seed=j.getLong("seed");Physics.Mission m=Physics.create(index,seed);String title=j.getString("title").trim(),story=j.getString("story").trim();if(title.length()<4||story.length()<25)throw new JSONException("Narrativa curta ou vazia: title="+title.length()+", story="+story.length());title=bounded(title,65);story=bounded(story,600);
  int step=j.getInt("solution_step");if(step<10||step>90)throw new JSONException("Solução fora dos limites");double parameter=j.getDouble("parameter");if(!Double.isFinite(parameter)||parameter<.4||parameter>12)throw new JSONException("Parâmetro inválido");
  m.parameter=parameter;m.solution=m.min+(m.max-m.min)*step/100.;m.target=m.value(m.solution);if(!Double.isFinite(m.target)||m.target<=0)throw new JSONException("Fase sem solução");if(m.type==4)m.parameter=m.target;
  m.title=title;m.story=story;m.source="Gemma local";m.environment=j.optInt("environment",0);if(m.environment<0||m.environment>2)throw new JSONException("Ambiente inválido");return m;
 }
 public static JSONObject json(Physics.Mission m) throws JSONException{return new JSONObject().put("version",1).put("type",m.type).put("seed",m.seed).put("title",m.title).put("story",m.story).put("parameter",m.type==4?3:m.parameter).put("solution_step",Math.round((m.solution-m.min)/(m.max-m.min)*100)).put("source",m.source).put("environment",m.environment);}
 private static String bounded(String text,int max){if(text.length()<=max)return text;int end=text.lastIndexOf(' ',max);if(end<max/2)end=max;if(Character.isHighSurrogate(text.charAt(end-1)))end--;return text.substring(0,end).trim();}
 public static String prompt(Physics.Mission m){return "Crie um mistério curto em português para PhysicalCraft, numa ruína científica. O jogador ajusta "+Physics.INPUT[m.type]+" para investigar "+Physics.OUTPUT[m.type]+". Tema: "+Physics.NAMES[m.type]+". Retorne só JSON: {\"version\":1,\"type\":"+m.type+",\"seed\":12345,\"title\":\"Título de 4 a 60 caracteres\",\"story\":\"Mistério de 25 a 160 caracteres, sem revelar a resposta\",\"parameter\":3,\"solution_step\":50,\"environment\":0}. Invente seed inteiro; parameter entre 0.4 e 12; solution_step entre 10 e 90; environment 0 floresta, 1 deserto ou 2 gelo. Sem equações, código ou markdown.";}
}
