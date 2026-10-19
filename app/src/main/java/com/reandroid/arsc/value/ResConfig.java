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
package com.reandroid.arsc.value;

import com.reandroid.arsc.item.ByteArray;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.HexUtil;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResConfig extends ResConfigBase implements JSONConvert<JSONObject>,
        Comparable<ResConfig> {
    private String mQualifiers;
    private int mQualifiersStamp;

    public ResConfig(){
        this(SIZE_64);
    }
    private ResConfig(int size){
        super(size);
        this.mQualifiersStamp = 0;
    }
    public boolean isEqualOrMoreSpecificThan(ResConfig resConfig){
        if(resConfig == null){
            return false;
        }
        if(resConfig == this || resConfig.isDefault()){
            return true;
        }
        byte[] bytes = ByteArray.trimTrailZeros(this.getValueBytes());
        byte[] otherBytes = ByteArray.trimTrailZeros(resConfig.getValueBytes());
        int max = otherBytes.length;
        if(max > bytes.length){
            return false;
        }
        for(int i = 0; i<max; i++){
            byte other = otherBytes[i];
            if(other == 0){
                continue;
            }
            if(bytes[i] != other){
                return false;
            }
        }
        return true;
    }
    public void copyFrom(ResConfig resConfig){
        if(resConfig == this || resConfig == null){
            return;
        }
        setValueBytes(resConfig.getValueBytes());
    }
    /**
     * returns null if parsing is ok, else returns unknown qualifiers
     * */
    public String[] parseQualifiers(String qualifiers){
        QualifierParser parser = new QualifierParser(this, qualifiers);
        parser.parse();
        return parser.getErrors();
    }
    public String getLocale(){
        StringBuilder builder = new StringBuilder();
        String str = getLanguage();
        if(str != null){
            builder.append(str);
        }
        str = getRegion();
        if(str != null){
            if(builder.length() != 0){
                builder.append('-');
            }
            builder.append(str);
        }
        str = getLocaleScriptInternal();
        if(str != null){
            if(builder.length() != 0){
                builder.append('-');
            }
            builder.append(str);
        }
        return builder.toString();
    }
    public String[] parseLocale(String locale){
        QualifierParser parser = new QualifierParser(this, locale);
        parser.parseLocale();
        return parser.getErrors();
    }

    public char[] getLanguageChars(){
        byte[] bytes = getLanguageBytes();
        return unPackLanguage(bytes[0], bytes[1]);
    }
    public void setLanguage(char[] chars){
        setLanguageBytes(packLanguage(chars));
    }
    public void setLanguage(String language){
        char[] chs = null;
        if(language!= null){
            chs = language.toCharArray();
        }
        setLanguage(chs);
    }
    public String getLanguage(){
        char[] chars = getLanguageChars();
        if(isNullChars(chars)){
            return null;
        }
        return new String(chars);
    }
    public char[] getRegionChars(){
        byte[] bytes = getRegionBytes();
        return unPackRegion(bytes[0], bytes[1]);
    }
    public void setRegion(char[] chars){
        setRegionBytes(packRegion(chars));
    }
    public void setRegion(String region){
        char[] chars = null;
        if(region!= null){
            if(region.length() ==3 && region.charAt(0) == 'r'){
                region = region.substring(1);
            }
            chars = region.toCharArray();
        }
        setRegion(chars);
    }
    public String getRegion(){
        char[] chars = getRegionChars();
        if(isNullChars(chars)){
            return null;
        }
        return new String(chars);
    }
    public Orientation getOrientation(){
        return Orientation.valueOf(getOrientationValue());
    }
    public void setOrientation(Orientation orientation){
        setOrientation(Orientation.update(orientation, getOrientationValue()));
    }
    public Touchscreen getTouchscreen(){
        return Touchscreen.valueOf(getTouchscreenValue());
    }
    public void setTouchscreen(Touchscreen touchscreen){
        setTouchscreen(Touchscreen.update(touchscreen, getTouchscreenValue()));
    }
    public Density getDensity(){
        return Density.valueOf(getDensityValue());
    }
    public void setDensity(Density density){
        setDensity(Density.update(density, getDensityValue()));
    }
    public Keyboard getKeyboard(){
        return Keyboard.valueOf(getKeyboardValue());
    }
    public void setKeyboard(Keyboard keyboard){
        setKeyboard(Keyboard.update(keyboard, getKeyboardValue()));
    }
    public Navigation getNavigation(){
        return Navigation.valueOf(getNavigationValue());
    }
    public void setNavigation(Navigation navigation){
        setNavigation(Navigation.update(navigation, getNavigationValue()));
    }
    public InputFlagsKeysHidden getInputFlagsKeysHidden(){
        return InputFlagsKeysHidden.valueOf(getInputFlagsValue());
    }
    public void setInputFlagsKeysHidden(InputFlagsKeysHidden keysHidden){
        setInputFlags(InputFlagsKeysHidden.update(keysHidden, getInputFlagsValue()));
    }
    public InputFlagsNavHidden getInputFlagsNavHidden(){
        return InputFlagsNavHidden.valueOf(getInputFlagsValue());
    }
    public void setInputFlagsNavHidden(InputFlagsNavHidden navHidden){
        setInputFlags(InputFlagsNavHidden.update(navHidden, getInputFlagsValue()));
    }
    public Gender getGender(){
        return Gender.valueOf(getGenderValue());
    }
    public void setGender(Gender gender){
        setGenderValue(Gender.update(gender, getGenderValue()));
    }
    public void setScreenSize(int width, int height){
        this.setScreenWidth(width);
        this.setScreenHeight(height);
    }
    public ScreenLayoutSize getScreenLayoutSize(){
        return ScreenLayoutSize.valueOf(getScreenLayout());
    }
    public void setScreenLayoutSize(ScreenLayoutSize layoutSize){
        setScreenLayout(ScreenLayoutSize.update(layoutSize, getScreenLayout()));
    }
    public ScreenLayoutLong getScreenLayoutLong(){
        return ScreenLayoutLong.valueOf(getScreenLayout());
    }
    public void setScreenLayoutLong(ScreenLayoutLong layoutLong){
        setScreenLayout(ScreenLayoutLong.update(layoutLong, getScreenLayout()));
    }
    public ScreenLayoutDir getScreenLayoutDir(){
        return ScreenLayoutDir.valueOf(getScreenLayout());
    }
    public void setScreenLayoutDir(ScreenLayoutDir layoutDir){
        setScreenLayout(ScreenLayoutDir.update(layoutDir, getScreenLayout()));
    }
    public UiModeType getUiModeType(){
        return UiModeType.valueOf(getUiMode());
    }
    public void setUiModeType(UiModeType uiModeType){
        setUiMode(UiModeType.update(uiModeType, getUiMode()));
    }
    public UiModeNight getUiModeNight(){
        return UiModeNight.valueOf(getUiMode());
    }
    public void setUiModeNight(UiModeNight uiModeNight){
        setUiMode(UiModeNight.update(uiModeNight, getUiMode()));
    }

    public void setLocaleScript(char[] chs){
        byte[] bts = toByteArray(chs, LEN_localeScript);
        setLocaleScript(bts);
    }
    public void setLocaleScript(String script){
        char[] chs = null;
        script = trimPostfix(script, POSTFIX_locale);
        if(script!= null){
            chs = script.toCharArray();
        }
        setLocaleScript(chs);
    }
    public char[] getLocaleScriptChars(){
        return trimEndingZero(toCharArray(getLocaleScriptBytes()));
    }
    private String getLocaleScriptInternal(){
        char[] chs = getLocaleScriptChars();
        if(chs == null){
            return null;
        }
        return new String(chs);
    }
    public String getLocaleScript(){
        String script = getLocaleScriptInternal();
        if(script == null){
            return null;
        }
        script = ensureLength(script, 3, POSTFIX_locale);
        return script;
    }
    public void setLocaleVariant(char[] chs){
        byte[] bts =toByteArray(chs, LEN_localeVariant);
        setLocaleVariant(bts);
    }
    public void setLocaleVariant(String variant){
        if(variant!= null){
            variant = variant.toLowerCase(java.util.Locale.ROOT);
        }
        setLocaleVariantInternal(variant);
    }
    private void setLocaleVariantInternal(String variant){
        char[] chs = null;
        variant = trimPostfix(variant, POSTFIX_locale);
        if(variant!= null){
            chs = variant.toCharArray();
        }
        setLocaleVariant(chs);
    }
    public char[] getLocaleVariantChars(){
        return trimEndingZero(toCharArray(getLocaleVariantBytes()));
    }
    private String getLocaleVariantInternal(){
        char[] chs = getLocaleVariantChars();
        if(chs == null){
            return null;
        }
        return new String(chs);
    }
    public String getLocaleVariant(){
        String variant = getLocaleVariantInternal();
        if(variant == null){
            return null;
        }
        variant = ensureLength(variant, 5, POSTFIX_locale);
        return variant.toUpperCase(java.util.Locale.ROOT);
    }
    public ScreenLayoutRound getScreenLayoutRound(){
        return ScreenLayoutRound.valueOf(getScreenLayout2());
    }
    public void setScreenLayoutRound(ScreenLayoutRound layoutRound){
        setScreenLayout2(ScreenLayoutRound.update(layoutRound, getScreenLayout2()));
    }
    public ColorModeWide getColorModeWide(){
        return ColorModeWide.valueOf(getColorMode());
    }
    public void setColorModeWide(ColorModeWide colorModeWide){
        setColorMode(ColorModeWide.update(colorModeWide, getColorMode()));
    }
    public ColorModeHdr getColorModeHdr(){
        return ColorModeHdr.valueOf(getColorMode());
    }
    public void setColorModeHdr(ColorModeHdr colorModeHdr){
        setColorMode(ColorModeHdr.update(colorModeHdr, getColorMode()));
    }
    public String getUnknownHexBytes(){
        return getUnknownHexBytes(8);
    }
    public String getUnknownHexBytes(int limit){
        byte[] bytes = trimEndingZero(getUnknownBytes());
        if(isNullBytes(bytes)){
            return null;
        }
        String result = null;
        if(limit < 0){
            limit = bytes.length;
        }
        if(bytes.length < limit){
            limit = bytes.length;
        }
        for (int i = 0; i < limit; i++) {
            result = HexUtil.toHex2(result, bytes[i]);
        }
        return result;
    }
    public void setUnknownBytes(String hexBytes){
        if(hexBytes == null || hexBytes.length() == 0){
            return;
        }
        int length = hexBytes.length();
        if(length % 2 != 0){
            return;
        }
        char[] chars = hexBytes.toCharArray();
        length = chars.length;
        byte[] bytes = new byte[length/2];
        try{
            for(int i = 0; i < length; i += 2){
                bytes[i / 2] = (byte) HexUtil.parseHex(new String(chars, i, 2));
            }
        }catch (NumberFormatException ignored){
            return;
        }
        setUnknownBytes(bytes);
    }
    /*** END OF SET/GET VALUES ***/

    public String getQualifiers(){
        int hash = this.hashCode();
        if(mQualifiers == null || mQualifiersStamp!=hash){
            mQualifiers = new QualifierBuilder(this).build();
            mQualifiersStamp = hash;
        }
        return mQualifiers;
    }

    public boolean isEqualQualifiers(String qualifiers){
        return this.equals(parse(qualifiers));
    }
    public boolean isDefault(){
        return isNullBytes(getValueBytes());
    }
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        if(isDefault()){
            return jsonObject;
        }
        int val= getMcc();
        if(val!= 0){
            jsonObject.put(NAME_mcc, val);
        }
        val= getMnc();
        if(val!= 0){
            jsonObject.put(NAME_mnc, val);
        }
        String str = getLanguage();
        if(str!= null){
            jsonObject.put(NAME_language, str);
        }
        str = getRegion();
        if(str!= null){
            jsonObject.put(NAME_region, str);
        }
        jsonObject.put(NAME_orientation, Flag.toString(getOrientation()));
        jsonObject.put(NAME_touchscreen, Flag.toString(getTouchscreen()));
        jsonObject.put(NAME_density, Flag.toString(getDensity()));
        jsonObject.put(NAME_keyboard, Flag.toString(getKeyboard()));
        jsonObject.put(NAME_navigation, Flag.toString(getNavigation()));
        jsonObject.put(NAME_input_flags_keys_hidden, Flag.toString(getInputFlagsKeysHidden()));
        jsonObject.put(NAME_input_flags_nav_hidden, Flag.toString(getInputFlagsNavHidden()));
        jsonObject.put(NAME_gender, Flag.toString(getGender()));
        val = getScreenWidth();
        if(val!= 0){
            jsonObject.put(NAME_screenWidth, val);
        }
        val = getScreenHeight();
        if(val!= 0){
            jsonObject.put(NAME_screenHeight, val);
        }
        val = getSdkVersion();
        if(val!= 0){
            jsonObject.put(NAME_sdkVersion, val);
        }
        val = getMinorVersion();
        if(val!= 0){
            jsonObject.put(NAME_minorVersion, val);
        }
        jsonObject.put(NAME_screen_layout_size, Flag.toString(getScreenLayoutSize()));
        jsonObject.put(NAME_screen_layout_long, Flag.toString(getScreenLayoutLong()));
        jsonObject.put(NAME_screen_layout_dir, Flag.toString(getScreenLayoutDir()));
        jsonObject.put(NAME_ui_mode_type, Flag.toString(getUiModeType()));
        jsonObject.put(NAME_ui_mode_night, Flag.toString(getUiModeNight()));
        val = getSmallestScreenWidthDp();
        if(val!= 0){
            jsonObject.put(NAME_smallestScreenWidthDp, val);
        }
        val = getScreenWidthDp();
        if(val!= 0){
            jsonObject.put(NAME_screenWidthDp, val);
        }
        val = getScreenHeightDp();
        if(val!= 0){
            jsonObject.put(NAME_screenHeightDp, val);
        }
        str = getLocaleScriptInternal();
        if(str!= null){
            jsonObject.put(NAME_localeScript, str);
        }
        str = getLocaleVariantInternal();
        if(str!= null){
            jsonObject.put(NAME_localeVariant, str);
        }
        jsonObject.put(NAME_screen_layout_round, Flag.toString(getScreenLayoutRound()));
        jsonObject.put(NAME_color_mode_wide, Flag.toString(getColorModeWide()));
        jsonObject.put(NAME_color_mode_hdr, Flag.toString(getColorModeHdr()));
        str = getUnknownHexBytes(-1);
        if(str != null){
            jsonObject.put(NAME_unknownBytes, str);
            jsonObject.put(NAME_config_size, getConfigSize());
        }
        return jsonObject;
    }
    @Override
    public void fromJson(JSONObject json) {
        if(json.isEmpty()){
            resetValueBytes();
            return;
        }
        int configSize = json.optInt(NAME_config_size, 0);
        if(configSize == 0){
            configSize = SIZE_64;
        }
        trimToSize(configSize);

        setMcc(json.optInt(NAME_mcc));
        setMnc(json.optInt(NAME_mnc));
        setLanguage(json.optString(NAME_language));
        setRegion(json.optString(NAME_region));
        setOrientation(Orientation.valueOf(json.optString(NAME_orientation)));
        setTouchscreen(Touchscreen.valueOf(json.optString(NAME_touchscreen)));
        setDensity(Density.valueOf(json.optString(NAME_density)));
        setKeyboard(Keyboard.valueOf(json.optString(NAME_keyboard)));
        setNavigation(Navigation.valueOf(json.optString(NAME_navigation)));
        setInputFlagsKeysHidden(InputFlagsKeysHidden.valueOf(json.optString(NAME_input_flags_keys_hidden)));
        setInputFlagsNavHidden(InputFlagsNavHidden.valueOf(json.optString(NAME_input_flags_nav_hidden)));
        setGender(Gender.valueOf(json.optString(NAME_gender)));
        setScreenWidth(json.optInt(NAME_screenWidth));
        setScreenHeight(json.optInt(NAME_screenHeight));
        setSdkVersion(json.optInt(NAME_sdkVersion));
        setMinorVersion(json.optInt(NAME_minorVersion));
        setScreenLayoutSize(ScreenLayoutSize.valueOf(json.optString(NAME_screen_layout_size)));
        setScreenLayoutLong(ScreenLayoutLong.valueOf(json.optString(NAME_screen_layout_long)));
        setScreenLayoutDir(ScreenLayoutDir.valueOf(json.optString(NAME_screen_layout_dir)));
        setUiModeType(UiModeType.valueOf(json.optString(NAME_ui_mode_type)));
        setUiModeNight(UiModeNight.valueOf(json.optString(NAME_ui_mode_night)));
        setSmallestScreenWidthDp(json.optInt(NAME_smallestScreenWidthDp));
        setScreenWidthDp(json.optInt(NAME_screenWidthDp));
        setScreenHeightDp(json.optInt(NAME_screenHeightDp));
        setLocaleScript(json.optString(NAME_localeScript));
        setLocaleVariantInternal(json.optString(NAME_localeVariant));
        setScreenLayoutRound(ScreenLayoutRound.valueOf(json.optString(NAME_screen_layout_round)));
        setColorModeWide(ColorModeWide.valueOf(json.optString(NAME_color_mode_wide)));
        setColorModeHdr(ColorModeHdr.valueOf(json.optString(NAME_color_mode_hdr)));
        setUnknownBytes(json.optString(NAME_unknownBytes));
    }
    @Override
    public int hashCode(){
        byte[] bts = ByteArray.trimTrailZeros(getValueBytes());
        return Arrays.hashCode(bts);
    }
    @Override
    public boolean equals(Object obj){
        if(obj==this){
            return true;
        }
        if(obj== null){
            return false;
        }
        if(obj instanceof ResConfig){
            ResConfig other = (ResConfig)obj;
            byte[] bts1 = getValueBytes();
            byte[] bts2 = other.getValueBytes();
            return ByteArray.equalsIgnoreTrailZero(bts1, bts2);
        }
        return false;
    }
    @Override
    public String toString(){
        String q = getQualifiers();
        if(q.length() == 0) {
            return "[DEFAULT]";
        }
        return "[" + q + "]";
    }
    @Override
    public int compareTo(ResConfig config) {
        int i = CompareUtil.compare(getMnc(), config.getMnc());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getMcc(), config.getMcc());
        if(i != 0) {
            return i;
        }
        i = compareLocale(config);
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getGenderValue(), config.getGenderValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getDensityValue(), config.getDensityValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getTouchscreenValue(), config.getTouchscreenValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getOrientationValue(), config.getOrientationValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getNavigationValue(), config.getNavigationValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getKeyboardValue(), config.getKeyboardValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getInputFlagsValue(), config.getInputFlagsValue());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getScreenWidth(), config.getScreenWidth());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getScreenHeight(), config.getScreenHeight());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getSdkVersion(), config.getSdkVersion());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getScreenLayout(), config.getScreenLayout());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getScreenLayout2(), config.getScreenLayout2());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getColorMode(), config.getColorMode());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getUiMode(), config.getUiMode());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getSmallestScreenWidthDp(), config.getSmallestScreenWidthDp());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getScreenHeightDp(), config.getScreenHeightDp());
        if(i != 0) {
            return i;
        }
        i = CompareUtil.compare(getScreenWidthDp(), config.getScreenWidthDp());
        if(i != 0) {
            return i;
        }
        return 0;
    }

    public static ResConfig parse(String qualifiers){
        ResConfig resConfig = new ResConfig();
        resConfig.parseQualifiers(qualifiers);
        return resConfig;
    }
    public static ResConfig getDefault(){
        ResConfig resConfig = DEFAULT_INSTANCE;
        if(resConfig.isDefault()){
            return resConfig;
        }
        resConfig.resetValueBytes();
        resConfig.setConfigSize(SIZE_16);
        return resConfig;
    }


    private static char[] unPackLanguage(byte in0, byte in1) {
        return unpackLanguageOrRegion(in0, in1, 'a');
    }
    private static char[] unPackRegion(byte in0, byte in1) {
        return unpackLanguageOrRegion(in0, in1, '0');
    }
    private static char[] unpackLanguageOrRegion(byte in0, byte in1, char base) {
        char[] out;
        if ((in0 & 0x80) != 0) {
            out = new char[3];
            byte first = (byte) (in1 & 0x1f);
            byte second = (byte) (((in1 & 0xe0) >> 5) + ((in0 & 0x03) << 3));
            byte third = (byte) ((in0 & 0x7c) >> 2);

            out[0] = (char) (first + base);
            out[1] = (char) (second + base);
            out[2] = (char) (third + base);
        }else if (in0 != 0 && in1 != 0) {
            out = new char[2];
            out[0] = (char) in0;
            out[1] = (char) in1;
        }else {
            out = new char[2];
        }
        return out;
    }
    private static byte[] packLanguage(char[] language) {
        return packLanguageOrRegion(language, 'a');
    }
    private static byte[] packRegion(char[] region) {
        return packLanguageOrRegion(region, '0');
    }
    private static byte[] packLanguageOrRegion(char[] in, char base) {
        byte[] out = new byte[2];
        if(in == null || in.length<2){
            return out;
        }
        if (in.length == 2 || in[2] == 0 || in[2] == '-') {
            out[0] = (byte) in[0];
            out[1] = (byte) in[1];
        } else {
            byte first = (byte) ((in[0] - base) & 0x007f);
            byte second = (byte) ((in[1] - base) & 0x007f);
            byte third = (byte) ((in[2] - base) & 0x007f);

            out[0] = (byte) (0x80 | (third << 2) | (second >> 3));
            out[1] = (byte) ((second << 5) | first);
        }
        return out;
    }

    public static final class Orientation extends Flag{
        public static final int MASK = 0x0f;

        public static final Orientation PORT = new Orientation("port", 0x01);
        public static final Orientation LAND = new Orientation("land", 0x02);
        public static final Orientation SQUARE = new Orientation("square", 0x03);

        public static final Orientation[] VALUES = new Orientation[]{
                PORT,
                LAND,
                SQUARE
        };
        private Orientation(String name, int flag) {
            super(name, flag);
        }
        public static Orientation valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static Orientation valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static Orientation fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static Orientation fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(Orientation flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class Touchscreen extends Flag{
        public static final int MASK = 0x0f;

        public static final Touchscreen NOTOUCH = new Touchscreen("notouch", 0x01);
        public static final Touchscreen STYLUS = new Touchscreen("stylus", 0x02);
        public static final Touchscreen FINGER = new Touchscreen("finger", 0x03);

        public static final Touchscreen[] VALUES = new Touchscreen[]{
                NOTOUCH,
                STYLUS,
                FINGER
        };
        private Touchscreen(String name, int flag) {
            super(name, flag);
        }
        public static Touchscreen valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static Touchscreen valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static Touchscreen fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static Touchscreen fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(Touchscreen flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class Density extends Flag{
        public static final int MASK = 0xffff;

        public static final Density LDPI = new Density("ldpi", 120);
        public static final Density MDPI = new Density("mdpi", 160);
        public static final Density TVDPI = new Density("tvdpi", 213);
        public static final Density HDPI = new Density("hdpi", 240);
        public static final Density XHDPI = new Density("xhdpi", 320);
        public static final Density XXHDPI = new Density("xxhdpi", 480);
        public static final Density XXXHDPI = new Density("xxxhdpi", 640);
        public static final Density ANYDPI = new Density("anydpi", 0xfffe);
        public static final Density NODPI = new Density("nodpi", 0xffff);

        public static final Density[] VALUES = new Density[]{LDPI,
                MDPI,
                TVDPI,
                HDPI,
                XHDPI,
                XXHDPI,
                XXXHDPI,
                ANYDPI,
                NODPI
        };
        private Density(String name, int flag) {
            super(name, flag);
        }
        public static Density valueOf(int flag){
            if(flag== 0){
                return null;
            }
            Density density = Flag.valueOf(VALUES, MASK, flag);
            if(density == null){
                flag = flag & MASK;
                density = new Density(flag+"dpi", flag);
            }
            return density;
        }
        public static Density valueOf(String name){
            if(name == null || name.length() < 4){
                return null;
            }
            name = name.toLowerCase(java.util.Locale.ROOT);
            if(name.charAt(0) == '-'){
                name = name.substring(1);
            }
            Density density = Flag.valueOf(VALUES, name);
            if(density == null && name.endsWith("dpi")){
                name = name.substring(0, name.length()-3);
                try{
                    int flag = Integer.parseInt(name);
                    density = new Density(flag+"dpi", flag);
                }catch (NumberFormatException ignored){
                }
            }
            return density;
        }
        public static Density fromQualifiers(String qualifiers){
            return fromQualifiers(qualifiers.split("\\s*-\\s*"));
        }
        public static Density fromQualifiers(String[] qualifiers){
            if(qualifiers == null){
                return null;
            }
            for(int i = 0; i < qualifiers.length; i++){
                Density density = valueOf(qualifiers[i]);
                if(density== null){
                    continue;
                }
                qualifiers[i] = null;
                return density;
            }
            return null;
        }
        public static int update(Density flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class Keyboard extends Flag{
        public static final int MASK = 0x0f;

        public static final Keyboard NOKEYS = new Keyboard("nokeys", 0x01);
        public static final Keyboard QWERTY = new Keyboard("qwerty", 0x02);
        public static final Keyboard KEY12 = new Keyboard("12key", 0x03);

        public static final Keyboard[] VALUES = new Keyboard[]{
                NOKEYS,
                QWERTY,
                KEY12
        };
        private Keyboard(String name, int flag) {
            super(name, flag);
        }
        public static Keyboard valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static Keyboard valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static Keyboard fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static Keyboard fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(Keyboard flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class Navigation extends Flag{
        public static final int MASK = 0x0f;

        public static final Navigation NONAV = new Navigation("nonav", 0x01);
        public static final Navigation DPAD = new Navigation("dpad", 0x02);
        public static final Navigation TRACKBALL = new Navigation("trackball", 0x03);
        public static final Navigation WHEEL = new Navigation("wheel", 0x04);
        public static final Navigation[] VALUES = new Navigation[]{
                NONAV,
                DPAD,
                TRACKBALL,
                WHEEL
        };
        private Navigation(String name, int flag) {
            super(name, flag);
        }
        public static Navigation valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static Navigation valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static Navigation fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static Navigation fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(Navigation flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class InputFlagsKeysHidden extends Flag{
        public static final int MASK = 0x03;

        public static final InputFlagsKeysHidden KEYSEXPOSED = new InputFlagsKeysHidden("keysexposed", 0x01);
        public static final InputFlagsKeysHidden KEYSHIDDEN = new InputFlagsKeysHidden("keyshidden", 0x02);
        public static final InputFlagsKeysHidden KEYSSOFT = new InputFlagsKeysHidden("keyssoft", 0x03);
        public static final InputFlagsKeysHidden[] VALUES = new InputFlagsKeysHidden[]{
                KEYSEXPOSED,
                KEYSHIDDEN,
                KEYSSOFT
        };
        private InputFlagsKeysHidden(String name, int flag) {
            super(name, flag);
        }
        public static InputFlagsKeysHidden valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static InputFlagsKeysHidden valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static InputFlagsKeysHidden fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static InputFlagsKeysHidden fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(InputFlagsKeysHidden flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class InputFlagsNavHidden extends Flag{
        public static final int MASK = 0x0C;

        public static final InputFlagsNavHidden NAVEXPOSED = new InputFlagsNavHidden("navexposed", 0x04);
        public static final InputFlagsNavHidden NAVHIDDEN = new InputFlagsNavHidden("navhidden", 0x08);
        public static final InputFlagsNavHidden[] VALUES = new InputFlagsNavHidden[]{
                NAVEXPOSED,
                NAVHIDDEN
        };
        private InputFlagsNavHidden(String name, int flag) {
            super(name, flag);
        }
        public static InputFlagsNavHidden valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static InputFlagsNavHidden valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static InputFlagsNavHidden fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static InputFlagsNavHidden fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(InputFlagsNavHidden flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class Gender extends Flag{
        public static final int MASK = 0b11;

        public static final Gender NEUTER = new Gender("neuter", 1);
        public static final Gender FEMININE = new Gender("feminine", 2);
        public static final Gender MASCULINE = new Gender("masculine", 3);
        public static final Gender[] VALUES = new Gender[]{
                NEUTER,
                FEMININE,
                MASCULINE
        };
        private Gender(String name, int flag) {
            super(name, flag);
        }
        public static Gender valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static Gender valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static Gender fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static Gender fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(Gender flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class UiModeType extends Flag{
        public static final int MASK = 0x0f;

        public static final UiModeType NORMAL = new UiModeType("normal", 0x01);
        public static final UiModeType DESK = new UiModeType("desk", 0x02);
        public static final UiModeType CAR = new UiModeType("car", 0x03);
        public static final UiModeType TELEVISION = new UiModeType("television", 0x04);
        public static final UiModeType APPLIANCE = new UiModeType("appliance", 0x05);
        public static final UiModeType WATCH = new UiModeType("watch", 0x06);
        public static final UiModeType VRHEADSET = new UiModeType("vrheadset", 0x07);
        public static final UiModeType GODZILLAUI = new UiModeType("godzillaui", 0x0b);
        public static final UiModeType SMALLUI = new UiModeType("smallui", 0x0c);
        public static final UiModeType MEDIUMUI = new UiModeType("mediumui", 0x0d);
        public static final UiModeType LARGEUI = new UiModeType("largeui", 0x0e);
        public static final UiModeType HUGEUI = new UiModeType("hugeui", 0x0f);

        private static final UiModeType[] VALUES = new UiModeType[]{
                NORMAL,
                DESK,
                CAR,
                TELEVISION,
                APPLIANCE,
                WATCH,
                VRHEADSET,
                GODZILLAUI,
                SMALLUI,
                MEDIUMUI,
                LARGEUI,
                HUGEUI
        };

        private UiModeType(String name, int flag) {
            super(name, flag);
        }
        public static UiModeType valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static UiModeType valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static UiModeType fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static UiModeType fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(UiModeType flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class UiModeNight extends Flag{
        public static final int MASK = 0x30;
        public static final UiModeNight NOTNIGHT = new UiModeNight("notnight",0x10);
        public static final UiModeNight NIGHT = new UiModeNight("night",0x20);
        private static final UiModeNight[] VALUES = new UiModeNight[]{
                NOTNIGHT,
                NIGHT
        };
        private UiModeNight(String name, int flag) {
            super(name, flag);
        }
        public static UiModeNight valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static UiModeNight valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static UiModeNight fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static UiModeNight fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(UiModeNight flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class ScreenLayoutSize extends Flag{
        public static final int MASK = 0x0f;

        public static final ScreenLayoutSize SMALL = new ScreenLayoutSize("small", 0x01);
        public static final ScreenLayoutSize NORMAL = new ScreenLayoutSize("normal", 0x02);
        public static final ScreenLayoutSize LARGE = new ScreenLayoutSize("large", 0x03);
        public static final ScreenLayoutSize XLARGE = new ScreenLayoutSize("xlarge", 0x04);
        public static final ScreenLayoutSize[] VALUES = new ScreenLayoutSize[]{
                SMALL,
                NORMAL,
                LARGE,
                XLARGE
        };
        private ScreenLayoutSize(String name, int flag) {
            super(name, flag);
        }
        public static ScreenLayoutSize valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static ScreenLayoutSize valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static ScreenLayoutSize fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static ScreenLayoutSize fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(ScreenLayoutSize flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class ScreenLayoutLong extends Flag{
        public static final int MASK = 0x30;
        public static final ScreenLayoutLong NOTLONG = new ScreenLayoutLong("notlong", 0x10);
        public static final ScreenLayoutLong LONG = new ScreenLayoutLong("long", 0x20);
        public static final ScreenLayoutLong[] VALUES = new ScreenLayoutLong[]{
                NOTLONG,
                LONG
        };
        private ScreenLayoutLong(String name, int flag) {
            super(name, flag);
        }
        public static ScreenLayoutLong valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static ScreenLayoutLong valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static ScreenLayoutLong fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static ScreenLayoutLong fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(ScreenLayoutLong flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class ScreenLayoutDir extends Flag{
        public static final int MASK = 0xC0;
        public static final ScreenLayoutDir LDLTR = new ScreenLayoutDir("ldltr", 0x40);
        public static final ScreenLayoutDir LDRTL = new ScreenLayoutDir("ldrtl", 0x80);
        public static final ScreenLayoutDir[] VALUES = new ScreenLayoutDir[]{
                LDLTR,
                LDRTL
        };
        private ScreenLayoutDir(String name, int flag) {
            super(name, flag);
        }
        public static ScreenLayoutDir valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static ScreenLayoutDir valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static ScreenLayoutDir fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static ScreenLayoutDir fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(ScreenLayoutDir flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class ScreenLayoutRound extends Flag{
        public static final int MASK = 0x03;
        public static final ScreenLayoutRound NOTROUND = new ScreenLayoutRound("notround", 0x01);
        public static final ScreenLayoutRound ROUND = new ScreenLayoutRound("round", 0x02);
        public static final ScreenLayoutRound[] VALUES = new ScreenLayoutRound[]{
                NOTROUND,
                ROUND
        };
        private ScreenLayoutRound(String name, int flag) {
            super(name, flag);
        }
        public static ScreenLayoutRound valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static ScreenLayoutRound valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static ScreenLayoutRound fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static ScreenLayoutRound fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(ScreenLayoutRound flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class ColorModeWide extends Flag{
        public static final int MASK = 0x03;
        public static final ColorModeWide NOWIDECG = new ColorModeWide("nowidecg", 0x01);
        public static final ColorModeWide WIDECG = new ColorModeWide("widecg", 0x02);
        public static final ColorModeWide[] VALUES = new ColorModeWide[]{
                NOWIDECG,
                WIDECG
        };
        private ColorModeWide(String name, int flag) {
            super(name, flag);
        }
        public static ColorModeWide valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static ColorModeWide valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static ColorModeWide fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static ColorModeWide fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(ColorModeWide flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }
    public static final class ColorModeHdr extends Flag{
        public static final int MASK = 0x0C;
        public static final ColorModeHdr LOWDR = new ColorModeHdr("lowdr", 0x04);
        public static final ColorModeHdr HIGHDR = new ColorModeHdr("highdr", 0x08);
        public static final ColorModeHdr[] VALUES = new ColorModeHdr[]{
                LOWDR,
                HIGHDR
        };
        private ColorModeHdr(String name, int flag) {
            super(name, flag);
        }
        public static ColorModeHdr valueOf(int flag){
            return Flag.valueOf(VALUES, MASK, flag);
        }
        public static ColorModeHdr valueOf(String name){
            return Flag.valueOf(VALUES, name);
        }
        public static ColorModeHdr fromQualifiers(String qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static ColorModeHdr fromQualifiers(String[] qualifiers){
            return Flag.fromQualifiers(VALUES, qualifiers);
        }
        public static int update(ColorModeHdr flag, int value){
            return Flag.update(MASK, flag, value);
        }
    }

    public static class Flag implements Comparable<Flag> {
        private final String name;
        private final int flag;
        Flag(String name, int flag){
            this.name = name;
            this.flag = flag;
        }
        public int getFlag() {
            return flag;
        }
        @Override
        public int compareTo(Flag flag) {
            if(flag == null) {
                return 1;
            }
            return CompareUtil.compare(getFlag(), flag.getFlag());
        }
        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
        @Override
        public int hashCode() {
            return super.hashCode();
        }
        @Override
        public String toString() {
            return name;
        }
        public static String toString(Flag flag){
            if(flag!= null){
                return flag.toString();
            }
            return null;
        }
        static<T extends Flag> T fromQualifiers(T[] values, String qualifiers){
            if(qualifiers == null){
                return null;
            }
            return fromQualifiers(values, qualifiers.split("\\s*-\\s*"));
        }
        static<T extends Flag> T fromQualifiers(T[] values, String[] qualifiers){
            if(qualifiers == null){
                return null;
            }
            for(int i = 0; i < qualifiers.length; i++){
                T flag = Flag.valueOf(values, qualifiers[i]);
                if(flag != null){
                    qualifiers[i] = null;
                    return flag;
                }
            }
            return null;
        }
        static<T extends Flag> T valueOf(T[] values, int mask, int flagValue){
            flagValue = flagValue & mask;
            for(T flag:values){
                if(flagValue == flag.getFlag()){
                    return flag;
                }
            }
            return null;
        }
        static<T extends Flag> T valueOf(T[] values, String name){
            if(name == null || name.length() == 0){
                return null;
            }
            if(name.charAt(0) == '-'){
                name = name.substring(1);
            }
            name = name.toLowerCase(java.util.Locale.ROOT);
            for(T flag:values){
                if(name.equals(flag.toString())){
                    return flag;
                }
            }
            return null;
        }
        public static int update(int mask, Flag flag, int value){
            int flip = (~mask) & 0xff;
            value = value & flip;
            if(flag != null){
                value = value | flag.getFlag();
            }
            return value;
        }
    }

    static class QualifierBuilder{
        private final ResConfig mConfig;
        private StringBuilder mBuilder;
        private String mNumberingSystem;
        public QualifierBuilder(ResConfig resConfig){
            this.mConfig = resConfig;
        }
        public String build(){
            ResConfig resConfig = this.mConfig;
            if(resConfig.isDefault()){
                return "";
            }
            this.mBuilder = new StringBuilder();
            appendPrefixedNumber("mcc", resConfig.getMcc());
            appendPrefixedNumber("mnc", resConfig.getMnc());

            appendLanguageAndRegion();

            appendFlag(resConfig.getGender());

            appendFlag(resConfig.getScreenLayoutDir());
            appendDp("sw", resConfig.getSmallestScreenWidthDp());
            appendDp("w", resConfig.getScreenWidthDp());
            appendDp("h", resConfig.getScreenHeightDp());

            appendFlag(resConfig.getScreenLayoutSize());
            appendFlag(resConfig.getScreenLayoutLong());
            appendFlag(resConfig.getScreenLayoutRound());

            appendFlag(resConfig.getColorModeWide());
            appendFlag(resConfig.getColorModeHdr());

            appendFlag(resConfig.getOrientation());
            appendFlag(resConfig.getUiModeType());
            appendFlag(resConfig.getUiModeNight());

            appendFlag(resConfig.getDensity());

            appendFlag(resConfig.getTouchscreen());
            appendFlag(resConfig.getInputFlagsKeysHidden());
            appendFlag(resConfig.getKeyboard());
            appendFlag(resConfig.getInputFlagsNavHidden());
            appendFlag(resConfig.getNavigation());

            appendScreenWidthHeight();

            // append resConfig.getMinorVersion()
            appendPrefixedNumber("v", resConfig.getSdkVersion());

            //appendLocaleNumberingSystem();
            appendUnknownBytes();

            return mBuilder.toString();
        }
        private void appendScreenWidthHeight(){
            ResConfig resConfig = this.mConfig;
            int width = resConfig.getScreenWidth();
            int height = resConfig.getScreenHeight();
            if(width == 0 && height == 0){
                return;
            }
            mBuilder.append('-').append(width).append('x').append(height);
        }
        private void appendLanguageAndRegion(){
            ResConfig resConfig = this.mConfig;
            String language = resConfig.getLanguage();
            String region = resConfig.getRegion();
            String script = resConfig.getLocaleScript();
            String variant = resConfig.getLocaleVariant();
            if(language == null && region == null){
                return;
            }
            StringBuilder builder = this.mBuilder;
            char separator;
            if(script != null || variant != null || (region != null && region.length() == 3)){
                builder.append('-');
                builder.append('b');
                separator = '+';
            }else {
                separator = '-';
            }
            if(language!= null){
                builder.append(separator);
                builder.append(language);
            }
            if(region!= null){
                builder.append(separator);
                if(region.length() == 2){
                    builder.append('r');
                }
                builder.append(region);
            }
            if(script!= null){
                builder.append(separator);
                builder.append(script);
            }
            if(variant!= null){
                builder.append(separator);
                builder.append(variant);
            }
        }
        private void appendLocaleNumberingSystem(){
            String numberingSystem = mNumberingSystem;
            if(numberingSystem== null){
                return;
            }
            StringBuilder builder = mBuilder;
            builder.append("-u+nu+");
            builder.append(numberingSystem);
        }
        private void appendUnknownBytes(){
            String unknownHexBytes = mConfig.getUnknownHexBytes();
            if(unknownHexBytes == null){
                return;
            }
            StringBuilder builder = mBuilder;
            builder.append('-');
            builder.append(ResConfig.UNKNOWN_BYTES);
            builder.append(unknownHexBytes);
        }
        private void appendFlag(ResConfig.Flag flag){
            if(flag== null){
                return;
            }
            mBuilder.append('-').append(flag.toString());
        }
        private void appendDp(String prefix, int number){
            if(number == 0){
                return;
            }
            StringBuilder builder = this.mBuilder;
            builder.append('-');
            if(prefix!= null){
                builder.append(prefix);
            }
            builder.append(number);
            builder.append("dp");
        }
        private void appendPrefixedNumber(String prefix, int number){
            if(number == 0){
                return;
            }
            StringBuilder builder = this.mBuilder;
            builder.append('-');
            builder.append(prefix);
            builder.append(number);
        }
    }
    static class QualifierParser{
        private final ResConfig mConfig;
        private final String[] mQualifiers;
        private final int mPreferredSize;
        private boolean mEmpty;
        private boolean mLanguageRegionParsed;
        private boolean mParseComplete;

        public QualifierParser(ResConfig resConfig, String[] qualifiers){
            this.mConfig = resConfig;
            this.mQualifiers = qualifiers;
            this.mPreferredSize = resConfig.getConfigSize();
        }
        public QualifierParser(ResConfig resConfig, String qualifiers){
            this(resConfig, splitQualifiers(qualifiers));
        }

        public void parse(){
            if(this.mParseComplete){
                return;
            }
            if(isEmpty()){
                onParseComplete();
                return;
            }
            ResConfig resConfig = this.mConfig;
            resConfig.setConfigSize(ResConfig.SIZE_64);
            parsePrefixedNumber();
            parseDp();
            parseWidthHeight();
            parseLocaleNumberingSystem();
            parseUnknownBytes();
            if(isEmpty()){
                onParseComplete();
                return;
            }
            String[] qualifiers = this.mQualifiers;
            resConfig.setOrientation(ResConfig.Orientation.fromQualifiers(qualifiers));
            resConfig.setTouchscreen(ResConfig.Touchscreen.fromQualifiers(qualifiers));
            resConfig.setDensity(ResConfig.Density.fromQualifiers(qualifiers));
            resConfig.setKeyboard(ResConfig.Keyboard.fromQualifiers(qualifiers));
            resConfig.setNavigation(ResConfig.Navigation.fromQualifiers(qualifiers));
            if(isEmpty()){
                onParseComplete();
                return;
            }
            resConfig.setInputFlagsKeysHidden(ResConfig.InputFlagsKeysHidden.fromQualifiers(qualifiers));
            resConfig.setInputFlagsNavHidden(ResConfig.InputFlagsNavHidden.fromQualifiers(qualifiers));
            resConfig.setGender(ResConfig.Gender.fromQualifiers(qualifiers));
            resConfig.setScreenLayoutSize(ResConfig.ScreenLayoutSize.fromQualifiers(qualifiers));
            resConfig.setScreenLayoutLong(ResConfig.ScreenLayoutLong.fromQualifiers(qualifiers));
            resConfig.setScreenLayoutDir(ResConfig.ScreenLayoutDir.fromQualifiers(qualifiers));
            if(isEmpty()){
                onParseComplete();
                return;
            }
            resConfig.setUiModeType(ResConfig.UiModeType.fromQualifiers(qualifiers));
            resConfig.setUiModeNight(ResConfig.UiModeNight.fromQualifiers(qualifiers));

            resConfig.setScreenLayoutRound(ResConfig.ScreenLayoutRound.fromQualifiers(qualifiers));

            resConfig.setColorModeWide(ResConfig.ColorModeWide.fromQualifiers(qualifiers));
            resConfig.setColorModeHdr(ResConfig.ColorModeHdr.fromQualifiers(qualifiers));
            if(isEmpty()){
                onParseComplete();
                return;
            }
            parseLocaleScriptVariant();
            parseLanguage();
            parseRegion();
            onParseComplete();
        }
        public void parseLocale(){
            if(mParseComplete){
                return;
            }
            if(isEmpty()){
                ResConfig resConfig = mConfig;
                resConfig.setLanguage((String) null);
                resConfig.setRegion((String) null);
                resConfig.setLocaleScript((String) null);
                mParseComplete = true;
                return;
            }
            parseLanguage();
            parseLocaleRegion();
            String[] qualifiers = this.mQualifiers;
            String script = null;
            if(qualifiers != null){
                for(int i = 0; i < qualifiers.length; i++){
                    String qualifier = qualifiers[i];
                    if(qualifier == null || qualifier.length() < 2){
                        continue;
                    }
                    script = qualifier;
                    qualifiers[i] = null;
                    break;
                }
            }
            mConfig.setLocaleScript(script);
            mParseComplete = true;
        }
        public String[] getErrors(){
            if(!this.mParseComplete){
                return null;
            }
            String[] qualifiers = this.mQualifiers;
            if(isEmpty(qualifiers)){
                return null;
            }
            int length = qualifiers.length;
            String[] tmp = new String[length];
            int count = 0;
            for(int i = 0; i < length; i++){
                String qualifier = qualifiers[i];
                if(qualifier == null || qualifier.length() == 0){
                    continue;
                }
                tmp[count] = qualifier;
                count++;
            }
            if(count == 0){
                return null;
            }
            if(count == length){
                return tmp;
            }
            String[] errors = new String[count];
            System.arraycopy(tmp, 0, errors, 0, count);
            return errors;
        }
        private void onParseComplete(){
            this.mConfig.trimToSize(this.mPreferredSize);
            this.mParseComplete = true;
        }

        private void parsePrefixedNumber(){
            if(isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parsePrefixedNumber(qualifiers[i])){
                    qualifiers[i] = null;
                }
            }
        }
        private boolean parsePrefixedNumber(String qualifier){
            if(qualifier == null){
                return false;
            }
            Matcher matcher = PATTERN_PREFIX_NUMBER.matcher(qualifier);
            if(!matcher.find()){
                return false;
            }
            String prefix = matcher.group(1);
            int value = Integer.parseInt(matcher.group(2));
            ResConfig resConfig = mConfig;
            if("mcc".equals(prefix)){
                resConfig.setMcc(value);
            }else if("mnc".equals(prefix)) {
                resConfig.setMnc(value);
            }else if("v".equals(prefix)){
                resConfig.setSdkVersion(value);
            }else {
                return false;
            }
            return true;
        }
        private void parseDp(){
            if(isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseDp(qualifiers[i])){
                    qualifiers[i] = null;
                }
            }
        }
        private boolean parseDp(String qualifier){
            if(qualifier == null){
                return false;
            }
            Matcher matcher = PATTERN_DP.matcher(qualifier);
            if(!matcher.find()){
                return false;
            }
            String prefix = matcher.group(1);
            int value = Integer.parseInt(matcher.group(2));
            ResConfig resConfig = this.mConfig;
            if("sw".equals(prefix)){
                resConfig.setSmallestScreenWidthDp(value);
            }else if("w".equals(prefix)) {
                resConfig.setScreenWidthDp(value);
            }else if("h".equals(prefix)){
                resConfig.setScreenHeightDp(value);
            }else {
                return false;
            }
            return true;
        }
        private void parseWidthHeight(){
            if(isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseWidthHeight(qualifiers[i])){
                    qualifiers[i] = null;
                    return;
                }
            }
        }
        private boolean parseWidthHeight(String qualifier){
            if(qualifier == null){
                return false;
            }
            Matcher matcher = PATTERN_WIDTH_HEIGHT.matcher(qualifier);
            if(!matcher.find()){
                return false;
            }
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));
            ResConfig resConfig = this.mConfig;
            resConfig.setScreenWidth(width);
            resConfig.setScreenHeight(height);
            return true;
        }
        private void parseLocaleNumberingSystem(){
            if(isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseLocaleNumberingSystem(qualifiers[i])){
                    qualifiers[i] = null;
                    return;
                }
            }
        }
        private void parseUnknownBytes(){
            if(isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseUnknownBytes(qualifiers[i])){
                    qualifiers[i] = null;
                    return;
                }
            }
        }
        private boolean parseLocaleNumberingSystem(String qualifier){
            if(qualifier == null){
                return false;
            }
            Matcher matcher = PATTERN_LOCALE_NUMBERING_SYSTEM.matcher(qualifier);
            if(!matcher.find()){
                return false;
            }
            //TODO: where to set ?
            return true;
        }
        private void parseLocaleScriptVariant(){
            if(this.mLanguageRegionParsed || isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseLocaleScriptVariant(qualifiers[i])){
                    qualifiers[i] = null;
                    this.mLanguageRegionParsed = true;
                    return;
                }
            }
        }
        private boolean parseLocaleScriptVariant(String qualifier){
            if(qualifier == null || qualifier.length() < 4 ){
                return false;
            }
            char[] chars = qualifier.toCharArray();
            if(chars[0] != 'b' || chars[1] != '+'){
                return false;
            }
            Matcher matcher = PATTERN_LOCALE_SCRIPT_VARIANT.matcher(qualifier);
            if(matcher.find()) {
                ResConfig resConfig = this.mConfig;
                resConfig.setLanguage(trimPlus(matcher.group(1)));
                resConfig.setRegion(trimPlus(matcher.group(2)));
                resConfig.setLocaleScript(trimPlus(matcher.group(3)));
                resConfig.setLocaleVariant(trimPlus(matcher.group(4)));
                return true;
            }
            return false;
        }

        private void parseLanguage(){
            if(mLanguageRegionParsed || isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseLanguage(qualifiers[i])){
                    qualifiers[i] = null;
                    return;
                }
            }
        }
        private boolean parseLanguage(String qualifier){
            if(!isLanguage(qualifier)){
                return false;
            }
            this.mConfig.setLanguage(qualifier);
            return true;
        }
        private void parseLocaleRegion(){
            if(mLanguageRegionParsed || isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseLocaleRegion(qualifiers[i])){
                    qualifiers[i] = null;
                    return;
                }
            }
        }
        private void parseRegion(){
            if(mLanguageRegionParsed || isEmpty()){
                return;
            }
            String[] qualifiers = this.mQualifiers;
            for(int i = 0; i < qualifiers.length; i++){
                if(parseRegion(qualifiers[i])){
                    qualifiers[i] = null;
                    return;
                }
            }
        }
        private boolean parseLocaleRegion(String qualifier){
            if(!isLocaleRegion(qualifier)){
                return false;
            }
            this.mConfig.setRegion(qualifier);
            return true;
        }
        private boolean parseRegion(String qualifier){
            if(!isRegion(qualifier)){
                return false;
            }
            this.mConfig.setRegion(qualifier);
            return true;
        }
        private boolean parseUnknownBytes(String qualifier){
            if(qualifier == null){
                return false;
            }
            if(!qualifier.startsWith(ResConfig.UNKNOWN_BYTES)){
                return false;
            }
            qualifier = qualifier.substring(ResConfig.UNKNOWN_BYTES.length());
            this.mConfig.setUnknownBytes(qualifier);
            return true;
        }


        private boolean isEmpty(){
            if(!mEmpty){
                mEmpty = isEmpty(mQualifiers);
            }
            return mEmpty;
        }

        private static boolean isEmpty(String[] qualifiers){
            if(qualifiers == null){
                return true;
            }
            for(int i = 0; i < qualifiers.length; i++){
                String qualifier = qualifiers[i];
                if(qualifier == null){
                    continue;
                }
                if(qualifier.length() == 0){
                    qualifiers[i] = null;
                    continue;
                }
                return false;
            }
            return true;
        }
        private static String trimPlus(String text){
            if(text == null||text.length() == 0){
                return null;
            }
            if(text.charAt(0) == '+'){
                text = text.substring(1);
            }
            return text;
        }
        private static boolean isLanguage(String qualifier){
            if(qualifier == null){
                return false;
            }
            char[] chars = qualifier.toCharArray();
            int length = chars.length;
            if(length != 2  && length !=3 ){
                return false;
            }
            for(int i = 0; i < length; i++){
                if(!isAtoZLower(chars[i])) {
                    return false;
                }
            }
            return true;
        }
        private static boolean isLocaleRegion(String qualifier){
            if(qualifier == null){
                return false;
            }
            int length = qualifier.length();
            if(length != 2 && length != 3){
                return false;
            }
            char[] chars = qualifier.toCharArray();
            if(length == 2){
                for(char ch : chars){
                    if(!isAtoZUpper(ch)){
                        return false;
                    }
                }
                return true;
            }
            for(char ch : chars){
                if(!isDigit(ch)){
                    return false;
                }
            }
            return true;
        }
        private static boolean isRegion(String qualifier){
            if(qualifier == null || qualifier.length() != 3){
                return false;
            }
            char[] chars = qualifier.toCharArray();
            boolean checkDigit = false;
            for(int i = 0; i < chars.length; i++){
                char ch = chars[i];
                if(i == 0){
                    if(ch == 'r'){
                        continue;
                    }
                    checkDigit = isDigit(ch);
                    if(checkDigit){
                        continue;
                    }
                    return false;
                }
                if(checkDigit){
                    if(!isDigit(ch)){
                        return false;
                    }
                }else if(!isAtoZUpper(ch)) {
                    return false;
                }
            }
            return true;
        }
        private static String[] splitQualifiers(String qualifier){
            if(qualifier == null || qualifier.length() == 0){
                return null;
            }
            return qualifier.split("-");
        }
        private static boolean isDigit(char ch){
            return ch <= '9' && ch >= '0';
        }
        private static boolean isAtoZLower(char ch){
            return ch <= 'z' && ch >= 'a';
        }
        private static boolean isAtoZUpper(char ch){
            return ch <= 'Z' && ch >= 'A';
        }

        private static final Pattern PATTERN_PREFIX_NUMBER = Pattern.compile("^([mcnv]+)([0-9]+)$");
        private static final Pattern PATTERN_DP = Pattern.compile("^([swh]+)([0-9]+)dp$");
        private static final Pattern PATTERN_WIDTH_HEIGHT = Pattern.compile("^([0-9]+)[xX]([0-9]+)$");
        private static final Pattern PATTERN_LOCALE_NUMBERING_SYSTEM = Pattern.compile("^u\\+nu\\+(.{1,8})$");
        private static final Pattern PATTERN_LOCALE_SCRIPT_VARIANT = Pattern.compile("^b(\\+[a-z]{2})?(\\+r?[A-Z0-9]{2,3})?(\\+[A-Z][a-z]{3})?(\\+[A-Z]{2,8})?$");
    }

    private static final ResConfig DEFAULT_INSTANCE = new ResConfig(SIZE_16);

    private static final char POSTFIX_locale = '#';
}
