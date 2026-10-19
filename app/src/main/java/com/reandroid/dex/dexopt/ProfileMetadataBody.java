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

public class ProfileMetadataBody extends ProfileBody implements LinkableProfileItem {

    private final CountedBlockList<ProfileMetadataHeaderV1> headerList;
    private final CountedBlockList<ProfileMetadata> dataList;

    public ProfileMetadataBody(ProfileVersion version,
                               IntegerReference count,
                               IntegerReference sizeUncompressed,
                               IntegerReference sizeCompressed) {
        super(2, version, sizeUncompressed, sizeCompressed);

        IntegerReference headerCountReference = new IntegerReference() {
            @Override
            public int get() {
                if (version.get() == ProfileVersion.METADATA_V001_N) {
                    return count.get();
                }
                return 0;
            }
            @Override
            public void set(int value) {
                if (version.get() == ProfileVersion.METADATA_V001_N) {
                    count.set(value);
                }
            }
            @Override
            public String toString() {
                return Integer.toString(get());
            }
        };
        this.headerList = new CountedBlockList<>(ProfileMetadataHeaderV1.CREATOR, headerCountReference);

        Creator<ProfileMetadata> creator = new Creator<ProfileMetadata>() {
            @Override
            public ProfileMetadata newInstance() {
                if (version.get() == ProfileVersion.METADATA_V001_N) {
                    throw new RuntimeException("Must call newInstanceAt");
                }
                return new ProfileMetadataV2();
            }
            @Override
            public ProfileMetadata newInstanceAt(int index) {
                if (version.get() == ProfileVersion.METADATA_V001_N) {
                    if (headerList.size() <= index) {
                        headerList.setSize(index + 1);
                    }
                    return new ProfileMetadataV1(headerList.get(index));
                }
                return new ProfileMetadataV2();
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
    public ProfileMetadata get(int i) {
        return dataList.get(i);
    }
    @Override
    public Iterator<ProfileMetadata> iterator() {
        return dataList.iterator();
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
