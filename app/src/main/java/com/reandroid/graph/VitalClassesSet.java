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
package com.reandroid.graph;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ResFile;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.dex.base.UsageMarker;
import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.model.DexClass;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.io.IOUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.reandroid.utils.function.Predicate;

public class VitalClassesSet extends BaseApkModuleProcessor implements Predicate<TypeKey> {

    private final ApkBuildOption buildOption;
    private final Set<TypeKey> mainClasses;
    private final Set<TypeKey> sourceStringClasses;
    private final Set<String> elementNameSuffix;

    private boolean scanned;

    public VitalClassesSet(ApkBuildOption buildOption, ApkModule apkModule, DexClassRepository classRepository) {
        super(apkModule, classRepository);
        this.buildOption = buildOption;
        this.mainClasses = new HashSet<>();
        this.sourceStringClasses = new HashSet<>();
        this.elementNameSuffix = new HashSet<>();
    }

    public Iterator<TypeKey> getMainClasses() {
        return mainClasses.iterator();
    }
    public Iterator<TypeKey> getDexSourceStringClasses() {
        return sourceStringClasses.iterator();
    }
    @Override
    public boolean test(TypeKey typeKey) {
        return mainClasses.contains(typeKey);
    }
    public boolean containsSourceString(TypeKey typeKey) {
        return sourceStringClasses.contains(typeKey);
    }
    public void updateSourceStrings() {
        Set<TypeKey> sourceStringClasses = this.sourceStringClasses;
        if(sourceStringClasses.isEmpty()) {
            return;
        }
        DexClassRepository repository = getClassRepository();
        Iterator<TypeKey> iterator = ArrayIterator.of(sourceStringClasses.toArray());
        SectionType<StringId> sectionType = SectionType.STRING_ID;
        while (iterator.hasNext()) {
            TypeKey typeKey = iterator.next();
            StringKey stringKey = new StringKey(typeKey.getSourceName());
            if(!repository.contains(sectionType, stringKey)) {
                sourceStringClasses.remove(typeKey);
            }
        }
    }

    @Override
    public void apply() {
        if(scanned) {
            return;
        }
        scanned = true;
        debug("Scanning ...");
        scanOnXml();
        scanUsedByNative();
        scanUsedByMetaInfServices();
        scanRequiredByUser();
        scanOthers();
        scanOnResourceStrings();
        scanOnDexStrings();
        verbose("Classes: " + mainClasses.size());
    }

    private void scanOnResourceStrings() {
        debug("Searching on resource strings ...");
        TableBlock tableBlock = getApkModule().getTableBlock();
        Iterator<ResourceEntry> iterator = tableBlock.getLocalResources("string");
        ResConfig def = ResConfig.getDefault();
        while (iterator.hasNext()) {
            ResourceEntry resourceEntry = iterator.next();
            Entry entry = resourceEntry.get(def);
            if(entry != null) {
                String value = entry.getValueAsString();
                if(maybeValidSourceType(value)) {
                    addType(TypeKey.parse(value));
                }
            }
        }
    }
    private void scanOnDexStrings() {
        if(!getBuildOption().isProcessClassNamesOnStrings()) {
            return;
        }
        debug("Searching on dex strings ...");
        Set<TypeKey> sourceStringClasses = this.sourceStringClasses;
        DexClassRepository repository = getClassRepository();
        Iterator<StringId> iterator = repository.getItems(SectionType.STRING_ID);
        while (iterator.hasNext()) {
            StringId stringId = iterator.next();
            if(stringId.containsUsage(UsageMarker.USAGE_INSTRUCTION) ||
                    stringId.containsUsage(UsageMarker.USAGE_STATIC_VALUES)) {
                String str = stringId.getString();
                if(maybeValidSourceType(str)) {
                    TypeKey typeKey = TypeKey.parse(str);
                    if(repository.containsClass(typeKey)) {
                        sourceStringClasses.add(typeKey);
                    }
                }
            }
        }
    }
    private void scanOnXml() {
        debug("Scanning xml ...");
        scanOnXml(getApkModule().getAndroidManifest());
        scanOnResourceXmlFiles();
        scanElementSuffix();
    }
    private void scanElementSuffix() {
        Set<String> elementNameSuffix = this.elementNameSuffix;
        Iterator<DexClass> iterator = getClassRepository().getDexClasses(
                typeKey -> elementNameSuffix.contains(typeKey.getSimpleName()));
        while (iterator.hasNext()) {
            addType(iterator.next().getKey());
        }
        elementNameSuffix.clear();
    }
    private void scanOnResourceXmlFiles() {
        List<ResFile> resFileList = getApkModule().listResFiles();
        debug("Searching required classes on res files: " + resFileList.size());
        for(ResFile resFile : resFileList) {
            scanOnXml(resFile.getResXmlDocument());
        }
    }
    private void scanOnXml(ResXmlDocument resXmlDocument) {
        if(resXmlDocument == null) {
            return;
        }
        Iterator<String> iterator = resXmlDocument.getStringPool().getStrings();
        while (iterator.hasNext()) {
            addType(TypeKey.parse(iterator.next()));
        }
        loadElementNames(resXmlDocument);
    }
    private void loadElementNames(ResXmlDocument resXmlDocument) {
        if(resXmlDocument != null) {
            Set<String> elementNameSuffix = this.elementNameSuffix;
            Iterator<ResXmlElement> iterator = resXmlDocument.recursiveElements();
            while (iterator.hasNext()) {
                ResXmlElement element = iterator.next();
                elementNameSuffix.add(element.getName(false));
            }
        }
    }
    private void scanUsedByNative() {
        debug("Searching used by native ...");
        Set<TypeKey> mainClasses = this.mainClasses;
        Iterator<DexClass> iterator = getClassRepository().getDexClasses(
                typeKey -> !mainClasses.contains(typeKey));
        while (iterator.hasNext()) {
            DexClass dexClass = iterator.next();
            if(dexClass.usesNative()) {
                mainClasses.add(dexClass.getKey());
            }
        }
    }
    private void scanUsedByMetaInfServices() {
        debug("Searching classes on META-INF/services/ ...");
        Iterator<InputSource> iterator = getZipEntryMap()
                .withinDirectory("META-INF/services/");
        while (iterator.hasNext()) {
            scanUsedByMetaInfServices(iterator.next());
        }
    }
    private void scanUsedByMetaInfServices(InputSource inputSource) {
        addType(TypeKey.parse(inputSource.getSimpleName()));
        String content;
        try {
            content = IOUtil.readUtf8(inputSource.openStream());
        } catch (IOException exception) {
            warn("Failed to process '" + inputSource.getAlias() + "', error = "
                    + exception.getMessage());
            return;
        }
        String[] lines = StringsUtil.split(content, '\n', true);
        for(String line : lines) {
            line = line.trim();
            addType(TypeKey.parse(line));
        }
    }
    private void scanOthers() {
        scanImplSuffix();
    }
    private void scanImplSuffix() {
        // FIXME: this is mainly to keep Landroidx/work/impl/WorkDatabase_Impl;
        // TODO: find universal rule
        this.keepClasses(typeKey -> typeKey.getTypeName().endsWith("_Impl;"));
    }
    private void scanRequiredByUser() {
        keepClasses(getBuildOption().getKeepClasses());
    }
    public void keepClasses(Predicate<? super TypeKey> filter) {
        if(filter == null) {
            return;
        }
        Iterator<DexClass> iterator = getClassRepository().getDexClasses(filter);
        while (iterator.hasNext()) {
            addType(iterator.next().getKey());
        }
    }
    private void addType(TypeKey typeKey) {
        if(typeKey == null) {
            return;
        }
        typeKey = typeKey.getDeclaring();
        Set<TypeKey> mainClasses = this.mainClasses;
        if(!mainClasses.contains(typeKey)) {
            if(getClassRepository().containsClass(typeKey)) {
                mainClasses.add(typeKey);
            }
        }
    }

    private ApkBuildOption getBuildOption() {
        return buildOption;
    }

    private boolean maybeValidSourceType(String type) {
        if(StringsUtil.isEmpty(type)) {
            return false;
        }
        int length = type.length();
        if(length < 3 || type.indexOf('.') < 0) {
            return false;
        }
        for(int i = 0; i < length; i++) {
            if(!isValidSimpleName(type.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    private boolean isValidSimpleName(char ch) {
        switch (ch) {
            case ' ':
            case '\n':
            case '\r':
            case '\t':
            case '(':
            case ')':
            case '[':
            case ']':
            case '<':
            case '>':
            case ',':
            case '/':
            case '\\':
            case '!':
            case '@':
            case '#':
            case '%':
            case '^':
            case '&':
            case '*':
            case '+':
            case '=':
            case '|':
            case '\'':
            case '"':
            case ';':
            case ':':
            case '?':
                return false;
            default:
                return true;
        }
    }
}
