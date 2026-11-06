package com.termux.app.ra;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;

/**
 * Single-Activity native Termux UI. Original TerminalView/PTY is not reimplemented.
 * A native solar background is decorative and never intercepts terminal input.
 */
public final class RaTerminalChrome {
    private final RaSolarBackdropView backdrop;
    private final RaSystemHud hud;
    private String animationMode="eco";
    private final TermuxActivity activity;

    private static int dp(TermuxActivity a, float n) { return (int)(n*a.getResources().getDisplayMetrics().density+.5f); }

    private static GradientDrawable bg(int color, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);shape.setCornerRadius(15f);
        if (border!=0) shape.setStroke(1,border);
        return shape;
    }

    public RaTerminalChrome(TermuxActivity activity, TerminalView terminal) {
        this.activity=activity;
        ViewGroup originalParent=(ViewGroup)terminal.getParent();
        if (originalParent==null) throw new IllegalStateException("Termux TerminalView has no parent");
        int originalIndex=originalParent.indexOfChild(terminal);
        ViewGroup.LayoutParams outerParams=terminal.getLayoutParams();
        originalParent.removeView(terminal);

        LinearLayout shell=new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.BLACK);

        LinearLayout header=new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(activity,10),0,dp(activity,9),0);
        header.setBackgroundColor(Color.rgb(2,4,8));
        shell.addView(header,new LinearLayout.LayoutParams(-1,dp(activity,49)));

        TextView menu=text("☰",20,0xffb7f2ff);
        menu.setGravity(Gravity.CENTER);
        header.addView(menu,new LinearLayout.LayoutParams(dp(activity,36),-1));
        menu.setContentDescription("Open Termux session drawer");
        menu.setOnClickListener(v -> {
            DrawerLayout drawer=activity.findViewById(R.id.drawer_layout);
            if (drawer!=null) drawer.openDrawer(Gravity.LEFT);
        });

        RaLogoView logo = new RaLogoView(activity);
        header.addView(logo,new LinearLayout.LayoutParams(dp(activity,64),-1));

        LinearLayout label=new LinearLayout(activity);
        label.setOrientation(LinearLayout.VERTICAL);
        label.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=text("RA TERMINAL",14,Color.WHITE);
        name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView sub=text("PERSONAL  •  NATIVE TERMUX",8,0xff73b8cf);
        label.addView(name);label.addView(sub);
        header.addView(label,new LinearLayout.LayoutParams(0,-1,1));
        TextView status=text("● LIVE",9,0xff69f1d4);
        status.setPadding(dp(activity,7),dp(activity,5),dp(activity,7),dp(activity,5));
        status.setBackground(bg(0xff0a1e27,0xff168aa7));
        header.addView(status);
        TextView perf=text("ECO",9,0xff8beeff);
        perf.setPadding(dp(activity,5),dp(activity,5),0,dp(activity,5));
        header.addView(perf);
        perf.setContentDescription("Change neon background animation performance");
        perf.setOnClickListener(v -> {
            animationMode="eco".equals(animationMode)?"flow":("flow".equals(animationMode)?"off":"eco");
            perf.setText(animationMode.toUpperCase(java.util.Locale.US));
            backdrop.setAnimationMode(animationMode);
        });

        FrameLayout scene=new FrameLayout(activity);
        shell.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
        backdrop=new RaSolarBackdropView(activity);
        scene.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout foreground=new LinearLayout(activity);
        final boolean compact=activity.getResources().getConfiguration().screenWidthDp<520;
        foreground.setOrientation(compact?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        scene.addView(foreground,new FrameLayout.LayoutParams(-1,-1));

        terminal.setBackgroundColor(Color.TRANSPARENT);
        hud=new RaSystemHud(activity);
        if(compact){
            terminal.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
            foreground.addView(terminal);
            LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(activity,65));
            hp.setMargins(dp(activity,4),0,dp(activity,4),dp(activity,2));
            foreground.addView(hud,hp);
        }else{
            terminal.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1));
            foreground.addView(terminal);
            LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(activity,112),-1);
            hp.setMargins(dp(activity,2),dp(activity,7),dp(activity,5),dp(activity,7));
            foreground.addView(hud,hp);
        }

        originalParent.addView(shell,originalIndex,outerParams);
        status.setContentDescription("Toggle live resource panels");
        status.setOnClickListener(v -> {
            boolean showing = hud.getVisibility() == View.VISIBLE;
            hud.setVisibility(showing ? View.GONE : View.VISIBLE);
            status.setText(showing ? "○ HUD" : "● LIVE");
        });
        terminal.requestFocus();
    }

    private TextView text(String s,int size,int color) {
        TextView t=new TextView(activity);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setFontFeatureSettings("tnum");return t;
    }
    public void resume(){ backdrop.resume();hud.resume(); }
    public void pause(){ hud.pause();backdrop.pause(); }
    public void destroy(){ hud.pause();backdrop.release(); }
}
