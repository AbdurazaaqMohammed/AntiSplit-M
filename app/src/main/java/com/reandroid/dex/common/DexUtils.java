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

package com.reandroid.dex.common;

import com.reandroid.dex.key.TypeKey;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.NumbersUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import com.reandroid.utils.function.Function;

public class DexUtils {

    public static List<File> listDexFiles(File dir){
        ArrayCollection<File> results = new ArrayCollection<>();
        if(!dir.isDirectory()){
            return results;
        }
        File[] files = dir.listFiles();
        if(files == null){
            return results;
        }
        for(File file : files){
            if(file.isFile() && file.getName().endsWith(".dex")){
                results.add(file);
            }
        }
        results.sort(getDexPathComparator());
        return results;
    }
    public static<T> Comparator<T> getDexPathComparator(){
        return DexUtils::compareDex;
    }
    public static<T, E> Comparator<T> getDexPathComparator(Function<T, E> function){
        return (dex1, dex2) -> compareDex(function, dex1, dex2);
    }
    public static<T, E> int compareDex(Function<T, E> function, T dexPath1, T dexPath2){
        return compareDex(function.apply(dexPath1), function.apply(dexPath2));
    }
    public static int compareDex(Object dexPath1, Object dexPath2){
        if(dexPath1 == dexPath2){
            return 0;
        }
        if(dexPath1 == null){
            return 1;
        }
        if(dexPath2 == null){
            return -1;
        }
        return compareDexPath(dexPath1.toString(), dexPath2.toString());
    }
    public static int compareDexPath(String path1, String path2){
        if(path1 == null){
            return 1;
        }
        if(path2 == null){
            return -1;
        }
        if(path1.equals(path2)){
            return 0;
        }
        return Integer.compare(getDexNumber(path1), getDexNumber(path2));
    }
    private static int getDexNumber(String path){
        int i = path.lastIndexOf('/');
        if(i < 0){
            i = path.lastIndexOf(File.separatorChar);
        }
        if(i >= 0){
            path = path.substring(i + 1);
        }
        String name = "classes";
        if(path.equals(name)){
            return 0;
        }
        if(!path.startsWith(name)){
            return 0xffff;
        }
        path = path.substring(name.length());
        i = path.indexOf('.');
        if(i == 0){
            return 0;
        }else if(i < 0){
            return 0xffff;
        }
        path = path.substring(0, i);
        try{
            return Integer.parseInt(path);
        }catch (NumberFormatException ignored){
            return 0xffff;
        }
    }
    public static String[] splitParameters(String parameters) {
        if(StringsUtil.isEmpty(parameters)){
            return null;
        }
        int length = parameters.length();
        String[] results = new String[length];
        int count = 0;
        boolean array = false;
        int start = 0;
        for(int i = 0; i < length; i++){
            boolean pop = false;
            char ch = parameters.charAt(i);
            if(ch == '[') {
                array = true;
            }else if(ch == ';'){
                pop = true;
            }else if((array || (i - start) == 0) && isPrimitive(ch)){
                pop = true;
                array = false;
            }else {
                array = false;
            }
            if(pop) {
                results[count] = parameters.substring(start, i + 1);
                count ++;
                start = i + 1;
            }
        }
        if(count == 0){
            return null;
        }
        if(count == length){
            return results;
        }
        String[] tmp = new String[count];
        System.arraycopy(results, 0, tmp, 0, count);
        return tmp;
    }
    public static String quoteString(String text){
        StringBuilder builder = new StringBuilder(text.length() + 2);
        try {
            appendQuotedString(builder, text);
        } catch (IOException ignored) {
        }
        return builder.toString();
    }
    public static String encodeString(String text){
        StringBuilder builder = new StringBuilder(text.length());
        try {
            encodeString(builder, text);
        } catch (IOException ignored) {
        }
        return builder.toString();
    }
    public static void appendQuotedString(Appendable appendable, String text) throws IOException {
        appendable.append('"');
        encodeString(appendable, text);
        appendable.append('"');
    }
    public static boolean encodeString(Appendable appendable, String text) throws IOException {
        boolean unicodeDetected = false;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if ((c >= ' ') && (c < 0x7f)) {
                if ((c == '\'') || (c == '\"') || (c == '\\')) {
                    appendable.append('\\');
                }
                appendable.append(c);
                continue;
            } else if (c <= 0x7f) {
                switch (c) {
                    case '\n':
                        appendable.append("\\n");
                        continue;
                    case '\r':
                        appendable.append("\\r");
                        continue;
                    case '\t':
                        appendable.append("\\t");
                        continue;
                }
            }
            encodeToHexChar(appendable, c);
            if(!unicodeDetected && c != '…' && c > 0xff) {
                unicodeDetected = true;
            }
        }
        return unicodeDetected;
    }
    public static void appendCommentString(int maxLength, Appendable appendable, String text) throws IOException {
        if (appendable == null) {
            return;
        }
        int length = NumbersUtil.min(maxLength, text.length());
        appendable.append('\'');
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if ((c >= ' ') && (c < 0x7f)) {
                appendable.append(c);
                continue;
            } else if (c <= 0x7f) {
                switch (c) {
                    case '\n':
                        appendable.append("\\n");
                        continue;
                    case '\r':
                        appendable.append("\\r");
                        continue;
                    case '\t':
                        appendable.append("\\t");
                        continue;
                }
            }
            if(Character.isDefined(c) && !Character.isWhitespace(c)) {
                appendable.append(c);
            } else {
                encodeToHexChar(appendable, c);
            }
        }
        appendable.append('\'');
    }
    public static String quoteChar(char ch){
        StringBuilder builder = new StringBuilder();
        try {
            appendSingleQuotedChar(builder, ch);
        } catch (IOException ignored) {
        }
        return builder.toString();
    }
    public static void appendSingleQuotedChar(Appendable appendable, char ch) throws IOException {
        if ((ch >= ' ') && (ch < 0x7f)) {
            appendable.append('\'');
            if ((ch == '\'') || (ch == '\"') || (ch == '\\')) {
                appendable.append('\\');
            }
            appendable.append(ch);
            appendable.append('\'');
            return;
        } else if (ch <= 0x7f) {
            switch (ch) {
                case '\b':
                    appendable.append("'\\b'");
                    return;
                case '\f':
                    appendable.append("'\\f'");
                    return;
                case '\n':
                    appendable.append("'\\n'");
                    return;
                case '\r':
                    appendable.append("'\\r'");
                    return;
                case '\t':
                    appendable.append("'\\t'");
                    return;
            }
        }

        appendable.append('\'');
        encodeToHexChar(appendable, ch);
        appendable.append('\'');
    }
    public static void encodeToHexChar(Appendable appendable, char c) throws IOException {
        appendable.append("\\u");
        appendable.append(Character.forDigit(c >> 12, 16));
        appendable.append(Character.forDigit((c >> 8) & 0x0f, 16));
        appendable.append(Character.forDigit((c >> 4) & 0x0f, 16));
        appendable.append(Character.forDigit(c & 0x0f, 16));
    }
    public static void encodeToHexChar(StringBuilder builder, char c) {
        try {
            encodeToHexChar((Appendable) builder, c);
        } catch (IOException ignored) {}
    }
    public static String encodeToHexChar(char c) {
        StringBuilder builder = new StringBuilder(6);
        encodeToHexChar(builder, c);
        return builder.toString();
    }
    /**
     * Use StringKey.decodeEscapedString
     * */
    @Deprecated
    public static String decodeString(String text) {
        if(text.indexOf('\\') < 0){
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int length = text.length();
        boolean escaped = false;
        for(int i = 0; i < length; i++){
            char ch = text.charAt(i);
            if(escaped){
                escaped = false;
                if(ch == 'u'){
                    Character character = nextHex(text, i + 1);
                    if(character != null){
                        sb.append(character);
                        i = i + 4;
                        continue;
                    }
                }
                sb.append(getEscaped(ch));
            }else if(ch == '\\'){
                escaped = true;
            }else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    private static char getEscaped(char ch){
        switch(ch) {
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                return ch;
        }
    }
    private static Character nextHex(String text, int start){
        int length = text.length();
        int end = start + 4;
        if (end > length) {
            return null;
        }
        int value = 0;
        for (int i = start; i < end; i++) {
            int v = HexUtil.decodeHexChar(text.charAt(i));
            if (v == -1) {
                return null;
            }
            value = value << 4;
            value = value | v;
        }
        return (char) value;
    }
    public static boolean isJavaFramework(String name){
        return name.startsWith("Ljava/");
    }

    public static String toSignatureType(String type){
        if(type == null){
            return null;
        }
        int length = type.length();
        if(length == 0){
            return type;
        }
        int i = 0;
        while (type.charAt(i) == '['){
            i++;
        }
        length = length - 1;
        if(i == length || type.charAt(length) == '<' || type.charAt(length) != ';'){
            if(i != 0){
                return type.substring(i);
            }
            return type;
        }
        StringBuilder builder = new StringBuilder(length - i);
        builder.append(type, i, length);
        builder.append('<');
        return builder.toString();
    }
    public static String toDeclaringType(String type){
        if(type == null){
            return null;
        }
        int length = type.length();
        if(length == 0){
            return type;
        }
        int i = 0;
        while (type.charAt(i) == '['){
            i++;
        }
        length = length - 1;
        if(i == length || type.charAt(length) == ';' || type.charAt(length) != '<'){
            if(i != 0){
                return type.substring(i);
            }
            return type;
        }
        StringBuilder builder = new StringBuilder(length - i);
        builder.append(type, i, length);
        builder.append(';');
        return builder.toString();
    }
    public static String makeArrayType(String type, int dimension){
        if(type == null){
            return null;
        }
        int length = type.length();
        if(length == 0){
            return type;
        }
        int i = 0;
        while (type.charAt(i) == '['){
            i++;
        }
        if(i == dimension){
            return type;
        }
        if(i > dimension){
            i = i - dimension;
            return type.substring(i);
        }
        i = dimension - i;
        StringBuilder builder = new StringBuilder(length + i);
        while (i > 0){
            builder.append('[');
            i--;
        }
        builder.append(type);
        return builder.toString();
    }
    public static int countArrayPrefix(String type){
        if(type == null){
            return 0;
        }
        int length = type.length();
        if(length < 2){
            return 0;
        }
        int i = 0;
        while (type.charAt(i) == '['){
            i++;
        }
        return i;
    }
    public static boolean isTypeArray(String type){
        if(type == null){
            return false;
        }
        int length = type.length();
        if(length < 2){
            return false;
        }
        int i = 0;
        while (type.charAt(i) == '['){
            i++;
        }
        if(i == 0){
            return false;
        }
        length = length - 1;
        if(i == length){
            return isPrimitive(type.charAt(i));
        }
        return type.charAt(i) == 'L' && type.charAt(length) == ';';
    }
    public static boolean isTypeSignature(String type){
        if(type == null){
            return false;
        }
        int length = type.length();
        if(length < 3){
            return false;
        }
        return type.charAt(0) == 'L' && type.charAt(length - 1) == '<';
    }
    public static boolean isTypeOrSignature(String type){
        if(type == null){
            return false;
        }
        int length = type.length();
        if(length < 3){
            return false;
        }
        char last = type.charAt(length - 1);
        return type.charAt(0) == 'L' && (last == '<' || last == ';');
    }
    public static boolean isTypeObject(String type){
        if(type == null || type.length() < 2){
            return false;
        }
        char ch = type.charAt(0);
        return ch == 'L' || ch == '[';
    }
    public static boolean isPlatform(TypeKey typeKey){
        if(typeKey == null){
            return false;
        }
        return isPlatform(typeKey.getTypeName());
    }
    public static boolean isPlatform(String type){
        if(type == null){
            return false;
        }
        int length = type.length();
        if(length == 0){
            return false;
        }
        int i = 0;
        while (i < length && type.charAt(i) == '['){
            i++;
        }
        if(i >= length){
            return false;
        }
        if(i != 0){
            type = type.substring(i);
        }
        for(String platformPackage : PLATFORM_PACKAGES){
            if(type.startsWith(platformPackage)){
                return true;
            }
        }
        return false;
    }
    public static boolean isPrimitive(String type){
        if(type == null || type.length() != 1){
            return false;
        }
        return isPrimitive(type.charAt(0));
    }
    public static boolean isPrimitive(char ch){
        switch (ch){
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'V':
            case 'Z':
                return true;
            default:
                return false;
        }
    }
    public static boolean looksSignatureType(String name){
        int length = name.length();
        if(length < 3){
            return false;
        }
        return name.charAt(0) == 'L' && name.charAt(length - 1) == '<';
    }

    public static String toSourceName(String binaryName){
        int i = binaryName.indexOf('L');
        binaryName = binaryName.substring(i + 1);
        i = binaryName.indexOf(';');
        if(i < 0){
            i = binaryName.indexOf('<');
        }
        if(i > 0){
            binaryName = binaryName.substring(0, i);
        }
        return binaryName.replace('/', '.');
    }
    public static String toBinaryName(String sourceName){
        return 'L' + sourceName.replace('.', '/') + ';';
    }
    public static String toBinaryPackageName(String sourceName) {
        if(sourceName.indexOf('/') > 0) {
            return sourceName;
        }
        String binName = 'L' + sourceName.replace('.', '/');
        if(binName.charAt(sourceName.length() - 1) != '/') {
            binName = binName + '/';
        }
        return binName;
    }
    public static String getPackageName(String className) {
        if(className.length() < 3){
            return StringsUtil.EMPTY;
        }
        int start = 0;
        while (className.charAt(start) == '['){
            start ++;
        }
        int i = className.lastIndexOf('/');
        if(i > start){
            return className.substring(start, i + 1);
        }
        i = start;
        if(className.charAt(i) == 'L'){
            i++;
        }
        return className.substring(start, i);
    }
    public static String toSourceFileName(String className){
        String simple = getSimpleName(className);
        int i = simple.indexOf('$');
        if(i > 0){
            simple = simple.substring(0, i);
        }
        return simple + ".java";
    }
    public static String getSimpleName(String className) {
        className = trimArrayPrefix(className);
        if(className.length() < 2){
            return className;
        }
        int i = className.lastIndexOf('/');
        if (i < 0){
            i = 0;
        }
        i++;
        className = className.substring(i);
        i = className.length() - 1;
        if(className.charAt(i) == ';' || className.charAt(i) == '<'){
            className = className.substring(0, i);
        }
        return className;
    }
    public static String getSimpleInnerName(String className) {
        className = trimArrayPrefix(className);
        if(className.length() < 2){
            return className;
        }
        int i = className.lastIndexOf('/');
        if (i < 0){
            i = 0;
        }
        i++;
        className = className.substring(i);
        i = className.length() - 1;
        if(className.charAt(i) == ';' || className.charAt(i) == '<'){
            className = className.substring(0, i);
        }
        i = className.indexOf('$');
        if(i < 0){
            return className;
        }
        String[] split = StringsUtil.split(className, '$', true);
        split = StringsUtil.removeEmpty(split);
        i = split.length;
        if(i < 2){
            return className;
        }
        return split[i - 1];
    }
    public static String getParentClassName(String className) {
        className = trimArrayPrefix(className);
        if(className.length() < 2){
            return className;
        }
        int i = className.lastIndexOf('/');
        if (i < 0){
            i = 0;
        }
        i++;
        int j = className.lastIndexOf('$');
        if(j <= i){
            return className;
        }
        className = className.substring(0, j);
        while (className.length() > 0 && className.charAt(className.length() - 1) == '$'){
            className = className.substring(0, className.length() - 1);
        }
        return className + ";";
    }
    public static String createChildClass(String type, String simpleName){
        int i = type.length();
        if(i < 2){
            return type;
        }
        i = i - 1;
        char ch = type.charAt(i);
        if(ch == ';' || ch == '<'){
            type = type.substring(0, i);
        }
        return type + "$" + simpleName + ";";
    }
    public static String trimArrayPrefix(String className) {
        int i = 0;
        while (i < className.length() && className.charAt(i) == '['){
            i++;
        }
        if(i == 0){
            return className;
        }
        return className.substring(i);
    }
    public static String[] splitSignatures(String text){
        ArrayCollection<String> results = new ArrayCollection<>(5);
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for(int i = 0; i < length; i++){
            char ch = text.charAt(i);
            builder.append(ch);
            if(isSignatureSymbol(ch)){
                results.add(builder.toString());
                builder = new StringBuilder();
            }
        }
        if(builder.length() != 0){
            results.add(builder.toString());
        }
        return results.toArray(new String[results.size()]);
    }
    private static boolean isSignatureSymbol(char ch){
        switch (ch){
            case '+':
            case '-':
            case '*':
            case ':':
            case ';':
            case '<':
            case '>':
            case '?':
                return true;
            default:
                return false;
        }
    }

    public static final String[] PLATFORM_PACKAGES = new String[]{
            "Ljava/",
            "Landroid/",
            "Ldalvik/",
            "Lorg/json/",
            "Lorg/xmlpull/",
    };
}
