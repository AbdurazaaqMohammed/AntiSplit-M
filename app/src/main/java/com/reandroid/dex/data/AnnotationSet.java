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
package com.reandroid.dex.data;

import com.reandroid.dex.base.DexPositionAlign;
import com.reandroid.dex.base.PositionAlignedItem;
import com.reandroid.dex.base.UsageMarker;
import com.reandroid.dex.common.FullRefresh;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.AnnotationItemKey;
import com.reandroid.dex.key.AnnotationSetKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyReference;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.program.AnnotatedProgram;
import com.reandroid.dex.sections.Section;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliAnnotationItem;
import com.reandroid.dex.smali.model.SmaliAnnotationSet;
import com.reandroid.dex.value.DexValueBlock;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;

import java.io.IOException;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class AnnotationSet extends AnnotationsList<AnnotationItem>
        implements KeyReference, SmaliFormat, PositionAlignedItem,
        FullRefresh, AnnotatedProgram {

    public AnnotationSet(){
        super(SectionType.ANNOTATION_ITEM, UsageMarker.USAGE_ANNOTATION, new DexPositionAlign());
    }

    public boolean remove(TypeKey typeKey) {
        return removeIf(item -> ObjectsUtil.equals(typeKey, item.getType()));
    }
    public boolean remove(AnnotationItemKey itemKey) {
        return removeIf(item -> ObjectsUtil.equals(itemKey, item.getKey()));
    }
    public boolean removeAnnotationIf(Predicate<? super AnnotationItemKey> predicate) {
        return removeIf(item -> predicate.test(item.getKey()));
    }
    @Override
    public boolean isBlank() {
        return isEmpty();
    }

    @Override
    public AnnotationSetKey getKey() {
        AnnotationSetKey lastKey = getLastKey();
        if (lastKey != null && equalsKey(lastKey)) {
            return lastKey;
        }
        int size = size();
        AnnotationItemKey[] elements;
        if (size == 0) {
            elements = null;
        } else {
            elements = new AnnotationItemKey[size];
            getItemKeys(elements);
        }
        return checkKey(AnnotationSetKey.of(elements));
    }
    @Override
    public void setKey(Key key) {
        super.setKey(key);
    }
    @Override
    public SectionType<AnnotationSet> getSectionType() {
        return SectionType.ANNOTATION_SET;
    }

    public AnnotationItemKey getItemKey(int i) {
        return (AnnotationItemKey) super.getItemKey(i);
    }
    public DexValueBlock<?> getValue(TypeKey typeKey, String name){
        AnnotationElement element = getElement(typeKey, name);
        if(element != null){
            return element.getValueBlock();
        }
        return null;
    }
    public AnnotationElement getElement(TypeKey typeKey, String name){
        AnnotationItem annotationItem = get(typeKey);
        if(annotationItem != null){
            return annotationItem.get(name);
        }
        return null;
    }
    public AnnotationItem get(TypeKey typeKey) {
        for(AnnotationItem item : this){
            if(typeKey.equals(item.getType())){
                return item;
            }
        }
        return null;
    }
    public Iterator<AnnotationItem> getAll(TypeKey typeKey) {
        return FilterIterator.of(iterator(), item -> typeKey.equals(item.getType()));
    }
    public boolean contains(TypeKey typeKey) {
        for(AnnotationItem item : this){
            if(typeKey.equals(item.getType())){
                return true;
            }
        }
        return false;
    }
    public AnnotationItem getOrCreate(TypeKey typeKey){
        AnnotationItem item = get(typeKey);
        if(item != null){
            return item;
        }
        return addNewItem(typeKey);
    }
    public AnnotationItem getOrCreate(TypeKey type, String name){
        AnnotationItem item = get(type, name);
        if(item != null){
            return item;
        }
        return addNew(type, name);
    }
    private AnnotationItem addNewItem(TypeKey typeKey){
        AnnotationItem item = addNewItem();
        item.setType(typeKey);
        return item;
    }
    private AnnotationItem addNew(TypeKey type, String name){
        AnnotationItem item = getOrCreate(type);
        item.getOrCreate(name);
        return item;
    }
    public AnnotationItem get(TypeKey type, String name){
        for (AnnotationItem item : this) {
            if (type.equals(item.getType())
                    && item.contains(name)) {
                return item;
            }
        }
        return null;
    }
    public AnnotationItem getOrCreate(AnnotationItemKey annotationItemKey) {
        AnnotationItem item = get(annotationItemKey);
        if (item == null) {
            item = addNewItem(annotationItemKey);
        }
        return item;
    }
    public AnnotationItem get(AnnotationItemKey annotationItemKey){
        for (AnnotationItem item : this) {
            if (annotationItemKey.equals(item.getKey())) {
                return item;
            }
        }
        return null;
    }

    public void replaceKeys(Key search, Key replace){
        for(AnnotationItem annotationItem : this){
            annotationItem.replaceKeys(search, replace);
        }
    }
    @Override
    public void refreshFull() {
        sort();
    }
    public boolean sort() {
        return super.sort(CompareUtil.getComparableComparator());
    }



    /////////////////// AnnotatedProgram /////////////////////
    @Override
    public AnnotationSetKey getAnnotation() {
        return getKey();
    }
    @Override
    public void setAnnotation(AnnotationSetKey annotationSet) {
        setKey(annotationSet);
    }
    @Override
    public void clearAnnotations() {
        clear();
    }
    @Override
    public boolean hasAnnotations() {
        return !isEmpty();
    }
    @Override
    public boolean hasAnnotation(TypeKey typeKey) {
        return contains(typeKey);
    }
    @Override
    public AnnotationItemKey getAnnotation(TypeKey typeKey) {
        AnnotationItem item = get(typeKey);
        if (item != null) {
            return item.getKey();
        }
        return null;
    }
    @Override
    public Key getAnnotationValue(TypeKey typeKey, String name) {
        DexValueBlock<?> value = getValue(typeKey, name);
        if (value != null) {
            return value.getKey();
        }
        return null;
    }

    @Override
    public boolean removeAnnotation(TypeKey typeKey) {
        return remove(typeKey);
    }
    @Override
    public void addAnnotation(AnnotationItemKey annotation) {
        remove(annotation.getType());
        addNewItem().setKey(annotation);
    }
    //----------------- AnnotatedProgram -------------------//

    @Override
    public Iterator<IdItem> usedIds(){
        return new IterableIterator<AnnotationItem, IdItem>(iterator()) {
            @Override
            public Iterator<IdItem> iterator(AnnotationItem element) {
                return element.usedIds();
            }
        };
    }
    public void merge(AnnotationSet annotationSet){
        if(annotationSet == this){
            return;
        }
        for(AnnotationItem coming : annotationSet){
            addNewItem(coming.getKey());
        }
    }
    public void fromSmali(SmaliAnnotationSet smaliAnnotationSet){
        Iterator<SmaliAnnotationItem> iterator = smaliAnnotationSet.iterator();
        while (iterator.hasNext()) {
            SmaliAnnotationItem smaliAnnotationItem = iterator.next();
            getOrCreate(smaliAnnotationItem.getKey());
        }
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        writer.appendAllWithDoubleNewLine(iterator());
    }

    @Override
    public String toString() {
        if (getOffsetReference() == null) {
            return super.toString();
        }
        int size = size();
        if (size == 0) {
            return "@EMPTY";
        }
        return StringsUtil.join(iterator(), ", ");
    }
    public static class EmptyAnnotationSet extends AnnotationSet{

        public EmptyAnnotationSet(){
            super();
            addUsageType(UsageMarker.USAGE_ANNOTATION);
        }

        @Override
        public boolean isBlank() {
            return isRemoved();
        }

        @Override
        protected void onRefreshed() {
            super.onRefreshed();
            Section<AnnotationSet> section = getSection(getSectionType());
            if(section.getCount() == 1){
                addUsageType(UsageMarker.USAGE_ANNOTATION);
            }
        }

        @Override
        public void clearUsageType() {
            Section<AnnotationSet> section = getSection(getSectionType());
            if(section.getCount() != 1){
                super.clearUsageType();
            }
        }
    }
}
