package com.termux.app.ra;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.view.TerminalView;

/**
 * Minimal native ValTherOn chrome for the real Termux terminal.
 * No video, no animated backgrounds, no replacement PTY or synthetic terminal.
 */
public final class RaTerminalChrome {
    private final TermuxActivity activity;
    private final RaSessionTabsView tabs;
    private final RaSystemHud hud;

    private int dp(float n) {
        return (int) (n * activity.getResources().getDisplayMetrics().density + .5f);
    }

    private TextView label(String value, int sp, int color) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setSingleLine(true);
        return t;
    }

    public RaTerminalChrome(TermuxActivity activity, TerminalView terminal) {
        this.activity = activity;
        ViewGroup parent = (ViewGroup) terminal.getParent();
        if (parent == null) throw new IllegalStateException("Original Termux terminal parent missing");
        int originalIndex = parent.indexOfChild(terminal);
        ViewGroup.LayoutParams originalParams = terminal.getLayoutParams();
        parent.removeView(terminal);

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.BLACK);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(13), 0, dp(8), 0);
        header.setBackgroundColor(0xff080b10);
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView brand = label(">_  ValTherOn", 16, 0xffe6edf5);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        brand.setContentDescription("ValTherOn native terminal");
        header.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));

        TextView status = label("CPU --   RAM --", 11, 0xff5bcebd);
        status.setTypeface(Typeface.MONOSPACE);
        status.setContentDescription("Live performance; tap for detailed monitoring");
        header.addView(status, new LinearLayout.LayoutParams(-2, -1));

        // One-tap split monitor control: makes the htop pane discoverable.
        TextView split = label("▤", 23, 0xff85bcdf);
        split.setGravity(Gravity.CENTER);
        split.setContentDescription("Open split terminal with live htop monitor");
        header.addView(split, new LinearLayout.LayoutParams(dp(36), -1));
        split.setOnClickListener(v -> new RaSplitLauncher(activity).launch());

        TextView more = label("⋮", 24, 0xff96a4b5);
        more.setGravity(Gravity.CENTER);
        more.setContentDescription("More Termux options");
        header.addView(more, new LinearLayout.LayoutParams(dp(37), -1));
        more.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(activity, more);
            menu.getMenu().add(0, 1, 0, "Split terminal + live htop");
            menu.getMenu().add(0, 2, 1, "Command autocomplete");
            menu.getMenu().add(0, 3, 2, "Command search");
            menu.getMenu().add(0, 4, 3, "Termux sessions");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) new RaSplitLauncher(activity).launch();
                else if (item.getItemId() == 2) new RaCommandPalette(activity).show(true);
                else if (item.getItemId() == 3) new RaCommandPalette(activity).show(false);
                else if (item.getItemId() == 4) {
                    DrawerLayout drawer = activity.findViewById(R.id.drawer_layout);
                    if (drawer != null) drawer.openDrawer(Gravity.LEFT);
                }
                return true;
            });
            menu.show();
        });

        tabs = new RaSessionTabsView(activity);
        shell.addView(tabs, new LinearLayout.LayoutParams(-1, dp(46)));

        // A single real TerminalView remains owned by the original TermuxActivity.
        terminal.setBackgroundColor(Color.BLACK);
        shell.addView(terminal, new LinearLayout.LayoutParams(-1, 0, 1));
        parent.addView(shell, originalIndex, originalParams);

        hud = new RaSystemHud(activity);
        hud.setMetricsListener((cpu, ram) -> status.setText(cpu + "   " + ram));
        status.setOnClickListener(v -> showDetails());
        terminal.requestFocus();
    }

    private void showDetails() {
        RaSystemHud details = new RaSystemHud(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setBackgroundColor(0xff080b10);
        content.setPadding(dp(12), dp(8), dp(12), dp(8));
        content.addView(details, new LinearLayout.LayoutParams(-1, dp(360)));
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Performance")
            .setView(content)
            .setPositiveButton("Close", (d, which) -> {})
            .create();
        dialog.setOnShowListener(d -> details.resume());
        dialog.setOnDismissListener(d -> details.pause());
        dialog.show();
    }

    public void refreshTabs() { tabs.refresh(); }
    public void resume() { hud.resume(); refreshTabs(); }
    public void pause() { hud.pause(); }
    public void destroy() { hud.pause(); hud.setMetricsListener(null); }
}
