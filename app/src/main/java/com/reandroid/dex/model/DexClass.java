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

import com.reandroid.common.ReflectionUtil;
import com.reandroid.dex.common.AccessFlag;
import com.reandroid.dex.dalvik.DalvikInnerClass;
import com.reandroid.dex.data.ClassData;
import com.reandroid.dex.data.FieldDef;
import com.reandroid.dex.data.MethodDef;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.id.IdItem;
import com.reandroid.dex.key.FieldKey;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.key.TypeListKey;
import com.reandroid.dex.program.ClassProgram;
import com.reandroid.dex.program.FieldProgram;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.dex.smali.model.SmaliField;
import com.reandroid.dex.smali.model.SmaliMethod;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.InstanceIterator;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.collection.SingleIterator;
import com.reandroid.utils.collection.UniqueIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.reandroid.utils.function.Predicate;

public class DexClass extends DexDeclaration implements ClassProgram, Comparable<DexClass> {

    private final DexLayout dexLayout;
    private final ClassId classId;

    public DexClass(DexLayout dexLayout, ClassId classId) {
        this.dexLayout = dexLayout;
        this.classId = classId;
    }

    public boolean usesNative() {
        if (isNative()) {
            return true;
        }
        Iterator<DexMethod> methods = declaredMethods();
        while (methods.hasNext()) {
            DexMethod dexMethod = methods.next();
            if (dexMethod.isNative()) {
                return true;
            }
        }
        Iterator<DexField> fields = declaredFields();
        while (fields.hasNext()) {
            DexField dexField = fields.next();
            if (dexField.isNative()) {
                return true;
            }
        }
        return false;
    }
    public void replaceKeys(Key search, Key replace) {
        getId().replaceKeys(search, replace);
    }
    public Set<DexClass> getRequired() {
        return getRequired(null);
    }
    public Set<DexClass> getRequired(Predicate<TypeKey> exclude) {
        Set<DexClass> results = new HashSet<>();
        results.add(this);
        searchRequired(exclude, results);
        return results;
    }
    private void searchRequired(Predicate<TypeKey> exclude, Set<DexClass> results) {
        DexClassRepository dexClassRepository = getClassRepository();
        Iterator<TypeKey> iterator = usedTypes();
        while (iterator.hasNext()) {
            TypeKey typeKey = iterator.next();
            typeKey = typeKey.getDeclaring();
            if (exclude != null && !exclude.test(typeKey)) {
                continue;
            }
            DexClass dexClass = dexClassRepository.getDexClass(typeKey);
            if (dexClass == null || results.contains(dexClass)) {
                continue;
            }
            results.add(dexClass);
            dexClass.searchRequired(exclude, results);
        }
    }
    public Iterator<TypeKey> usedTypes() {
        Iterator<Key> iterator = ComputeIterator.of(getId().usedIds(), IdItem::getKey);
        Iterator<Key> mentioned = new IterableIterator<Key, Key>(iterator) {
            @SuppressWarnings("unchecked")
            @Override
            public Iterator<Key> iterator(Key element) {
                return (Iterator<Key>) element.contents();
            }
        };
        return InstanceIterator.of(mentioned, TypeKey.class);
    }
    public DexMethod getStaticConstructor() {
        MethodKey methodKey = MethodKey.CONSTRUCTOR_STATIC
                .changeDeclaring(getDefining());
        return getDeclaredMethod(methodKey);
    }
    public DexField getField(FieldKey fieldKey) {
        if (!isAccessibleTo(fieldKey.getDeclaring())) {
            return null;
        }
        DexField dexField = getDeclaredField(fieldKey);
        if (dexField != null) {
            return dexField;
        }
        DexClass superClass = getSuperClass();
        if (superClass != null) {
            dexField = superClass.getField(fieldKey);
            if (dexField != null) {
                if (dexField.isAccessibleTo(getDefining())) {
                    return dexField;
                }else {
                    return null;
                }
            }
        }
        Iterator<DexClass> iterator = getInterfaceClasses();
        while (iterator.hasNext()) {
            dexField = iterator.next().getField(fieldKey);
            if (dexField != null) {
                if (dexField.isAccessibleTo(getDefining())) {
                    return dexField;
                }
            }
        }
        return null;
    }
    public DexField getDeclaredField(FieldKey fieldKey, boolean ignoreType) {
        if (ignoreType) {
            return getDeclaredField(fieldKey.getName());
        }
        return getDeclaredField(fieldKey);
    }
    public DexField getDeclaredField(FieldKey fieldKey) {
        Iterator<DexField> iterator = declaredFields();
        while (iterator.hasNext()) {
            DexField dexField = iterator.next();
            if (fieldKey.equalsIgnoreDeclaring(dexField.getKey())) {
                return dexField;
            }
        }
        return null;
    }
    public DexField getDeclaredField(String name) {
        Iterator<DexField> iterator = declaredFields();
        while (iterator.hasNext()) {
            DexField dexField = iterator.next();
            if (ObjectsUtil.equals(dexField.getName(), name)) {
                return dexField;
            }
        }
        return null;
    }
    public DexMethod getMethod(MethodKey methodKey) {
        DexMethod dexMethod = getDeclaredMethod(methodKey);
        if (dexMethod != null) {
            return dexMethod;
        }
        Iterator<DexClass> iterator = getSuperTypes();
        while (iterator.hasNext()) {
            DexClass dexClass = iterator.next();
            dexMethod = dexClass.getDeclaredMethod(methodKey);
            if (dexMethod == null) {
                continue;
            }
            if (!dexMethod.isAccessibleTo(methodKey.getDeclaring())) {
                // TODO: should not reach here ?
                continue;
            }
            return dexMethod;
        }
        return null;
    }
    public Iterator<DexMethod> getMethods(MethodKey methodKey) {
        return getMethods(methodKey, false);
    }
    public Iterator<DexMethod> getMethods(MethodKey methodKey, boolean ignoreAccessibility) {
        TypeKey declaring = getKey();
        return CombiningIterator.two(getDeclaredMethods(methodKey),
                ComputeIterator.of(getSuperTypes(), dexClass -> {
                    DexMethod method = dexClass.getDeclaredMethod(methodKey);
                    if (method != null && !method.isPrivate()) {
                        if (ignoreAccessibility || method.isAccessibleTo(declaring)) {
                            return method;
                        }
                    }
                    return null;
                }));
    }
    public Iterator<DexMethod> getExtending(MethodKey methodKey) {
        return CombiningIterator.of(getDeclaredMethod(methodKey),
                ComputeIterator.of(getExtending(),
                        dexClass -> dexClass.getExtending(methodKey)));
    }
    public Iterator<DexMethod> getImplementations(MethodKey methodKey) {
        return CombiningIterator.of(getDeclaredMethod(methodKey),
         ComputeIterator.of(getImplementations(),
                dexClass -> dexClass.getImplementations(methodKey)));
    }
    public Iterator<MethodKey> getOverridingKeys(MethodKey methodKey) {
        MethodKey key = methodKey.changeDeclaring(getKey());
        return CombiningIterator.of(CombiningIterator.singleOne(
                key,
                SingleIterator.of(getBridging(methodKey))
                ),
                ComputeIterator.of(getOverriding(),
                        dexClass -> dexClass.getOverridingKeys(key)));
    }
    private MethodKey getBridging(MethodKey methodKey) {
        DexMethod dexMethod = getDeclaredMethod(methodKey, false);
        if (dexMethod != null) {
            return dexMethod.getBridging();
        }
        return null;
    }
    public boolean containsDeclaredMethod(MethodKey methodKey) {
        if (methodKey == null) {
            return false;
        }
        ClassData classData = getClassData();
        if (classData == null) {
            return false;
        }
        return classData.getMethod(methodKey) != null;
    }
    public DexMethod getDeclaredMethod(MethodKey methodKey) {
        return getDeclaredMethod(methodKey, false);
    }
    public DexMethod getDeclaredMethod(MethodKey methodKey, boolean ignoreReturnType) {
        Iterator<DexMethod> iterator = declaredMethods();
        while (iterator.hasNext()) {
            DexMethod dexMethod = iterator.next();
            MethodKey key = dexMethod.getKey();
            if (methodKey.equalsNameAndParameters(key)) {
                if (ignoreReturnType || methodKey.equalsReturnType(key)) {
                    return dexMethod;
                }
            }
        }
        return null;
    }
    public Iterator<DexMethod> getDeclaredMethods(MethodKey methodKey) {
        return FilterIterator.of(declaredMethods(),
                dexMethod -> methodKey.equalsNameAndParameters(dexMethod.getKey()));
    }
    public Iterator<DexClass> getOverridingAndSuperTypes() {
        return CombiningIterator.two(getOverriding(), getSuperTypes());
    }
    public Iterator<DexClass> getSuperTypes() {

        Iterator<DexClass> iterator = CombiningIterator.two(
                SingleIterator.of(getSuperClass()),
                getInterfaceClasses());

        iterator = new IterableIterator<DexClass, DexClass>(iterator) {
            @Override
            public Iterator<DexClass> iterator(DexClass element) {
                return CombiningIterator.two(SingleIterator.of(element), element.getSuperTypes());
            }
        };
        return new UniqueIterator<>(iterator).exclude(this);
    }
    public Iterator<DexClass> getOverriding() {
        return CombiningIterator.two(getExtending(), getImplementations());
    }
    public Iterator<DexClass> getExtending() {
        return getDexLayout().searchExtending(getKey());
    }
    public Iterator<DexClass> getImplementations() {
        return getDexLayout().searchImplementations(getKey());
    }
    public DexClass getSuperClass() {
        return search(getSuperClassKey());
    }
    public boolean isInstance(TypeKey typeKey) {
        if (typeKey == null) {
            return false;
        }
        if (typeKey.equals(TypeKey.OBJECT)) {
            return true;
        }
        TypeKey superType = getSuperClassKey();
        if (typeKey.equals(getKey()) || typeKey.equals(superType)) {
            return true;
        }
        DexClass superClass = search(superType);
        if (superClass != null && superClass.isInstance(typeKey)) {
            if (superClass.isInstance(typeKey)) {
                return true;
            }
        }
        TypeListKey typeListKey = getInterfacesKey();
        if (typeListKey.contains(typeKey)) {
            return true;
        }
        if (superClass == null && ReflectionUtil.isInstanceReflection(superType, typeKey)) {
            return true;
        }
        for (TypeKey key : typeListKey) {
            DexClass dexClass = search(key);
            if (dexClass != null) {
                if (dexClass.isInstance(typeKey)) {
                    return true;
                }
            } else if (ReflectionUtil.isInstanceReflection(key, typeKey)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    @Override
    public Iterator<? extends FieldProgram> getDeclaredFields() {
        return declaredFields();
    }
    @Override
    public Iterator<DexField> declaredFields() {
        return CombiningIterator.two(getStaticFields(), getInstanceFields());
    }

    public DexField getOrCreateStaticField(FieldKey fieldKey) {
        return initializeField(getOrCreateStatic(fieldKey));
    }
    public FieldDef getOrCreateStatic(FieldKey fieldKey) {
        return getOrCreateClassData().getOrCreateStatic(fieldKey);
    }
    @Override
    public Iterator<DexField> getStaticFields() {
        return ComputeIterator.of(getId().getStaticFields(), this::initializeField);
    }
    @Override
    public Iterator<DexField> getInstanceFields() {
        return ComputeIterator.of(getId().getInstanceFields(), this::initializeField);
    }

    @Override
    public int getStaticFieldsCount() {
        return getId().getStaticFieldsCount();
    }
    @Override
    public int getInstanceFieldsCount() {
        return getId().getInstanceFieldsCount();
    }
    @Override
    public boolean hasStaticFields() {
        return getId().hasStaticFields();
    }
    @Override
    public boolean hasInstanceFields() {
        return getId().hasInstanceFields();
    }

    public DexField getOrCreateInstanceField(FieldKey fieldKey) {
        return initializeField(getOrCreateInstance(fieldKey));
    }
    public FieldDef getOrCreateInstance(FieldKey fieldKey) {
        return getOrCreateClassData().getOrCreateInstance(fieldKey);
    }
    public Iterator<DexMethod> getDeclaredMethods(Predicate<DexMethod> filter) {
        Iterator<DexMethod> iterator = declaredMethods();
        if (filter == null) {
            return iterator;
        }
        return FilterIterator.of(iterator, filter);
    }
    // use declaredMethods();
    @Deprecated
    @Override
    public Iterator<DexMethod> getDeclaredMethods() {
        return declaredMethods();
    }
    @Override
    public Iterator<DexMethod> declaredMethods() {
        return CombiningIterator.two(getDirectMethods(), getVirtualMethods());
    }
    @Override
    public Iterator<DexMethod> getDirectMethods() {
        return ComputeIterator.of(getId().getDirectMethods(), this::initializeMethod);
    }
    @Override
    public Iterator<DexMethod> getVirtualMethods() {
        return ComputeIterator.of(getId().getVirtualMethods(), this::initializeMethod);
    }

    @Override
    public int getDirectMethodsCount() {
        return getId().getDirectMethodsCount();
    }
    @Override
    public int getVirtualMethodsCount() {
        return getId().getVirtualMethodsCount();
    }

    @Override
    public boolean hasDirectMethods() {
        return getId().hasDirectMethods();
    }
    @Override
    public boolean hasVirtualMethods() {
        return getId().hasVirtualMethods();
    }


    public DexMethod getOrCreateDirectMethod(MethodKey methodKey) {
        return initializeMethod(getOrCreateClassData().getOrCreateDirect(methodKey));
    }
    public DexMethod getOrCreateVirtualMethod(MethodKey methodKey) {
        return initializeMethod(getOrCreateClassData().getOrCreateVirtual(methodKey));
    }
    public DexMethod getOrCreateStaticMethod(MethodKey methodKey) {
        DexMethod dexMethod = getOrCreateDirectMethod(methodKey);
        dexMethod.addAccessFlag(AccessFlag.STATIC);
        return dexMethod;
    }

    DexField initializeField(FieldDef fieldDef) {
        return new DexField(this, fieldDef);
    }
    DexMethod initializeMethod(MethodDef methodDef) {
        return new DexMethod(this, methodDef);
    }

    public Iterator<DexInstruction> getDexInstructions() {
        return getDexInstructions(null);
    }
    public Iterator<DexInstruction> getDexInstructions(Predicate<DexMethod> filter) {
        return new IterableIterator<DexMethod, DexInstruction>(getDeclaredMethods(filter)) {
            @Override
            public Iterator<DexInstruction> iterator(DexMethod element) {
                return element.getInstructions();
            }
        };
    }
    public void decode(SmaliWriter writer, File outDir) throws IOException {
        File file = new File(outDir, toFilePath());
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create dir: " + dir);
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        writer.setWriter(new OutputStreamWriter(outputStream));
        append(writer);
        writer.close();
        outputStream.close();
    }
    private String toFilePath() {
        String name = getDefining().getTypeName();
        name = name.substring(1, name.length()-1);
        name = name.replace('/', File.separatorChar);
        return name + ".smali";
    }

    @Override
    public DexLayout getDexLayout() {
        return dexLayout;
    }
    @Override
    public ClassId getId() {
        return classId;
    }
    @Override
    public DexClass getDexClass() {
        return this;
    }
    @Override
    public TypeKey getKey() {
        return getId().getKey();
    }
    @Override
    public ClassId getDefinition() {
        return getId();
    }

    @Override
    public TypeKey getSuperClassKey() {
        return getId().getSuperClassKey();
    }
    public void setSuperClass(TypeKey superClass) {
        getId().setSuperClass(superClass);
    }
    public String getSourceFileName() {
        return getId().getSourceFileName();
    }
    public void setSourceFile(String sourceFile) {
        getId().setSourceFile(sourceFile);
    }

    public Iterator<DexClass> getInterfaceClasses() {
        return ComputeIterator.of(getInterfacesKey().iterator(), this::search);
    }
    DexClass search(TypeKey typeKey) {
        return getClassRepository().getDexClass(typeKey);
    }
    public boolean containsInterface(TypeKey typeKey) {
        return getInterfacesKey().contains(typeKey);
    }
    @Override
    public TypeListKey getInterfacesKey() {
        return getId().getInterfacesKey();
    }
    public void addInterface(TypeKey typeKey) {
        TypeListKey typeListKey = getInterfacesKey()
                .remove(typeKey)
                .add(typeKey);
        getId().setInterfaces(typeListKey);
    }
    public void removeInterface(TypeKey typeKey) {
        TypeListKey typeListKey = getInterfacesKey()
                .remove(typeKey);
        getId().setInterfaces(typeListKey);
    }
    public void clearInterfaces() {
        getId().setInterfaces(TypeListKey.empty());
    }
    public void clearDebug() {
        Iterator<DexMethod> iterator = declaredMethods();
        while (iterator.hasNext()) {
            iterator.next().clearDebug();
        }
    }
    public void fixDalvikInnerClassName() {
        DalvikInnerClass dalvikInnerClass = DalvikInnerClass.of(this);
        if (dalvikInnerClass != null && dalvikInnerClass.hasName()) {
            String inner = getKey().getSimpleInnerName();
            if (inner != null) {
                dalvikInnerClass.setName(inner);
            }
        }
    }
    public Set<Key> fixAccessibility() {
        DexClassRepository repository = getClassRepository();
        Set<Key> results = new HashSet<>();
        Iterator<Key> iterator = getId().usedKeys();
        while (iterator.hasNext()) {
            Key key = iterator.next();
            if (!results.contains(key) && fixAccessibility(repository.getDexDeclaration(key))) {
                results.add(key);
            }
        }
        fixMethodAccessibility(results);
        return results;
    }
    private void fixMethodAccessibility(Set<Key> results) {
        Iterator<DexMethod> iterator = declaredMethods();
        while (iterator.hasNext()) {
            DexMethod dexMethod = iterator.next();
            if (dexMethod.isPrivate()) {
                continue;
            }
            Iterator<DexMethod> superMethods = getMethods(dexMethod.getKey(), true);
            while (superMethods.hasNext()) {
                DexMethod method = superMethods.next();
                if (fixAccessibility(method)) {
                    results.add(method.getKey());
                }
            }
            Iterator<DexMethod> extendingMethods = getExtending(dexMethod.getKey());
            while (superMethods.hasNext()) {
                DexMethod method = extendingMethods.next();
                MethodKey key = method.getKey();
                if (!results.contains(key) && fixAccessibility(method)) {
                    results.add(key);
                }
            }
        }
    }
    private boolean fixAccessibility(DexDeclaration declaration) {
        if (declaration == null || declaration.isPrivate() ||
                (declaration.hasAccessFlag(AccessFlag.CONSTRUCTOR, AccessFlag.STATIC))) {
            return false;
        }
        if (!declaration.isAccessibleTo(this)) {
            declaration.addAccessFlag(AccessFlag.PUBLIC);
            return true;
        }
        return false;
    }
    public TypeKey getDalvikEnclosingClass() {
        Key key = getId().getDalvikEnclosing();
        if (key != null) {
            return key.getDeclaring();
        }
        return null;
    }
    public String getDalvikInnerClassName() {
        DalvikInnerClass dalvikInnerClass = DalvikInnerClass.of(this);
        if (dalvikInnerClass != null) {
            return dalvikInnerClass.getName();
        }
        return null;
    }
    public void updateDalvikInnerClassName(String name) {
        DalvikInnerClass dalvikInnerClass = DalvikInnerClass.of(this);
        if (dalvikInnerClass != null) {
            dalvikInnerClass.setName(name);
        }
    }
    public void createDalvikInnerClassName(String name) {
        DalvikInnerClass.getOrCreate(this).setName(name);
    }

    ClassData getOrCreateClassData() {
        return getId().getOrCreateClassData();
    }
    ClassData getClassData() {
        return getId().getClassData();
    }
    @Override
    public void removeSelf() {
        getDefinition().removeSelf();
    }
    public void edit() {
        getId().edit();
    }
    @Override
    public int compareTo(DexClass dexClass) {
        return getKey().compareTo(dexClass.getKey());
    }
    @Override
    public void append(SmaliWriter writer) throws IOException {
        getId().append(writer);
    }

    public DexMethod parseMethod(SmaliReader reader) throws IOException {
        SmaliMethod smaliMethod = new SmaliMethod();
        smaliMethod.setDefining(getKey());
        smaliMethod.parse(reader);
        MethodKey methodKey = smaliMethod.getKey();
        DexMethod dexMethod;
        if (smaliMethod.isDirect()) {
            dexMethod = getOrCreateDirectMethod(methodKey);
        } else {
            dexMethod = getOrCreateVirtualMethod(methodKey);
        }
        dexMethod.getDefinition().fromSmali(smaliMethod);
        return dexMethod;
    }
    public DexField parseField(SmaliReader reader) throws IOException {
        SmaliField smaliField = new SmaliField();
        smaliField.setDefining(getKey());
        smaliField.parse(reader);
        FieldKey fieldKey = smaliField.getKey();
        DexField dexField;
        if (smaliField.isInstance()) {
            dexField = getOrCreateInstanceField(fieldKey);
        } else {
            dexField = getOrCreateStaticField(fieldKey);
        }
        dexField.getDefinition().fromSmali(smaliField);
        return dexField;
    }
    public DexField parseInterfaces(SmaliReader reader) throws IOException {
        SmaliField smaliField = new SmaliField();
        smaliField.setDefining(getKey());
        smaliField.parse(reader);
        FieldKey fieldKey = smaliField.getKey();
        DexField dexField;
        if (smaliField.isInstance()) {
            dexField = getOrCreateInstanceField(fieldKey);
        } else {
            dexField = getOrCreateStaticField(fieldKey);
        }
        dexField.getDefinition().fromSmali(smaliField);
        return dexField;
    }
    public void writeSmali(SmaliWriter writer, File file) throws IOException {
        writer.setWriter(file);
        append(writer);
        writer.close();
    }
    public String toSmali() throws IOException {
        return SmaliWriter.toString(this);
    }
    public String toSmali(SmaliWriter writer) throws IOException {
        return SmaliWriter.toString(writer,this);
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DexClass dexClass = (DexClass) obj;
        if (!isInSameFile(dexClass)) {
            return false;
        }
        return getKey().equals(dexClass.getKey());
    }

    @Override
    public String toString() {
        return SmaliWriter.toStringSafe(this);
    }
}
