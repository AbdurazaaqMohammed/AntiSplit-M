package com.reandroid.commons.utils.log;

import java.io.PrintStream;

public class StdLogger extends Logger{
    private static StdLogger sInstance;
    private final Object mLock=new Object();
    private final PrintStream printStream;
    private boolean mSameLine;
    private StdLogger() {
        super();
        this.printStream=System.err;
    }
    @Override
    public void writeLine(String line) {
        synchronized (mLock){
            if(line==null){
                line="null";
            }
            if(mSameLine){
                writeSameLine(" ");
                mSameLine = false;
            }
            printStream.println(line);
        }
    }
    @Override
    public void writeSameLine(String line) {
        synchronized (mLock){
            if (!ConsoleUtil.isConsole()) {
                return;
            }
            if(line == null){
                line = "null";
            }
            mSameLine = true;
            int width = checkConsoleWidth();
            StringBuilder builder = new StringBuilder();
            builder.append('\r');
            for(int i = 0; i < width; i++){
                builder.append(' ');
            }
            builder.append('\r');
            // leave space for blinking cursor;
            width = width - 1;
            builder.append(' ');
            if(line.length() > width){
                line = line.substring(0, width);
            }
            builder.append(line);
            builder.append('\r');
            printStream.print(builder.toString());
        }
    }
    static StdLogger getInstance(){
        if(sInstance!=null){
            return sInstance;
        }
        synchronized (StdLogger.class){
            sInstance=new StdLogger();
            sInstance.setEnable(true);
            return sInstance;
        }
    }
}
