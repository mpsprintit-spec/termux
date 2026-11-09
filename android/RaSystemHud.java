package com.termux.app.ra;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.net.TrafficStats;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;
import android.view.View;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

/** Real local Android metrics. Never generate dummy values if readings are unavailable. */
public final class RaSystemHud extends View {
    public interface MetricsListener { void onMetrics(String cpu, String ram); }
    private MetricsListener metricsListener;
    public void setMetricsListener(MetricsListener listener) { this.metricsListener = listener; }

    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Context context;
    private static final long PERIOD=2000L;
    private boolean running=false;
    private long prevCpuTotal=-1,prevCpuIdle=-1;
    private long prevAppCpu=-1,prevAppMs=-1;
    private long prevRx=-1,prevTx=-1,prevNetworkTime=-1;
    private double cpuPercent=Double.NaN, ramPercent=Double.NaN, diskPercent=Double.NaN;
    private double networkDown=Double.NaN,networkUp=Double.NaN;
    private long memUsed=-1,memTotal=-1,diskUsed=-1,diskTotal=-1;
    private long appHeap=-1;
    private boolean cpuWholeSystem=false;
    private final float[] cpuSamples=new float[25];
    private int cpuCount=0;
    private final Runnable ticker=new Runnable(){@Override public void run(){if(!running)return;sample();invalidate();if(metricsListener!=null)metricsListener.onMetrics((cpuWholeSystem?"CPU ":"APP ")+pct(cpuPercent),"RAM "+pct(ramPercent));handler.postDelayed(this,PERIOD);}};

    public RaSystemHud(Context context){super(context);this.context=context;setContentDescription("Live CPU, memory, storage and network statistics");}
    public void resume(){if(running)return;running=true;handler.post(ticker);}
    public void pause(){running=false;handler.removeCallbacks(ticker);}
    private float dp(float n){return getResources().getDisplayMetrics().density*n;}

    private void sample(){
        // /proc/stat can be blocked by Android SELinux. Fall back explicitly to this app's CPU.
        try(BufferedReader reader=new BufferedReader(new FileReader("/proc/stat"))){
            String first=reader.readLine();
            if(first!=null&&first.startsWith("cpu ")){
                String[] a=first.trim().split("\\s+");
                long total=0,idle=0;
                for(int i=1;i<a.length;i++){long n=Long.parseLong(a[i]);total+=n;if(i==4||i==5)idle+=n;}
                if(prevCpuTotal>=0&&total>prevCpuTotal){cpuPercent=100.0*(1.0-(double)(idle-prevCpuIdle)/(total-prevCpuTotal));cpuWholeSystem=true;}
                prevCpuTotal=total;prevCpuIdle=idle;
            }
        }catch(Exception ignored){cpuWholeSystem=false;}
        long now=SystemClock.elapsedRealtime();
        long appCpu=Process.getElapsedCpuTime();
        if(!cpuWholeSystem && prevAppMs>=0 && now>prevAppMs){
            cpuPercent=Math.max(0,100.0*(appCpu-prevAppCpu)/(now-prevAppMs));
        }
        prevAppMs=now;prevAppCpu=appCpu;
        if(!Double.isNaN(cpuPercent)){
            if(cpuCount<cpuSamples.length) cpuSamples[cpuCount++]=(float)cpuPercent;
            else {System.arraycopy(cpuSamples,1,cpuSamples,0,cpuSamples.length-1);cpuSamples[cpuCount-1]=(float)cpuPercent;}
        }
        try {
            ActivityManager am=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();
            if(am!=null){am.getMemoryInfo(mi);memTotal=mi.totalMem;memUsed=mi.totalMem-mi.availMem;ramPercent=memTotal>0?100.0*memUsed/memTotal:Double.NaN;}
        }catch(Exception ignored){memTotal=-1;memUsed=-1;ramPercent=Double.NaN;}
        try{
            StatFs stat=new StatFs(Environment.getDataDirectory().getAbsolutePath());
            diskTotal=stat.getTotalBytes();diskUsed=Math.max(0,diskTotal-stat.getAvailableBytes());
            diskPercent=diskTotal>0?100.0*diskUsed/diskTotal:Double.NaN;
        }catch(Exception ignored){diskTotal=-1;diskUsed=-1;diskPercent=Double.NaN;}
        try{
            long rx=TrafficStats.getTotalRxBytes(),tx=TrafficStats.getTotalTxBytes();
            if(rx!=TrafficStats.UNSUPPORTED && tx!=TrafficStats.UNSUPPORTED){
                if(prevNetworkTime>=0&&now>prevNetworkTime){
                    networkDown=Math.max(0,(rx-prevRx)*1000.0/(now-prevNetworkTime));
                    networkUp=Math.max(0,(tx-prevTx)*1000.0/(now-prevNetworkTime));
                }
                prevRx=rx;prevTx=tx;prevNetworkTime=now;
            } else {networkDown=networkUp=Double.NaN;}
        }catch(Exception ignored){networkDown=networkUp=Double.NaN;}
        Runtime rt=Runtime.getRuntime();appHeap=rt.totalMemory()-rt.freeMemory();
    }

    private void text(Canvas c,String t,float x,float y,float size,int color,boolean bold){
        p.reset();p.setAntiAlias(true);p.setColor(color);p.setTextSize(dp(size));p.setTypeface(bold?android.graphics.Typeface.create("sans-serif-medium",0):android.graphics.Typeface.create("sans-serif",0));
        c.drawText(t,x,y,p);
    }
    private void box(Canvas c,float left,float top,float right,float bottom){
        p.reset();p.setAntiAlias(true);p.setColor(0xe8051018);p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(left,top,right,bottom,dp(12),dp(12),p);
        p.setColor(0xff16748a);p.setStrokeWidth(dp(.75f));p.setStyle(Paint.Style.STROKE);
        c.drawRoundRect(left+.5f,top+.5f,right-.5f,bottom-.5f,dp(12),dp(12),p);
        p.setStyle(Paint.Style.FILL);
    }
    private static String pct(double d){return Double.isNaN(d)?"--":String.format(Locale.US,"%.0f%%",d);}
    private static String gig(long n){return n<0?"--":String.format(Locale.US,"%.1f GB",n/1073741824.0);}
    private static String speed(double b){
        if(Double.isNaN(b))return "--";
        if(b>=1048576)return String.format(Locale.US,"%.1f MB/s",b/1048576);
        if(b>=1024)return String.format(Locale.US,"%.0f KB/s",b/1024);
        return String.format(Locale.US,"%.0f B/s",b);
    }
    private void bar(Canvas c,float left,float y,float w,float value){
        p.setAntiAlias(true);p.setColor(0xff153142);p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(left,y,left+w,y+dp(6),dp(3),dp(3),p);
        if(!Float.isNaN(value)){
            float ww=w*Math.max(0,Math.min(100,value))/100f;
            p.setShader(new LinearGradient(left,y,left+w,y,new int[]{0xff0af4f1,0xff3d96ff,0xffb54efc},null,Shader.TileMode.CLAMP));
            c.drawRoundRect(left,y,left+ww,y+dp(6),dp(3),dp(3),p);p.setShader(null);
        }
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight(),gap=dp(6);
        if(w>h*1.7f && h>=dp(48)){
            float cell=w/5f;
            String[] labels={"APP CPU","RAM","STORAGE","NETWORK","APP PID"};
            String[] values={pct(cpuPercent),pct(ramPercent),pct(diskPercent),speed(networkDown),String.valueOf(Process.myPid())};
            for(int i=0;i<5;i++){
                float left=cell*i+dp(1),right=cell*(i+1)-dp(1);
                box(c,left,0,right,h-dp(2));
                text(c,labels[i],left+dp(4),dp(16),i==2||i==3?7:8,0xff81bdd0,true);
                text(c,values[i],left+dp(4),dp(37),i==3?8:10,0xff50eeff,true);
                if(i==1||i==2)bar(c,left+dp(4),h-dp(13),cell-dp(10),(float)(i==1?ramPercent:diskPercent));
            }
            return;
        }
        if(w<dp(85)||h<=dp(140))return;
        float itemH=Math.min(dp(105),(h-gap*6)/5f);
        boolean tight=itemH<dp(81);
        float titleSize=tight?9:10;
        float top=gap;
        final int white=0xffc9eff7,cyan=0xff50eaff,muted=0xff87acbc;
        for(int i=0;i<5;i++){
            float bottom=top+itemH;
            box(c,0,top,w,bottom);
            String name,value,info;
            switch(i){
                case 0:name=cpuWholeSystem?"CPU":"APP CPU";value=pct(cpuPercent);info=cpuWholeSystem?"All cores":"Per-core equiv.";break;
                case 1:name="RAM";value=pct(ramPercent);info=gig(memUsed)+" used";break;
                case 2:name="STORAGE";value=pct(diskPercent);info=gig(diskUsed)+" used";break;
                case 3:name="NETWORK";value="LIVE";info="↓ "+speed(networkDown);break;
                default:name="APP";value="PID";info=String.valueOf(Process.myPid());break;
            }
            text(c,name,dp(8),top+dp(16),titleSize,white,true);
            p.setTextSize(dp(10));float estimate=value.length()*dp(5.5f);
            text(c,value,Math.max(dp(10),w-estimate-dp(5)),top+dp(16),10,cyan,true);
            if(!tight){
                if(i==0&&cpuCount>1){
                    float left=dp(9),right=w-dp(9),base=bottom-dp(27);
                    Path line=new Path();for(int j=0;j<cpuCount;j++){float x=left+(right-left)*j/Math.max(1,cpuSamples.length-1),y=base-(Math.min(100,cpuSamples[j])/100f)*dp(18);if(j==0)line.moveTo(x,y);else line.lineTo(x,y);}
                    p.reset();p.setAntiAlias(true);p.setStyle(Paint.Style.STROKE);p.setColor(cyan);p.setStrokeWidth(dp(1.2f));c.drawPath(line,p);p.setStyle(Paint.Style.FILL);
                }else if(i==1 || i==2){bar(c,dp(8),bottom-dp(30),w-dp(16),(float)(i==1?ramPercent:diskPercent));}
                text(c,info,dp(8),bottom-dp(9),9,muted,false);
                if(i==3)text(c,"↑ "+speed(networkUp),dp(8),bottom-dp(25),9,cyan,false);
                if(i==4)text(c,"Heap: "+gig(appHeap),dp(8),bottom-dp(25),8,cyan,false);
            } else {
                text(c,info,dp(8),bottom-dp(8),8,muted,false);
            }
            top=bottom+gap;
        }
    }
}
