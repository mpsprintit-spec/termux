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
 * Open a real two-pane tmux session inside a fresh Termux PTY.
 * Upper pane: interactive shell. Lower pane: live htop; no sample data or fake view.
 *
 * Only a newly created shell receives commands; existing sessions are untouched.
 */
public final class RaSplitLauncher {
    private final TermuxActivity activity;
    private final Handler main = new Handler(Looper.getMainLooper());

    public RaSplitLauncher(TermuxActivity activity) {
        this.activity = activity;
    }

    public void launch() {
        boolean hasTmux = installedExecutable("tmux");
        boolean hasHtop = installedExecutable("htop");
        if (hasTmux && hasHtop) {
            createSessionWithCommand("monitor", makeSplitCommand());
            return;
        }

        // Package installation changes Termux and may use network/storage.
        // Ask for explicit consent; do not silently install.
        String missing;
        if (!hasTmux && !hasHtop) {
            missing = "tmux and htop";
        } else {
            missing = hasTmux ? "htop" : "tmux";
        }
        String packages = (!hasTmux ? "tmux " : "") + (!hasHtop ? "htop" : "");
        String command = "pkg install -y " + packages.trim() + " && " + makeSplitCommand();

        new AlertDialog.Builder(activity)
            .setTitle("Set up live split monitor")
            .setMessage("The lower terminal pane runs real htop, while the upper pane " +
                "remains an interactive shell.\n\n" +
                "To open this view, Termux needs " + missing + ". " +
                "Install the required package(s) using the Termux package manager?")
            .setPositiveButton("Install and open", (dialog, which) ->
                createSessionWithCommand("monitor setup", command))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private static boolean installedExecutable(String name) {
        File binary = new File(TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH, name);
        return binary.isFile() && binary.canExecute();
    }

    private static String makeSplitCommand() {
        // Private per-launch tmux session: never attach to or alter existing sessions.
        String name = "valtheron_" + android.os.Process.myPid() + "_" +
            Long.toString(android.os.SystemClock.uptimeMillis(), 36);

        // Split vertically (top/bottom); give htop about 58% of rows.
        // When htop quits, its pane remains as an interactive shell.
        // Disable only this session's tmux status line, conserving mobile height.
        // Focus starts in the upper interactive shell.
        return "tmux new-session -d -s " + name +
            " && tmux split-window -v -p 58 -t " + name +
            ":0 'htop; exec sh'" +
            " && tmux set-option -t " + name + " status off" +
            " && tmux select-pane -t " + name + ":0.0" +
            " && tmux attach-session -t " + name;
    }

    private void createSessionWithCommand(String name, String command) {
        TermuxService service = activity.getTermuxService();
        TermuxTerminalSessionActivityClient client = activity.getTermuxTerminalSessionClient();
        if (service == null || client == null || activity.isFinishing()) return;

        // MAX_SESSIONS in the pinned Termux activity client.
        if (service.getTermuxSessionsSize() >= 8) {
            activity.showToast("Session limit reached; close a session first.", true);
            return;
        }

        TerminalSession before = activity.getCurrentSession();
        client.addNewSession(false, name);
        TerminalSession created = activity.getCurrentSession();
        if (created == null || created == before) {
            activity.showToast("Could not create the split monitor session.", true);
            return;
        }

        byte[] bytes = (command + "\n").getBytes(StandardCharsets.UTF_8);
        sendWhenReady(created, bytes, 0);
    }

    private void sendWhenReady(TerminalSession session, byte[] command, int tries) {
        if (activity.isFinishing() || activity.isDestroyed() || !session.isRunning()) return;
        if (session.getPid() > 0) {
            // Shell startup is asynchronous; allow a short initialization window.
            main.postDelayed(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed() &&
                    session.isRunning() && session.getPid() > 0) {
                    session.write(command, 0, command.length);
                }
            }, 300);
            return;
        }
        if (tries < 30) {
            main.postDelayed(() -> sendWhenReady(session, command, tries + 1), 100);
        } else {
            activity.showToast("Split monitor shell did not start.", true);
        }
    }
}
