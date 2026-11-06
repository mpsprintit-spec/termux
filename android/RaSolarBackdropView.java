package com.termux.app.ra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * Native GPU-composited background: one cached solar texture plus animated,
 * low-cost plasma filaments and two blue waves. No WebView/JS/GL scene.
 * The Termux TerminalView and metrics panel live above this decorative View.
 *
 * This is a physically inspired artistic simulation, not an SDO recording
 * or a magnetohydrodynamic plasma model.
 */
public final class RaSolarBackdropView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path stroke = new Path();
    private final RectF dest = new RectF();
    private Bitmap surface;
    private Shader halo;
    private float cx, cy, radius;
    private boolean running = false;
    private String mode = "eco";
    private float frozenTime = 0f;
    private long lastDrawTime = 0L;
    private final long boot = SystemClock.uptimeMillis();

    public RaSolarBackdropView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void resume() { running = true; invalidate(); }
    public void pause() { running = false; }
    public void release() { running = false; if (surface != null) { surface.recycle(); surface = null; } }
    public void setAnimationMode(String next) {
        if (!"flow".equals(next) && !"off".equals(next)) next = "eco";
        if ("off".equals(next)) frozenTime = (SystemClock.uptimeMillis() - boot) / 1000f;
        mode = next;
        lastDrawTime = 0L;
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        cx = w * 0.52f;
        cy = h * 0.42f;
        radius = Math.min(w * 0.31f, h * 0.225f);
        if (surface != null) { surface.recycle(); surface = null; }
        // Texture is generated ONCE per layout change, not once per animation frame.
        surface = createSolarSurface(280);
        halo = new RadialGradient(cx, cy, radius * 1.55f,
            new int[]{0x22ff781a, 0x1fff590a, 0x0dff4400, Color.TRANSPARENT},
            new float[]{0f, .63f, .79f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0 || surface == null) return;
        long now = SystemClock.uptimeMillis();
        float seconds = "off".equals(mode) ? frozenTime : (now - boot) / 1000f;
        canvas.drawColor(Color.BLACK);

        paint.reset(); paint.setAntiAlias(true);
        paint.setShader(halo);
        canvas.drawCircle(cx, cy, radius * 1.55f, paint);
        paint.setShader(null);

        paint.setFilterBitmap(true);
        dest.set(cx-radius, cy-radius, cx+radius, cy+radius);
        canvas.drawBitmap(surface, null, dest, paint);
        paint.setFilterBitmap(false);

        // Solar prominence loops: anchored to the limb; slowly evolving plasma arcs.
        drawProminences(canvas, seconds);
        // Bright plasma streams sliding over the photosphere.
        drawSurfacePlasma(canvas, seconds);
        // The blue accent is deliberately thin and separate from the warm sun.
        drawWaves(canvas, seconds);
        if (running && !"off".equals(mode)) {
            long interval = "flow".equals(mode) ? 50L : 95L;
            postInvalidateDelayed(interval);
        }
        lastDrawTime = now;
    }

    private void drawProminences(Canvas canvas, float t) {
        paint.reset(); paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 9; k++) {
            float a = (float)(k * Math.PI * 2 / 9 + .16);
            float b = a + .24f + .05f * (float)Math.sin(t*.23f+k);
            float raise = radius * (.16f + .10f * (float)Math.sin(k*2.7+t*.31));
            float ax = cx + radius*.955f*(float)Math.cos(a);
            float ay = cy + radius*.955f*(float)Math.sin(a);
            float bx = cx + radius*.955f*(float)Math.cos(b);
            float by = cy + radius*.955f*(float)Math.sin(b);
            float c1a = a + .07f;
            float c2a = b - .07f;
            float reach = radius + raise;
            stroke.reset(); stroke.moveTo(ax,ay);
            stroke.cubicTo(cx+reach*(float)Math.cos(c1a),cy+reach*(float)Math.sin(c1a),
                           cx+reach*(float)Math.cos(c2a),cy+reach*(float)Math.sin(c2a),bx,by);
            paint.setColor(k%3 == 0 ? 0x9cff6822 : 0xaaffab3d);
            paint.setStrokeWidth(Math.max(1f, radius*.014f));
            canvas.drawPath(stroke,paint);
            paint.setStrokeWidth(Math.max(.5f, radius*.004f));
            paint.setColor(0xaaffed89);
            canvas.drawPath(stroke,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSurfacePlasma(Canvas canvas, float t) {
        canvas.save();
        stroke.reset();
        stroke.addCircle(cx, cy, radius*.94f, Path.Direction.CW);
        canvas.clipPath(stroke);
        paint.reset(); paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int row = 0; row < 10; row++) {
            float y = cy + radius*(-.79f + row*.177f);
            stroke.reset();
            float firstX=cx-radius;
            for (int point=0;point<=19;point++) {
                float x=firstX + point*radius*2/19f;
                float wave=(float)Math.sin(x/radius*8.5f + t*.42f + row*2.5f);
                float mod=(float)Math.sin(x/radius*16f-t*.3f+row);
                float yy=y + (wave*.028f + mod*.009f)*radius;
                if(point==0)stroke.moveTo(x,yy);else stroke.lineTo(x,yy);
            }
            paint.setStrokeWidth(Math.max(.65f, radius*.006f));
            paint.setColor(row%3==0 ? 0x61ffe89a : 0x44ffa52c);
            canvas.drawPath(stroke,paint);
        }
        canvas.restore();
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawWaves(Canvas canvas, float t) {
        paint.reset(); paint.setAntiAlias(true);paint.setStyle(Paint.Style.STROKE);
        float w = getWidth(), h = getHeight();
        for(int i=0;i<3;i++) {
            stroke.reset();
            float base=h*(.70f+i*.088f);
            for(int j=0;j<=32;j++){
                float x=w*j/32f;
                float y=base+(float)Math.sin(x/w*9.2f+t*.27f+i*.8f)*h*.012f;
                if(j==0)stroke.moveTo(x,y);else stroke.lineTo(x,y);
            }
            paint.setStrokeWidth(i==1?1.4f:1f);
            paint.setColor(i==1?0x9a21d8ff:0x7b2579d6);
            canvas.drawPath(stroke,paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private static int hash(int x,int y,int seed) {
        int h=x*374761393+y*668265263+seed*1442695041;
        h=(h^(h>>>13))*1274126177;
        return h^(h>>>16);
    }
    private static float interp(float a,float b,float t) {
        t=t*t*(3f-2f*t);
        return a+(b-a)*t;
    }
    private static float noise(float x,float y,int seed) {
        int xi=(int)Math.floor(x),yi=(int)Math.floor(y);
        float tx=x-xi,ty=y-yi;
        float n00=(hash(xi,yi,seed)&65535)/65535f;
        float n10=(hash(xi+1,yi,seed)&65535)/65535f;
        float n01=(hash(xi,yi+1,seed)&65535)/65535f;
        float n11=(hash(xi+1,yi+1,seed)&65535)/65535f;
        return interp(interp(n00,n10,tx),interp(n01,n11,tx),ty);
    }
    private static int clamp(float x) {
        return Math.max(0,Math.min(255,Math.round(x)));
    }
    private static Bitmap createSolarSurface(int n) {
        int[] pix=new int[n*n];
        float center=(n-1)/2f,inv=1f/(n*.49f);
        for(int y=0;y<n;y++){
            for(int x=0;x<n;x++){
                float u=(x-center)*inv,v=(y-center)*inv,r2=u*u+v*v;
                if(r2>1f){pix[y*n+x]=0;continue;}
                float z=(float)Math.sqrt(1-r2);
                float fine=noise((u+1)*74,(v+1)*74,3);
                float grain=noise((u+1)*36,(v+1)*36,5);
                float large=noise((u+1)*9,(v+1)*9,7);
                float turbulent=(fine-.5f)*.28f+(grain-.5f)*.31f+(large-.5f)*.35f;
                float spotA=(u+.34f)*(u+.34f)/.014f+(v-.19f)*(v-.19f)/.007f;
                float spotB=(u-.43f)*(u-.43f)/.01f+(v+.31f)*(v+.31f)/.014f;
                float spots=(spotA<1f?.30f*(1-spotA):0f)+(spotB<1f?.25f*(1-spotB):0f);
                float brightness=(.35f+.65f*(float)Math.pow(z,.40f));
                float q=Math.max(.18f,1f+turbulent-spots);
                int r=clamp((243+24*grain)*brightness*q);
                int g=clamp((125+70*fine+30*large)*brightness*q);
                int b=clamp((19+34*grain)*brightness*q);
                int alpha=clamp(255f*Math.min(1f,Math.max(0f,(1f-(float)Math.sqrt(r2))*n*.22f)));
                pix[y*n+x]=Color.argb(alpha,r,g,b);
            }
        }
        Bitmap result=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);
        result.setPixels(pix,0,n,0,0,n,n);
        return result;
    }
}
