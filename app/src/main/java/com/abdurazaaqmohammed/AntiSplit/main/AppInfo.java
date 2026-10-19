package com.abdurazaaqmohammed.AntiSplit.main;

public class AppInfo {
    public String name;
    String packageName;
    public long lastUpdated;
    public long firstInstall;

    public AppInfo(String name, String packageName, long lastUpdated, long firstInstall) {
        this.name = name;
        this.packageName = packageName;
        this.lastUpdated = lastUpdated;
        this.firstInstall = firstInstall;
    }
}
