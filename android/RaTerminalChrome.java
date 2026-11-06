package com.termux.app.ra;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.drawerlayout.widget.DrawerLayout;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;

/**
 * Single-Activity native Termux UI. Original TerminalView/PTY is not reimplemented.
 * The WebView is a decorative, noninteractive layer, not a browser or terminal.
 */
public final class RaTerminalChrome {
    private final WebView backdrop;
    private final RaSystemHud hud;
    private final TermuxActivity activity;

    private static int dp(TermuxActivity a, float n) { return (int)(n*a.getResources().getDisplayMetrics().density+.5f); }

    private static GradientDrawable bg(int color, int border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);shape.setCornerRadius(15f);
        if (border!=0) shape.setStroke(1,border);
        return shape;
    }

    @SuppressLint("SetJavaScriptEnabled")
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

        FrameLayout scene=new FrameLayout(activity);
        shell.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
        backdrop=new WebView(activity);
        backdrop.setBackgroundColor(Color.BLACK);
        backdrop.setFocusable(false);backdrop.setFocusableInTouchMode(false);
        backdrop.setClickable(false);
        backdrop.setVerticalScrollBarEnabled(false);backdrop.setHorizontalScrollBarEnabled(false);
        WebSettings settings=backdrop.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT>=21) settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        backdrop.setWebChromeClient(new WebChromeClient());
        backdrop.setWebViewClient(new WebViewClient());
        backdrop.loadUrl("file:///android_asset/ra_background.html");
        scene.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout foreground=new LinearLayout(activity);
        foreground.setOrientation(LinearLayout.HORIZONTAL);
        scene.addView(foreground,new FrameLayout.LayoutParams(-1,-1));

        terminal.setBackgroundColor(Color.TRANSPARENT);
        terminal.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1));
        foreground.addView(terminal);

        hud=new RaSystemHud(activity);
        int width=dp(activity,112);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(width,-1);
        hp.setMargins(dp(activity,2),dp(activity,7),dp(activity,5),dp(activity,7));
        foreground.addView(hud,hp);

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
    public void resume(){ backdrop.onResume();hud.resume(); }
    public void pause(){ hud.pause();backdrop.onPause(); }
    public void destroy(){ hud.pause();backdrop.stopLoading();backdrop.loadUrl("about:blank");backdrop.destroy(); }
}
