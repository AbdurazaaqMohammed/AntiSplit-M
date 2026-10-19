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
package com.reandroid.dex.dexopt;

import com.reandroid.arsc.base.Creator;
import com.reandroid.arsc.container.CountedBlockList;
import com.reandroid.arsc.item.IntegerReference;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class ProfileDataBody extends ProfileBody {

    private final CountedBlockList<ProfileDataHeader> headerList;
    private final CountedBlockList<DexProfileData> dataList;

    public ProfileDataBody(ProfileVersion version,
                           IntegerReference count,
                           IntegerReference sizeUncompressed,
                           IntegerReference sizeCompressed) {
        super(2, version, sizeUncompressed, sizeCompressed);

        this.headerList = new CountedBlockList<>(ProfileDataHeader.CREATOR, count);

        Creator<DexProfileData> creator = new Creator<DexProfileData>() {
            @Override
            public DexProfileData newInstance() {
                throw new RuntimeException("Must call newInstanceAt");
            }
            @Override
            public DexProfileData newInstanceAt(int index) {
                if (headerList.size() <= index) {
                    headerList.setSize(index + 1);
                }
                return new DexProfileDataVersionP(headerList.get(index));
            }
        };

        this.dataList = new CountedBlockList<>(creator, count);

        addChild(0, headerList);
        addChild(1, dataList);
    }

    @Override
    public int size() {
        return dataList.size();
    }
    @Override
    public void setSize(int size) {
        dataList.setSize(size);
    }
    @Override
    public DexProfileData get(int i) {
        return dataList.get(i);
    }
    @Override
    public DexProfileData get(String name) {
        return (DexProfileDataVersionP) super.get(name);
    }
    @Override
    public Iterator<DexProfileData> iterator() {
        return dataList.clonedIterator();
    }
    @Override
    public boolean removeIfName(Predicate<String> predicate) {
        return dataList.removeIf(dexProfileData -> predicate.test(dexProfileData.getName()));
    }
    @Override
    public boolean removeData(int i) {
        return dataList.remove(i) != null;
    }
}
