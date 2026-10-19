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
package com.reandroid.dex.sections;

import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.arsc.item.NumberIntegerReference;
import com.reandroid.dex.base.IntegerPair;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.data.AnnotationGroup;
import com.reandroid.dex.data.AnnotationItem;
import com.reandroid.dex.data.AnnotationSet;
import com.reandroid.dex.data.AnnotationsDirectory;
import com.reandroid.dex.data.ClassData;
import com.reandroid.dex.data.CodeItem;
import com.reandroid.dex.data.DebugInfo;
import com.reandroid.dex.data.EncodedArray;
import com.reandroid.dex.data.HiddenApiRestrictions;
import com.reandroid.dex.data.StringData;
import com.reandroid.dex.data.TypeList;
import com.reandroid.dex.header.DexHeader;
import com.reandroid.dex.id.CallSiteId;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.FieldId;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.id.MethodHandleId;
import com.reandroid.dex.id.MethodId;
import com.reandroid.dex.id.ProtoId;
import com.reandroid.dex.id.StringId;
import com.reandroid.dex.id.TypeId;
import com.reandroid.dex.key.AnnotationGroupKey;
import com.reandroid.dex.key.AnnotationItemKey;
import com.reandroid.dex.key.AnnotationSetKey;
import com.reandroid.dex.key.CallSiteKey;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodHandleKey;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.ProtoKey;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.CollectionUtil;

import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Function;
import com.reandroid.utils.function.Predicate;


public abstract class SectionType<T extends SectionItem> implements Creator<T> {


    public static final SectionType<DexHeader> HEADER;

    public static final SectionType<StringId> STRING_ID;
    public static final SectionType<TypeId> TYPE_ID;
    public static final SectionType<ProtoId> PROTO_ID;
    public static final SectionType<FieldId> FIELD_ID;
    public static final SectionType<MethodId> METHOD_ID;
    public static final SectionType<ClassId> CLASS_ID;
    public static final SectionType<CallSiteId> CALL_SITE_ID;
    public static final SectionType<MethodHandleId> METHOD_HANDLE;

    public static final SectionType<TypeList> TYPE_LIST;
    public static final SectionType<AnnotationItem> ANNOTATION_ITEM;
    public static final SectionType<AnnotationSet> ANNOTATION_SET;
    public static final SectionType<AnnotationGroup> ANNOTATION_GROUP;
    public static final SectionType<AnnotationsDirectory> ANNOTATION_DIRECTORY;
    public static final SectionType<ClassData> CLASS_DATA;
    public static final SectionType<CodeItem> CODE;
    public static final SectionType<StringData> STRING_DATA;
    public static final SectionType<DebugInfo> DEBUG_INFO;
    public static final SectionType<EncodedArray> ENCODED_ARRAY;
    public static final SectionType<HiddenApiRestrictions> HIDDEN_API;

    public static final SectionType<MapList> MAP_LIST;


    private static final SectionType<?>[] READ_ORDER;
    private static final SectionType<?>[] DATA_REMOVE_ORDER;

    private static final SectionType<?>[] R8_ORDER;
    private static final SectionType<?>[] DEX_LIB2_ORDER;

    private static final SectionType<?>[] SORT_SECTIONS_ORDER;

    static {

        HEADER = new SectionType<DexHeader>("HEADER", 0x0000){
            @Override
            public DexHeader newInstance() {
                return new DexHeader();
            }

            @Override
            public boolean isSpecialSection() {
                return true;
            }
            @Override
            public Section<DexHeader> createSection(IntegerPair countAndOffset){
                return new SpecialSection<>(countAndOffset, this);
            }
            @Override
            public SpecialSection<DexHeader> createSpecialSection(IntegerReference offset){
                return new SpecialSection<>(offset, this);
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        MAP_LIST = new SectionType<MapList>("MAP_LIST", 0x1000) {
            @Override
            public MapList newInstance() {
                return new MapList(new NumberIntegerReference());
            }

            @Override
            public boolean isSpecialSection() {
                return true;
            }
            @Override
            public Section<MapList> createSection(IntegerPair countAndOffset){
                return new SpecialSection<>(countAndOffset, this);
            }
            @Override
            public SpecialSection<MapList> createSpecialSection(IntegerReference offset){
                return new SpecialSection<>(offset, this);
            }
            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        STRING_ID = new SectionType<StringId>("STRING_ID", 0x0001) {

            @Override
            public StringId newInstance() {
                return new StringId();
            }

            @Override
            public boolean isIdSection() {
                return true;
            }

            @Override
            public boolean isSharedSection() {
                return true;
            }

            @Override
            public int getReferenceType() {
                return 0;
            }
            @Override
            public StringIdSection createSection(IntegerPair countAndOffset) {
                return new StringIdSection(countAndOffset, this);
            }
            
        };

        STRING_DATA = new SectionType<StringData>("STRING_DATA", 0x2002) {
            @Override
            public StringData newInstance() {
                return new StringData();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }

            @Override
            public boolean isSharedSection() {
                return true;
            }

            @Override
            public StringDataSection createSection(IntegerPair countAndOffset){
                return new StringDataSection(countAndOffset, this);
            }
        };

        TYPE_ID = new SectionType<TypeId>("TYPE_ID", 0x0002) {
            @Override
            public TypeId newInstance() {
                return new TypeId();
            }
            @Override
            public int getReferenceType() {
                return 1;
            }
            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public Section<TypeId> createSection(IntegerPair countAndOffset){
                return new IdSection<>(countAndOffset, this);
            }
        };

        TYPE_LIST = new SectionType<TypeList>("TYPE_LIST", 0x1001) {
            @Override
            public TypeList newInstance() {
                return new TypeList();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public Section<TypeList> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        PROTO_ID = new SectionType<ProtoId>("PROTO_ID", 0x0003) {
            @Override
            public ProtoId newInstance() {
                return new ProtoId();
            }

            @Override
            public int getReferenceType() {
                return 4;
            }
            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public Section<ProtoId> createSection(IntegerPair countAndOffset) {
                return new IdSection<>(countAndOffset, this);
            }
        };

        FIELD_ID = new SectionType<FieldId>("FIELD_ID", 0x0004) {

            @Override
            public FieldId newInstance() {
                return new FieldId();
            }

            @Override
            public int getReferenceType() {
                return 2;
            }
            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public Section<FieldId> createSection(IntegerPair countAndOffset) {
                return new IdSection<>(countAndOffset, this);
            }
        };

        METHOD_ID = new SectionType<MethodId>("METHOD_ID", 0x0005) {
            @Override
            public MethodId newInstance() {
                return new MethodId();
            }

            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public int getReferenceType() {
                return 3;
            }
            @Override
            public Section<MethodId> createSection(IntegerPair countAndOffset) {
                return new IdSection<>(countAndOffset, this);
            }
        };

        ANNOTATION_ITEM = new SectionType<AnnotationItem>("ANNOTATION_ITEM", 0x2004) {
            @Override
            public AnnotationItem newInstance() {
                return new AnnotationItem();
            }
            
            @Override
            public Section<AnnotationItem> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
        };

        ANNOTATION_SET = new SectionType<AnnotationSet>("ANNOTATION_SET", 0x1003) {
            @Override
            public AnnotationSet newInstance() {
                return new AnnotationSet();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public AnnotationSetSection createSection(IntegerPair countAndOffset){
                return new AnnotationSetSection(countAndOffset, this);
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        ANNOTATION_GROUP = new SectionType<AnnotationGroup>("ANNOTATION_GROUP", 0x1002) {

            @Override
            public AnnotationGroup newInstance() {
                return new AnnotationGroup();
            }
            
            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<AnnotationGroup> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        ANNOTATION_DIRECTORY = new SectionType<AnnotationsDirectory>("ANNOTATIONS_DIRECTORY", 0x2006) {
            @Override
            public AnnotationsDirectory newInstance() {
                return new AnnotationsDirectory();
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<AnnotationsDirectory> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }
        };

        CALL_SITE_ID = new SectionType<CallSiteId>("CALL_SITE_ID", 0x0007) {
            @Override
            public CallSiteId newInstance() {
                return new CallSiteId();
            }

            @Override
            public int getReferenceType() {
                return 5;
            }

            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public Section<CallSiteId> createSection(IntegerPair countAndOffset) {
                return new IdSection<>(countAndOffset, this);
            }
        };

        METHOD_HANDLE = new SectionType<MethodHandleId>("METHOD_HANDLE", 0x0008) {
            @Override
            public MethodHandleId newInstance() {
                return new MethodHandleId();
            }

            @Override
            public int getReferenceType() {
                return 6;
            }
            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public Section<MethodHandleId> createSection(IntegerPair countAndOffset) {
                return new IdSection<>(countAndOffset, this);
            }
        };

        DEBUG_INFO = new SectionType<DebugInfo>("DEBUG_INFO", 0x2003) {
            @Override
            public DebugInfo newInstance() {
                return new DebugInfo();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<DebugInfo> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }
        };

        CODE = new SectionType<CodeItem>("CODE", 0x2001) {

            @Override
            public CodeItem newInstance() {
                return new CodeItem();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<CodeItem> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        ENCODED_ARRAY = new SectionType<EncodedArray>("ENCODED_ARRAY", 0x2005) {
            @Override
            public EncodedArray newInstance() {
                return new EncodedArray();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<EncodedArray> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }
        };

        CLASS_DATA = new SectionType<ClassData>("CLASS_DATA", 0x2000) {
            @Override
            public ClassData newInstance() {
                return new ClassData();
            }
            
            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<ClassData> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }
        };

        CLASS_ID = new SectionType<ClassId>("CLASS_ID", 0x0006) {
            @Override
            public ClassId newInstance() {
                return new ClassId();
            }

            @Override
            public int getReferenceType() {
                return 7;
            }
            @Override
            public boolean isIdSection() {
                return true;
            }
            @Override
            public ClassIdSection createSection(IntegerPair countAndOffset) {
                return new ClassIdSection(countAndOffset);
            }
        };

        HIDDEN_API = new SectionType<HiddenApiRestrictions>("HIDDEN_API", 0xF000) {
            @Override
            public HiddenApiRestrictions newInstance() {
                return new HiddenApiRestrictions();
            }

            @Override
            public boolean isDataSection() {
                return true;
            }
            @Override
            public DataSection<HiddenApiRestrictions> createSection(IntegerPair countAndOffset){
                return new DataSection<>(countAndOffset, this);
            }

            @Override
            public int sectionAlignment() {
                return 4;
            }
        };

        READ_ORDER = new SectionType[]{
                HEADER,
                MAP_LIST,
                STRING_ID,
                STRING_DATA,
                TYPE_ID,
                TYPE_LIST,
                PROTO_ID,
                FIELD_ID,
                METHOD_ID,
                METHOD_HANDLE,
                ANNOTATION_ITEM,
                ANNOTATION_SET,
                ANNOTATION_GROUP,
                ANNOTATION_DIRECTORY,
                ENCODED_ARRAY,
                CALL_SITE_ID,
                DEBUG_INFO,
                CODE,
                CLASS_DATA,
                CLASS_ID,
                HIDDEN_API
        };

        DATA_REMOVE_ORDER = new SectionType[]{
                CLASS_DATA,
                CODE,
                DEBUG_INFO,
                ANNOTATION_DIRECTORY,
                ANNOTATION_GROUP,
                ANNOTATION_SET,
                ANNOTATION_ITEM,
                ENCODED_ARRAY,
                CALL_SITE_ID,
                METHOD_HANDLE,
                METHOD_ID,
                FIELD_ID,
                PROTO_ID,
                TYPE_LIST,
                TYPE_ID,
                STRING_ID,
                STRING_DATA
        };

        R8_ORDER = new SectionType[]{
                HEADER,
                STRING_ID,
                TYPE_ID,
                PROTO_ID,
                FIELD_ID,
                METHOD_ID,
                CLASS_ID,
                CALL_SITE_ID,
                METHOD_HANDLE,
                CODE,
                DEBUG_INFO,
                TYPE_LIST,
                STRING_DATA,
                ANNOTATION_ITEM,
                CLASS_DATA,
                ENCODED_ARRAY,
                ANNOTATION_SET,
                ANNOTATION_GROUP,
                ANNOTATION_DIRECTORY,
                HIDDEN_API,
                MAP_LIST
        };

        SORT_SECTIONS_ORDER = new SectionType[]{
                STRING_DATA,
                STRING_ID,
                TYPE_ID,
                TYPE_LIST,
                PROTO_ID,
                FIELD_ID,
                METHOD_ID,
                METHOD_HANDLE,
                ANNOTATION_ITEM,
                ANNOTATION_SET,
                ANNOTATION_GROUP,
                ANNOTATION_DIRECTORY,
                ENCODED_ARRAY,
                CALL_SITE_ID,
                DEBUG_INFO,
                CODE,
                CLASS_DATA,
                CLASS_ID,
                HIDDEN_API,
                MAP_LIST,
                HEADER
        };

        DEX_LIB2_ORDER = new SectionType[]{
                HEADER,
                STRING_ID,
                TYPE_ID,
                PROTO_ID,
                FIELD_ID,
                METHOD_ID,
                CLASS_ID,
                STRING_DATA,
                TYPE_LIST,
                ENCODED_ARRAY,
                CALL_SITE_ID,
                ANNOTATION_ITEM,
                ANNOTATION_SET,
                ANNOTATION_GROUP,
                ANNOTATION_DIRECTORY,
                DEBUG_INFO,
                CODE,
                CLASS_DATA,
                HIDDEN_API,
                MAP_LIST
        };
    }

    private final String name;
    private final int type;

    private SectionType(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public int getType() {
        return type;
    }
    public boolean isIdSection(){
        return false;
    }
    public boolean isDataSection(){
        return false;
    }
    public boolean isSpecialSection(){
        return false;
    }
    public boolean isSharedSection() {
        return false;
    }
    public Section<T> createSection(IntegerPair countAndOffset){
        return null;
    }
    public Section<T> createSpecialSection(IntegerReference offset){
        throw new RuntimeException("Not implemented for: " + getName());
    }
    public int getReferenceType(){
        return 7;
    }
    
    public int sectionAlignment() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
    @Override
    public int hashCode() {
        return type;
    }
    @Override
    public String toString() {
        return getName();
    }

    public Creator<T> getCreator() {
        return this;
    }

    @Override
    public abstract T newInstance();

    @SuppressWarnings("unchecked")
    public static<T1 extends SectionItem> SectionType<T1> get(int type){
        for (SectionType<?> sectionType : R8_ORDER) {
            if (type == sectionType.type) {
                return (SectionType<T1>) sectionType;
            }
        }
        return null;
    }
    public static SectionType<? extends IdItem> getReferenceType(int reference){
        switch (reference){
            case 0:
                return STRING_ID;
            case 1:
                return TYPE_ID;
            case 2:
                return FIELD_ID;
            case 3:
                return METHOD_ID;
            case 4:
                return PROTO_ID;
            case 5:
                return CALL_SITE_ID;
            case 6:
                return METHOD_HANDLE;
            default:
                return null;
        }
    }

    public static SectionType<? extends IdItem> getIdSectionType(Key key) {
        if (key instanceof StringKey) {
            return STRING_ID;
        }
        if (key instanceof TypeKey) {
            return TYPE_ID;
        }
        if (key instanceof ProtoKey) {
            return PROTO_ID;
        }
        if (key instanceof FieldKey) {
            return FIELD_ID;
        }
        if (key instanceof MethodKey) {
            return METHOD_ID;
        }
        if (key instanceof MethodHandleKey) {
            return METHOD_HANDLE;
        }
        if (key instanceof CallSiteKey) {
            return CALL_SITE_ID;
        }
        return null;
    }

    public static SectionType<?> getSectionType(Key key) {
        SectionType<?> type = null;
        if (key != null) {
            type = getIdSectionType(key);
            if (type == null) {
                if (key instanceof AnnotationItemKey) {
                    type = ANNOTATION_ITEM;
                } else if (key instanceof AnnotationSetKey) {
                    type = ANNOTATION_SET;
                } else if (key instanceof AnnotationGroupKey) {
                    type = ANNOTATION_GROUP;
                }
            }
        }
        return type;
    }

    public static Iterator<SectionType<?>> getSectionTypes(){
        return new ArrayIterator<>(R8_ORDER);
    }
    public static<T1> Comparator<T1> getReadComparator(Function<? super T1, SectionType<?>> function){
        return comparator(READ_ORDER, function);
    }
    public static<T1> Comparator<T1> comparator(SectionType<?>[] sortOrder, Function<? super T1, SectionType<?>> function){
        return new OrderBasedComparator<>(sortOrder, function);
    }
    public static SectionType<?>[] getR8Order() {
        return R8_ORDER.clone();
    }
    public static SectionType<?>[] getDexLib2Order() {
        return DEX_LIB2_ORDER.clone();
    }
    public static SectionType<?>[] getRemoveOrderList() {
        return DATA_REMOVE_ORDER.clone();
    }
    public static SectionType<?>[] getSortSectionsOrder() {
        return SORT_SECTIONS_ORDER.clone();
    }

    public static Iterator<SectionType<?>> getIdSectionTypes() {
        return ArrayIterator.of(R8_ORDER, SectionType::isIdSection);
    }

    public static Predicate<SectionType<?>> exceptDebug() {
        return except(SectionType.DEBUG_INFO);
    }
    public static Predicate<SectionType<?>> exceptDebugCodeAnnotations() {
        return except(
                DEBUG_INFO,
                CODE,
                ANNOTATION_ITEM,
                ANNOTATION_SET,
                ANNOTATION_GROUP,
                ANNOTATION_DIRECTORY
        );
    }
    public static Predicate<SectionType<?>> minimal() {
        return except(
                DEBUG_INFO,
                CODE,
                ANNOTATION_ITEM,
                ANNOTATION_SET,
                ANNOTATION_GROUP,
                ANNOTATION_DIRECTORY,
                ENCODED_ARRAY,
                METHOD_HANDLE,
                CALL_SITE_ID,
                HIDDEN_API
        );
    }
    public static Predicate<SectionType<?>> classNames() {
        return type -> {
            if (type == null) {
                return false;
            }
            if (type.isSpecialSection()) {
                return true;
            }
            return type == CLASS_ID
                    || type == STRING_ID
                    || type == STRING_DATA
                    || type == TYPE_ID;
        };
    }
    public static Predicate<SectionType<?>> except(SectionType<?> ... types) {
        if (types == null || types.length == 0) {
            return CollectionUtil.getAcceptAll();
        }
        return sectionType -> {
            for (SectionType<?> type : types) {
                if (sectionType == type) {
                    return false;
                }
            }
            return true;
        };
    }



    static class OrderBasedComparator<T1> implements Comparator<T1> {
        private final Function<? super T1, SectionType<?>> function;
        private final SectionType<?>[] sortOrder;

        public OrderBasedComparator(SectionType<?>[] sortOrder, Function<? super T1, SectionType<?>> function){
            this.sortOrder = sortOrder;
            this.function = function;
        }
        private int getOrder(SectionType<?> sectionType){
            SectionType<?>[] sortOrder = this.sortOrder;
            int length = sortOrder.length;
            for(int i = 0; i < length; i++){
                if(sortOrder[i] == sectionType){
                    return i;
                }
            }
            return length - 2;
        }
        private int getOrder(T1 item){
            if(item == null){
                return this.sortOrder.length - 1;
            }
            return getOrder(this.function.apply(item));
        }
        @Override
        public int compare(T1 item1, T1 item2) {
            return Integer.compare(getOrder(item1), getOrder(item2));
        }
    }
}
