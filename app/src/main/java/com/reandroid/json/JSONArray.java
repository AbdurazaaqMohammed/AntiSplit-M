/*
 * Copyright (c) 2002 JSON.org (now "Public Domain")
 * This is NOT property of REAndroid
 * This package is renamed from org.json.* to avoid class conflict when used on android platforms
*/
package com.reandroid.json;

import com.reandroid.common.FileChannelInputStream;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONArray extends JSONItem implements Iterable<Object> {

    private final ArrayList<Object> myArrayList;

    public JSONArray() {
        this.myArrayList = new ArrayList<>();
    }

    public JSONArray(JSONTokener x) throws JSONException {
        this();
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSONArray text must start with '['");
        }
        
        char nextChar = x.nextClean();
        if (nextChar == 0) {
            // array is unclosed. No ']' found, instead EOF
            throw x.syntaxError("Expected a ',' or ']'");
        }
        if (nextChar != ']') {
            x.back();
            for (;;) {
                if (x.nextClean() == ',') {
                    x.back();
                    this.myArrayList.add(JSONObject.NULL);
                } else {
                    x.back();
                    this.myArrayList.add(x.nextValue());
                }
                switch (x.nextClean()) {
                case 0:
                    // array is unclosed. No ']' found, instead EOF
                    throw x.syntaxError("Expected a ',' or ']'");
                case ',':
                    nextChar = x.nextClean();
                    if (nextChar == 0) {
                        // array is unclosed. No ']' found, instead EOF
                        throw x.syntaxError("Expected a ',' or ']'");
                    }
                    if (nextChar == ']') {
                        return;
                    }
                    x.back();
                    break;
                case ']':
                    return;
                default:
                    throw x.syntaxError("Expected a ',' or ']'");
                }
            }
        }
    }

    public JSONArray(String source) throws JSONException {
        this(new JSONTokener(source));
    }

    public JSONArray(Collection<?> collection) {
        if (collection == null) {
            this.myArrayList = new ArrayList<>();
        } else {
            this.myArrayList = new ArrayList<>(collection.size());
            this.addAll(collection, true);
            this.myArrayList.trimToSize();
        }
    }

    public JSONArray(Iterable<?> iter) {
        this();
        if (iter == null) {
            return;
        }
        this.addAll(iter, true);
    }

    public JSONArray(JSONArray array) {
        if (array == null) {
            this.myArrayList = new ArrayList<>();
        } else {
            // shallow copy directly the internal array lists as any wrapping
            // should have been done already in the original JSONArray
            this.myArrayList = new ArrayList<>(array.myArrayList);
        }
    }

    JSONArray(Object array) throws JSONException {
        this();
        if (!array.getClass().isArray()) {
            throw new JSONException(
                    "JSONArray initial value should be a string or collection or array.");
        }
        this.addAll(array, true);
    }

    public JSONArray(int initialCapacity) throws JSONException {
    	if (initialCapacity < 0) {
            throw new JSONException(
                    "JSONArray initial capacity cannot be negative.");
    	}
    	this.myArrayList = new ArrayList<>(initialCapacity);
    }

    public JSONArray(File file) throws IOException {
        this(new FileChannelInputStream(file));
    }
    public JSONArray(Reader reader){
        this(new JSONTokener(reader));
    }
    public JSONArray(InputStream inputStream) throws JSONException {
        this(new JSONTokener(inputStream));
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }
    public void trimToSize(){
        myArrayList.trimToSize();
    }
    public ArrayList<Object> getArrayList(){
        return myArrayList;
    }

    @Override
    public Iterator<Object> iterator() {
        return this.myArrayList.iterator();
    }

    public Object get(int index) throws JSONException {
        Object object = this.opt(index);
        if (object == null) {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        return object;
    }

    public boolean getBoolean(int index) throws JSONException {
        Object object = this.get(index);
        if (object.equals(Boolean.FALSE)
                || (object instanceof String && ((String) object)
                        .equalsIgnoreCase("false"))) {
            return false;
        } else if (object.equals(Boolean.TRUE)
                || (object instanceof String && ((String) object)
                        .equalsIgnoreCase("true"))) {
            return true;
        }
        throw wrongValueFormatException(index, "boolean", null);
    }

    public double getDouble(int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).doubleValue();
        }
        try {
            return Double.parseDouble(object.toString());
        } catch (Exception e) {
            throw wrongValueFormatException(index, "double", e);
        }
    }

    public float getFloat(int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Float)object).floatValue();
        }
        try {
            return Float.parseFloat(object.toString());
        } catch (Exception e) {
            throw wrongValueFormatException(index, "float", e);
        }
    }

    public Number getNumber(int index) throws JSONException {
        Object object = this.get(index);
        try {
            if (object instanceof Number) {
                return (Number)object;
            }
            return JSONObject.stringToNumber(object.toString());
        } catch (Exception e) {
            throw wrongValueFormatException(index, "number", e);
        }
    }

    public <E extends Enum<E>> E getEnum(Class<E> clazz, int index) throws JSONException {
        E val = optEnum(clazz, index);
        if(val==null) {
            // JSONException should really take a throwable argument.
            // If it did, I would re-implement this with the Enum.valueOf
            // method and place any thrown exception in the JSONException
            throw wrongValueFormatException(index, "enum of type "
                    + JSONObject.quote(clazz.getSimpleName()), null);
        }
        return val;
    }

    public BigDecimal getBigDecimal (int index) throws JSONException {
        Object object = this.get(index);
        BigDecimal val = JSONObject.objectToBigDecimal(object, null);
        if(val == null) {
            throw wrongValueFormatException(index, "BigDecimal", object, null);
        }
        return val;
    }

    public BigInteger getBigInteger (int index) throws JSONException {
        Object object = this.get(index);
        BigInteger val = JSONObject.objectToBigInteger(object, null);
        if(val == null) {
            throw wrongValueFormatException(index, "BigInteger", object, null);
        }
        return val;
    }

    public int getInt(int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).intValue();
        }
        try {
            return Integer.parseInt(object.toString());
        } catch (Exception e) {
            throw wrongValueFormatException(index, "int", e);
        }
    }

    public JSONArray getJSONArray(int index) throws JSONException {
        Object object = this.get(index);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        throw wrongValueFormatException(index, "JSONArray", null);
    }

    public JSONObject getJSONObject(int index) throws JSONException {
        Object object = this.get(index);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        throw wrongValueFormatException(index, "JSONObject", null);
    }

    public long getLong(int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).longValue();
        }
        try {
            return Long.parseLong(object.toString());
        } catch (Exception e) {
            throw wrongValueFormatException(index, "long", e);
        }
    }

    public String getString(int index) throws JSONException {
        Object object = this.get(index);
        if (object instanceof String) {
            return (String) object;
        }
        throw wrongValueFormatException(index, "String", null);
    }

    public boolean isNull(int index) {
        return JSONObject.NULL.equals(this.opt(index));
    }

    public String join(String separator) throws JSONException {
        int len = this.length();
        if (len == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(
                   JSONObject.valueToString(this.myArrayList.get(0)));

        for (int i = 1; i < len; i++) {
            sb.append(separator)
              .append(JSONObject.valueToString(this.myArrayList.get(i)));
        }
        return sb.toString();
    }

    public int length() {
        return this.myArrayList.size();
    }

    public Object opt(int index) {
        return (index < 0 || index >= this.length()) ? null : this.myArrayList
                .get(index);
    }

    public boolean optBoolean(int index) {
        return this.optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean defaultValue) {
        try {
            return this.getBoolean(index);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double optDouble(int index) {
        return this.optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        final double doubleValue = val.doubleValue();
        // if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
        // return defaultValue;
        // }
        return doubleValue;
    }

    public float optFloat(int index) {
        return this.optFloat(index, Float.NaN);
    }

    public float optFloat(int index, float defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        final float floatValue = val.floatValue();
        // if (Float.isNaN(floatValue) || Float.isInfinite(floatValue)) {
        // return floatValue;
        // }
        return floatValue;
    }

    public int optInt(int index) {
        return this.optInt(index, 0);
    }

    public int optInt(int index, int defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.intValue();
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, int index) {
        return this.optEnum(clazz, index, null);
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, int index, E defaultValue) {
        try {
            Object val = this.opt(index);
            if (JSONObject.NULL.equals(val)) {
                return defaultValue;
            }
            if (clazz.isAssignableFrom(val.getClass())) {
                // we just checked it!
                @SuppressWarnings("unchecked")
                E myE = (E) val;
                return myE;
            }
            return Enum.valueOf(clazz, val.toString());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    public BigInteger optBigInteger(int index, BigInteger defaultValue) {
        Object val = this.opt(index);
        return JSONObject.objectToBigInteger(val, defaultValue);
    }

    public BigDecimal optBigDecimal(int index, BigDecimal defaultValue) {
        Object val = this.opt(index);
        return JSONObject.objectToBigDecimal(val, defaultValue);
    }

    public JSONArray optJSONArray(int index) {
        Object o = this.opt(index);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    public JSONObject optJSONObject(int index) {
        Object o = this.opt(index);
        return o instanceof JSONObject ? (JSONObject) o : null;
    }

    public long optLong(int index) {
        return this.optLong(index, 0);
    }

    public long optLong(int index, long defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.longValue();
    }

    public Number optNumber(int index) {
        return this.optNumber(index, null);
    }

    public Number optNumber(int index, Number defaultValue) {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val)) {
            return defaultValue;
        }
        if (val instanceof Number){
            return (Number) val;
        }
        
        if (val instanceof String) {
            try {
                return JSONObject.stringToNumber((String) val);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String optString(int index) {
        return this.optString(index, "");
    }

    public String optString(int index, String defaultValue) {
        Object object = this.opt(index);
        return JSONObject.NULL.equals(object) ? defaultValue : object
                .toString();
    }

    public JSONArray put(boolean value) {
        return this.put(value ? Boolean.TRUE : Boolean.FALSE);
    }

    public JSONArray put(Collection<?> value) {
        return this.put(new JSONArray(value));
    }

    public JSONArray put(double value) throws JSONException {
        return this.put(Double.valueOf(value));
    }
    

    public JSONArray put(float value) throws JSONException {
        return this.put(Float.valueOf(value));
    }

    public JSONArray put(int value) {
        return this.put(Integer.valueOf(value));
    }

    public JSONArray put(long value) {
        return this.put(Long.valueOf(value));
    }

    public JSONArray put(Map<?, ?> value) {
        return this.put(new JSONObject(value));
    }

    public JSONArray put(Object value) {
        JSONObject.testValidity(value);
        this.myArrayList.add(value);
        return this;
    }

    public JSONArray put(int index, boolean value) throws JSONException {
        return this.put(index, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public JSONArray put(int index, Collection<?> value) throws JSONException {
        return this.put(index, new JSONArray(value));
    }

    public JSONArray put(int index, double value) throws JSONException {
        return this.put(index, Double.valueOf(value));
    }

    public JSONArray put(int index, float value) throws JSONException {
        return this.put(index, Float.valueOf(value));
    }

    public JSONArray put(int index, int value) throws JSONException {
        return this.put(index, Integer.valueOf(value));
    }

    public JSONArray put(int index, long value) throws JSONException {
        return this.put(index, Long.valueOf(value));
    }

    public JSONArray put(int index, Map<?, ?> value) throws JSONException {
        this.put(index, new JSONObject(value));
        return this;
    }

    public JSONArray put(int index, Object value) throws JSONException {
        if (index < 0) {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        if (index < this.length()) {
            JSONObject.testValidity(value);
            this.myArrayList.set(index, value);
            return this;
        }
        if(index == this.length()){
            // simple append
            return this.put(value);
        }
        // if we are inserting past the length, we want to grow the array all at once
        // instead of incrementally.
        this.myArrayList.ensureCapacity(index + 1);
        while (index != this.length()) {
            // we don't need to test validity of NULL objects
            this.myArrayList.add(JSONObject.NULL);
        }
        return this.put(value);
    }

    public JSONArray putAll(Collection<?> collection) {
        this.addAll(collection, false);
        return this;
    }
    

    public JSONArray putAll(Iterable<?> iter) {
        this.addAll(iter, false);
        return this;
    }

    public JSONArray putAll(JSONArray array) {
        // directly copy the elements from the source array to this one
        // as all wrapping should have been done already in the source.
        this.myArrayList.addAll(array.myArrayList);
        return this;
    }

    public JSONArray putAll(Object array) throws JSONException {
        this.addAll(array, false);
        return this;
    }
    

    public Object query(String jsonPointer) {
        return query(new JSONPointer(jsonPointer));
    }
    

    public Object query(JSONPointer jsonPointer) {
        return jsonPointer.queryFrom(this);
    }
    

    public Object optQuery(String jsonPointer) {
    	return optQuery(new JSONPointer(jsonPointer));
    }
    

    public Object optQuery(JSONPointer jsonPointer) {
        try {
            return jsonPointer.queryFrom(this);
        } catch (JSONPointerException e) {
            return null;
        }
    }

    public Object remove(int index) {
        return index >= 0 && index < this.length()
            ? this.myArrayList.remove(index)
            : null;
    }

    public boolean similar(Object other) {
        if (!(other instanceof JSONArray)) {
            return false;
        }
        int len = this.length();
        if (len != ((JSONArray)other).length()) {
            return false;
        }
        for (int i = 0; i < len; i += 1) {
            Object valueThis = this.myArrayList.get(i);
            Object valueOther = ((JSONArray)other).myArrayList.get(i);
            if(valueThis == valueOther) {
            	continue;
            }
            if(valueThis == null) {
            	return false;
            }
            if (valueThis instanceof JSONObject) {
                if (!((JSONObject)valueThis).similar(valueOther)) {
                    return false;
                }
            } else if (valueThis instanceof JSONArray) {
                if (!((JSONArray)valueThis).similar(valueOther)) {
                    return false;
                }
            } else if (!valueThis.equals(valueOther)) {
                return false;
            }
        }
        return true;
    }

    public JSONObject toJSONObject(JSONArray names) throws JSONException {
        if (names == null || names.isEmpty() || this.isEmpty()) {
            return null;
        }
        JSONObject jo = new JSONObject(names.length());
        for (int i = 0; i < names.length(); i += 1) {
            jo.put(names.getString(i), this.opt(i));
        }
        return jo;
    }

    @Override
    public Writer write(Writer writer, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean needsComma = false;
            writer.write('[');
            final int newIndent = indent + indentFactor;
            ArrayList<?> arrayList = this.myArrayList;
            int length = arrayList.size();
            for (int i = 0; i < length; i += 1) {
                if (needsComma) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, newIndent);
                try {
                    writeValue(writer, arrayList.get(i),
                            indentFactor, newIndent);
                } catch (Exception e) {
                    throw new JSONException("Unable to write JSONArray value at index: " + i, e);
                }
                needsComma = true;
            }
            if(needsComma){
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write(']');
            return writer;
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }

    public List<Object> toList() {
        List<Object> results = new ArrayList<>(this.myArrayList.size());
        for (Object element : this.myArrayList) {
            if (element == null || JSONObject.NULL.equals(element)) {
                results.add(null);
            } else if (element instanceof JSONArray) {
                results.add(((JSONArray) element).toList());
            } else if (element instanceof JSONObject) {
                results.add(((JSONObject) element).toMap());
            } else {
                results.add(element);
            }
        }
        return results;
    }

    public boolean isEmpty() {
        return this.myArrayList.isEmpty();
    }

    public void sort(Comparator comparator) {
        java.util.Collections.sort(this.myArrayList, comparator);
    }

    private void addAll(Collection<?> collection, boolean wrap) {
        this.myArrayList.ensureCapacity(this.myArrayList.size() + collection.size());
        if (wrap) {
            for (Object o: collection){
                this.put(JSONObject.wrap(o));
            }
        } else {
            for (Object o: collection){
                this.put(o);
            }
        }
    }

    private void addAll(Iterable<?> iter, boolean wrap) {
        if (wrap) {
            for (Object o: iter){
                this.put(JSONObject.wrap(o));
            }
        } else {
            for (Object o: iter){
                this.put(o);
            }
        }
    }
    

    private void addAll(Object array, boolean wrap) throws JSONException {
        if (array.getClass().isArray()) {
            int length = Array.getLength(array);
            this.myArrayList.ensureCapacity(this.myArrayList.size() + length);
            if (wrap) {
                for (int i = 0; i < length; i += 1) {
                    this.put(JSONObject.wrap(Array.get(array, i)));
                }
            } else {
                for (int i = 0; i < length; i += 1) {
                    this.put(Array.get(array, i));
                }
            }
        } else if (array instanceof JSONArray) {
            // use the built in array list `addAll` as all object
            // wrapping should have been completed in the original
            // JSONArray
            this.myArrayList.addAll(((JSONArray)array).myArrayList);
        } else if (array instanceof Collection) {
            this.addAll((Collection<?>)array, wrap);
        } else if (array instanceof Iterable) {
            this.addAll((Iterable<?>)array, wrap);
        } else {
            throw new JSONException(
                    "JSONArray initial value should be a string or collection or array.");
        }
    }
    

    private static JSONException wrongValueFormatException(
            int idx,
            String valueType,
            Throwable cause) {
        return new JSONException(
                "JSONArray[" + idx + "] is not a " + valueType + "."
                , cause);
    }
    

    private static JSONException wrongValueFormatException(
            int idx,
            String valueType,
            Object value,
            Throwable cause) {
        return new JSONException(
                "JSONArray[" + idx + "] is not a " + valueType + " (" + value + ")."
                , cause);
    }

}
