package com.edward.physicalcraft;

import java.util.Locale;
import java.util.Random;

/** Deterministic scientific authority: model text never supplies the solution or executable code. */
public final class Physics {
 public static final String[] NAMES={"Forças e movimento","Queda livre","Lançamentos","Energia","Alavancas","Flutuação","Eletricidade","Calor","Ondas","Óptica","Órbitas","Relatividade","Quântica"};
 public static final String[] TITLES={"O guardião imóvel","A torre sem relógio","O sinal do outro lado","A usina adormecida","A porta de pedra","O arquivo submerso","O farol apagado","A estufa congelada","O eco perdido","O observatório cego","O satélite errante","Os relógios discordam","A luz que abre portas"};
 public static final String[] STORIES={
  "Uma plataforma guarda a primeira memória da ilha. Seu motor responde à força, mas a carga resiste à mudança. Encontre a aceleração pedida.",
  "O relógio da torre parou. Use a queda de uma esfera como medida de tempo. A altura certa sincroniza o mecanismo.",
  "Uma ponte desabou. Envie uma cápsula até o receptor usando o lançador. A velocidade decide onde ela cai.",
  "Uma turbina precisa de uma esfera chegando com a velocidade certa. Ajuste a altura da rampa para transformar energia potencial em movimento.",
  "Uma porta pesada protege o arquivo. Você tem uma alavanca: descubra onde aplicar a força para equilibrar o peso.",
  "Um cofre está no fundo do reservatório. Ajuste o volume do flutuador para equilibrar exatamente o peso com o empuxo.",
  "O farol perdeu sua fonte. Ajuste a tensão para que a corrente no resistor alcance o valor necessário ao circuito.",
  "Uma amostra precisa aquecer antes que o laboratório abra. Forneça a energia necessária, considerando massa e calor específico.",
  "Uma antena responde somente a um comprimento de onda. Sintonize a frequência e observe a distância entre cristas.",
  "O mapa celeste está desfocado. Mova o objeto diante da lente convergente para projetar a imagem na posição indicada.",
  "Um satélite precisa entrar em órbita circular. Escolha o raio orbital que corresponde à velocidade exigida pelo sistema.",
  "Dois relógios já não concordam. Ajuste a velocidade da nave e descubra como o fator de Lorentz altera a comparação dos tempos.",
  "Uma placa metálica emite elétrons quando recebe luz. Ajuste a frequência dos fótons para atingir a energia cinética pedida."};
 public static final String[] RULES={"F = m·a. Sem atrito, dobrar a força dobra a aceleração. A mesma força acelera menos uma massa maior.","t = √(2h/g). Queda a partir do repouso, sem resistência do ar. A massa não muda o tempo de queda.","R = v²·sen(2θ)/g. Partida e chegada na mesma altura, sem resistência do ar. Aqui θ = 45°.","mgh = mv²/2. A massa se cancela. Rampa sem atrito; v = √(2gh).","F·L = m·g·r. Equilíbrio de torques. A força é perpendicular à alavanca; r é o braço da carga.","Empuxo = ρ·V·g. O volume indicado é o volume submerso. Em equilíbrio, empuxo e peso têm a mesma intensidade.","I = V/R. Resistor ôhmico ideal, temperatura constante. Dobrar a tensão dobra a corrente.","Q = m·c·ΔT. Sem perdas de calor e sem mudança de fase. O resultado é a variação de temperatura.","v = f·λ. A velocidade no meio é fixa. Frequência maior corresponde a comprimento de onda menor.","1/f = 1/dₒ + 1/dᵢ. Lente fina convergente, objeto além do foco; imagem real, invertida.","v = √(μ/r). Órbita circular ideal em torno da Terra. r é medido desde o centro da Terra, não da superfície.","γ = 1/√(1−β²), com β = v/c. Δt = γ·Δτ. Relógios inerciais, sem gravidade; v sempre menor que c.","Kmáx = max(0, h·f − φ). O modelo calcula a energia máxima dos fotoelétrons. Abaixo do limiar não há emissão."};
 public static final String[] INPUT={"Força (N)","Altura (m)","Velocidade (m/s)","Altura da rampa (m)","Braço da força (m)","Volume submerso (L)","Tensão (V)","Calor fornecido (kJ)","Frequência (Hz)","Distância do objeto (cm)","Raio orbital (km)","Velocidade / c","Frequência (THz)"};
 public static final String[] OUTPUT={"Aceleração","Tempo de queda","Alcance","Velocidade final","Torque aplicado","Empuxo","Corrente","Aquecimento","Comprimento de onda","Distância da imagem","Velocidade orbital","Fator de Lorentz","Energia dos elétrons"};
 public static final String[] UNIT={"m/s²","s","m","m/s","N·m","N","A","°C","m","cm","km/s","×","eV"};
 public static final double[] MIN={1,1,2,0.5,0.2,1,0.5,1,40,12,6700,0.05,600};
 public static final double[] MAX={60,40,25,25,5,30,24,100,800,60,30000,0.95,1500};
 public static final class Mission {
  public int index,type,environment; public long seed; public double parameter,solution,min,max,target; public String title,story,source="Campanha local";
  public double value(double input){return evaluate(type,input,parameter);}
  public double tolerance(){return Math.max(0.015,Math.abs(target)*0.035);}
  public boolean success(double input){return Double.isFinite(input)&&input>=min&&input<=max&&Math.abs(value(input)-target)<=tolerance();}
  public String context(){switch(type){case 0:return "Carga: "+fmt(parameter)+" kg · sem atrito";case 1:case 2:case 3:return "g = 9,81 m/s² · modelo ideal";case 4:return "Carga: "+fmt(parameter)+" N·m · força de 20 N";case 5:return "Água: 1.000 kg/m³ · peso = "+fmt(target)+" N";case 6:return "Resistência: "+fmt(parameter)+" Ω";case 7:return "Massa: "+fmt(parameter)+" kg · c = 4.180 J/(kg·°C)";case 8:return "Velocidade do som: 340 m/s";case 9:return "Lente convergente · f = 10 cm";case 10:return "Terra · μ = 398.600 km³/s²";case 11:return "β = v/c · referencial inercial";default:return "Função trabalho φ = 2,0 eV";}}
 }
 public static Mission create(int index,long seed){
  Mission m=new Mission();m.index=index;m.type=(index<26?Math.min(12,Math.max(0,index/2)):(index-26)%13);m.seed=seed;Random r=new Random(seed+index*1009L);m.min=MIN[m.type];m.max=MAX[m.type];m.parameter=2+r.nextInt(7);if(m.type==7)m.parameter=0.4+r.nextInt(6)*0.2;
  // Pick an exact slider step. Every shipped and generated challenge has a reachable solution.
  int step=20+r.nextInt(61);m.solution=m.min+(m.max-m.min)*step/100.0;m.target=m.value(m.solution);if(m.type==4)m.parameter=m.target;
  m.title=TITLES[m.type];m.story=STORIES[m.type];return m;
 }
 public static double evaluate(int type,double x,double p){switch(type){case 0:return x/p;case 1:return Math.sqrt(2*x/9.81);case 2:return x*x/9.81;case 3:return Math.sqrt(2*9.81*x);case 4:return 20*x;case 5:return x*9.81;case 6:return x/p;case 7:return x*1000/(p*4180);case 8:return 340/x;case 9:return 1/(0.1-1/x);case 10:return Math.sqrt(398600/x);case 11:return 1/Math.sqrt(1-x*x);case 12:return Math.max(0,0.004135667696*x-2);default:throw new IllegalArgumentException("unknown physics");}}
 public static String fmt(double d){if(Math.abs(d)>=1000)return String.format(Locale.ROOT,"%.0f",d);return String.format(Locale.ROOT,"%.2f",d).replace('.',',');}
 public static double height(double x,double z){return Math.floor(3+7*noise(x/44,z/44)+3*noise(x/19+73,z/19-41)+noise(x/7,z/7));}
 private static double hash(long x,long z){long n=x*0x632BE59BD9B4E019L+z*0x9E3779B97F4A7C15L+730211;n=(n^(n>>>30))*0xBF58476D1CE4E5B9L;n=(n^(n>>>27))*0x94D049BB133111EBL;return ((n^(n>>>31))>>>11)*0x1.0p-53;}
 private static double noise(double x,double z){long ix=(long)Math.floor(x),iz=(long)Math.floor(z);double tx=x-ix,tz=z-iz;tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);double a=hash(ix,iz)*(1-tx)+hash(ix+1,iz)*tx,b=hash(ix,iz+1)*(1-tx)+hash(ix+1,iz+1)*tx;return a*(1-tz)+b*tz;}
 public static double stationX(int i){return 14+i*28.0;}
 public static double stationZ(int i){return Math.sin(i*1.3)*18;}
}
