package com.termux.app.ra;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

/** Smooth, fully vector RA monogram. No bitmap or square logo placeholder. */
public final class RaLogoView extends View {
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path monogram = new Path();
    private final Path swash = new Path();
    public RaLogoView(Context context) {
        super(context);
        setContentDescription("RA personal monogram");
        monogram.moveTo(14,58);
        monogram.cubicTo(23,30,24,11,34,7);
        monogram.cubicTo(43,2,51,7,48,17);
        monogram.cubicTo(45,28,31,29,23,30);
        monogram.cubicTo(43,27,58,40,57,59);
        monogram.moveTo(62,58);
        monogram.cubicTo(78,29,97,8,106,8);
        monogram.cubicTo(113,8,111,32,111,58);
        monogram.moveTo(77,42);
        monogram.cubicTo(90,41,105,39,126,38);
        swash.moveTo(13,58);
        swash.cubicTo(40,64,88,59,130,51);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float scale = Math.min(getWidth()/146f, getHeight()/72f);
        c.save();
        c.translate((getWidth()-146f*scale)/2f,(getHeight()-72f*scale)/2f);
        c.scale(scale,scale);
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeWidth(5.2f);
        ink.setShader(new LinearGradient(5,14,131,58,
                new int[]{0xff00efff,0xff46aaff,0xffb94dff}, null,Shader.TileMode.CLAMP));
        ink.setShadowLayer(11,0,0,0xff1d9bff);
        c.drawPath(monogram,ink);
        ink.setStrokeWidth(2.5f);
        ink.setShadowLayer(6,0,0,0xffb64dff);
        c.drawPath(swash,ink);
        ink.clearShadowLayer();
        ink.setShader(null);
        c.restore();
    }
}
