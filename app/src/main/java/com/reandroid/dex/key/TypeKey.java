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
package com.reandroid.dex.key;

import com.reandroid.dex.common.DexUtils;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.reflect.Type;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class TypeKey implements TypeDescriptorKey, ProgramKey {

    private final String typeName;
    private String simpleName;
    private PackageKey packageKey;

    public TypeKey(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public ElementType getElementType() {
        return ElementType.TYPE;
    }

    public String getTypeName() {
        return typeName;
    }
    public boolean isClassDefinition() {
        String name = getTypeName();
        int length = name.length() - 1;
        return length >= 2 && name.charAt(0) == 'L' && name.charAt(length) == ';';
    }
    public String getBinaryName() {
        String name = getTypeName();
        int length = name.length() - 1;
        if (length < 2 || name.charAt(length) != ';' || name.charAt(0) != 'L') {
            return name;
        }
        return name.substring(1, length);
    }
    public String getJarClassPath() {
        if (isTypeArray()) {
            return getDeclaring().getJarClassPath();
        }
        return getBinaryName() + ".class";
    }
    public char shorty() {
        String name = getTypeName();
        if (name.length() == 1) {
            return name.charAt(0);
        }
        return 'L';
    }
    public String detailedShorty() {
        return detailedShorty(null);
    }
    public String detailedShorty(Predicate<TypeKey> platformPredicate) {
        StringBuilder builder = new StringBuilder();
        appendDetailedShorty(builder, platformPredicate);
        return builder.toString();
    }
    public void appendDetailedShorty(StringBuilder builder, Predicate<TypeKey> platformPredicate) {
        String name = getTypeName();
        int length = name.length();
        if (length == 0) {
            return;
        }
        if (length == 1) {
            builder.append(name.charAt(0));
            return;
        }
        int i = 0;
        while (i < length && name.charAt(i) == '[') {
            builder.append('[');
            i ++;
        }
        char c = name.charAt(i);
        if (c != 'L') {
            builder.append(c);
            return;
        }
        if (platformPredicate != null) {
            TypeKey typeKey = i == 0 ? this : new TypeKey(name.substring(i));
            if (platformPredicate.test(typeKey)) {
                builder.append(typeKey.getTypeName());
                return;
            }
        }
        builder.append(c);
        int j = name.lastIndexOf('/', i);
        if (j > i) {
            i = j;
        }
        i = i + 1;
        j = name.indexOf('$', i);
        if (j > i && length > j + 2) {
            builder.append('$');
        }
    }

    public String getSourceName() {
        if (!isTypeArray()) {
            String type = getTypeName();
            if (type.length() == 1) {
                TypeKey typeKey = PrimitiveTypeKey.primitiveType(type.charAt(0));
                if (typeKey != null) {
                    return typeKey.getSourceName();
                }
            }
            return DexUtils.toSourceName(type);
        }
        return getTypeName().replace('/', '.');
    }
    @Override
    public TypeKey getDeclaring() {
        String main = getDeclaringName();
        if (main.equals(getTypeName())) {
            return this;
        }
        return TypeKey.create(main);
    }
    public Iterator<TypeKey> types() {
        Iterator<TypeKey> iterator = SingleIterator.of(this);
        if (isTypeArray()) {
            iterator = CombiningIterator.singleOne(getDeclaring(), iterator);
        }
        return iterator;
    }
    @Override
    public Iterator<Key> contents() {
        return CombiningIterator.two(types(), SingleIterator.of(getPackage()));
    }

    @Override
    public TypeKey replaceKey(Key search, Key replace) {
        if (search instanceof TypeKey) {
            if (search.equals(this)) {
                return (TypeKey) replace;
            }
            int array = getArrayDimension();
            if (array != 0) {
                TypeKey typeKey = setArrayDimension(0);
                if (!typeKey.isPrimitive()) {
                    TypeKey update = typeKey.replaceKey(search, replace);
                    if (typeKey != update) {
                        return update.setArrayDimension(array);
                    }
                }
            }
        } else if (search instanceof PackageKey) {
            PackageKey packageKey = getPackage();
            if (packageKey != null) {
                PackageKey update = packageKey.replaceKey(search, replace);
                if (packageKey != update) {
                    return update.type(getSimpleName())
                            .setArrayDimension(getArrayDimension());
                }
            }
        }
        return this;
    }

    public String getDeclaringName() {
        return DexUtils.toDeclaringType(getTypeName());
    }
    public String getSignatureTypeName() {
        return DexUtils.toSignatureType(getTypeName());
    }
    public TypeKey setArrayDimension(int dimension) {
        if (dimension == getArrayDimension()) {
            return this;
        }
        return new TypeKey(getArrayType(dimension));
    }
    public String getArrayType(int dimension) {
        return DexUtils.makeArrayType(getTypeName(), dimension);
    }
    public int getArrayDimension() {
        return DexUtils.countArrayPrefix(getTypeName());
    }

    public boolean isTypeArray() {
        String name = getTypeName();
        return name.length() > 1 && name.charAt(0) == '[';
    }
    public boolean isTypeObject() {
        return DexUtils.isTypeObject(getTypeName());
    }
    public boolean isPrimitive() {
        String name = getTypeName();
        if (name.length() != 1) {
            return false;
        }
        return isPrimitive(name.charAt(0));
    }
    public boolean isPrimitiveArray() {
        String name = getTypeName();
        int length = name.length();
        if (length < 2 || name.charAt(0) != '[') {
            return false;
        }
        for (int i = 1; i < length; i++) {
            char c = name.charAt(i);
            if (c == '[') {
                continue;
            }
            i ++;
            return i == length && isPrimitive(c);
        }
        return false;
    }
    public boolean isPrimitiveComponent() {
        return isPrimitive() || isPrimitiveArray();
    }
    public boolean isWide() {
        String name = getTypeName();
        if (name.length() != 1) {
            return false;
        }
        return name.equals(TYPE_D.getTypeName()) || name.equals(TYPE_J.getTypeName());
    }

    public String getSimpleName() {
        if (simpleName == null) {
            simpleName = DexUtils.getSimpleName(getTypeName());
        }
        return simpleName;
    }
    public String getSimpleInnerName() {
        String simple = getSimpleName();
        int i = simple.lastIndexOf('$');
        if (i > 0 && i < simple.length() - 1) {
            return simple.substring(i + 1);
        }
        return null;
    }
    public boolean isInnerName() {
        return getSimpleInnerName() != null;
    }

    public PackageKey getPackage() {
        PackageKey packageKey = this.packageKey;
        if (packageKey == null) {
            packageKey = PackageKey.of(this);
            this.packageKey = packageKey;
        }
        return packageKey;
    }
    public TypeKey setPackage(PackageKey packageKey) {
        if (packageKey.equals(getPackage())) {
            return this;
        }
        return packageKey.type(getSimpleName())
                .setArrayDimension(getArrayDimension());
    }
    public String getPackageName() {
        return DexUtils.getPackageName(getTypeName());
    }
    public String getPackageSourceName() {
        String packageName = getPackageName();
        int i = packageName.length() - 1;
        if (i < 1) {
            return StringsUtil.EMPTY;
        }
        return packageName.substring(1, i).replace('/', '.');
    }
    public TypeKey changeTypeName(String typeName) {
        return changeTypeName(create(typeName));
    }
    public TypeKey changeTypeName(TypeKey typeKey) {
        if (this.equals(typeKey)) {
            return this;
        }
        return typeKey.setArrayDimension(getArrayDimension());
    }
    public TypeKey renamePackage(String from, String to) {
        String packageName = getPackageName();
        if (packageName.equals(from)) {
            return setPackage(packageName, to);
        }
        int i = from.length();
        if (i == 1 || packageName.length() < i || !packageName.startsWith(from)) {
            return this;
        }
        return setPackage(packageName, packageName.replace(from, to));
    }
    public TypeKey renamePackage(PackageKey from, PackageKey to) {
        PackageKey packageKey = getPackage();
        PackageKey result = packageKey.replace(from, to);
        if (packageKey == result) {
            return this;
        }
        return result.type(getSimpleName()).setArrayDimension(getArrayDimension());
    }
    public TypeKey setPackage(String packageName) {
        return setPackage(getPackageName(), packageName);
    }
    private TypeKey setPackage(String myPackage, String packageName) {
        if (myPackage.equals(packageName)) {
            return this;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(packageName);
        int i = packageName.length() - 1;
        if (i > 0 && packageName.charAt(i) != '/') {
            builder.append('/');
        }
        builder.append(getSimpleName());
        String name = getTypeName();
        char postFix = name.charAt(name.length() - 1);
        if (postFix == ';' || postFix == '<') {
            builder.append(postFix);
        }
        TypeKey typeKey = getOrCreate(builder.toString());
        return typeKey.setArrayDimension(getArrayDimension());
    }
    public boolean isPackage(String packageName) {
        return isPackage(packageName, !"L".equals(packageName));
    }
    public boolean isPackage(String packageName, boolean checkSubPackage) {
        if (isPrimitive()) {
            return false;
        }
        String name = getPackageName();
        if (checkSubPackage) {
            return name.startsWith(packageName);
        }
        return name.equals(packageName);
    }
    public boolean isPackage(PackageKey packageKey, boolean checkSubPackage) {
        if (packageKey == null) {
            return false;
        }
        PackageKey key = getPackage();
        if (key == null) {
            return false;
        }
        if (checkSubPackage) {
            return key.contains(packageKey);
        }
        return key.equals(packageKey);
    }
    public boolean startsWith(String prefix) {
        return getTypeName().startsWith(prefix);
    }
    public boolean isOuterOf(TypeKey typeKey) {
        return isOuterOf(typeKey, false);
    }
    public boolean isOuterOf(TypeKey typeKey, boolean immediate) {
        if (typeKey == null) {
            return false;
        }
        String name1 = this.getTypeName();
        String name2 = typeKey.getTypeName();
        if (name1.length() >= name2.length()) {
            return false;
        }
        int diff = StringsUtil.diffStart(name1, name2);
        if (diff < 0 || name1.charAt(diff) != ';' || name2.charAt(diff) != '$') {
            return false;
        }
        int i = diff + 1;
        if (name1.indexOf('/', i) > 0 || name2.indexOf('/', i) > 0) {
            return false;
        }
        if (immediate) {
            int length = name2.length();
            while (i < length && name2.charAt(i) == '$') {
                i ++;
            }
            return i != length && name2.indexOf('$', i) < 0;
        }
        return true;
    }
    public TypeKey getTopEnclosing() {
        TypeKey result = this;
        while (true) {
            TypeKey enc = result.getEnclosingClass();
            if (enc == result) {
                break;
            }
            result = enc;
        }
        return result;
    }
    public int getInnerDepth() {
        int result = 0;
        TypeKey typeKey = this;
        while (true) {
            TypeKey enc = typeKey.getEnclosingClass();
            if (enc.equals(typeKey)) {
                return result;
            }
            typeKey = enc;
            result ++;
        }
    }
    public TypeKey getEnclosingClass() {
        String type = getTypeName();
        String parent = DexUtils.getParentClassName(type);
        if (type.equals(parent)) {
            return this;
        }
        return getOrCreate(parent);
    }
    public TypeKey createInnerClass(String simpleName) {
        String type = getTypeName();
        String child = DexUtils.createChildClass(type, simpleName);
        if (type.equals(child)) {
            return this;
        }
        return new TypeKey(child);
    }
    public Iterator<String> iteratePackageNames() {
        if (getTypeName().indexOf('/') < 0) {
            return EmptyIterator.of();
        }
        String packageName = getPackageName();

        return new Iterator<String>() {
            String name = packageName;
            @Override
            public boolean hasNext() {
                String name = this.name;
                int i = name.length();
                if (i == 0) {
                    return false;
                }
                return name.charAt(i - 1) == '/';
            }
            @Override
            public String next() {
                String result = this.name;
                String name = result;
                while (name.charAt(name.length() - 1) == '/') {
                    name = name.substring(0, name.length() - 1);
                }
                int i = name.lastIndexOf('/');
                if (i > 0) {
                    name = name.substring(0, i + 1);
                }
                this.name = name;
                return result;
            }
        };
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.append(getTypeName());
    }

    public boolean equalsPackage(TypeKey typeKey) {
        if (typeKey == this) {
            return true;
        }
        if (typeKey == null) {
            return false;
        }
        String name1 = StringsUtil.trimStart(getTypeName(), '[');
        String name2 = StringsUtil.trimStart(typeKey.getTypeName(), '[');
        if (name1.charAt(0) != 'L' || name2.charAt(0) != 'L') {
            return false;
        }
        if (name1.equals(name2)) {
            return true;
        }
        int start = StringsUtil.diffStart(name1, name2);
        if (start < 0) {
            return false;
        }
        return StringsUtil.indexOfFrom(name1, start, '/') < 0 &&
                StringsUtil.indexOfFrom(name2, start, '/') < 0;
    }
    @Override
    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }
        if (!(obj instanceof TypeKey)) {
            return StringsUtil.compareToString(this, obj);
        }
        TypeKey key = (TypeKey) obj;
        return CompareUtil.compare(getTypeName(), key.getTypeName());
    }
    public boolean equalsName(String typeName) {
        return getTypeName().equals(typeName);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TypeKey)) {
            return false;
        }
        TypeKey key = (TypeKey) obj;
        return getTypeName().equals(key.getTypeName());
    }
    @Override
    public int hashCode() {
        return getTypeName().hashCode();
    }
    @Override
    public String toString() {
        return getTypeName();
    }

    public static TypeKey parse(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        if (name.indexOf('>') > 0 ||
                name.indexOf('(') > 0 ||
                name.indexOf('@') > 0) {
            return null;
        }
        if (name.indexOf('/') > 0 ||
                name.indexOf(';') > 0 ||
                name.charAt(0) == '[') {
            return getOrCreate(name.replace('.', '/'));
        }
        return parseSourceName(name);
    }
    public static TypeKey fromClassPath(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        String ext = ".class";
        int length = path.length();
        int extLength = ext.length();
        if (length <= extLength || !path.endsWith(ext) || path.charAt(0) == '[') {
            return null;
        }
        return getOrCreate('L' + path.substring(1, length - extLength) + ';');
    }
    private static TypeKey parseSourceName(String name) {
        int length = name.length();
        int arrayDimension = 0;
        int i = name.indexOf('[');
        while (i > 0 && i < length && name.charAt(i) == '[') {
            arrayDimension ++;
            i ++;
            if (i == length || name.charAt(i) != ']') {
                return null;
            }
            i ++;
        }
        length = name.length() - (arrayDimension * 2);
        if (length == 0) {
            return null;
        }
        if (arrayDimension != 0) {
            name = name.substring(0, length);
        }
        TypeKey typeKey = primitiveSourceType(name);
        if (typeKey == null) {
            name = name.replace('.', '/');
            typeKey = getOrCreate('L' + name + ';');
        }
        return typeKey.setArrayDimension(arrayDimension);
    }
    public static TypeKey convert(Class<?> type) {
        String name = type.getName();
        if (type.isArray()) {
            return create(name.replace('.', '/'));
        }
        if (type.isPrimitive()) {
            return primitiveSourceType(name);
        }
        return getOrCreate('L' + name.replace('.', '/') + ';');
    }
    public static TypeKey convert(Type type) {
        if (type instanceof Class<?>) {
            return convert((Class<?>) type);
        }
        String typeName = type instanceof Class ? ((Class<?>) type).getName() : type.toString();
        String name = dropSourceSignatures(typeName);
        if (name.charAt(0) == '[') {
            return create(name.replace('.', '/'));
        }
        // e.g. java.lang.Class[]
        int i = name.indexOf('[');
        int array = 0;
        if (i > 0) {
            array = StringsUtil.countChar(name, '[');
            name = name.substring(0, i);
        }
        TypeKey typeKey = primitiveSourceType(name);
        if (typeKey == null) {
            typeKey = getOrCreate('L' + name.replace('.', '/') + ';');
        }
        if (array != 0 && typeKey != null) {
            typeKey = typeKey.setArrayDimension(array);
        }
        return typeKey;
    }
    private static String dropSourceSignatures(String sourceTypeName) {
        if (sourceTypeName.indexOf('<') < 0) {
            return sourceTypeName;
        }
        int length = sourceTypeName.length();
        StringBuilder builder = new StringBuilder(length);
        int depth = 0;
        for (int i = 0; i < length; i++) {
            char c = sourceTypeName.charAt(i);
            if (c == '<') {
                depth ++;
            } else if (c == '>') {
                depth --;
            } else if (depth == 0) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /**
     * Fully validates and creates
     * */
    public static TypeKey of(String typeName) {
        if (typeName == null) {
            return null;
        }
        int length = typeName.length();
        if (length == 0) {
            return null;
        }
        int i = 0;
        while (i < length && typeName.charAt(i) == '[') {
            i ++;
        }
        if (i == length) {
            return null;
        }
        char c = typeName.charAt(i);
        i ++;
        if (c != 'L') {
            if (i != length) {
                return null;
            }
            TypeKey typeKey = primitiveType(c);
            if (i == 1 || typeKey == null) {
                return typeKey;
            }
            return new TypeKey(typeName);
        }
        length = length - 1;
        if (i >= length || typeName.charAt(length) != ';') {
            return null;
        }
        // TODO: check here for invalid characters

        return getOrCreate(typeName);
    }

    /**
     * Quickly validates and creates TypeKey, use this for high certain type names
     * */
    public static TypeKey create(String typeName) {
        if (typeName == null) {
            return null;
        }
        int length = typeName.length();
        if (length == 0) {
            return null;
        }
        if (length != 1) {
            return getOrCreate(typeName);
        }
        return primitiveType(typeName.charAt(0));
    }
    public static TypeKey read(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        int position = reader.position();
        byte b = reader.get();
        if (b != 'L' && b != '[') {
            char c = reader.readASCII();
            TypeKey typeKey = primitiveType(c);
            if (typeKey == null) {
                reader.position(position);
                throw new SmaliParseException("Invalid type '" + c + "'.", reader);
            }
            return typeKey;
        }
        int arrayLength = 0;
        while (reader.get(position) == '[') {
            arrayLength ++;
            position ++;
        }
        b = reader.get(position);
        if (b != 'L') {
            char c = (char) b;
            TypeKey typeKey = primitiveType(c);
            if (typeKey == null) {
                throw new SmaliParseException("Invalid type name '" + c + "'", reader);
            }
            reader.skip(arrayLength + 1);
            if (arrayLength != 0) {
                typeKey = typeKey.setArrayDimension(arrayLength);
            }
            return typeKey;
        }
        int i = reader.indexOfBeforeLineEnd(';');
        if (i < 0) {
            throw new SmaliParseException("Invalid type name, missing ';'", reader);
        }
        int length = (i - reader.position()) + 1;
        if ((length - arrayLength) < 3) {
            throw new SmaliParseException("Invalid type name", reader);
        }
        // TODO: support white space name for dex V040+
        return getOrCreate(reader.readString(length));
    }

    static TypeKey parseBinaryType(String text, int start, int end) {
        int arrayDimension = 0;
        boolean begin = false;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '[') {
                if (begin) {
                    return null;
                }
                arrayDimension ++;
            } else if (c == ';') {
                if (begin) {
                    return create(text.substring(start, i + 1));
                } else {
                    return null;
                }
            } else if (!begin) {
                if (c != 'L') {
                    TypeKey typeKey = primitiveType(c);
                    if (typeKey != null) {
                        typeKey = typeKey.setArrayDimension(arrayDimension);
                    }
                    return typeKey;
                }
                begin = true;
            }
        }
        return null;
    }
    private static TypeKey getOrCreate(String name) {
        int length = name.length();
        //Ljava/lang/Exception;
        if (length > 12  && length < 22 && name.charAt(1) == 'j') {
            if (name.equals("Ljava/lang/Object;")) {
                return OBJECT;
            }
            if (name.equals("Ljava/lang/String;")) {
                return STRING;
            }
            if (name.equals("Ljava/lang/Exception;")) {
                return EXCEPTION;
            }
        }
        return new TypeKey(name);
    }

    public static boolean isPrimitive(char ch) {
        return primitiveType(ch) != null;
    }
    public static TypeKey primitiveType(char ch) {
        switch (ch) {
            case 'B':
                return TYPE_B;
            case 'C':
                return TYPE_C;
            case 'D':
                return TYPE_D;
            case 'F':
                return TYPE_F;
            case 'I':
                return TYPE_I;
            case 'J':
                return TYPE_J;
            case 'S':
                return TYPE_S;
            case 'V':
                return TYPE_V;
            case 'Z':
                return TYPE_Z;
            default:
                return null;
        }
    }
    private static TypeKey primitiveSourceType(String sourceName) {
        int length = sourceName.length();
        // int = 3, boolean = 7
        if (length < 3 || length > 7) {
            return null;
        }
        if (sourceName.equals(TYPE_B.getSourceName())) {
            return TYPE_B;
        }
        if (sourceName.equals(TYPE_D.getSourceName())) {
            return TYPE_D;
        }
        if (sourceName.equals(TYPE_F.getSourceName())) {
            return TYPE_F;
        }
        if (sourceName.equals(TYPE_I.getSourceName())) {
            return TYPE_I;
        }
        if (sourceName.equals(TYPE_J.getSourceName())) {
            return TYPE_J;
        }
        if (sourceName.equals(TYPE_S.getSourceName())) {
            return TYPE_S;
        }
        if (sourceName.equals(TYPE_V.getSourceName())) {
            return TYPE_V;
        }
        if (sourceName.equals(TYPE_Z.getSourceName())) {
            return TYPE_Z;
        }
        return null;
    }


    static class PrimitiveTypeKey extends TypeKey {
        private final String sourceName;

        public PrimitiveTypeKey(String type, String sourceName) {
            super(type);
            this.sourceName = sourceName;
        }

        @Override
        public String getBinaryName() {
            return getTypeName();
        }
        @Override
        public boolean isClassDefinition() {
            return false;
        }
        @Override
        public boolean uses(Key key) {
            return equals(key);
        }
        @Override
        public String getSourceName() {
            return sourceName;
        }
        @Override
        public TypeKey getDeclaring() {
            return this;
        }
        @Override
        public boolean isPrimitive() {
            return true;
        }
        @Override
        public boolean isPrimitiveArray() {
            return false;
        }
        @Override
        public boolean isPrimitiveComponent() {
            return true;
        }
        @Override
        public boolean isWide() {
            return false;
        }
        @Override
        public boolean isTypeObject() {
            return false;
        }
        @Override
        public boolean isTypeArray() {
            return false;
        }
        @Override
        public boolean isOuterOf(TypeKey typeKey, boolean immediate) {
            return false;
        }
        @Override
        public boolean isOuterOf(TypeKey typeKey) {
            return false;
        }

        @Override
        public boolean isInnerName() {
            return false;
        }

        @Override
        public boolean isPackage(PackageKey packageKey, boolean checkSubPackage) {
            return false;
        }
        @Override
        public PackageKey getPackage() {
            return null;
        }
        @Override
        public TypeKey replaceKey(Key search, Key replace) {
            if (equals(search)) {
                return (TypeKey) replace;
            }
            return this;
        }

        @Override
        public Iterator<TypeKey> types() {
            return SingleIterator.of(this);
        }
        @Override
        public Iterator<Key> contents() {
            return SingleIterator.of(this);
        }
    }

    public static final TypeKey TYPE_B = new PrimitiveTypeKey("B", "byte");
    public static final TypeKey TYPE_C = new PrimitiveTypeKey("C", "char");
    public static final TypeKey TYPE_D = new PrimitiveTypeKey("D", "double") {
        @Override
        public boolean isWide() {
            return true;
        }
    };
    public static final TypeKey TYPE_F = new PrimitiveTypeKey("F", "float");
    public static final TypeKey TYPE_I = new PrimitiveTypeKey("I", "int");
    public static final TypeKey TYPE_J = new PrimitiveTypeKey("J", "long") {
        @Override
        public boolean isWide() {
            return true;
        }
    };
    public static final TypeKey TYPE_S = new PrimitiveTypeKey("S", "short");
    public static final TypeKey TYPE_V = new PrimitiveTypeKey("V", "void");
    public static final TypeKey TYPE_Z = new PrimitiveTypeKey("Z", "boolean");

    public static final TypeKey CLASS = new TypeKey("Ljava/lang/Class;");
    public static final TypeKey OBJECT = new TypeKey("Ljava/lang/Object;");
    public static final TypeKey STRING = new TypeKey("Ljava/lang/String;");
    public static final TypeKey EXCEPTION = new TypeKey("Ljava/lang/Exception;");

    public static final TypeKey DALVIK_AnnotationDefault = new TypeKey("Ldalvik/annotation/AnnotationDefault;");
    public static final TypeKey DALVIK_EnclosingClass = new TypeKey("Ldalvik/annotation/EnclosingClass;");
    public static final TypeKey DALVIK_EnclosingMethod = new TypeKey("Ldalvik/annotation/EnclosingMethod;");
    public static final TypeKey DALVIK_InnerClass = new TypeKey("Ldalvik/annotation/InnerClass;");
    public static final TypeKey DALVIK_MemberClass = new TypeKey("Ldalvik/annotation/MemberClasses;");
    public static final TypeKey DALVIK_MethodParameters = new TypeKey("Ldalvik/annotation/MethodParameters;");
    public static final TypeKey DALVIK_Signature = new TypeKey("Ldalvik/annotation/Signature;");
    public static final TypeKey DALVIK_Throws = new TypeKey("Ldalvik/annotation/Throws;");
}
