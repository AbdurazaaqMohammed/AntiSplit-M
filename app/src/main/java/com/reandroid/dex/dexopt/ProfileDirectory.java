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

import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.dex.model.DexFile;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.Crc32;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.io.FileUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class ProfileDirectory implements LinkableProfileItem, Closeable {

    private ProfileDataFile mProfileFile;
    private ProfileMetadataFile mMetadataFile;

    public ProfileDirectory() {
    }

    public boolean hasProfile() {
        return getProfileFile() != null;
    }
    public ProfileDataFile getProfileFile() {
        return mProfileFile;
    }
    public void setProfileFile(ProfileDataFile profileFile) {
        this.mProfileFile = profileFile;
    }
    public ProfileMetadataFile getMetadataFile() {
        return mMetadataFile;
    }
    public void setMetadataFile(ProfileMetadataFile metadataFile) {
        this.mMetadataFile = metadataFile;
    }
    public DexProfileData getProfile(String name) {
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            return profile.get(name);
        }
        return null;
    }
    public ProfileMetadata getMetadata(String name) {
        ProfileMetadataFile metadata = getMetadataFile();
        if (metadata != null) {
            return metadata.get(name);
        }
        return null;
    }
    public void removeIfName(Predicate<String> predicate) {
        ProfileFile profileFile = getProfileFile();
        if (profileFile != null) {
            profileFile.removeIfName(predicate);
        }
        profileFile = getMetadataFile();
        if (profileFile != null) {
            profileFile.removeIfName(predicate);
        }
    }
    public void ensureDexFile(String name) {
        ProfileFile profileFile = getProfileFile();
        if (profileFile != null) {
            profileFile.getOrCreate(name);
        }
        profileFile = getMetadataFile();
        if (profileFile != null) {
            profileFile.getOrCreate(name);
        }
    }

    public void readApk(ZipEntryMap apk) throws IOException {
        if (getProfileFile() == null) {
            InputSource source = apk.getInputSource(ProfileFile.PATH_PROF);
            if (source != null) {
                setProfileFile(ProfileDataFile.read(source.openStream()));
            }
        }
        if (getMetadataFile() == null) {
            InputSource source = apk.getInputSource(ProfileFile.PATH_PROFM);
            if (source != null) {
                setMetadataFile(ProfileMetadataFile.read(source.openStream()));
            }
        }
    }
    public void encodeJsonDir(File decodeDir) throws IOException {
        File fileProf = new File(decodeDir, ProfileFile.JSON_NAME_PROF);
        if (fileProf.isFile()) {
            ProfileDataFile profile = getProfileFile();
            if (profile == null) {
                setProfileFile(new ProfileDataFile());
                profile = getProfileFile();
            }
            profile.fromJson(new JSONObject(fileProf));
        }
        File fileProfm = new File(decodeDir, ProfileFile.JSON_NAME_PROFM);
        if (fileProfm.isFile()) {
            ProfileMetadataFile metadata = getMetadataFile();
            if (metadata == null) {
                setMetadataFile(new ProfileMetadataFile());
                metadata = getMetadataFile();
            }
            metadata.fromJson(new JSONObject(fileProfm));
        }
    }
    public void decodeToJsonDir(File decodeDir) throws IOException {
        File fileProf = new File(decodeDir, ProfileFile.JSON_NAME_PROF);
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            profile.toJson().write(fileProf);
        }
        File fileProfm = new File(decodeDir, ProfileFile.JSON_NAME_PROFM);
        ProfileMetadataFile metadata = getMetadataFile();
        if (metadata != null) {
            metadata.toJson().write(fileProfm);
        }
    }

    public void writeTo(File dexOptDir) throws IOException {
        ProfileFile profileFile = getProfileFile();
        if (profileFile != null) {
            profileFile.write(new File(dexOptDir, ProfileFile.NAME_PROF));
        }
        profileFile = getMetadataFile();
        if (profileFile != null) {
            profileFile.write(new File(dexOptDir, ProfileFile.NAME_PROFM));
        }
    }
    public void writeTo(ZipEntryMap zipEntryMap) {
        ProfileFile profileFile = getProfileFile();
        if (profileFile != null) {
            zipEntryMap.add(new ByteInputSource(profileFile.getBytes(), ProfileFile.PATH_PROF));
        }
        profileFile = getMetadataFile();
        if (profileFile != null) {
            zipEntryMap.add(new ByteInputSource(profileFile.getBytes(), ProfileFile.PATH_PROFM));
        }
    }
    public void updateChecksum(ZipEntryMap zipEntryMap) throws IOException {
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            profile.updateChecksum(zipEntryMap);
        }
    }
    public void updateFileChecksum(File file) throws IOException {
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            profile.updateFileChecksum(file);
        }
    }
    public void updateFileChecksum(InputSource inputSource) throws IOException {
        DexProfileData data = getProfile(inputSource.getAlias());
        if (data != null) {
            data.setChecksum(inputSource.getCrc());
        }
    }
    public void updateChecksumInDexDirectory(File dexRoot) throws IOException {
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            Iterator<DexProfileData> iterator = profile.iterator();
            while (iterator.hasNext()) {
                DexProfileData data = iterator.next();
                File file = new File(dexRoot, data.getName());
                if (file.isFile()) {
                    data.setChecksum(Crc32.of(file));
                }
            }
        }
    }
    private void linkUninitialized(InputSource dexInputSource) throws IOException {
        String name = dexInputSource.getAlias();
        if (!isUninitialized(name)) {
            return;
        }
        DexProfileData data = getProfile(name);
        ProfileMetadata metadata = getMetadata(name);
        DexFile dexFile = DexFile.read(dexInputSource.openStream(), SectionType.minimal());
        dexFile.setSimpleName(name);
        if (data != null && !data.isInitialized()) {
            data.update(dexFile);
        }
        if (metadata != null && !metadata.isInitialized()) {
            metadata.update(dexFile);
        }
    }
    private void linkUninitialized(File file) throws IOException {
        String name = file.getName();
        if (!isUninitialized(name)) {
            return;
        }
        DexProfileData data = getProfile(name);
        ProfileMetadata metadata = getMetadata(name);
        DexFile dexFile = DexFile.read(file, SectionType.minimal());
        dexFile.setSimpleName(name);
        if (data != null && !data.isInitialized()) {
            data.update(dexFile);
        }
        if (metadata != null && !metadata.isInitialized()) {
            metadata.update(dexFile);
        }
    }
    private boolean isUninitialized(String name) {
        DexProfileData data = getProfile(name);
        ProfileMetadata metadata = getMetadata(name);
        if (data == null && metadata == null) {
            return false;
        }
        if (data != null && data.isInitialized()) {
            if (metadata == null || metadata.isInitialized()) {
                return false;
            }
        }
        if (metadata != null && metadata.isInitialized()) {
            if (data == null || data.isInitialized()) {
                return false;
            }
        }
        return true;
    }
    public void syncApk(ZipEntryMap zipEntryMap) throws IOException {
        List<InputSource> dexList = listDexFiles(zipEntryMap);
        if (dexList.isEmpty()) {
            return;
        }
        removeIfName(name -> !zipEntryMap.contains(name));
        for (InputSource inputSource : dexList) {
            ensureDexFile(inputSource.getAlias());
            linkUninitialized(inputSource);
            updateFileChecksum(inputSource);
        }
    }
    public void syncDexDirectory(File dexRoot) throws IOException {
        List<File> dexList = FileUtil.listClassesDex(dexRoot);
        if (dexList.isEmpty()) {
            return;
        }
        removeIfName(name -> {
            File file = new File(dexRoot, name);
            return !dexList.contains(file);
        });
        for (File file : dexList) {
            ensureDexFile(file.getName());
            updateFileChecksum(file);
            linkUninitialized(file);
        }
    }
    public void syncSmaliDirectory(File smaliRoot) {
        List<File> smaliList = listSmaliDirectories(smaliRoot);
        if (smaliList.isEmpty()) {
            return;
        }
        removeIfName(name -> {
            String ext = ".dex";
            if (!name.endsWith(ext)) {
                return true;
            }
            name = name.substring(0, name.length() - ext.length());
            File file = new File(smaliRoot, name);
            return !smaliList.contains(file);
        });
        for (File file : smaliList) {
            ensureDexFile(file.getName() + ".dex");
        }
    }

    public void linkApk(ZipEntryMap zipEntryMap) throws IOException {
        List<InputSource> dexList = listDexFiles(zipEntryMap);
        for (InputSource source : dexList) {
            linkDex(source);
        }
    }
    public void linkDex(InputSource inputSource) throws IOException {
        String name = inputSource.getAlias();
        DexProfileData profile = getProfile(name);
        ProfileMetadata metadata = getMetadata(name);
        if (profile == null && metadata == null) {
            return;
        }
        DexFile dexFile = DexFile.read(inputSource.openStream(), SectionType.minimal());
        dexFile.setSimpleName(name);
        if (profile != null) {
            profile.link(dexFile);
        }
        if (metadata != null) {
            metadata.link(dexFile);
        }
        dexFile.close();
    }
    public void updateDexDirectory(File dexRoot) throws IOException {
        List<File> dexList = FileUtil.listClassesDex(dexRoot);
        for (File file : dexList) {
            updateDexFile(file);
        }
    }
    public void updateDexFile(File file) throws IOException {
        String name = file.getName();
        DexProfileData profile = getProfile(name);
        ProfileMetadata metadata = getMetadata(name);
        if (profile == null && metadata == null) {
            return;
        }
        DexFile dexFile = DexFile.read(file, SectionType.minimal());
        dexFile.setSimpleName(name);
        if (profile != null) {
            profile.update(dexFile);
        }
        if (metadata != null) {
            metadata.update(dexFile);
        }
        dexFile.close();
    }
    @Override
    public void link(DexFile dexFile) {
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            profile.link(dexFile);
        }
        ProfileMetadataFile metadata = getMetadataFile();
        if (metadata != null) {
            metadata.link(dexFile);
        }
    }

    @Override
    public void update(DexFile dexFile) {
        ProfileDataFile profile = getProfileFile();
        if (profile != null) {
            profile.update(dexFile);
        }
        ProfileMetadataFile metadata = getMetadataFile();
        if (metadata != null) {
            metadata.update(dexFile);
        }
    }
    public void refresh() {
        ProfileFile profileFile = getProfileFile();
        if (profileFile != null) {
            profileFile.refresh();
        }
        profileFile = getMetadataFile();
        if (profileFile != null) {
            profileFile.refresh();
        }
    }

    @Override
    public void close() {
        setProfileFile(null);
        setMetadataFile(null);
    }
    @Override
    public String toString() {
        return "DexOpt{" +
                "profile=" + mProfileFile +
                ", metadata=" + mMetadataFile +
                '}';
    }

    private static List<InputSource> listDexFiles(ZipEntryMap zipEntryMap) {
        List<InputSource> results = CollectionUtil.toList(zipEntryMap.iteratorWithPath(
                path -> DexFile.getDexFileNumber(path) >= 0));
        java.util.Collections.sort(results, (i1, i2) -> CompareUtil.compare(DexFile.getDexFileNumber(i1.getAlias()),
                DexFile.getDexFileNumber(i2.getAlias())));
        return results;
    }
    private static List<File> listSmaliDirectories(File root) {
        List<File> results = new ArrayCollection<>();
        if (!root.isDirectory()) {
            return results;
        }
        File[] files = root.listFiles();
        if (files == null) {
            return results;
        }
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            String name = file.getName() + ".dex";
            if (DexFile.getDexFileNumber(name) >= 0) {
                results.add(file);
            }
        }
        return results;
    }
}
