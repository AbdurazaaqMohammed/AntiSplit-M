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
package com.reandroid.dex.ins;

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.item.BlockItem;
import com.reandroid.utils.CompareUtil;

public class HandlerOffset extends BlockItem implements Comparable<HandlerOffset>{

    private InsTryItem mTryItem;

    HandlerOffset() {
        super(8);
    }

    public int getIdx() {
        InsTryItem tryItem = getTryItem();
        if (tryItem != null) {
            return tryItem.getIndex();
        }
        return getIndex();
    }
    public int getStartAddress() {
        return Block.getInteger(getBytesInternal(), 0);
    }
    public void setStartAddress(int value) {
        Block.putInteger(getBytesInternal(), 0, value);
    }
    public int getCatchCodeUnit() {
        return Block.getShortUnsigned(getBytesInternal(), 4);
    }
    public void setCatchCodeUnit(int value) {
        Block.putShort(getBytesInternal(), 4, value);
    }
    public int getOffset() {
        return Block.getShortUnsigned(getBytesInternal(), 6);
    }
    public void setOffset(int value) {
        Block.putShort(getBytesInternal(), 6, value);
    }

    InsTryItem getTryItem() {
        return mTryItem;
    }
    void setTryItem(InsTryItem tryItem) {
        this.mTryItem = tryItem;
    }

    void removeSelf(){
        this.mTryItem = null;
        HandlerOffsetArray offsetArray = getParentInstance(HandlerOffsetArray.class);
        if(offsetArray != null){
            offsetArray.remove(this);
        }
    }
    @Override
    public int compareTo(HandlerOffset handlerOffset) {
        if(handlerOffset == null){
            return 0;
        }
        InsTryItem tryItem = getTryItem();
        if(tryItem == null){
            throw new NullPointerException("Unlinked handler offset: " + this.toString());
        }
        InsTryItem other = handlerOffset.getTryItem();
        if(other == null){
            throw new NullPointerException("Unlinked handler offset: " + handlerOffset.toString());
        }
        return CompareUtil.compare(tryItem.getIndex(), other.getIndex());
    }
    public void merge(HandlerOffset handlerOffset){
        setBytes(handlerOffset);
    }

    @Override
    public String toString() {
        return "(" + getIndex() + " : start=" + getStartAddress()
                + ", catch=" + getCatchCodeUnit()
                + ", offset=" + getOffset() + ")";
    }
}
