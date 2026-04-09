package com.reandroid.commons.utils.log;

import com.reandroid.commons.utils.ElapsedTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Logger {
    private static Logger sInstance;
    private final ElapsedTime elapsedTime;
    private final ElapsedTime logTime;
    private final ElapsedTime widthCheckTime;
    private final List<Logger> subLoggers;
    private boolean hideTime;
    private boolean enable=true;
    private int mConsoleWidth;
    Logger(){
        this.elapsedTime=new ElapsedTime();
        this.logTime=new ElapsedTime();
        this.widthCheckTime = new ElapsedTime();
        this.subLoggers=new ArrayList<>();
    }
    public void every2Second(String msg){
        if(!isEnable()){
            return;
        }
        if(!logTime.isExpired(INTERVAL)){
            return;
        }
        logTime.reset();
        StringBuilder line=buildLine(null, msg);
        writeLine(line.toString());
        for(Logger sub:subLoggers){
            sub.info(msg);
        }
    }
    public void every2SecondSameLine(String msg){
        if(!isEnable()){
            return;
        }
        if(!logTime.isExpired(INTERVAL)){
            return;
        }
        logTime.reset();
        StringBuilder line=buildLine(null, msg);
        writeSameLine(line.toString());
        for(Logger sub:subLoggers){
            sub.writeSameLine(msg);
        }
    }
    public void onSameLine(String msg){
        if(!isEnable()){
            return;
        }
        StringBuilder line=buildLine(null, msg);
        writeSameLine(line.toString());
        for(Logger sub:subLoggers){
            sub.onSameLine(msg);
        }
    }
    public void info(String msg){
        if(!isEnable()){
            return;
        }
        StringBuilder line=buildLine(TYPE_INFO, msg);
        writeLine(line.toString());
        for(Logger sub:subLoggers){
            sub.info(msg);
        }
    }
    public void warn(String msg){
        if(!isEnable()){
            return;
        }
        StringBuilder line=buildLine(TYPE_WARN, msg);
        writeLine(line.toString());
        for(Logger sub:subLoggers){
            sub.warn(msg);
        }
    }
    public void error(String msg){
        if(!isEnable()){
            return;
        }
        StringBuilder line=buildLine(TYPE_ERROR, msg);
        writeLine(line.toString());
        for(Logger sub:subLoggers){
            sub.error(msg);
        }
    }
    public void error(Throwable tr){
        if(!isEnable()){
            return;
        }
        error(null, tr);
    }
    public void error(String msg, Throwable tr){
        if(!isEnable()){
            return;
        }
        StringBuilder line;
        if(msg!=null){
            line=buildLine(TYPE_ERROR, msg);
        }else {
            line=new StringBuilder();
        }
        if(tr!=null){
            if(msg==null){
                line=buildLine(TYPE_ERROR, tr.getClass().getName()+" "+tr.getMessage());
            }else {
                line.append(' ').append(tr.getClass().getSimpleName()).append(' ').append(tr.getMessage());
            }
            appendStackTraces(line, tr);
        }
        writeLine(line.toString());
        for(Logger sub:subLoggers){
            sub.error(msg, tr);
        }
    }

    public void addSubLogger(Logger logger){
        if(logger==null||logger==this){
            return;
        }
        if(!subLoggers.contains(logger)){
            subLoggers.add(logger);
        }
    }

    public boolean isEnable() {
        return enable;
    }
    public void setEnable(boolean enable) {
        setEnable(enable, false);
    }
    public void setEnable(boolean enable, boolean setSubLoggers) {
        this.enable = enable;
        if(setSubLoggers){
            for(Logger sub:subLoggers){
                sub.setEnable(enable, true);
            }
        }
    }
    public boolean isHideTime() {
        return hideTime;
    }
    public void setHideTime(boolean hide) {
        this.hideTime = hide;
    }

    private void appendStackTraces(StringBuilder builder, Throwable tr){
        StackTraceElement[] elements=tr.getStackTrace();
        appendStackTraces(builder, elements);
        Throwable cause=tr.getCause();
        if(cause==null){
            return;
        }
        builder.append('\n').append(TRACE_TAB1).append("Caused by: ");
        builder.append(cause.getClass().getSimpleName());
        builder.append(' ').append(cause.getMessage());
        elements=cause.getStackTrace();
        appendStackTraces(builder, elements);
    }
    private void appendStackTraces(StringBuilder builder, StackTraceElement[] traceElements){
        if(traceElements==null){
            return;
        }
        int len=traceElements.length;
        if(len>MAX_TRACE){
            len=MAX_TRACE;
        }
        for(int i=0;i<len;i++){
            StackTraceElement element=traceElements[i];
            appendStackTrace(builder, element);
        }
    }
    private void appendStackTrace(StringBuilder builder, StackTraceElement element){
        if(element==null){
            return;
        }
        builder.append('\n');
        builder.append(TRACE_TAB2);
        builder.append("at ").append(element.toString());
    }
    private StringBuilder buildLine(String type, String msg){
        StringBuilder builder=new StringBuilder();
        boolean hide=isHideTime();
        if(!hide){
            builder.append(elapsedTime.now());
            builder.append(' ');
        }
        if(type!=null){
            builder.append(type).append(':').append(' ');
        }
        builder.append(msg);
        return builder;
    }
    public abstract void writeLine(String line);
    public abstract void writeSameLine(String line);

    public static void i(String msg){
        getLogger().info(msg);
    }
    public static void w(String msg){
        getLogger().warn(msg);
    }
    public static void e(String msg){
        getLogger().error(msg);
    }
    public static void e(Throwable tr){
        getLogger().error(tr);
    }
    public static void e(String msg, Throwable tr){
        getLogger().error(msg, tr);
    }
    public static void sameLine(String msg){
        getLogger().onSameLine(msg);
    }
    public static void timely(String msg){
        getLogger().every2Second(msg);
    }
    public static void timelySameLine(String msg){
        getLogger().every2SecondSameLine(msg);
    }

    int checkConsoleWidth(){
        if(mConsoleWidth == 0){
            updateConsoleWidth();
            return mConsoleWidth;
        }
        Boolean succeedOnce = ConsoleUtil.getSucceedOnce();
        if(succeedOnce != null && !succeedOnce){
            return mConsoleWidth;
        }
        if(widthCheckTime.isExpired(UPDATE_CONSOLE_INTERVAL)){
            updateConsoleWidth();
        }
        return mConsoleWidth;
    }
    private void updateConsoleWidth() {
        widthCheckTime.reset();
        mConsoleWidth = ConsoleUtil.getConsoleWidth();
        if (mConsoleWidth <= 0) {
            mConsoleWidth = 80;
        }
    }
    public static void clearFileLoggers(){
        Logger logger=getLogger();
        List<Logger> subList=new ArrayList<>(logger.subLoggers);
        for(Logger sub:subList){
            if(sub instanceof FileLogger){
                FileLogger fileLogger=(FileLogger) sub;
                fileLogger.onShutdown();
                logger.subLoggers.remove(fileLogger);
            }
        }
    }
    public static void addFileLogger(File file){
        FileLogger fileLogger=FileLogger.create(file);
        getLogger().addSubLogger(fileLogger);
    }
    public static void enableStdLogger(boolean enable){
        Logger logger=getLogger();
        logger.setEnable(enable, false);
        for(Logger sub: logger.subLoggers){
            if(!(sub instanceof StdLogger)){
                sub.setEnable(enable, false);
            }
        }
    }
    public static Logger getLogger(){
        if(sInstance!=null){
            return sInstance;
        }
        synchronized (Logger.class){
            sInstance=StdLogger.getInstance();
            return sInstance;
        }
    }

    private static final String TYPE_INFO="I";
    private static final String TYPE_WARN="W";
    private static final String TYPE_ERROR="E";
    private static final String TRACE_TAB1="    ";
    private static final String TRACE_TAB2="        ";
    private static final int MAX_TRACE=10;
    private static final long INTERVAL=2000;
    private static final long UPDATE_CONSOLE_INTERVAL = 4000;
}
