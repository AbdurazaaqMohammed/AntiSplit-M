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
package com.reandroid.identifiers;

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.io.FileUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TableIdentifier{
    private final List<PackageIdentifier> mPackages;
    private final Map<String, PackageIdentifier> mNameMap;
    private boolean mCaseInsensitive;
    public TableIdentifier() {
        this.mPackages = new ArrayList<>();
        this.mNameMap = new HashMap<>();
        this.mCaseInsensitive = FileUtil.isCaseInsensitiveFileSystem();
    }

    public void load(TableBlock tableBlock){
        if(tableBlock == null){
            return;
        }
        for(PackageBlock packageBlock : tableBlock.listPackages()){
            load(packageBlock);
        }
    }
    public PackageIdentifier load(PackageBlock packageBlock){
        PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.load(packageBlock);
        add(packageIdentifier);
        mNameMap.put(packageIdentifier.getName(), packageIdentifier);
        return packageIdentifier;
    }
    public void setTableBlock(TableBlock tableBlock){
        for(PackageBlock packageBlock : tableBlock.listPackages()){
            int id = packageBlock.getId();
            for(PackageIdentifier pi : getPackages()){
                if(pi.getId() == id){
                    pi.setPackageBlock(packageBlock);
                }
            }
        }
    }
    public void writeAllPublicXml(File resourcesDirectory) throws IOException {
        List<PackageIdentifier> packageList = getPackages();
        int index = 0;
        for(PackageIdentifier pi : getPackages()){
            index ++;
            String packageDir;
            PackageBlock packageBlock = pi.getPackageBlock();
            if(packageBlock != null){
                packageDir = packageBlock.buildDecodeDirectoryName();
            }else {
                packageDir = PackageBlock.DIRECTORY_NAME_PREFIX
                        + StringsUtil.formatNumber(index, packageList.size());
            }
            File file = toPublicXmlFile(resourcesDirectory, packageDir);
            pi.writePublicXml(file);
        }
    }
    private File toPublicXmlFile(File resourcesDirectory, String packageDir){
        File file = new File(resourcesDirectory, packageDir);
        file = new File(file, PackageBlock.RES_DIRECTORY_NAME);
        file = new File(file, PackageBlock.VALUES_DIRECTORY_NAME);
        file = new File(file, PackageBlock.PUBLIC_XML);
        return file;
    }
    public void loadPublicXml(Collection<File> pubXmlFileList) throws IOException {
        for(File file : pubXmlFileList){
            try {
                loadPublicXml(file);
            } catch (XmlPullParserException ex) {
                throw new IOException(ex);
            }
        }
    }
    public PackageIdentifier loadPublicXml(File file) throws IOException, XmlPullParserException {
        PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.loadPublicXml(file);
        add(packageIdentifier);
        packageIdentifier.setTag(file);
        return packageIdentifier;
    }
    public PackageIdentifier loadPublicXml(InputStream inputStream) throws IOException, XmlPullParserException {
        PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.loadPublicXml(inputStream);
        add(packageIdentifier);
        return packageIdentifier;
    }
    public PackageIdentifier loadPublicXml(Reader reader) throws IOException, XmlPullParserException {PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.loadPublicXml(reader);
        add(packageIdentifier);
        return packageIdentifier;
    }
    public PackageIdentifier loadPublicXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        PackageIdentifier packageIdentifier = new PackageIdentifier();
        packageIdentifier.loadPublicXml(parser);
        add(packageIdentifier);
        return packageIdentifier;
    }
    public ResourceIdentifier get(String packageName, String type, String name){
        PackageIdentifier packageIdentifier = mNameMap.get(packageName);
        if(packageIdentifier != null){
            ResourceIdentifier ri = packageIdentifier.getResourceIdentifier(type, name);
            if(ri != null){
                return ri;
            }
        }
        for(PackageIdentifier pi : getPackages()){
            if(Objects.equals(packageName, pi.getName())){
                ResourceIdentifier ri = pi.getResourceIdentifier(type, name);
                if(ri != null){
                    return ri;
                }
            }
        }
        return null;
    }
    public ResourceIdentifier get(String type, String name){
        for(PackageIdentifier pi : getPackages()){
            ResourceIdentifier ri = pi.getResourceIdentifier(type, name);
            if(ri != null){
                return ri;
            }
        }
        return null;
    }
    public int countPackages(){
        return getPackages().size();
    }
    public void add(PackageIdentifier packageIdentifier){
        if(packageIdentifier != null){
            mPackages.add(packageIdentifier);
            packageIdentifier.setCaseInsensitive(isCaseInsensitive());
        }
    }
    public List<PackageIdentifier> getPackages() {
        return mPackages;
    }
    public PackageIdentifier getByTag(Object tag){
        for(PackageIdentifier pi : getPackages()){
            if(Objects.equals(tag, pi.getTag())){
                return pi;
            }
        }
        return null;
    }
    public PackageIdentifier getByPackage(PackageBlock packageBlock){
        for(PackageIdentifier pi : getPackages()){
            if(packageBlock == pi.getPackageBlock()){
                return pi;
            }
        }
        return null;
    }
    public PackageIdentifier get(int packageId){
        for(PackageIdentifier pi : getPackages()){
            if(packageId == pi.getId()){
                return pi;
            }
        }
        return null;
    }
    public List<PackageIdentifier> getAll(String packageName){
        List<PackageIdentifier> results = new ArrayList<>();
        for(PackageIdentifier pi : getPackages()){
            if(Objects.equals(packageName, pi.getName())){
                results.add(pi);
            }
        }
        return results;
    }
    public List<PackageIdentifier> getAll(int packageId){
        List<PackageIdentifier> results = new ArrayList<>();
        for(PackageIdentifier pi : getPackages()){
            if(packageId == pi.getId()){
                results.add(pi);
            }
        }
        return results;
    }
    public void clear(){
        for(PackageIdentifier identifier : getPackages()){
            identifier.clear();
        }
        mPackages.clear();
        mNameMap.clear();
    }
    public int renameSpecs(){
        int result = 0;
        for(PackageIdentifier pi : getPackages()){
            int renamed = pi.renameSpecs();
            result = result + renamed;
        }
        return result;
    }
    public int renameDuplicateSpecs(){
        updateCaseInsensitive(isCaseInsensitive());
        int result = 0;
        for(PackageIdentifier pi : getPackages()){
            int renamed = pi.renameDuplicateSpecs();
            result = result + renamed;
        }
        return result;
    }
    public int renameBadSpecs(){
        int result = 0;
        for(PackageIdentifier pi : getPackages()){
            int renamed = pi.renameBadSpecs();
            result = result + renamed;
        }
        return result;
    }
    public String validateSpecNames(){
        int duplicates = renameDuplicateSpecs();
        int bad = renameBadSpecs();
        if(duplicates == 0 && bad == 0){
            return null;
        }
        return "Spec names validated, duplicates = " + duplicates
                + ", bad = " + bad;
    }
    public boolean isCaseInsensitive(){
        return mCaseInsensitive;
    }
    public void setCaseInsensitive(boolean caseInsensitive){
        mCaseInsensitive = caseInsensitive;
        updateCaseInsensitive(caseInsensitive);
    }
    private void updateCaseInsensitive(boolean caseInsensitive){
        for(PackageIdentifier pi : getPackages()){
            pi.setCaseInsensitive(caseInsensitive);
        }
    }

    @Override
    public String toString(){
        return getClass().getSimpleName()
                + ": packages = "
                + countPackages();
    }
}
