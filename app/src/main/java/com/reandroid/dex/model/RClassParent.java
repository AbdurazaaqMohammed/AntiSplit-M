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

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.dex.common.AccessFlag;
import com.reandroid.dex.common.DexUtils;
import com.reandroid.dex.dalvik.DalvikMemberClass;
import com.reandroid.dex.id.ClassId;
import com.reandroid.dex.ins.Ins35c;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.data.*;
import com.reandroid.dex.key.MethodKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.io.IOUtil;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.*;

public class RClassParent extends DexClass {

    private final Map<String, RClass> mMembers;
    private final Set<PackageBlock> mPackageBlocks;

    public RClassParent(DexLayout dexLayout, ClassId classId) {
        super(dexLayout, classId);
        this.mMembers = new HashMap<>();
        this.mPackageBlocks = new HashSet<>();
    }

    public RField getRField(int resourceId){
        return load(getEntry(resourceId));
    }
    public boolean hasRField(int resourceId){
        return getEntry(resourceId) != null;
    }
    private ResourceEntry getEntry(int resourceId){
        for(PackageBlock packageBlock : mPackageBlocks){
            ResourceEntry entry = packageBlock.getResource(resourceId);
            if(entry != null){
                return entry;
            }
        }
        return null;
    }
    public void load(PackageBlock packageBlock){
        this.mPackageBlocks.add(packageBlock);
    }
    private RField load(ResourceEntry entry){
        if(entry == null || entry.isEmpty()){
            return null;
        }
        RClass rClass = getOrCreateMember(entry.getType());
        return rClass.load(entry);
    }
    public RClass getOrCreateMember(String simpleName){
        TypeKey typeKey = getKey().createInnerClass(simpleName);
        RClass rClass = mMembers.get(typeKey.getTypeName());
        if(rClass != null){
            return rClass;
        }
        addMemberAnnotation(simpleName);
        ClassId classId = getDexLayout().getOrCreateClassId(typeKey);
        rClass = new RClass(getDexLayout(), classId);
        mMembers.put(typeKey.getTypeName(), rClass);
        rClass.initialize();
        return rClass;
    }
    public void addMemberAnnotation(String simpleName) {
        DalvikMemberClass.getOrCreate(this).addSimpleName(simpleName);
    }
    public void initialize(){
        ClassId classId = getId();
        classId.addAccessFlag(AccessFlag.PUBLIC);
        ClassData classData = classId.getOrCreateClassData();
        MethodKey methodKey = MethodKey.CONSTRUCTOR.changeDeclaring(classId.getKey());
        if(classData.getMethod(methodKey) != null){
            return;
        }
        MethodDef methodDef = classData.getOrCreateDirect(methodKey);
        methodDef.addAccessFlag(AccessFlag.PUBLIC);
        methodDef.addAccessFlag(AccessFlag.CONSTRUCTOR);
        InstructionList insList = methodDef.getOrCreateInstructionList();
        Ins35c ins = insList.createNext(Opcode.INVOKE_DIRECT);
        ins.setKey(MethodKey.parse("Ljava/lang/Object;-><init>()V"));
        ins.setRegistersCount(1);
        ins.setRegister(0, 0);
        insList.createNext(Opcode.RETURN_VOID);
    }
    static boolean isRParentClassName(ClassId classId) {
        if(classId != null){
            return isRParentClassName(classId.getName());
        }
        return false;
    }
    static boolean isRParentClassName(String name) {
        if(name == null){
            return false;
        }
        return SIMPLE_NAME_PREFIX.equals(DexUtils.getSimpleName(name));
    }
    public static void serializePublicXml(Collection<RField> rFields, XmlSerializer serializer) throws IOException {
        serializer.startDocument("utf-8", null);
        serializer.text("\n");
        serializer.startTag(null, PackageBlock.TAG_resources);

        List<RField> fieldList = new ArrayCollection<>();
        fieldList.addAll(rFields);
        java.util.Collections.sort(fieldList, CompareUtil.getComparableComparator());
        for(RField rField : fieldList) {
            rField.serializePublicXml(serializer);
        }

        serializer.text("\n");
        serializer.endTag(null, PackageBlock.TAG_resources);
        serializer.endDocument();
        serializer.flush();
        IOUtil.close(serializer);
    }

    private static final String SIMPLE_NAME_PREFIX = "R";

}
