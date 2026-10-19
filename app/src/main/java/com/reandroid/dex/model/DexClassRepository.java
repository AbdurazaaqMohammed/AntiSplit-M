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
package com.reandroid.dex.model;

import com.reandroid.arsc.base.BlockRefresh;
import com.reandroid.arsc.item.IntegerReference;
import com.reandroid.dex.common.FullRefresh;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.data.CodeItem;
import com.reandroid.dex.data.DebugInfo;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.MethodId;
import com.reandroid.dex.id.SourceFile;
import com.reandroid.dex.key.AnnotationGroupKey;
import com.reandroid.dex.key.CallSiteKey;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyReference;
import com.reandroid.dex.key.MethodHandleKey;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.ProtoKey;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.key.TypeListKey;
import com.reandroid.dex.sections.Marker;
import com.reandroid.dex.sections.Section;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.collection.SingleIterator;
import com.reandroid.utils.collection.UniqueIterator;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public interface DexClassRepository extends FullRefresh, BlockRefresh {

    Iterator<DexClassModule> modules();
    DexClassRepository getRootRepository();

    default int getVersion() {
        int version = 0;
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            int v = iterator.next().getVersion();
            if (v > version) {
                version = v;
            }
        }
        return version;
    }
    default void setVersion(int version) {
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            iterator.next().setVersion(version);
        }
    }

    default int getCount(SectionType<?> sectionType) {
        int result = 0;
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            result += iterator.next().getCount(sectionType);
        }
        return result;
    }
    default int getDexClassesCount() {
        return getCount(SectionType.CLASS_ID);
    }
    default int shrink() {
        int result = 0;
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            result += iterator.next().shrink();
        }
        return result;
    }

    default DexClass getDexClass(TypeKey typeKey) {
        return searchClass(modules(), typeKey);
    }

    default DexClass searchClass(TypeKey typeKey) {
        return searchClass(getRootRepository().modules(), typeKey);
    }
    default DexClass searchClass(Iterator<DexClassModule> modules, TypeKey typeKey) {
        while (modules.hasNext()) {
            DexClass dexClass = modules.next().getDexClass(typeKey);
            if (dexClass != null) {
                return dexClass;
            }
        }
        return null;
    }

    default Iterator<DexClass> getDexClasses(Predicate<? super TypeKey> filter) {
        return new IterableIterator<DexClassModule, DexClass>(modules()) {
            @Override
            public Iterator<DexClass> iterator(DexClassModule element) {
                return element.getDexClasses(filter);
            }
        };
    }
    default Iterator<DexClass> getDexClassesCloned(Predicate<? super TypeKey> filter) {
        return new IterableIterator<DexClassModule, DexClass>(modules()) {
            @Override
            public Iterator<DexClass> iterator(DexClassModule element) {
                return element.getDexClassesCloned(filter);
            }
        };
    }

    default Iterator<DexClass> searchExtending(TypeKey typeKey) {
        UniqueIterator<DexClass> iterator = new UniqueIterator<>(
                new IterableIterator<DexClassModule, DexClass>(getRootRepository().modules()) {
                    @Override
                    public Iterator<DexClass> iterator(DexClassModule element) {
                        return element.getExtendingClasses(typeKey);
                    }
                });
        iterator.exclude(getDexClass(typeKey));
        return iterator;
    }
    default Iterator<DexClass> searchImplementations(TypeKey typeKey) {
        UniqueIterator<DexClass> iterator = new UniqueIterator<>(
                new IterableIterator<DexClassModule, DexClass>(getRootRepository().modules()) {
                    @Override
                    public Iterator<DexClass> iterator(DexClassModule element) {
                        return element.getImplementClasses(typeKey);
                    }
                });
        iterator.exclude(getDexClass(typeKey));
        return iterator;
    }
    default <T extends SectionItem> Iterator<Section<T>> getSections(SectionType<T> sectionType) {
        return new IterableIterator<DexClassModule, Section<T>>(modules()) {
            @Override
            public Iterator<Section<T>> iterator(DexClassModule element) {
                return element.getSections(sectionType);
            }
        };
    }
    default <T extends SectionItem> Iterator<T> getItems(SectionType<T> sectionType) {
        return new IterableIterator<Section<T>, T>(getSections(sectionType)) {
            @Override
            public Iterator<T> iterator(Section<T> element) {
                return element.iterator();
            }
        };
    }
    default <T extends SectionItem> Iterator<T> getClonedItems(SectionType<T> sectionType) {
        return new IterableIterator<Section<T>, T>(getSections(sectionType)) {
            @Override
            public Iterator<T> iterator(Section<T> element) {
                return element.clonedIterator();
            }
        };
    }
    default <T extends SectionItem> Iterator<T> getClonedItemsIf(
            SectionType<T> sectionType, Predicate<? super T> predicate) {
        return FilterIterator.of(getClonedItems(sectionType), predicate);
    }
    default <T extends SectionItem> Iterator<T> getClonedItemsIfKey(
            SectionType<T> sectionType, Predicate<? super Key> predicate) {
        if (predicate == null) {
            return getClonedItems(sectionType);
        }
        return getClonedItemsIf(sectionType, item -> predicate.test(item.getKey()));
    }
    default <T extends SectionItem> Iterator<T> getItems(SectionType<T> sectionType, Key key) {
        return new IterableIterator<Section<T>, T>(getSections(sectionType)) {
            @Override
            public Iterator<T> iterator(Section<T> element) {
                return element.getAll(key);
            }
        };
    }
    default <T extends SectionItem> Iterator<T> getItemsIf(
            SectionType<T> sectionType, Predicate<? super T> predicate) {
        return new IterableIterator<Section<T>, T>(getSections(sectionType)) {
            @Override
            public Iterator<T> iterator(Section<T> element) {
                return element.iterator(predicate);
            }
        };
    }
    default <T extends SectionItem> Iterator<T> getItemsIfKey(
            SectionType<T> sectionType, Predicate<? super Key> predicate) {
        if (predicate == null) {
            return getItems(sectionType);
        }
        return getItemsIf(sectionType, item -> predicate.test(item.getKey()));
    }
    default <T extends SectionItem> T getItem(SectionType<T> sectionType, int id) {
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            T item = iterator.next().getItem(sectionType, id);
            if (item != null) {
                return item;
            }
        }
        return null;
    }
    default <T extends SectionItem> T getItem(SectionType<T> sectionType, Key key) {
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            T item = iterator.next().getItem(sectionType, key);
            if (item != null) {
                return item;
            }
        }
        return null;
    }
    default boolean contains(SectionType<?> sectionType, Key key) {
        return getItems(sectionType, key).hasNext();
    }
    default boolean contains(Key key) {
        if (key == null) {
            return false;
        }
        if (key instanceof StringKey) {
            return contains(SectionType.STRING_ID, key);
        }
        if (key instanceof TypeKey) {
            return contains(SectionType.TYPE_ID, key);
        }
        if (key instanceof FieldKey) {
            return contains(SectionType.FIELD_ID, key);
        }
        if (key instanceof ProtoKey) {
            return contains(SectionType.PROTO_ID, key);
        }
        if (key instanceof MethodKey) {
            return contains(SectionType.METHOD_ID, key);
        }
        if (key instanceof TypeListKey) {
            return contains(SectionType.TYPE_LIST, key);
        }
        if (key instanceof MethodHandleKey) {
            return contains(SectionType.METHOD_HANDLE, key);
        }
        if (key instanceof CallSiteKey) {
            return contains(SectionType.CALL_SITE_ID, key);
        }
        if (key instanceof AnnotationGroupKey) {
            return contains(SectionType.ANNOTATION_GROUP, key);
        }
        throw new IllegalArgumentException("Unknown key type: " + key.getClass() + ", '" + key + "'");
    }
    default boolean containsClass(TypeKey key) {
        return contains(SectionType.CLASS_ID, key);
    }

    default <T1 extends SectionItem> boolean removeEntries(SectionType<T1> sectionType, Predicate<T1> filter) {
        Iterator<DexClassModule> iterator = modules();
        boolean result = false;
        while (iterator.hasNext()) {
            DexClassModule module = iterator.next();
            if (module.removeEntries(sectionType, filter)) {
                result = true;
            }
        }
        return result;
    }

    default <T1 extends SectionItem> boolean removeEntriesWithKey(SectionType<T1> sectionType, Predicate<? super Key> filter) {
        Iterator<DexClassModule> iterator = modules();
        boolean result = false;
        while (iterator.hasNext()) {
            DexClassModule module = iterator.next();
            if (module.removeEntriesWithKey(sectionType, filter)) {
                result = true;
            }
        }
        return result;
    }

    default <T1 extends SectionItem> boolean removeEntry(SectionType<T1> sectionType, Key key) {
        Iterator<DexClassModule> iterator = modules();
        boolean result = false;
        while (iterator.hasNext()) {
            DexClassModule module = iterator.next();
            if (module.removeEntry(sectionType, key)) {
                result = true;
            }
        }
        return result;
    }

    default void clearPoolMap() {
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            iterator.next().clearPoolMap();
        }
    }

    default boolean sort() {
        boolean sorted = false;
        Iterator<DexClassModule> iterator = modules();
        while (iterator.hasNext()) {
            if (iterator.next().sort()) {
                sorted = true;
            }
        }
        return sorted;
    }

    default Iterator<DexClass> findUserClasses(Key key) {
        return UniqueIterator.of(getDexClasses(),
                dexClass -> dexClass.uses(key));
    }
    default Iterator<DexClass> getDexClasses() {
        return getDexClasses(null);
    }
    default Iterator<DexClass> getDexClassesCloned() {
        return getDexClassesCloned(null);
    }
    default Iterator<DexClass> getPackageClasses(String packageName) {
        return getPackageClasses(packageName, true);
    }
    default Iterator<DexClass> getPackageClasses(String packageName, boolean includeSubPackages) {
        return getDexClasses(key -> key.isPackage(packageName, includeSubPackages));
    }
    default DexMethod getDeclaredMethod(MethodKey methodKey) {
        DexClass dexClass = getDexClass(methodKey.getDeclaring());
        if (dexClass != null) {
            DexMethod dexMethod = dexClass.getDeclaredMethod(methodKey, false);
            if (dexMethod == null) {
                dexMethod = dexClass.getDeclaredMethod(methodKey, true);
            }
            return dexMethod;
        }
        return null;
    }
    default DexMethod getDeclaredMethod(MethodKey methodKey, boolean ignoreReturnType) {
        DexClass dexClass = getDexClass(methodKey.getDeclaring());
        if (dexClass != null) {
            return dexClass.getDeclaredMethod(methodKey, ignoreReturnType);
        }
        return null;
    }
    default DexField getDeclaredField(FieldKey fieldKey, boolean ignoreType) {
        DexClass dexClass = getDexClass(fieldKey.getDeclaring());
        if (dexClass != null) {
            return dexClass.getDeclaredField(fieldKey, ignoreType);
        }
        return null;
    }
    default DexField getDeclaredField(FieldKey fieldKey) {
        DexClass dexClass = getDexClass(fieldKey.getDeclaring());
        if (dexClass != null) {
            return dexClass.getDeclaredField(fieldKey);
        }
        return null;
    }
    default DexDeclaration getDexDeclaration(Key key) {
        if (key instanceof TypeKey) {
            return getDexClass((TypeKey) key);
        }
        if (key instanceof MethodKey) {
            return getDeclaredMethod((MethodKey) key);
        }
        if (key instanceof FieldKey) {
            return getDeclaredField((FieldKey) key);
        }
        return null;
    }
    default Iterator<DexMethod> getDeclaredMethods() {
        return new IterableIterator<DexClass, DexMethod>(getDexClasses()) {
            @Override
            public Iterator<DexMethod> iterator(DexClass dexClass) {
                return dexClass.declaredMethods();
            }
        };
    }
    default Iterator<DexField> getDeclaredFields() {
        return new IterableIterator<DexClass, DexField>(getDexClasses()) {
            @Override
            public Iterator<DexField> iterator(DexClass dexClass) {
                return dexClass.declaredFields();
            }
        };
    }
    default Iterator<IntegerReference> visitIntegers() {
        return new DexIntegerVisitor(this);
    }

    default boolean removeClass(TypeKey typeKey) {
        return removeEntry(SectionType.CLASS_ID, typeKey);
    }

    default boolean removeClasses(Predicate<? super DexClass> filter) {
        Iterator<DexClassModule> iterator = modules();
        boolean result = false;
        while (iterator.hasNext()) {
            DexClassModule module = iterator.next();
            if (module.removeClasses(filter)) {
                result = true;
            }
        }
        return result;
    }
    default boolean removeClassesWithKeys(Predicate<? super TypeKey> filter) {
        return removeEntriesWithKey(SectionType.CLASS_ID, ObjectsUtil.cast(filter));
    }
    default boolean removeAnnotations(TypeKey typeKey) {
        return removeEntries(SectionType.ANNOTATION_ITEM,
                annotationItem -> typeKey.equals(annotationItem.getType()));
    }
    default void clearDebug() {
        Iterator<Section<DebugInfo>> iterator = getSections(SectionType.DEBUG_INFO);
        while (iterator.hasNext()) {
            iterator.next().removeSelf();
        }
    }

    default Iterator<? extends KeyReference> getExternalReferences() {
        return EmptyIterator.of();
    }

    default Iterator<FieldKey> findEquivalentFields(FieldKey fieldKey) {
        DexClass defining = getDexClass(fieldKey.getDeclaring());
        if (defining == null) {
            return EmptyIterator.of();
        }
        DexField dexField = defining.getField(fieldKey);
        if (dexField == null) {
            return EmptyIterator.of();
        }
        defining = dexField.getDexClass();

        FieldKey definingKey = dexField.getKey();

        Iterator<FieldKey> subKeys = ComputeIterator.of(getSuccessorClasses(defining.getKey()),
                dexClass -> {
                    FieldKey key = definingKey.changeDeclaring(dexClass.getKey());
                    DexField field = dexClass.getField(key);
                    if (field != null && definingKey.equals(field.getKey())) {
                        return key;
                    }
                    return null;
                }
        );
        return CombiningIterator.two(SingleIterator.of(definingKey), subKeys);
    }
    default Iterator<DexClass> getSuccessorClasses(TypeKey typeKey) {
        return new IterableIterator<DexClassModule, DexClass>(modules()) {
            @Override
            public Iterator<DexClass> iterator(DexClassModule element) {
                return element.getSuccessorClasses(typeKey);
            }
        };
    }
    default Iterator<MethodKey> findEquivalentMethods(MethodKey methodKey) {
        return findEquivalentMethods(methodKey, true);
    }
    default Iterator<MethodKey> findEquivalentMethods(MethodKey methodKey, boolean recursive) {
        if (methodKey == null) {
            return EmptyIterator.of();
        }
        DexClass defining = getDexClass(methodKey.getDeclaring());
        if (defining == null) {
            return EmptyIterator.of();
        }
        Iterator<DexMethod> iterator = defining.getMethods(methodKey);
        Iterator<MethodKey> results = new IterableIterator<DexMethod, MethodKey>(iterator) {
            @Override
            public Iterator<MethodKey> iterator(DexMethod element) {
                element = element.getDeclared();
                return CombiningIterator.singleOne(element.getKey(),
                        element.getOverridingKeys());
            }
        };
        if (recursive) {
            results = UniqueIterator.of(new IterableIterator<MethodKey, MethodKey>(results) {
                @Override
                public Iterator<MethodKey> iterator(MethodKey element) {
                    return findEquivalentMethods(element, false);
                }
            });
        }
        return results;
    }
    default Iterator<DexMethod> getMethods(MethodKey methodKey) {
        return ComputeIterator.of(findEquivalentMethods(methodKey), this::getDeclaredMethod);
    }
    default Iterator<MethodId> getMethodIds(MethodKey methodKey) {
        return new IterableIterator<MethodKey, MethodId>(findEquivalentMethods(methodKey)) {
            @Override
            public Iterator<MethodId> iterator(MethodKey element) {
                return getItems(SectionType.METHOD_ID, element);
            }
        };
    }

    default Iterator<Marker> getMarkers() {
        return new IterableIterator<DexClassModule, Marker>(modules()) {
            @Override
            public Iterator<Marker> iterator(DexClassModule element) {
                return element.getMarkers();
            }
        };
    }
    default void clearMarkers() {
        Iterator<Marker> iterator = getMarkers();
        while (iterator.hasNext()) {
            iterator.next().removeSelf();
        }
    }
    default void setClassSourceFileAll() {
        setClassSourceFileAll(SourceFile.SourceFile);
    }
    default void setClassSourceFileAll(String sourceFile) {
        Iterator<ClassId> iterator = getItems(SectionType.CLASS_ID);
        while (iterator.hasNext()) {
            ClassId classId = iterator.next();
            classId.setSourceFile(sourceFile);
        }
    }
    default void edit() {
        CollectionUtil.walk(FilterIterator.of(getItems(SectionType.CODE),
                CodeItem::flattenTryItems));
        Iterator<DexClass> iterator = getDexClasses();
        while (iterator.hasNext()) {
            iterator.next().edit();
        }
    }
}
