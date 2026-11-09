package com.termux.app.ra;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.termux.app.TermuxActivity;
import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalEmulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * On-demand command-history search and executable completion.
 * Does not execute history items automatically. Never writes an index to disk.
 */
public final class RaCommandPalette {
    private final TermuxActivity activity;
    private static final int BG = 0xff0b1016;
    private static final int FG = 0xffdde7f1;

    public RaCommandPalette(TermuxActivity activity) { this.activity = activity; }

    private int dp(int n) { return (int)(n * activity.getResources().getDisplayMetrics().density + .5f); }

    public void show(boolean completion) {
        TerminalSession session = activity.getCurrentSession();
        if (session == null) return;

        // Read the visible cursor token, not key events or guessed shell state.
        // Completion inserts only the missing suffix of the token actually on screen.
        final String existingWord = completion ? cursorWord(session) : "";

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(BG);
        content.setPadding(dp(12), dp(12), dp(12), dp(10));

        EditText query = new EditText(activity);
        query.setSingleLine(true);
        query.setTextColor(FG);
        query.setHintTextColor(0xff8995a6);
        query.setTextSize(15);
        query.setHint(completion ? "Command prefix, e.g. git, go..." : "Search command history");
        if (completion && !existingWord.isEmpty()) {
            query.setText(existingWord);
            query.setSelection(query.length());
        }
        content.addView(query, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView guide = new TextView(activity);
        guide.setText(completion
            ? "Insert the missing suffix only. No Enter is sent."
            : "Insert the selected command only. No Enter is sent.");
        guide.setTextColor(0xffa0acbb);
        guide.setTextSize(12);
        guide.setPadding(0, dp(6), 0, dp(7));
        content.addView(guide);

        ListView list = new ListView(activity);
        list.setDividerHeight(0);
        content.addView(list, new LinearLayout.LayoutParams(-1, dp(280)));

        List<String> original = completion ? loadExecutableNames() : loadHistory();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            activity, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(FG);
                row.setTextSize(14);
                row.setBackgroundColor(BG);
                row.setPadding(dp(10), dp(4), dp(6), dp(4));
                return row;
            }
        };
        list.setAdapter(adapter);
        refresh(adapter, original, query.getText().toString(), completion);

        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh(adapter, original, s.toString(), completion);
            }
            @Override public void afterTextChanged(Editable editable) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle(completion ? "Command completion" : "Command search")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            String selected = adapter.getItem(position);
            if (selected == null || !session.isRunning()) return;
            // Insert only a completion suffix when a partial token already exists.
            // Do not silently erase the user's current shell command.
            String insertion = selected;
            if (completion && !existingWord.isEmpty()) {
                if (!selected.startsWith(existingWord)) {
                    activity.showToast("Completion must start with the existing terminal prefix.", false);
                    return;
                }
                insertion = selected.substring(existingWord.length());
            }
            byte[] bytes = insertion.getBytes(StandardCharsets.UTF_8);
            session.write(bytes, 0, bytes.length);
            dialog.dismiss();
            activity.getTerminalView().requestFocus();
        });
        dialog.show();
    }

    private static String cursorWord(TerminalSession session) {
        try {
            TerminalEmulator em = session.getEmulator();
            if (em == null || em.getCursorCol() < 1) return "";
            int row = em.getCursorRow();
            int lastCol = em.getCursorCol() - 1;
            String before = em.getScreen().getSelectedText(0, row, lastCol, row, false);
            int split = Math.max(before.lastIndexOf(' '), before.lastIndexOf('\t'));
            String word = before.substring(split + 1).trim();
            // Keep this feature conservative: do not complete arbitrary control sequences.
            return word.matches("[a-zA-Z0-9_./-]{1,64}") ? word : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void refresh(ArrayAdapter<String> adapter, List<String> source,
                                String query, boolean completion) {
        adapter.clear();
        String q = query.toLowerCase(Locale.ROOT).trim();
        int count = 0;
        for (String word : source) {
            String lower = word.toLowerCase(Locale.ROOT);
            if (q.isEmpty() || (completion ? lower.startsWith(q) : lower.contains(q))) {
                adapter.add(word);
                if (++count == 60) break;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private static boolean sensitive(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("password") || lower.contains("passwd") ||
            lower.contains("token") || lower.contains("secret") ||
            lower.contains("api_key") || lower.contains("apikey") ||
            lower.contains("authorization") || lower.contains("sshpass") ||
            lower.contains("passphrase") || lower.startsWith("export ") ||
            lower.contains("private_key");
    }

    private static List<String> loadHistory() {
        Set<String> history = new LinkedHashSet<>();
        String home = TermuxConstants.TERMUX_HOME_DIR_PATH;
        for (String name : new String[]{".bash_history", ".zsh_history", ".local/share/fish/fish_history"}) {
            File file = new File(home, name);
            if (!file.isFile() || !file.canRead()) continue;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int n = 0;
                while ((line = reader.readLine()) != null && n++ < 25000) {
                    line = line.trim();
                    if (name.equals(".zsh_history") && line.startsWith(": ")) {
                        int i = line.indexOf(';');
                        if (i >= 0) line = line.substring(i + 1).trim();
                    }
                    if (name.contains("fish_history")) {
                        if (!line.startsWith("- cmd: ")) continue;
                        line = line.substring(7).trim();
                    }
                    if (line.isEmpty() || line.length() > 300 || sensitive(line)) continue;
                    history.add(line);
                    if (history.size() > 2000) {
                        String first = history.iterator().next();
                        history.remove(first);
                    }
                }
            } catch (Exception ignored) { /* No history is valid on a fresh installation. */ }
        }
        List<String> result = new ArrayList<>(history);
        Collections.reverse(result);
        return result;
    }

    private static List<String> loadExecutableNames() {
        Set<String> commands = new LinkedHashSet<>();
        Collections.addAll(commands,
            "cd", "ls", "pwd", "clear", "pkg", "apt", "git", "go", "gofmt",
            "python", "python3", "node", "npm", "ssh", "tmux", "nvim", "vim",
            "grep", "find", "cat", "cp", "mv", "mkdir", "rm", "curl", "tar",
            "htop", "top", "ps", "chmod", "which", "echo");
        File bin = TermuxConstants.TERMUX_BIN_PREFIX_DIR;
        File[] files = bin.listFiles();
        if (files != null) {
            for (File f : files) {
                if (commands.size() >= 1800) break;
                if (f.isFile() && f.canExecute() && f.getName().length() <= 64)
                    commands.add(f.getName());
            }
        }
        List<String> result = new ArrayList<>(commands);
        Collections.sort(result);
        return result;
    }
}
