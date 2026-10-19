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
package com.reandroid.arsc.container;

import com.reandroid.arsc.array.TypeBlockArray;
import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.base.BlockContainer;
import com.reandroid.arsc.chunk.ChunkType;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.SpecBlock;
import com.reandroid.arsc.chunk.TypeBlock;
import com.reandroid.arsc.header.HeaderBlock;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.TypeString;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.pool.SpecStringPool;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.ValueItem;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.MergingIterator;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.*;
import com.reandroid.utils.function.Predicate;

public class SpecTypePair extends BlockContainer<Block>
        implements Iterable<TypeBlock>, JSONConvert<JSONObject>, Comparable<SpecTypePair>{
    private final Block[] mChildes;
    private final SpecBlock mSpecBlock;
    private final TypeBlockArray mTypeBlockArray;

    public SpecTypePair(SpecBlock specBlock, TypeBlockArray typeBlockArray){
        this.mSpecBlock = specBlock;
        this.mTypeBlockArray = typeBlockArray;

        this.mChildes = new Block[]{specBlock, typeBlockArray};

        specBlock.setIndex(0);
        typeBlockArray.setIndex(1);

        specBlock.setParent(this);
        typeBlockArray.setParent(this);
    }
    public SpecTypePair(){
        this(new SpecBlock(), new TypeBlockArray());
    }

    public Iterator<ValueItem> allValues(){
        return new MergingIterator<>(new ComputeIterator<>(getTypeBlocks(),
                TypeBlock::allValues));
    }
    public void trimConfigSizes(int resConfigSize){
        Iterator<TypeBlock> iterator = getTypeBlocks();
        while (iterator.hasNext()){
            ResConfig resConfig = iterator.next().getResConfig();
            resConfig.trimToSize(resConfigSize);
        }
    }
    public boolean isTypeAttr(){
        // TODO: find better way
        TypeString typeString = getTypeString();
        if(typeString != null){
            return typeString.isTypeAttr();
        }
        return false;
    }
    public boolean isTypeId(){
        // TODO: find better way
        TypeString typeString = getTypeString();
        if(typeString != null){
            return typeString.isTypeId();
        }
        return false;
    }
    public ResourceEntry getResource(int entryId){
        if(entryId < 0 || entryId > getHighestEntryId()){
            return null;
        }
        PackageBlock packageBlock = getPackageBlock();
        if(packageBlock == null){
            return null;
        }
        int resourceId = packageBlock.getId() << 24;
        resourceId |= getId() << 16;
        resourceId |= entryId;
        return new ResourceEntry(packageBlock, resourceId);
    }
    public ResourceEntry getResource(String name){
        PackageBlock packageBlock = getPackageBlock();
        if(packageBlock == null){
            return null;
        }
        SpecStringPool specStringPool = packageBlock.getSpecStringPool();
        int resourceId = specStringPool
                .resolveResourceId(this, name);
        if(resourceId == 0){
            return null;
        }
        return new ResourceEntry(packageBlock, resourceId);
    }
    public Iterator<ResourceEntry> getResources(){
        final PackageBlock packageBlock = getPackageBlock();
        if(packageBlock == null){
            return EmptyIterator.of();
        }
        final int end = getHighestEntryId();
        final int id = (packageBlock.getId() << 24) | (getId() << 16);
        return new Iterator<ResourceEntry>() {
            private int mIndex;
            @Override
            public boolean hasNext() {
                return mIndex <= end;
            }
            @Override
            public ResourceEntry next() {
                if(mIndex > end){
                    throw new NoSuchElementException();
                }
                int resourceId = id | mIndex;
                mIndex++;
                return new ResourceEntry(packageBlock, resourceId);
            }
        };
    }
    public Iterator<Entry> getEntries(int entryId){
        return getEntries(entryId, false);
    }
    public Iterator<Entry> getEntries(int entryId, boolean skipNull){
        return new ComputeIterator<>(getTypeBlocks(), typeBlock -> {
            Entry entry = typeBlock.getEntry(entryId);
            if(entry == null || (skipNull && entry.isNull())){
                return null;
            }
            return entry;
        });
    }
    public Iterator<TypeBlock> getTypeBlocks(){
        return getTypeBlockArray().iterator();
    }
    public Iterator<TypeBlock> getTypeBlocks(Predicate<TypeBlock> filter){
        return getTypeBlockArray().iterator(filter);
    }
    public Iterator<TypeBlock> getTypeBlocks(ResConfig resConfig){
        return getTypeBlockArray().iterator(resConfig);
    }
    public Boolean hasComplexEntry(){
        return getTypeBlockArray().hasComplexEntry();
    }
    public void linkTableStringsInternal(TableStringPool tableStringPool) {
        for(TypeBlock typeBlock : this) {
            typeBlock.getEntryArray().linkTableStringsInternal(tableStringPool);
        }
    }
    public void linkSpecStringsInternal(SpecStringPool specStringPool) {
        for(TypeBlock typeBlock : this) {
            typeBlock.getEntryArray().linkSpecStringsInternal(specStringPool);
        }
    }
    public void destroy(){
        getSpecBlock().destroy();
        getTypeBlockArray().destroy();
    }
    public Entry getAnyEntry(short entryId){
        Entry result = null;
        Iterator<TypeBlock> iterator = getTypeBlockArray().iterator();
        while (iterator.hasNext()){
            TypeBlock typeBlock = iterator.next();
            Entry entry = typeBlock.getEntry(entryId);
            if(entry == null){
                continue;
            }
            if(!entry.isNull()){
                return entry;
            }
            if(result == null){
                result = entry;
            }
        }
        return result;
    }
    public Entry getAnyEntry(String name){
        Iterator<TypeBlock> iterator = getTypeBlockArray().iterator();
        while (iterator.hasNext()){
            TypeBlock typeBlock = iterator.next();
            if(typeBlock == null){
                continue;
            }
            Entry entry = typeBlock.getEntry(name);
            if(entry != null){
                return entry;
            }
        }
        return null;
    }
    public Entry getEntry(ResConfig resConfig, String entryName){
        return getTypeBlockArray().getEntry(resConfig, entryName);
    }
    public Entry getEntry(ResConfig resConfig, int entryId){
        return getTypeBlockArray().getEntry(resConfig, entryId);
    }
    public void sortTypes(){
        getTypeBlockArray().sort();
    }
    public boolean removeNullEntries(int startId){
        startId = 0x0000ffff & startId;
        boolean removed = getTypeBlockArray().removeNullEntries(startId);
        if(!removed){
            return false;
        }
        getSpecBlock().setEntryCount(startId);
        return true;
    }
    public void removeEmptyTypeBlocks(){
        getTypeBlockArray().removeEmptyBlocks();
    }
    public boolean isEmpty(){
        return getTypeBlockArray().isEmpty();
    }
    public int countTypeBlocks(){
        return getTypeBlockArray().size();
    }
    public Entry getOrCreateEntry(short entryId, String qualifiers){
        return getTypeBlockArray().getOrCreateEntry(entryId, qualifiers);
    }
    public Entry getOrCreateEntry(short entryId, ResConfig resConfig){
        return getTypeBlockArray().getOrCreateEntry(entryId, resConfig);
    }
    public Entry getEntry(short entryId, String qualifiers){
        return getTypeBlockArray().getEntry(entryId, qualifiers);
    }
    public TypeBlock getOrCreateTypeBlock(String qualifiers){
        return getTypeBlockArray().getOrCreate(qualifiers);
    }
    public TypeBlock getOrCreateTypeBlock(ResConfig resConfig){
        return getTypeBlockArray().getOrCreate(resConfig);
    }
    public TypeBlock getTypeBlock(String qualifiers){
        return getTypeBlockArray().getTypeBlock(qualifiers);
    }
    public TypeBlock getTypeBlock(ResConfig resConfig){
        return getTypeBlockArray().getTypeBlock(resConfig);
    }
    public Set<ResConfig> listResConfig(){
        return mTypeBlockArray.listResConfig();
    }
    public Iterator<ResConfig> getResConfigs(){
        return mTypeBlockArray.getResConfigs();
    }
    public Iterator<TypeBlock> iteratorNonEmpty(){
        return mTypeBlockArray.iteratorNonEmpty();
    }
    public boolean hasDuplicateResConfig(boolean ignoreEmpty){
        return mTypeBlockArray.hasDuplicateResConfig(ignoreEmpty);
    }

    public byte getTypeId(){
        return mSpecBlock.getTypeId();
    }
    public int getId(){
        return mSpecBlock.getId();
    }
    public void setTypeId(byte id){
        mSpecBlock.setTypeId(id);
        mTypeBlockArray.setTypeId(id);
    }
    public String getTypeName(){
        TypeString typeString = getTypeString();
        if(typeString!=null){
            return typeString.get();
        }
        return null;
    }
    public boolean isEqualTypeName(String typeName){
        return TypeBlock.isEqualTypeName(getTypeName(), typeName);
    }

    public SpecBlock getSpecBlock(){
        return mSpecBlock;
    }
    public TypeBlockArray getTypeBlockArray(){
        return mTypeBlockArray;
    }
    public PackageBlock getPackageBlock(){
        return getParent(PackageBlock.class);
    }
    public List<Entry> listEntries(int entryId){
        List<Entry> results=new ArrayCollection<>();
        Iterator<TypeBlock> itr = mTypeBlockArray.iterator(true);
        while (itr.hasNext()){
            TypeBlock typeBlock=itr.next();
            Entry entry = typeBlock.getEntry(entryId);
            if(entry ==null|| entry.isNull()){
                continue;
            }
            results.add(entry);
        }
        return results;
    }
    public List<Entry> listEntries(String entryName){
        List<Entry> results = new ArrayCollection<>();
        Iterator<TypeBlock> itr = mTypeBlockArray.iterator(true);
        while (itr.hasNext()){
            TypeBlock typeBlock = itr.next();
            Entry entry = typeBlock.getEntry(entryName);
            if(entry != null){
                results.add(entry);
            }
        }
        return results;
    }

    @Override
    public Iterator<TypeBlock> iterator() {
        return getTypeBlockArray().iterator();
    }
    @Override
    protected void onRefreshed() {

    }
    @Override
    public int getChildesCount() {
        return mChildes.length;
    }
    @Override
    public Block[] getChildes() {
        return mChildes;
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        HeaderBlock headerBlock=reader.readHeaderBlock();
        if(headerBlock==null){
            return;
        }
        ChunkType chunkType=headerBlock.getChunkType();
        if(chunkType == ChunkType.TYPE){
            readTypeBlock(reader);
            return;
        }
        if(chunkType!=ChunkType.SPEC){
            readUnexpectedNonSpecBlock(reader, headerBlock);
        }
        mSpecBlock.readBytes(reader);
    }
    private void readTypeBlock(BlockReader reader) throws IOException {
        TypeBlock typeBlock = mTypeBlockArray.createNext();
        typeBlock.readBytes(reader);
    }
    private void readUnexpectedNonSpecBlock(BlockReader reader, HeaderBlock headerBlock) throws IOException{
        throw new IOException("Unexpected block: "+headerBlock.toString()+", Should be: "+ChunkType.SPEC);
    }
    public int getHighestEntryCount(){
        return getTypeBlockArray().getHighestEntryCount();
    }
    public int getHighestEntryId(){
        return getTypeBlockArray().getHighestEntryId();
    }
    public TypeString getTypeString(){
        PackageBlock packageBlock = getPackageBlock();
        if(packageBlock!=null){
            return packageBlock.getTypeStringPool().getById(getId());
        }
        return null;
    }

    public void serializePublicXml(XmlSerializer serializer) throws IOException {
        ResourceEntry last = getResource(getHighestEntryId());
        boolean lastWritten = false;
        if(last != null){
            lastWritten = last.serializePublicXml(serializer);
        }
        Iterator<ResourceEntry> itr = getResources();
        while (itr.hasNext()){
            ResourceEntry resourceEntry = itr.next();
            if(lastWritten && resourceEntry.equals(last)){
                continue;
            }
            resourceEntry.serializePublicXml(serializer);
        }
    }
    @Override
    public JSONObject toJson() {
        return toJson(false);
    }
    @Override
    public void fromJson(JSONObject json) {
        getSpecBlock().fromJson(json.getJSONObject(SpecBlock.NAME_spec));
        getTypeBlockArray().fromJson(json.optJSONArray(NAME_types));
    }
    public JSONObject toJson(boolean specOnly) {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put(SpecBlock.NAME_spec, getSpecBlock().toJson());
        if(!specOnly){
            jsonObject.put(NAME_types, getTypeBlockArray().toJson());
        }
        return jsonObject;
    }
    public void merge(SpecTypePair typePair){
        if(typePair==null||typePair==this){
            return;
        }
        if(getTypeId() != typePair.getTypeId()){
            throw new IllegalArgumentException("Can not merge different id types: "
                    +getTypeId()+"!="+typePair.getTypeId());
        }
        getSpecBlock().merge(typePair.getSpecBlock());
        getTypeBlockArray().merge(typePair.getTypeBlockArray());
    }
    @Override
    public int compareTo(SpecTypePair specTypePair) {
        return Integer.compare(getId(), specTypePair.getId());
    }
    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append(HexUtil.toHex2(getTypeId()));
        builder.append(" (");
        TypeString ts = getTypeString();
        if(ts!=null){
            builder.append(ts.get());
        }else {
            builder.append("null");
        }
        builder.append(") config count=");
        builder.append(getTypeBlockArray().size());
        return builder.toString();
    }

    public static final String NAME_types = "types";
    public static final String NAME_sparse_types = "sparse_types";
}
