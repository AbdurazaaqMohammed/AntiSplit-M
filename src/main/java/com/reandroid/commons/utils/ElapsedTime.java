package com.reandroid.commons.utils;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ElapsedTime {
    private long startTime;
    private long previous;
    private boolean mIsPaused;
    public ElapsedTime(){
        startTime=System.currentTimeMillis();
    }

    public void reset(){
        startTime=System.currentTimeMillis();
        previous=0;
    }
    public String now(){
        return now(true);
    }
    public String now(boolean ignoreZero){
        long diff=elapsed();
        return msToDisplayTime(diff,true, ignoreZero);
    }
    public boolean isExpired(long ms){
        return elapsed()>=ms;
    }
    public String remaining(long ms){
        long el=elapsed();
        long diff=ms-el;
        if(diff<0){
            return "--:--";
        }
        return msToDisplayTime(diff,true, true);
    }

    public void pause() {
        if(mIsPaused){
            return;
        }
        this.previous = elapsed();
        this.mIsPaused = true;
    }
    public void resume() {
        if(!mIsPaused){
            return;
        }
        this.mIsPaused = false;
        this.startTime=System.currentTimeMillis();
    }
    public boolean isPaused() {
        return mIsPaused;
    }

    public long elapsed(){
        return getDiff()+previous;
    }
    private long getDiff(){
        if(mIsPaused){
            return 0;
        }
        return System.currentTimeMillis()-startTime;
    }
    private String msToDisplayTime(long l, boolean includeMs, boolean ignoreZero){
        final long hr = TimeUnit.MILLISECONDS.toHours(l);
        final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        if(!includeMs){
            return String.format(Locale.ENGLISH,"%02d:%02d:%02d", hr, min, sec);
        }
        final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        boolean showMs=min<10;
        if(ignoreZero){
            if(hr==0&&min==0){
                return String.format(Locale.ENGLISH,"%02d.%03d",  sec, ms);
            }
            if(hr==0){
                if(showMs){
                    return String.format(Locale.ENGLISH,"%02d:%02d.%03d", min, sec, ms);
                }
                return String.format(Locale.ENGLISH,"%02d:%02d", min, sec);
            }
        }
        if(showMs){
            return String.format(Locale.ENGLISH,"%02d:%02d:%02d.%03d", hr, min, sec, ms);
        }
        return String.format(Locale.ENGLISH,"%02d:%02d:%02d", hr, min, sec);
    }
}