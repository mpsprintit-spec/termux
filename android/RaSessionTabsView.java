package com.termux.app.ra;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.termux.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact, horizontally scrollable, half-rounded terminal tabs.
 *
 * This is a native view on top of real Termux sessions. Switching or creating
 * a tab always delegates to TermuxTerminalSessionActivityClient.
 */
public final class RaSessionTabsView extends HorizontalScrollView {
    private static final int SURFACE = 0xff090c10;
    private static final int TAB = 0xff10151c;
    private static final int ACTIVE = 0xff171e28;
    private static final int ACCENT = 0xff299ffe;
    private static final int FOREGROUND = 0xffecf2f8;
    private static final int MUTED = 0xffa0aab6;

    private final TermuxActivity activity;
    private final LinearLayout strip;
    private String snapshot = "";

    public RaSessionTabsView(TermuxActivity activity) {
        super(activity);
        this.activity = activity;
        setFillViewport(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        setBackgroundColor(SURFACE);

        strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(10), dp(2), dp(8), dp(0));
        addView(strip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        refresh();
    }

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value + .5f);
    }

    private GradientDrawable tabBackground(int color) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color);
        float r = dp(16);
        // Chrome-like half-rounded tab: rounded shoulders, flat lower edge.
        result.setCornerRadii(new float[]{r, r, r, r, dp(3), dp(3), dp(3), dp(3)});
        return result;
    }

    private String displayName(TerminalSession session, int position) {
        String named = session.mSessionName;
        if (named != null && !named.trim().isEmpty()) return compact(named);
        if (position == 0) return "main";
        String title = session.getTitle();
        if (title != null && !title.trim().isEmpty()) return compact(title);
        return "shell";
    }

    private static String compact(String name) {
        // Terminal titles can contain control chars; never render them in Android UI.
        String clean = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (clean.length() > 17) clean = clean.substring(0, 16) + "…";
        return clean.isEmpty() ? "shell" : clean;
    }

    /** Call on UI thread after session creation, switching, rename or closure. */
    public void refresh() {
        TermuxService service = activity.getTermuxService();
        List<TerminalSession> sessions = new ArrayList<>();
        if (service != null) {
            for (TermuxSession entry : new ArrayList<>(service.getTermuxSessions())) {
                if (entry != null && entry.getTerminalSession() != null) {
                    sessions.add(entry.getTerminalSession());
                }
            }
        }

        TerminalSession current = activity.getCurrentSession();
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < sessions.size(); i++) {
            TerminalSession s = sessions.get(i);
            state.append(s.mHandle).append(':').append(displayName(s, i))
                .append(':').append(s == current).append(':').append(s.isRunning()).append('|');
        }
        String newSnapshot = state.toString();
        if (newSnapshot.equals(snapshot)) return;
        snapshot = newSnapshot;

        strip.removeAllViews();
        View activeView = null;
        for (int i = 0; i < sessions.size(); i++) {
            TerminalSession s = sessions.get(i);
            boolean selected = s == current;
            LinearLayout wrapper = new LinearLayout(activity);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setBackground(tabBackground(selected ? ACTIVE : TAB));

            TextView title = new TextView(activity);
            title.setText(">_  " + displayName(s, i));
            title.setTextSize(13);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setTextColor(selected ? FOREGROUND : MUTED);
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            title.setPadding(dp(14), 0, dp(14), 0);
            title.setContentDescription("Session " + displayName(s, i));
            wrapper.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(35)));

            View underline = new View(activity);
            underline.setBackgroundColor(selected ? ACCENT : Color.TRANSPARENT);
            wrapper.addView(underline, new LinearLayout.LayoutParams(-1, dp(2)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(37));
            lp.setMargins(0, 0, dp(5), 0);
            strip.addView(wrapper, lp);

            wrapper.setOnClickListener(v -> {
                TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
                if (client != null) {
                    client.setCurrentSession(s);
                    refresh();
                    activity.getTerminalView().requestFocus();
                }
            });
            wrapper.setOnLongClickListener(v -> {
                TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
                if (client != null) client.renameSession(s);
                return true;
            });
            if (selected) activeView = wrapper;
        }

        TextView add = new TextView(activity);
        add.setText("+");
        add.setTextColor(0xffc0d5e9);
        add.setTextSize(23);
        add.setGravity(Gravity.CENTER);
        add.setBackground(tabBackground(TAB));
        add.setContentDescription("New Termux session");
        strip.addView(add, new LinearLayout.LayoutParams(dp(40), dp(37)));
        add.setOnClickListener(v -> {
            TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
            if (client != null && activity.getTermuxService() != null) client.addNewSession(false, null);
        });

        final View target = activeView;
        if (target != null) {
            post(() -> smoothScrollTo(
                Math.max(0, target.getLeft() - getWidth() / 4), 0));
        }
    }
}
