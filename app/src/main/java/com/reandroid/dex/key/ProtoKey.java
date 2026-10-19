package com.reandroid.dex.key;

import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.io.IOException;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class ProtoKey implements ProtoDescriptorKey {

    private final TypeListKey parameters;
    private final TypeKey returnType;

    private ProtoKey(TypeListKey parameters, TypeKey returnType) {
        super();
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public int getRegister(int index) {
        int result = 0;
        int size = getParametersCount();
        if(index < size){
            size = index;
        }
        for(int i = 0; i < size; i++){
            TypeKey typeKey = getParameter(i);
            result ++;
            if(typeKey.isWide()){
                result ++;
            }
        }
        return result;
    }
    public int getParameterIndex(int register) {
        int size = getParametersCount();
        int registerCount = 0;
        for(int i = 0; i < size; i++) {
            if(registerCount == register){
                return i;
            }
            TypeKey typeKey = getParameter(i);
            registerCount ++;
            if(typeKey.isWide()) {
                registerCount ++;
            }
        }
        return -1;
    }
    public ProtoKey removeParameter(int index) {
        return changeParameters(getParameters().remove(index));
    }
    public ProtoKey changeParameters(TypeListKey typeListKey) {
        if (TypeListKey.equalsIgnoreEmpty(getParameters(), typeListKey)) {
            return this;
        }
        return create(typeListKey, getReturnType());
    }
    public ProtoKey changeParameter(int index, TypeKey typeKey) {
        return changeParameters(getParameters().set(index, typeKey));
    }
    public ProtoKey changeReturnType(TypeKey typeKey) {
        if (getReturnType().equals(typeKey)) {
            return this;
        }
        return create(getParameters(), typeKey);
    }

    @Override
    public TypeKey getReturnType() {
        return returnType;
    }
    @Override
    public TypeKey getParameter(int i){
        return getParameters().get(i);
    }
    @Override
    public TypeListKey getParameters() {
        return parameters;
    }
    public Iterator<TypeKey> iterator(){
        return getParameters().iterator();
    }
    public String getShorty() {
        TypeListKey parameters = getParameters();
        int length = 1 + parameters.size();
        char[] results = new char[length];
        results[0] = getReturnType().shorty();
        for (int i = 1; i < length; i++) {
            results[i] = parameters.get(i - 1).shorty();
        }
        return new String(results);
    }
    public String getDetailedShorty() {
        return getDetailedShorty(null);
    }
    public String getDetailedShorty(Predicate<TypeKey> platformPredicate) {
        StringBuilder builder = new StringBuilder();
        getReturnType().appendDetailedShorty(builder, platformPredicate);
        TypeListKey parameters = getParameters();
        int size = parameters.size();
        for (int i = 0; i < size; i++) {
            parameters.get(i).appendDetailedShorty(builder, platformPredicate);
        }
        return builder.toString();
    }
    public int getParameterRegistersCount(){
        int result = 0;
        Iterator<TypeKey> iterator = iterator();
        while (iterator.hasNext()){
            TypeKey key = iterator.next();
            result ++;
            if(key.isWide()){
                result ++;
            }
        }
        return result;
    }
    @Override
    public Iterator<Key> contents() {
        return CombiningIterator.singleThree(
                ProtoKey.this,
                SingleIterator.of(StringKey.create(getShorty())),
                iterator(),
                SingleIterator.of(getReturnType()));
    }
    @Override
    public ProtoKey replaceKey(Key search, Key replace) {
        ProtoKey result = this;
        if (search.equals(result)) {
            return (ProtoKey) replace;
        }
        result = result.changeReturnType(getReturnType().replaceKey(search, replace));
        result = result.changeParameters(getParameters().replaceKey(search, replace));
        return result;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.append('(');
        writer.appendAll(iterator(), false);
        writer.append(')');
        getReturnType().append(writer);
    }

    @Override
    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }
        if (!(obj instanceof ProtoKey)) {
            return StringsUtil.compareToString(this, obj);
        }
        ProtoKey key = (ProtoKey) obj;
        int i = CompareUtil.compare(getReturnType(), key.getReturnType());
        if(i != 0) {
            return i;
        }
        return CompareUtil.compare(getParameters(), key.getParameters());
    }

    public boolean equalsReturnType(TypeKey returnType) {
        return getReturnType().equals(returnType);
    }
    public boolean equalsReturnType(ProtoKey protoKey) {
        if (protoKey == null) {
            return false;
        }
        if (protoKey == this) {
            return true;
        }
        return getReturnType().equals(protoKey.getReturnType());
    }
    public boolean equalsParameters(TypeListKey parameters) {
        return TypeListKey.equalsIgnoreEmpty(getParameters(), parameters);
    }
    public boolean equalsParameters(ProtoKey protoKey) {
        if (protoKey == null) {
            return false;
        }
        if (protoKey == this) {
            return true;
        }
        return TypeListKey.equalsIgnoreEmpty(getParameters(), protoKey.getParameters());
    }
    public boolean equals(TypeKey returnType, TypeListKey parameters) {
        if (!getReturnType().equals(returnType)) {
            return false;
        }
        TypeListKey key = getParameters();
        if (parameters == null) {
            return key.isEmpty();
        }
        return key.equals(parameters);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProtoKey)) {
            return false;
        }
        ProtoKey protoKey = (ProtoKey) obj;
        return ObjectsUtil.equals(getReturnType(), protoKey.getReturnType()) &&
                ObjectsUtil.equals(getParameters(), protoKey.getParameters());
    }

    @Override
    public int hashCode() {
        return ObjectsUtil.hash(getParameters(), getReturnType());
    }
    @Override
    public String toString() {
        return getParameters().toString() + getReturnType();
    }

    public static ProtoKey create(TypeKey returnType, TypeKey ... parameters) {
        return create(TypeListKey.create(parameters), returnType);
    }
    public static ProtoKey create(TypeListKey typeListKey, TypeKey returnType){
        if (returnType == null) {
            return null;
        }
        if (typeListKey == null) {
            typeListKey = TypeListKey.empty();
        }
        return new ProtoKey(typeListKey, returnType);
    }
    public static ProtoKey emptyParameters(TypeKey returnType) {
        return create(TypeListKey.empty(), returnType);
    }

    public static ProtoKey parse(String text) {
        if (text == null) {
            return null;
        }
        return parse(text, 0);
    }
    public static ProtoKey parse(String text, int start) {
        if (text.length() - start < 3 || text.charAt(start) != '(') {
            return null;
        }
        int i = text.indexOf(')', start);
        if (i < 0) {
            return null;
        }
        TypeListKey parameters = TypeListKey.parseParameters(text, start + 1, i);
        if (parameters == null) {
            return null;
        }
        TypeKey returnType = TypeKey.parseBinaryType(text, i + 1, text.length());
        if (returnType == null) {
            return null;
        }
        return create(parameters, returnType);
    }
    public static ProtoKey read(SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        TypeListKey parameters = TypeListKey.readParameters(reader);
        reader.skipWhitespacesOrComment();
        TypeKey returnType = TypeKey.read(reader);
        return create(parameters, returnType);
    }
}
