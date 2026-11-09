package com.termux.app.ra;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSession;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Real split terminal backed by tmux panes, running inside ONE real Termux PTY.
 * Uses a new isolated Termux session so existing programs are never injected with commands.
 */
public final class RaSplitLauncher {
    private final TermuxActivity activity;
    private final Handler main = new Handler(Looper.getMainLooper());

    public RaSplitLauncher(TermuxActivity activity) { this.activity = activity; }

    public void launch() {
        File tmux = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, "tmux");
        if (tmux.isFile() && tmux.canExecute()) {
            createSessionWithCommand("split", makeSplitCommand());
        } else {
            new AlertDialog.Builder(activity)
                .setTitle("Split terminal requires tmux")
                .setMessage("tmux provides two real interactive terminal panes in one Termux session. " +
                    "Install it from the Termux package manager first. This won't affect existing sessions.")
                .setPositiveButton("Install tmux", (dialog, which) ->
                    createSessionWithCommand("tmux setup", "pkg install tmux"))
                .setNegativeButton("Cancel", null).show();
        }
    }

    private static String makeSplitCommand() {
        String name = "valtheron_" + android.os.Process.myPid() + "_" +
            Long.toString(android.os.SystemClock.uptimeMillis(), 36);
        // -v gives top/bottom panes; tmux handles PTY, focus, resizing and keystrokes itself.
        return "tmux new-session -d -s " + name +
            " && tmux split-window -v -t " + name + ":0" +
            " && tmux attach-session -t " + name;
    }

    private void createSessionWithCommand(String name, String command) {
        TermuxService service = activity.getTermuxService();
        TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
        if (service == null || client == null) return;
        // Pinned Termux upstream enforces a maximum of eight sessions.
        if (service.getTermuxSessionsSize() >= 8) {
            activity.showToast("Session limit reached; close a session first.", true);
            return;
        }
        TerminalSession before = activity.getCurrentSession();
        client.addNewSession(false, name);
        TerminalSession created = activity.getCurrentSession();
        if (created == null || created == before) return;

        // Let the newly attached PTY start its shell before sending a command.
        final String line = command + "\n";
        final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        sendWhenReady(created, bytes, 0);
    }

    private void sendWhenReady(TerminalSession session, byte[] command, int tries) {
        if (!activity.isFinishing() && session.getPid() > 0 && session.isRunning()) {
            session.write(command, 0, command.length);
            return;
        }
        if (tries < 20 && !activity.isFinishing()) {
            main.postDelayed(() -> sendWhenReady(session, command, tries + 1), 100);
        } else {
            activity.showToast("New shell did not start; split command was not sent.", true);
        }
    }
}
