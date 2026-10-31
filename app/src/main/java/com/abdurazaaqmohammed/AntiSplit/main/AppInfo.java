package com.abdurazaaqmohammed.AntiSplit.main;

public class AppInfo {

    public final String name;
    public final String packageName;
    public final long lastUpdated;
    public final long firstInstall;

    public AppInfo(String name, String packageName, long lastUpdated, long firstInstall) {
        this.name = name;
        this.packageName = packageName;
        this.lastUpdated = lastUpdated;
        this.firstInstall = firstInstall;
    }
}
