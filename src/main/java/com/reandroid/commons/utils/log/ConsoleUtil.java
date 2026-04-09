/*
 * Taken from https://github.com/JesusFreke/smali
 */

package com.reandroid.commons.utils.log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleUtil {

    private static final boolean IS_WINDOWS;
    private static final int DEFAULT_WIDTH;

    static {
        String str = System.getProperty("os.name");
        if (str == null) {
            str = "";
        }
        IS_WINDOWS = str.toLowerCase().contains("windows");
        DEFAULT_WIDTH = 80;
    }

    private static Boolean succeedOnce;
    /**
     * Attempt to find the width of the console. If it can't get the width, return a default of 80
     * @return The current console width
     */
    public static int getConsoleWidth() {
        if (IS_WINDOWS) {
            try {
                return attemptMode();
            } catch (Throwable ignored) {
                succeedOnce = false;
            }
        } else {
            try {
                return attemptStty();
            } catch (Exception ex) {
                succeedOnce = false;
            }
        }

        return DEFAULT_WIDTH;
    }
    public static boolean isConsole() {
        Boolean succeed = succeedOnce;
        if (succeed == null) {
            getConsoleWidth();
            succeed = succeedOnce;
        }
        return succeed != null && succeed;
    }
    public static Boolean getSucceedOnce() {
        return succeedOnce;
    }
    private static int attemptStty() {
        String output = attemptCommand(new String[]{"sh", "-c", "stty size < /dev/tty"});
        if (output == null) {
            succeedOnce = false;
            return DEFAULT_WIDTH;
        }
        String[] vals = output.trim().split(" ");
        if (vals.length < 2) {
            logWidthIssue(output);
            succeedOnce = false;
            return DEFAULT_WIDTH;
        }
        try {
            int result = Integer.parseInt(vals[1]);
            if (result > 0) {
                if(succeedOnce == null || !succeedOnce){
                    succeedOnce = true;
                }
                return result;
            }
        } catch (NumberFormatException ignored) {
        }
        logWidthIssue(output);
        succeedOnce = false;
        return DEFAULT_WIDTH;
    }

    private static int attemptMode() {
        String output = attemptCommand(new String[]{"mode", "con"});
        if (output == null) {
            succeedOnce = false;
            return DEFAULT_WIDTH;
        }

        Pattern pattern = Pattern.compile("Columns:\\S*(\\d+)");
        Matcher m = pattern.matcher(output);
        if (m.find()) {
            int result = Integer.parseInt(m.group(1));
            if (result > 0) {
                if (succeedOnce == null || !succeedOnce) {
                    succeedOnce = true;
                }
                return result;
            }
        }
        logWidthIssue(output);
        succeedOnce = false;
        return DEFAULT_WIDTH;
    }

    public static String attemptCommand(String[] command) {
        StringBuffer buffer = null;

        try {
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (buffer == null) {
                    buffer = new StringBuffer();
                }

                buffer.append(line);
            }

            if (buffer != null) {
                return buffer.toString();
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean issue_logged;
    private static void logWidthIssue(String output) {
        if (issue_logged) {
            return;
        }
        issue_logged = true;
        String msg = "Failed to parse console width\n" +
                "Please create issue at https://github.com/REAndroid/APKEditor\n" +
                "os.name=" + System.getProperty("os.name")
                + "\nCommand output=" + output;
        System.err.println(msg);
    }
}
