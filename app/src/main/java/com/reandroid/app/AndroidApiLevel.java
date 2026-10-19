/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.app;

public class AndroidApiLevel implements Comparable<AndroidApiLevel>{

    public static final AndroidApiLevel B;
    public static final AndroidApiLevel B_1_1;
    public static final AndroidApiLevel C;
    public static final AndroidApiLevel D;
    public static final AndroidApiLevel E;
    public static final AndroidApiLevel E_0_1;
    public static final AndroidApiLevel E_MR1;
    public static final AndroidApiLevel F;
    public static final AndroidApiLevel G;
    public static final AndroidApiLevel G_MR1;
    public static final AndroidApiLevel H;
    public static final AndroidApiLevel H_MR1;
    public static final AndroidApiLevel H_MR2;
    public static final AndroidApiLevel I;
    public static final AndroidApiLevel I_MR1;
    public static final AndroidApiLevel J;
    public static final AndroidApiLevel J_MR1;
    public static final AndroidApiLevel J_MR2;
    public static final AndroidApiLevel K;
    public static final AndroidApiLevel K_WATCH;
    public static final AndroidApiLevel L;
    public static final AndroidApiLevel L_MR1;
    public static final AndroidApiLevel M;
    public static final AndroidApiLevel N;
    public static final AndroidApiLevel N_MR1;
    public static final AndroidApiLevel O;
    public static final AndroidApiLevel O_MR1;
    public static final AndroidApiLevel P;
    public static final AndroidApiLevel Q;
    public static final AndroidApiLevel R;
    public static final AndroidApiLevel S;
    public static final AndroidApiLevel S_V2;
    public static final AndroidApiLevel TIRAMISU;
    public static final AndroidApiLevel UPSIDE_DOWN_CAKE;
    public static final AndroidApiLevel VANILLA_ICE_CREAM;
    public static final AndroidApiLevel BAKLAVA;
    public static final AndroidApiLevel CINNAMON_BUN;

    public static final AndroidApiLevel ANDROID_PLATFORM;

    private static final AndroidApiLevel[] VALUES;

    public static final AndroidApiLevel LATEST;

    static {

        B = new AndroidApiLevel("B", 1, "1.0", "Android 1.0");
        B_1_1 = new AndroidApiLevel("B_1_1", 2, "1.1", "Petit Four");
        C = new AndroidApiLevel("C", 3, "1.5", "Cupcake");
        D = new AndroidApiLevel("D", 4, "1.6", "Donut");
        E = new AndroidApiLevel("E", 5, "2.0", "Eclair");
        E_0_1 = new AndroidApiLevel("E_0_1", 6, "2.0.1", "Eclair");
        E_MR1 = new AndroidApiLevel("E_MR1", 7, "2.1", "Eclair");
        F = new AndroidApiLevel("F", 8, "2.2", "2.2.3", "Froyo");
        G = new AndroidApiLevel("G", 9, "2.3", "2.3.2", "Gingerbread");
        G_MR1 = new AndroidApiLevel("G_MR1", 10, "2.3.3", "2.3.7", "Gingerbread");
        H = new AndroidApiLevel("H", 11, "3.0", "Honeycomb");
        H_MR1 = new AndroidApiLevel("H_MR1", 12, "3.1", "Honeycomb");
        H_MR2 = new AndroidApiLevel("H_MR2", 13, "3.2", "3.2.6", "Honeycomb");
        I = new AndroidApiLevel("I", 14, "4.0", "4.0.2", "Ice Cream Sandwich");
        I_MR1 = new AndroidApiLevel("I_MR1", 15, "4.0.3", "4.0.4", "Ice Cream Sandwich");
        J = new AndroidApiLevel("J", 16, "4.1", "4.1.2", "Jelly Bean");
        J_MR1 = new AndroidApiLevel("J_MR1", 17, "4.2", "4.2.2", "Jelly Bean");
        J_MR2 = new AndroidApiLevel("J_MR2", 18, "4.3", "4.3.1", "Jelly Bean");
        K = new AndroidApiLevel("K", 19, "4.4", "4.4.4", "Key Lime Pie");
        K_WATCH = new AndroidApiLevel("K_WATCH", 20, "4.4", "4.4.2", "Key Lime Pie");
        L = new AndroidApiLevel("L", 21, "5.0", "5.0.2", "Lemon Meringue Pie");
        L_MR1 = new AndroidApiLevel("L_MR1", 22, "5.1", "5.1.1", "Lemon Meringue Pie");
        M = new AndroidApiLevel("M", 23, "6.0", "6.0.1", "Macadamia Nut Cookie");
        N = new AndroidApiLevel("N", 24, "7.0", "New York Cheesecake");
        N_MR1 = new AndroidApiLevel("N_MR1", 25, "7.1", "7.1.2", "New York Cheesecake");
        O = new AndroidApiLevel("O", 26, "8.0", "Oatmeal Cookie");
        O_MR1 = new AndroidApiLevel("O_MR1", 27, "8.1", "Oatmeal Cookie");
        P = new AndroidApiLevel("P", 28, "9", "Pistachio Ice Cream");
        Q = new AndroidApiLevel("Q", 29, "10", "Quince Tart");
        R = new AndroidApiLevel("R", 30, "11", "Red Velvet Cake");
        S = new AndroidApiLevel("S", 31, "12", "Snow Cone");
        S_V2 = new AndroidApiLevel("S_V2", 32, "12.1", "Snow Cone v2");
        TIRAMISU = new AndroidApiLevel("TIRAMISU", 33, "13", "Tiramisu");
        UPSIDE_DOWN_CAKE = new AndroidApiLevel("UPSIDE_DOWN_CAKE", 34, "14", "Upside Down Cake");
        VANILLA_ICE_CREAM = new AndroidApiLevel("VANILLA_ICE_CREAM", 35, "15", "Vanilla Ice Cream");
        BAKLAVA = new AndroidApiLevel("BAKLAVA", 36, "16", "Baklava");
        CINNAMON_BUN = new AndroidApiLevel("CINNAMON_BUN", 37, "17", "Cinnamon Bun");

        ANDROID_PLATFORM = new AndroidApiLevel("ANDROID_PLATFORM", 10000, "10000", "ANDROID_PLATFORM");

        LATEST = CINNAMON_BUN;

        VALUES = new AndroidApiLevel[]{
                B,
                B_1_1,
                C,
                D,
                E,
                E_0_1,
                E_MR1,
                F,
                G,
                G_MR1,
                H,
                H_MR1,
                H_MR2,
                I,
                I_MR1,
                J,
                J_MR1,
                J_MR2,
                K,
                K_WATCH,
                L,
                L_MR1,
                M,
                N,
                N_MR1,
                O,
                O_MR1,
                P,
                Q,
                R,
                S,
                S_V2,
                TIRAMISU,
                UPSIDE_DOWN_CAKE,
                VANILLA_ICE_CREAM,
                BAKLAVA,
                CINNAMON_BUN,
                ANDROID_PLATFORM
        };

    }

    private final String name;
    private final int api;
    private final String version;
    private final String versionMax;
    private final String description;

    private AndroidApiLevel(String name, int api, String version, String versionMax, String description){
        this.name = name;
        this.api = api;
        this.version = version;
        this.versionMax = versionMax;
        this.description = description;
    }
    private AndroidApiLevel(String name, int api, String version, String description){
        this(name, api, version, null, description);
    }

    public int getApi() {
        return api;
    }
    public String getName() {
        return name;
    }
    public String getVersion() {
        return version;
    }
    public String getVersionMax() {
        return versionMax;
    }
    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(AndroidApiLevel apiLevel) {
        return Integer.compare(api, apiLevel.api);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return api;
    }
    @Override
    public String toString() {
        return name + "-" + api + " (" + description + " - " + version + ")";
    }


    public static AndroidApiLevel forApi(int api){
        for(AndroidApiLevel level : VALUES){
            if(api == level.getApi()){
                return level;
            }
        }
        return null;
    }
    public static AndroidApiLevel getMinAndroidApiLevelForDex(int dexVersion) {
        switch(dexVersion) {
            case 35:
                return B;
            case 37:
                return N;
            case 38:
                return O;
            case 39:
                return P;
            case 40:
                return R;
            case 41:
                return ANDROID_PLATFORM;
            default:
                return null;
        }
    }

    public static AndroidApiLevel[] values() {
        return VALUES.clone();
    }
}
