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

import com.reandroid.common.ArraySupplier;
import com.reandroid.dex.sections.DexContainerBlock;
import com.reandroid.dex.sections.DexLayoutBlock;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.ArraySupplierIterator;

import java.util.Iterator;
import java.util.List;

class DexFileLayoutController implements DexContainerBlock.LayoutBlockChangedListener,
        ArraySupplier<DexLayout>, Iterable<DexLayout> {

    private final DexFile dexFile;
    private final List<DexLayout> dexLayoutList;
    private boolean sortRequired;

    public DexFileLayoutController(DexFile dexFile) {
        this.dexFile = dexFile;
        this.dexLayoutList = new ArrayCollection<>();
    }

    @Override
    public Iterator<DexLayout> iterator() {
        return ArraySupplierIterator.of(this);
    }
    @Override
    public DexLayout get(int i) {
        return get(getContainerBlock().get(i));
    }
    @Override
    public int getCount() {
        return getContainerBlock().size();
    }

    private DexLayout get(DexLayoutBlock layoutBlock) {
        if (layoutBlock == null) {
            return null;
        }
        int index = layoutBlock.getIndex();
        List<DexLayout> dexLayoutList = this.dexLayoutList;
        if (index > 0 && index < dexLayoutList.size()) {
            DexLayout dexLayout = dexLayoutList.get(index);
            if (dexLayout != null && dexLayout.getDexLayoutBlock() == layoutBlock) {
                return dexLayout;
            }
        }
        for (DexLayout layout : dexLayoutList) {
            if (layoutBlock == layout.getDexLayoutBlock()) {
                sortRequired = true;
                return layout;
            }
        }
        DexLayout layout = new DexLayout(dexFile, layoutBlock);
        dexLayoutList.add(layout);
        return layout;
    }
    private DexContainerBlock getContainerBlock() {
        return dexFile.getContainerBlock();
    }
    public void refreshController() {
        if (!sortRequired) {
            return;
        }
        DexContainerBlock containerBlock = getContainerBlock();
        List<DexLayout> dexLayoutList = this.dexLayoutList;
        for (int i = 0; i < dexLayoutList.size(); i++) {
            DexLayout layout = dexLayoutList.get(i);
            if (!containerBlock.containsExact(layout.getDexLayoutBlock())) {
                dexLayoutList.remove(i);
                i --;
            }
        }
        java.util.Collections.sort(dexLayoutList, (layout1, layout2) -> CompareUtil.compare(layout1.getIndex(),
                layout2.getIndex()));
    }

    @Override
    public void onLayoutRemoved(DexLayoutBlock layoutBlock) {
        List<DexLayout> dexLayoutList = this.dexLayoutList;
        int size = dexLayoutList.size();
        for (int i = 0; i < size; i++) {
            DexLayout layout = dexLayoutList.get(i);
            if (layout.getDexLayoutBlock() == layoutBlock) {
                dexLayoutList.remove(i);
                break;
            }
        }
    }
    @Override
    public void onLayoutAdded(DexLayoutBlock layoutBlock) {
        this.get(layoutBlock);
    }
    @Override
    public void onLayoutsCleared() {
        this.dexLayoutList.clear();
    }
}
