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

import com.reandroid.arsc.container.BlockList;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.common.BytesOutputStream;
import com.reandroid.dex.common.FullRefresh;
import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.header.DexHeader;
import com.reandroid.dex.header.DexVersion;
import com.reandroid.utils.collection.ArrayUtil;
import com.reandroid.utils.collection.ComputeIterator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;


public class DexContainerBlock extends BlockList<DexLayoutBlock> implements
        FullRefresh, Iterable<DexLayoutBlock> {

    private LayoutBlockChangedListener layoutBlockChangedListener;
    private Object mTag;
    private String mSimpleName;

    private boolean mReading;

    public DexContainerBlock() {
        super();
    }

    public boolean isMultiLayout() {
        return size() > 1;
    }
    public boolean hasMultiLayoutVersion() {
        return getVersion() >= DexVersion.MinimumMultiLayoutVersion;
    }
    public boolean isEmpty() {
        for (DexLayoutBlock layoutBlock : this) {
            if (!layoutBlock.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int clearUnused() {
        int result = 0;
        while (true) {
            clearUsageTypes();
            refresh();
            int unused = clearUnusedItems();
            if (unused == 0) {
                break;
            }
            result += unused;
        }
        return result;
    }
    private int clearUnusedItems() {
        int result = 0;
        for(DexLayoutBlock dexLayout : this) {
            result += dexLayout.getSectionList().clearUnused();
        }
        return result;
    }
    private void clearUsageTypes() {
        for (DexLayoutBlock layoutBlock : this) {
            for (Section<?> section : layoutBlock.getSectionList()) {
                section.clearUsageTypes();
            }
        }
    }
    public void clearEmptyLayouts() {
        removeIf(DexLayoutBlock::isEmpty);
    }
    public void clearEmptySections() {
        for (DexLayoutBlock layoutBlock : this) {
            layoutBlock.clearEmptySections();
        }
    }
    public void clear() {
        clearChildes();
    }

    @Override
    public boolean add(DexLayoutBlock item) {
        boolean added = super.add(item);
        notifyAdded(item);
        return added;
    }
    @Override
    public void add(int index, DexLayoutBlock item) {
        super.add(index, item);
        notifyAdded(item);
    }
    @Override
    public void clearChildes() {
        super.clearChildes();
        notifyLayoutsCleared();
    }

    @Override
    public DexLayoutBlock createNext() {
        DexLayoutBlock layoutBlock = new DexLayoutBlock();
        add(layoutBlock);
        onCreatedNew(layoutBlock);
        return layoutBlock;
    }
    @Override
    public void ensureSize(int size) {
        while (size() < size) {
            createNextDefault();
        }
    }
    public DexLayoutBlock createNextDefault() {
        DexLayoutBlock layoutBlock = new DexLayoutBlock();
        add(layoutBlock);
        onCreatedNew(layoutBlock);
        layoutBlock.initialize(SectionType.getR8Order());
        transferSharedToLast();
        return layoutBlock;
    }
    private void onCreatedNew(DexLayoutBlock layoutBlock) {
        if (layoutBlock.getIndex() == 0) {
            return;
        }
        fixMinimumVersion();
        layoutBlock.setVersion(get(0).getVersion());
    }

    private void fixMinimumVersion() {
        if (isMultiLayout()) {
            int minVersion = DexVersion.MinimumMultiLayoutVersion;
            for (DexLayoutBlock layoutBlock : this) {
                if (layoutBlock.getVersion() < minVersion) {
                    layoutBlock.setVersion(minVersion);
                }
            }
        }
    }

    @Override
    public void refreshFull() {
        int[] checkSums = getCheckSums();
        boolean multiContainer = checkSums.length > 1;
        int repeat = size();
        for (int i = 0; i < repeat; i++) {
            refreshFullLayouts();
            if (multiContainer) {
                int[] changed = getCheckSums();
                if (ArrayUtil.areEqual(checkSums, changed)) {
                    break;
                }
                refresh();
                checkSums = changed;
                changed = getCheckSums();
                if (ArrayUtil.areEqual(checkSums, changed)) {
                    break;
                }
                checkSums = changed;
            }
        }
    }
    private int[] getCheckSums() {
        int size = size();
        int[] results = new int[size];
        for (int i = 0; i < size; i++) {
            DexLayoutBlock layoutBlock = get(i);
            results[i] = layoutBlock.getHeader().checksum.getValue();
        }
        return results;
    }
    private void refreshFullLayouts() {
        clearUnused();
        clearEmptyLayouts();
        for (DexLayoutBlock layoutBlock : this) {
            layoutBlock.refreshFull();
        }
    }

    @Override
    protected void onPreRefresh() {
        super.onPreRefresh();
        fixMinimumVersion();
    }
    @Override
    protected void onRefreshed() {
        super.onRefreshed();
    }

    @Override
    public void onPreRemove(DexLayoutBlock item) {
        super.onPreRemove(item);
        notifyRemoved(item);
    }

    private void notifyAdded(DexLayoutBlock layoutBlock) {
        LayoutBlockChangedListener listener = this.layoutBlockChangedListener;
        if (listener != null) {
            listener.onLayoutAdded(layoutBlock);
        }
    }
    private void notifyRemoved(DexLayoutBlock layoutBlock) {
        LayoutBlockChangedListener listener = this.layoutBlockChangedListener;
        if (listener != null) {
            listener.onLayoutRemoved(layoutBlock);
        }
    }
    private void notifyLayoutsCleared() {
        LayoutBlockChangedListener listener = this.layoutBlockChangedListener;
        if (listener != null) {
            listener.onLayoutsCleared();
        }
    }

    @Override
    protected void onReadBytes(BlockReader reader) throws IOException {
        readChildes(reader);
    }
    @Override
    public void readChildes(BlockReader reader) throws IOException {
        readBytes(reader, null);
    }
    public void readBytes(BlockReader reader, Predicate<SectionType<?>> filter) throws IOException {
        mReading = true;
        int size = size();
        for (int i = 0; i < size; i++) {
            DexLayoutBlock layoutBlock = get(i);
            layoutBlock.readBytes(reader, filter);
        }
        while (reader.isAvailable()) {
            createNext().readBytes(reader, filter);
        }
        transferSharedToLast();
        mReading = false;
    }
    private void transferSharedToLast() {
        int size = size() - 1;
        if (size < 1) {
            return;
        }
        DexLayoutBlock last = get(size);
        for (int i = 0; i < size; i ++) {
            last.transferSharedSectionsFrom(get(i));
        }
    }

    public int getFileSize() {
        int size = 0;
        for (DexLayoutBlock layoutBlock : this) {
            size += layoutBlock.getFileSize();
        }
        return size;
    }
    @Override
    public byte[] getBytes() {
        BytesOutputStream outputStream = new BytesOutputStream(getFileSize());
        try {
            writeBytes(outputStream);
            outputStream.close();
        } catch (IOException ignored) {
        }
        return outputStream.toByteArray();
    }

    public <T1 extends SectionItem> Iterator<Section<T1>> getSections(SectionType<T1> sectionType) {
        return ComputeIterator.of(getSectionLists(), sectionList -> sectionList.getSection(sectionType));
    }
    public Iterator<SectionList> getSectionLists() {
        return ComputeIterator.of(iterator(), DexLayoutBlock::getSectionList);
    }

    public boolean isReading() {
        return mReading;
    }

    public int getVersion() {
        DexLayoutBlock first = getFirst();
        if (first != null) {
            return first.getVersion();
        }
        return 0;
    }
    public void setVersion(int version) {
        for (DexLayoutBlock layoutBlock : this) {
            layoutBlock.setVersion(version);
        }
    }
    public void combineFrom(DexLayoutBlock layoutBlock) {
        combineFrom(MergeOptions.DEFAULT, layoutBlock);
    }
    public void combineFrom(MergeOptions options, DexLayoutBlock layoutBlock) {
        if (containsExact(layoutBlock) || layoutBlock.isEmpty()) {
            return;
        }
        DexLayoutBlock destination = getLast();
        if (destination == null || !destination.isEmpty()) {
            destination = createNextDefault();
        }
        destination.merge(options, layoutBlock);
        fixMinimumVersion();
    }
    public void combineFrom(DexContainerBlock containerBlock) {
        for (DexLayoutBlock layoutBlock : containerBlock) {
            combineFrom(layoutBlock);
        }
    }
    public void combineFrom(MergeOptions options, DexContainerBlock containerBlock) {
        for (DexLayoutBlock layoutBlock : containerBlock) {
            combineFrom(options, layoutBlock);
        }
    }
    public String getSimpleName() {
        return mSimpleName;
    }
    public void setSimpleName(String simpleName) {
        this.mSimpleName = simpleName;
    }
    public Object getTag() {
        return mTag;
    }
    public void setTag(Object tag) {
        this.mTag = tag;
    }

    public void setLayoutBlockChangedListener(LayoutBlockChangedListener layoutBlockChangedListener) {
        this.layoutBlockChangedListener = layoutBlockChangedListener;
    }

    public static DexContainerBlock createDefault() {
        DexContainerBlock containerBlock = new DexContainerBlock();
        containerBlock.createNextDefault();
        return containerBlock;
    }

    public static boolean isDexFile(InputStream inputStream) {
        try {
            return isDexFile(DexHeader.readHeader(inputStream));
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isDexFile(File file){
        if(file == null || !file.isFile()){
            return false;
        }
        try {
            return isDexFile(DexHeader.readHeader(file));
        } catch (IOException ignored) {
            return false;
        }
    }
    private static boolean isDexFile(DexHeader dexHeader){
        if(dexHeader == null){
            return false;
        }
        if(!dexHeader.magic.isValid()){
            return false;
        }
        int version = dexHeader.getVersion();
        return version > 0 && version < 1000;
    }

    public interface LayoutBlockChangedListener {
        void onLayoutRemoved(DexLayoutBlock layoutBlock);
        void onLayoutAdded(DexLayoutBlock layoutBlock);
        void onLayoutsCleared();
    }
}
