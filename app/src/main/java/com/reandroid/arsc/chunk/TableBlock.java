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
package com.reandroid.arsc.chunk;

import com.reandroid.arsc.ARSCLib;
import com.reandroid.arsc.ApkFile;
import com.reandroid.arsc.array.PackageArray;
import com.reandroid.arsc.header.HeaderBlock;
import com.reandroid.arsc.header.InfoHeader;
import com.reandroid.arsc.header.TableHeader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.model.ResourceName;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.StagedAliasEntry;
import com.reandroid.arsc.value.ValueItem;
import com.reandroid.common.BytesOutputStream;
import com.reandroid.common.ReferenceResolver;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import com.reandroid.utils.function.Predicate;

public class TableBlock extends Chunk<TableHeader>
        implements MainChunk, Iterable<PackageBlock>, JSONConvert<JSONObject> {

    private final TableStringPool mTableStringPool;
    private final PackageArray mPackageArray;
    private final List<TableBlock> mFrameWorks;
    private ApkFile mApkFile;
    private ReferenceResolver referenceResolver;
    private PackageBlock mCurrentPackage;
    private PackageBlock mEmptyTablePackage;

    public TableBlock() {
        super(new TableHeader(), 2);
        TableHeader header = getHeaderBlock();
        this.mTableStringPool = new TableStringPool(true);
        this.mPackageArray = new PackageArray(header.getPackageCount());
        this.mFrameWorks = new ArrayCollection<>();
        addChild(mTableStringPool);
        addChild(mPackageArray);
    }
    // Experimental
    public void changePackageId(int packageIdOld, int packageIdNew){
        for (PackageBlock packageBlock : this) {
            packageBlock.changePackageId(packageIdOld, packageIdNew);
        }
    }
    // Experimental
    public Iterator<ValueItem> allValues(){
        return new MergingIterator<>(new ComputeIterator<>(getPackages(),
                PackageBlock::allValues));
    }

    public PackageBlock getCurrentPackage(){
        return mCurrentPackage;
    }
    public void setCurrentPackage(PackageBlock packageBlock){
        mCurrentPackage = packageBlock;
    }
    public PackageBlock getPackageBlockByTag(Object tag){
        for(PackageBlock packageBlock : this){
            if(Objects.equals(tag, packageBlock.getTag())){
                return packageBlock;
            }
        }
        return null;
    }
    public Iterator<ResourceEntry> getResources(){
        return new IterableIterator<PackageBlock, ResourceEntry>(getPackages()) {
            @Override
            public Iterator<ResourceEntry> iterator(PackageBlock element) {
                return element.getResources();
            }
        };
    }
    public ResourceEntry getResource(int resourceId){
        if(resourceId == 0){
            return null;
        }
        Iterator<PackageBlock> iterator = getAllPackages();
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(resourceId);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        int staged = resolveStagedAlias(resourceId, 0);
        if(staged == 0 || staged == resourceId){
            return null;
        }
        iterator = getAllPackages();
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(staged);
            if(resourceEntry != null){
                return resourceEntry.setStageAliasId(resourceId);
            }
        }
        return null;
    }
    public ResourceEntry getResource(PackageBlock context, int resourceId){
        if(resourceId == 0){
            return null;
        }
        Iterator<PackageBlock> iterator = getAllPackages(context);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(resourceId);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        int staged = resolveStagedAlias(resourceId, 0);
        if(staged == 0 || staged == resourceId){
            return null;
        }
        iterator = getAllPackages(context);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(staged);
            if(resourceEntry != null){
                return resourceEntry.setStageAliasId(resourceId);
            }
        }
        return null;
    }
    public ResourceEntry getResource(ResourceName resourceName) {
        if(resourceName != null) {
            return getResource(resourceName.getPackageName(),
                    resourceName.getType(), resourceName.getName());
        }
        return null;
    }
    public ResourceEntry getResource(String packageName, String type, String name){
        Iterator<PackageBlock> iterator = getAllPackages(packageName);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(type, name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public ResourceEntry getResource(PackageBlock context, String type, String name){
        Iterator<PackageBlock> iterator = getAllPackages(context);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(type, name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public ResourceEntry getResource(PackageBlock context, String packageName, String type, String name){
        Iterator<PackageBlock> iterator = getAllPackages(context, packageName);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(type, name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public ResourceEntry getLocalResource(int resourceId){
        return getLocalResource( null, resourceId);
    }
    public ResourceEntry getLocalResource(PackageBlock context, int resourceId){
        Iterator<PackageBlock> iterator = getPackages(context);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(resourceId);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public ResourceEntry getLocalResource(PackageBlock context, String type, String name){
        Iterator<PackageBlock> iterator = getPackages(context);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(type, name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public ResourceEntry getLocalResource(String type, String name){
        return getLocalResource((String) null, type, name);
    }
    public ResourceEntry getLocalResource(String packageName, String type, String name){
        Iterator<PackageBlock> iterator = getPackages(packageName);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock.getResource(type, name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        return null;
    }
    public Iterator<ResourceEntry> getLocalResources(String type){
        return new IterableIterator<PackageBlock, ResourceEntry>(getPackages((String) null)) {
            @Override
            public Iterator<ResourceEntry> iterator(PackageBlock element) {
                return element.getResources(type);
            }
        };
    }
    public ResourceEntry getAttrResource(String prefix, String name){
        Iterator<PackageBlock> iterator = getAllPackages(prefix);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock
                    .getAttrResource(name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        if(prefix != null){
            return getAttrResource(null, name);
        }
        return null;
    }
    public ResourceEntry getAttrResource(PackageBlock context, String prefix, String name){
        Iterator<PackageBlock> iterator = getAllPackages(context, prefix);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock
                    .getAttrResource(name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        if(prefix != null){
            return getAttrResource(null, name);
        }
        return null;
    }
    public ResourceEntry getIdResource(PackageBlock context, String prefix, String name){
        Iterator<PackageBlock> iterator = getAllPackages(context, prefix);
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            ResourceEntry resourceEntry = packageBlock
                    .getIdResource(name);
            if(resourceEntry != null){
                return resourceEntry;
            }
        }
        if(prefix != null){
            return getAttrResource(null, name);
        }
        return null;
    }
    public int resolveResourceId(String packageName, String type, String name){
        Iterator<Entry> iterator = getEntries(packageName, type, name);
        if(iterator.hasNext()){
            return iterator.next().getResourceId();
        }
        return 0;
    }
    public Entry getEntry(String packageName, String type, String name){
        Iterator<PackageBlock> iterator = getAllPackages(packageName);
        Entry result = null;
        while (iterator.hasNext()){
            Entry entry = iterator.next().getEntry(type, name);
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
    public Iterator<Entry> getEntries(int resourceId){
        return getEntries(resourceId, true);
    }
    public Iterator<Entry> getEntries(int resourceId, boolean skipNull){

        final int packageId = (resourceId >> 24) & 0xff;
        final int typeId = (resourceId >> 16) & 0xff;
        final int entryId = resourceId & 0xffff;
        return new IterableIterator<PackageBlock, Entry>(getAllPackages(packageId)) {
            @Override
            public Iterator<Entry> iterator(PackageBlock element) {
                if(super.getCountValue() > 0){
                    super.stop();
                    return null;
                }
                return element.getEntries(typeId, entryId, skipNull);
            }
        };
    }
    public Iterator<Entry> getEntries(String packageName, String type, String name){
        return new IterableIterator<PackageBlock, Entry>(getAllPackages(packageName)) {
            @Override
            public Iterator<Entry> iterator(PackageBlock element) {
                if(super.getCountValue() > 0){
                    super.stop();
                    return null;
                }
                return element.getEntries(type, name);
            }
        };
    }
    public Iterator<PackageBlock> getPackages(String packageName){
        return new  FilterIterator<PackageBlock>(getPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                if(packageName != null && packageName.length() > 0){
                    return packageBlock.packageNameMatches(packageName);
                }
                return TableBlock.this == packageBlock.getTableBlock();
            }
        };
    }
    public Iterator<PackageBlock> getPackages(int packageId){
        if(packageId == 0){
            return EmptyIterator.of();
        }
        return new FilterIterator<PackageBlock>(getPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                return packageId == packageBlock.getId();
            }
        };
    }
    public Iterator<PackageBlock> getPackages(){
        return getPackages((PackageBlock) null);
    }
    public Iterator<PackageBlock> getPackages(PackageBlock context){
        PackageBlock current;
        if(context == null){
            current = getCurrentPackage();
        }else {
            current = context;
        }
        Iterator<PackageBlock> iterator = this.iterator();
        if(current == null){
            return iterator;
        }
        return new CombiningIterator<>(
                SingleIterator.of(current),
                new FilterIterator.Except<>(iterator, current)
        );
    }
    public void removePackage(PackageBlock packageBlock){
        getPackageArray().remove(packageBlock);
    }
    public Iterator<PackageBlock> getAllPackages(){
        return getAllPackages((PackageBlock) null);
    }
    public Iterator<PackageBlock> getAllPackages(PackageBlock context, String packageName){
        return new  FilterIterator<PackageBlock>(getAllPackages(context)) {
            @Override
            public boolean test(PackageBlock packageBlock){
                if(packageName != null){
                    return packageBlock.packageNameMatches(packageName);
                }
                return TableBlock.this == packageBlock.getTableBlock();
            }
        };
    }
    public Iterator<PackageBlock> getAllPackages(PackageBlock context){
        return new CombiningIterator<>(getPackages(context),
                new IterableIterator<TableBlock, PackageBlock>(frameworks()) {
                    @Override
                    public Iterator<PackageBlock> iterator(TableBlock element) {
                        return element.getPackages();
                    }
                });
    }
    public Iterator<PackageBlock> getAllPackages(int packageId){
        return new  FilterIterator<PackageBlock>(getAllPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                return packageId == packageBlock.getId();
            }
        };
    }
    public Iterator<PackageBlock> getAllPackages(String packageName){
        return new  FilterIterator<PackageBlock>(getAllPackages()) {
            @Override
            public boolean test(PackageBlock packageBlock){
                if(packageName != null){
                    return packageBlock.packageNameMatches(packageName);
                }
                return TableBlock.this == packageBlock.getTableBlock();
            }
        };
    }
    public boolean removeUnusedSpecs(){
        boolean result = false;
        for(PackageBlock packageBlock : this){
            if (packageBlock.removeUnusedSpecs()) {
                result = true;
            }
        }
        return result;
    }
    public String refreshFull(){
        int sizeOld = getHeaderBlock().getChunkSize();
        StringBuilder message = new StringBuilder();
        boolean appendOnce = false;
        if(getTableStringPool().removeUnusedStrings()){
            message.append("Removed unused table strings");
            appendOnce = true;
        }
        for(PackageBlock packageBlock : this){
            String packageMessage = packageBlock.refreshFull(false);
            if(packageMessage == null){
                continue;
            }
            if(appendOnce){
                message.append("\n");
            }
            message.append("Package: ");
            message.append(packageBlock.getName());
            message.append("\n  ");
            packageMessage = packageMessage.replaceAll("\n", "\n  ");
            message.append(packageMessage);
            appendOnce = true;
        }
        refresh();
        int sizeNew = getHeaderBlock().getChunkSize();
        if(sizeOld != sizeNew){
            if(appendOnce){
                message.append("\n");
            }
            message.append("Table size changed = ");
            message.append(sizeOld);
            message.append(", ");
            message.append(sizeNew);
            appendOnce = true;
        }
        if(appendOnce){
            return message.toString();
        }
        return null;
    }
    private void linkStringsInternal() {
        linkTableStringsInternal(getTableStringPool());
        for(PackageBlock packageBlock : this) {
            packageBlock.linkSpecStringsInternal(packageBlock.getSpecStringPool());
        }
    }
    public void linkTableStringsInternal(TableStringPool tableStringPool){
        for(PackageBlock packageBlock : this){
            packageBlock.linkTableStringsInternal(tableStringPool);
        }
    }
    public List<Entry> resolveReference(int referenceId){
        return resolveReference(referenceId, null);
    }
    public List<Entry> resolveReferenceWithConfig(int referenceId, ResConfig resConfig){
        ReferenceResolver resolver = this.referenceResolver;
        if(resolver == null){
            resolver = new ReferenceResolver(this);
            this.referenceResolver = resolver;
        }
        return resolver.resolveWithConfig(referenceId, resConfig);
    }
    public List<Entry> resolveReference(int referenceId, Predicate<Entry> filter){
        ReferenceResolver resolver = this.referenceResolver;
        if(resolver == null){
            resolver = new ReferenceResolver(this);
            this.referenceResolver = resolver;
        }
        return resolver.resolveAll(referenceId, filter);
    }
    public Iterator<PackageBlock> iterator(){
        return getPackageArray().iterator();
    }
    public PackageBlock get(int index){
        return getPackageArray().get(index);
    }
    public void clear(){
        getPackageArray().destroy();
        getStringPool().clear();
        clearFrameworks();
        refresh();
    }
    public int size(){
        return getPackageArray().size();
    }
    public boolean isEmpty(){
        if(size() == 0){
            return true;
        }
        Iterator<PackageBlock> iterator = getPackages();
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            if(!packageBlock.isEmpty()){
                return false;
            }
        }
        return true;
    }
    public boolean initializeAsEmpty() {
        if(isEmpty()) {
            setNull(true);
            setCurrentPackage(pickOrEmptyPackage());
            return true;
        }
        return false;
    }
    public boolean isMultiPackage() {
        return size() > 1;
    }

    public PackageBlock pickOne(){
        PackageBlock current = getCurrentPackage();
        if(current != null && current.getTableBlock() == this){
            return current;
        }
        return getPackageArray().pickOne();
    }
    public PackageBlock pickOne(int packageId){
        return getPackageArray().pickOne(packageId);
    }
    public PackageBlock pickOrEmptyPackage(){
        PackageBlock packageBlock = this.pickOne();
        if(packageBlock == null){
            packageBlock = this.mEmptyTablePackage;
            if(packageBlock == null){
                packageBlock = PackageBlock.createEmptyPackage(this);
                this.mEmptyTablePackage = packageBlock;
            }
        }
        return packageBlock;
    }
    public void sortPackages(){
        getPackageArray().sort();
    }
    public Iterable<PackageBlock> listPackages(){
        return getPackageArray().listItems();
    }
    public Iterator<ResConfig> getResConfigs(){
        return new MergingIterator<>(new ComputeIterator<>(iterator(),
                PackageBlock::getResConfigs));
    }
    @Override
    public TableStringPool getStringPool() {
        return mTableStringPool;
    }
    @Override
    public ApkFile getApkFile(){
        return mApkFile;
    }
    @Override
    public void setApkFile(ApkFile apkFile){
        this.mApkFile = apkFile;
    }
    @Override
    public TableBlock getTableBlock() {
        return this;
    }

    public TableStringPool getTableStringPool(){
        return mTableStringPool;
    }
    public PackageBlock getPackageBlockById(int pkgId){
        return getPackageArray().getPackageBlockById(pkgId);
    }
    public PackageBlock newPackage(int id, String name){
        PackageBlock packageBlock = getPackageArray().createNext();
        packageBlock.setId(id);
        if(name != null){
            packageBlock.setName(name);
        }
        return packageBlock;
    }
    public PackageBlock getOrCreatePackage(int id, String name){
        PackageBlock packageBlockId = getPackageArray()
                .getPackageBlockById(id);
        if(packageBlockId == null){
            return newPackage(id, name);
        }
        if(name == null){
            return packageBlockId;
        }
        PackageBlock packageBlockName = getPackageArray()
                .getPackageBlockByName(name);
        if(packageBlockId == packageBlockName){
            return packageBlockId;
        }
        return newPackage(id, name);
    }
    public PackageArray getPackageArray(){
        return mPackageArray;
    }
    public void trimConfigSizes(int resConfigSize){
        for(PackageBlock packageBlock : this) {
            packageBlock.trimConfigSizes(resConfigSize);
        }
    }

    private void refreshPackageCount(){
        int count = getPackageArray().size();
        getHeaderBlock().getPackageCount().set(count);
    }
    @Override
    protected void onChunkRefreshed() {
        refreshPackageCount();
    }
    @Override
    protected void onPreRefresh() {
        getPackageArray().removeIf(PackageBlock::isEmpty);
        super.onPreRefresh();
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        if(reader.available() == 0){
            setNull(true);
            return;
        }
        TableHeader tableHeader = getHeaderBlock();
        tableHeader.readBytes(reader);
        if(tableHeader.getChunkType() != ChunkType.TABLE){
            throw new IOException("Not resource table: " + tableHeader);
        }
        boolean stringPoolLoaded = false;
        InfoHeader infoHeader = InfoHeader.read(reader);
        PackageArray packageArray = mPackageArray;
        packageArray.clear();
        while(infoHeader != null && reader.isAvailable()){
            ChunkType chunkType=infoHeader.getChunkType();
            if(chunkType==ChunkType.STRING){
                if(!stringPoolLoaded){
                    mTableStringPool.readBytes(reader);
                    stringPoolLoaded=true;
                }
            }else if(chunkType==ChunkType.PACKAGE){
                PackageBlock packageBlock=packageArray.createNext();
                packageBlock.readBytes(reader);
            }else {
                UnknownChunk unknownChunk=new UnknownChunk();
                unknownChunk.readBytes(reader);
                addChild(unknownChunk);
            }
            infoHeader=reader.readHeaderBlock();
        }
        reader.close();
        linkStringsInternal();
    }

    public void readBytes(File file) throws IOException{
        BlockReader reader=new BlockReader(file);
        super.readBytes(reader);
    }
    public void readBytes(InputStream inputStream) throws IOException{
        BlockReader reader=new BlockReader(inputStream);
        super.readBytes(reader);
    }
    public final int writeBytes(File file) throws IOException{
        if(isNull()){
            throw new IOException("Can NOT save null block");
        }
        File dir=file.getParentFile();
        if(dir!=null && !dir.exists()){
            dir.mkdirs();
        }
        OutputStream outputStream=new FileOutputStream(file);
        int length = super.writeBytes(outputStream);
        outputStream.close();
        return length;
    }
    public int searchResourceIdAlias(int resourceId){
        return resolveStagedAlias(resourceId, 0);
    }
    public int resolveStagedAlias(int stagedResId, int def){
        StagedAliasEntry stagedAliasEntry = getStagedAlias(stagedResId);
        if(stagedAliasEntry != null){
            return stagedAliasEntry.getFinalizedResId();
        }
        return def;
    }
    public StagedAliasEntry getStagedAlias(int stagedResId){
        Iterator<PackageBlock> iterator = getAllPackages();
        while (iterator.hasNext()){
            PackageBlock packageBlock = iterator.next();
            StagedAliasEntry stagedAliasEntry =
                    packageBlock.searchByStagedResId(stagedResId);
            if(stagedAliasEntry != null){
                return stagedAliasEntry;
            }
        }
        return null;
    }
    public List<TableBlock> getFrameWorks(){
        return mFrameWorks;
    }
    public Iterator<TableBlock> frameworks(){
        List<TableBlock> frameworkList = getFrameWorks();
        if(frameworkList.size() == 0){
            return EmptyIterator.of();
        }
        return frameworkList.iterator();
    }
    public boolean isAndroid(){
        PackageBlock packageBlock = pickOne();
        if(packageBlock == null){
            return false;
        }
        return "android".equals(packageBlock.getName())
                && packageBlock.getId() == 0x01;
    }
    public boolean hasFramework(){
        return getFrameWorks().size() != 0;
    }
    public void addFrameworks(Iterator<TableBlock> iterator) {
        List<TableBlock> frameworkList = CollectionUtil.toList(iterator);
        for(TableBlock framework : frameworkList) {
            addFramework(framework);
        }
    }
    public void addFramework(TableBlock frameworkTable){
        if(frameworkTable != null && !containsFramework(frameworkTable)){
            mFrameWorks.add(frameworkTable);
        }
    }
    public boolean containsFramework(TableBlock tableBlock) {
        if(tableBlock == null){
            return false;
        }
        if(this.isSimilarTo(tableBlock)) {
            return true;
        }
        for(TableBlock framework : mFrameWorks) {
            if(framework.containsFramework(tableBlock)) {
                return true;
            }
        }
        return false;
    }
    public void removeFramework(TableBlock tableBlock){
        mFrameWorks.remove(tableBlock);
    }
    public void clearFrameworks(){
        mFrameWorks.clear();
    }
    public PackageBlock parsePublicXml(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        PackageBlock packageBlock = newPackage(0, null);
        packageBlock.parsePublicXml(parser);
        return packageBlock;
    }
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject=new JSONObject();

        jsonObject.put(ARSCLib.NAME_arsc_lib_version, ARSCLib.getVersion());

        jsonObject.put(NAME_packages, getPackageArray().toJson());
        return jsonObject;
    }
    @Override
    public void fromJson(JSONObject json) {
        getPackageArray().fromJson(json.getJSONArray(NAME_packages));
        refresh();
    }
    public void merge(TableBlock tableBlock){
        if(tableBlock == null || tableBlock == this){
            return;
        }
        getStringPool().merge(tableBlock.getStringPool());
        getPackageArray().merge(tableBlock.getPackageArray());
        refresh();
    }
    @Override
    public byte[] getBytes(){
        BytesOutputStream outputStream = new BytesOutputStream(
                getHeaderBlock().getChunkSize());
        try {
            writeBytes(outputStream);
            outputStream.close();
        } catch (IOException ignored) {
        }
        return outputStream.toByteArray();
    }
    public boolean isSimilarTo(TableBlock tableBlock) {
        if(tableBlock == this) {
            return true;
        }
        if(tableBlock == null) {
            return false;
        }
        int size = this.size();
        if(size != tableBlock.size()) {
            return false;
        }
        for(int i = 0; i < size; i++) {
            if(!get(i).isSimilarTo(tableBlock.get(i))){
                return false;
            }
        }
        return true;
    }
    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(": packages = ");
        builder.append(mPackageArray.size());
        builder.append(", size = ");
        builder.append(getHeaderBlock().getChunkSize());
        builder.append(" bytes");
        return builder.toString();
    }

    public static TableBlock load(File file) throws IOException{
        return load(new FileInputStream(file));
    }
    public static TableBlock load(InputStream inputStream) throws IOException{
        TableBlock tableBlock=new TableBlock();
        tableBlock.readBytes(inputStream);
        return tableBlock;
    }
    public static TableBlock createEmpty() {
        TableBlock tableBlock = new TableBlock();
        tableBlock.initializeAsEmpty();
        return tableBlock;
    }

    public static boolean isResTableBlock(InputStream inputStream){
        try {
            HeaderBlock headerBlock= BlockReader.readHeaderBlock(inputStream);
            return isResTableBlock(headerBlock);
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isResTableBlock(BlockReader blockReader){
        if(blockReader==null){
            return false;
        }
        try {
            HeaderBlock headerBlock = blockReader.readHeaderBlock();
            return isResTableBlock(headerBlock);
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isResTableBlock(HeaderBlock headerBlock){
        if(headerBlock==null){
            return false;
        }
        ChunkType chunkType=headerBlock.getChunkType();
        return chunkType==ChunkType.TABLE;
    }
    public static final String FILE_NAME = ObjectsUtil.of("resources.arsc");
    public static final String FILE_NAME_JSON = ObjectsUtil.of("resources.arsc.json");

    private static final String NAME_packages = ObjectsUtil.of("packages");
    public static final String NAME_styled_strings = ObjectsUtil.of("styled_strings");

    public static final String JSON_FILE_NAME = ObjectsUtil.of("resources.arsc.json");
    public static final String DIRECTORY_NAME = ObjectsUtil.of("resources");

    public static final String RES_JSON_DIRECTORY_NAME = ObjectsUtil.of("res-json");
    public static final String RES_FILES_DIRECTORY_NAME = ObjectsUtil.of("res-files");

    public static final String ATTR_null_table = ObjectsUtil.of("null-table");
}
