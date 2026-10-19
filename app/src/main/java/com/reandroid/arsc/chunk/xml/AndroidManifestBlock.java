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
package com.reandroid.arsc.chunk.xml;

import com.reandroid.app.AndroidManifest;
import com.reandroid.arsc.ApkFile;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.ComputeIterator;
import com.reandroid.xml.XMLPath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

@SuppressWarnings("unused")
public class AndroidManifestBlock extends ResXmlDocument implements AndroidManifest {

    private int mGuessedPackageId;

    public AndroidManifestBlock() {
        super();
        super.getStringPool().setUtf8(false);
    }

    public ResXmlElement getOrCreateNamedElement(XMLPath xmlPath, String value) {
        ResXmlElement element = getNamedElement(xmlPath, value);
        if (element == null) {
            element = newChildElement(xmlPath);
            element.getOrCreateAndroidAttribute(NAME_name, ID_name)
                    .setValueAsString(value);
            ensureAboveApplication(element);
        }
        return element;
    }
    private void ensureAboveApplication(ResXmlElement element) {
        String tag = element.getName();
        ResXmlElement parent = element.getParentElement();
        if (parent != null && !element.equalsName(TAG_application) && parent.equalsName(TAG_manifest)) {
            int i = parent.lastIndexOf(tag);
            if (i < 0) {
                i = parent.lastIndexOf(TAG_application);
            } else if (i == element.getIndex()) {
                return;
            }
            if (i >= 0) {
                parent.moveTo(element, i);
            }
        }
    }
    public ResXmlElement getNamedElement(XMLPath xmlPath, String value) {
        ResXmlAttribute attribute = xmlPath.attribute(ID_name)
                .value(value)
                .alternateValue(value.startsWith(".") ? fullClassName(value) : relativeClassName(value))
                .findFirst(this);
        if (attribute != null) {
            return attribute.getParentElement();
        }
        return null;
    }
    public ApkFile.ApkType guessApkType() {
        if (isSplit()) {
            return ApkFile.ApkType.SPLIT;
        }
        Boolean core = isCoreApp();
        if (core != null && core) {
            return ApkFile.ApkType.CORE;
        }
        if (getMainActivity() != null) {
            return ApkFile.ApkType.BASE;
        }
        return null;
    }
    public Boolean isCoreApp() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlAttribute attribute = manifest.searchAttributeByName(NAME_coreApp);
        if (attribute == null) {
            return null;
        }
        if (attribute.getValueType() != ValueType.BOOLEAN) {
            return null;
        }
        return attribute.getValueAsBoolean();
    }
    public boolean isSplit() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return false;
        }
        return manifest.searchAttributeByName(NAME_split) != null;
    }
    public String getSplit() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlAttribute attribute = manifest.searchAttributeByName(NAME_split);
        if (attribute != null) {
            return attribute.getValueAsString();
        }
        return null;
    }
    public void setSplit(String split, boolean forceCreate) {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return;
        }
        ResXmlAttribute attribute;
        if (forceCreate) {
            attribute = manifest.getOrCreateAttribute(NAME_split, 0);
        } else {
            attribute = manifest.searchAttributeByName(NAME_split);
            if (attribute == null) {
                return;
            }
        }
        attribute.setValueAsString(split);
    }

    /**
     * Returns "include" value from split module/fusing
     * e.g.
     * <dist:module type="asset-pack">
     *     <dist:fusing include="true" />
     * </dist:module>
     */
    public boolean isFusingInclude() {
        ResXmlAttribute attribute = PATH_MANIFEST.parse("/module/fusing;include=true")
                .findFirst(this);
        if (attribute != null && attribute.getValueType() == ValueType.BOOLEAN) {
            return attribute.getValueAsBoolean();
        }
        return false;
    }
    public String[] getFusedModuleNames() {
        ResXmlAttribute attribute = PATH_APPLICATION_META_DATA_NAME
                .value(VALUE_com_android_dynamic_apk_fused_modules)
                .findFirst(this);
        if (attribute != null && attribute.getValueType() == ValueType.STRING) {
            return StringsUtil.split(attribute.getValueAsString(), ',');
        }
        return null;
    }
    public void addFusedModuleNames(String ... names) {
        if (names == null || names.length == 0) {
            return;
        }
        ResXmlAttribute attribute = getOrCreateNamedElement(PATH_APPLICATION_META_DATA,
                VALUE_com_android_dynamic_apk_fused_modules)
                .getOrCreateAndroidAttribute(NAME_value, ID_value);

        ArrayCollection<String> nameList = new ArrayCollection<>();
        String value = attribute.getValueAsString();
        if (value != null) {
            nameList.addAll(StringsUtil.split(value, ','));
        }
        for (String name : names) {
            if (!StringsUtil.isEmpty(name) && !nameList.contains(name)) {
                nameList.add(name);
            }
        }
        attribute.setValueAsString(StringsUtil.join(nameList, ','));
    }
    public boolean clearFusedModuleNames() {
        return removeElements(PATH_APPLICATION_META_DATA_NAME
                .attribute(ID_name)
                .value(VALUE_com_android_dynamic_apk_fused_modules));
    }
    // TODO: find a better way
    public int guessCurrentPackageId() {
        if (mGuessedPackageId == 0) {
            mGuessedPackageId = ((getIconResourceId()>>24) & 0xff);
        }
        return mGuessedPackageId;
    }
    @Override
    PackageBlock selectPackageBlock(TableBlock tableBlock) {
        ResourceEntry resourceEntry = tableBlock.getResource(getIconResourceId());
        if (resourceEntry == null) {
            return super.selectPackageBlock(tableBlock);
        }
        PackageBlock packageBlock = resourceEntry.getPackageBlock();
        if (packageBlock.getTableBlock() != tableBlock) {
            return super.selectPackageBlock(tableBlock);
        }
        return packageBlock;
    }
    public int getIconResourceId() {
        ResXmlElement applicationElement = getApplicationElement();
        if (applicationElement == null) {
            return 0;
        }
        ResXmlAttribute attribute = applicationElement.searchAttributeByResourceId(ID_icon);
        if (attribute == null || attribute.getValueType() != ValueType.REFERENCE) {
            return 0;
        }
        return attribute.getData();
    }
    public void setIconResourceId(int resourceId) {
        ResXmlElement applicationElement = getOrCreateApplicationElement();
        ResXmlAttribute iconAttribute =
                applicationElement.getOrCreateAndroidAttribute(NAME_icon, ID_icon);
        iconAttribute.setTypeAndData(ValueType.REFERENCE, resourceId);
    }
    public int getRoundIconResourceId() {
        ResXmlElement applicationElement = getApplicationElement();
        if (applicationElement == null) {
            return 0;
        }
        ResXmlAttribute attribute = applicationElement.searchAttributeByResourceId(ID_roundIcon);
        if (attribute == null || attribute.getValueType() != ValueType.REFERENCE) {
            return 0;
        }
        return attribute.getData();
    }
    public void setRoundIconResourceId(int resourceId) {
        ResXmlElement applicationElement = getOrCreateApplicationElement();
        ResXmlAttribute iconAttribute =
                applicationElement.getOrCreateAndroidAttribute(NAME_icon, ID_roundIcon);
        iconAttribute.setTypeAndData(ValueType.REFERENCE, resourceId);
    }
    public Integer getApplicationLabelReference() {
        ResXmlElement applicationElement = getApplicationElement();
        if (applicationElement == null) {
            return null;
        }
        ResXmlAttribute labelAttribute =
                applicationElement.searchAttributeByResourceId(ID_label);
        if (labelAttribute == null || labelAttribute.getValueType() != ValueType.REFERENCE) {
            return null;
        }
        return labelAttribute.getData();
    }
    public void setApplicationLabel(int resourceId) {
        ResXmlElement applicationElement = getOrCreateApplicationElement();
        ResXmlAttribute labelAttribute =
                applicationElement.getOrCreateAndroidAttribute(NAME_label, ID_label);
        labelAttribute.setTypeAndData(ValueType.REFERENCE, resourceId);
    }
    public String getApplicationLabelString() {
        ResXmlElement applicationElement = getApplicationElement();
        if (applicationElement == null) {
            return null;
        }
        ResXmlAttribute labelAttribute =
                applicationElement.searchAttributeByResourceId(ID_label);
        if (labelAttribute == null || labelAttribute.getValueType() != ValueType.STRING) {
            return null;
        }
        return labelAttribute.getValueAsString();
    }
    public void setApplicationLabel(String label) {
        ResXmlElement applicationElement = getOrCreateApplicationElement();
        ResXmlAttribute labelAttribute =
                applicationElement.getOrCreateAndroidAttribute(NAME_label, ID_label);
        labelAttribute.setValueAsString(label);
    }

    public Boolean isExtractNativeLibs() {
        ResXmlElement application = getApplicationElement();
        if (application != null) {
            ResXmlAttribute attribute = application
                    .searchAttributeByResourceId(ID_extractNativeLibs);
            if (attribute != null && attribute.getValueType() == ValueType.BOOLEAN) {
                return attribute.getValueAsBoolean();
            }
        }
        return null;
    }
    public void setExtractNativeLibs(Boolean value) {
        ResXmlElement application = value == null ?
                getApplicationElement() :
                getOrCreateApplicationElement();
        if (application != null) {
            if (value == null) {
                application.removeAttributesWithId(ID_extractNativeLibs);
            } else {
                ResXmlAttribute attribute = application
                        .getOrCreateAndroidAttribute(NAME_extractNativeLibs, ID_extractNativeLibs);
                attribute.setValueAsBoolean(value);
            }
        }
    }

    public boolean isDebuggable() {
        ResXmlElement application = getApplicationElement();
        if (application != null) {
            ResXmlAttribute attribute = application
                    .searchAttributeByResourceId(ID_debuggable);
            if (attribute != null) {
                return attribute.getValueAsBoolean();
            }
        }
        return false;
    }
    public void setDebuggable(boolean debuggable) {
        ResXmlElement application = getApplicationElement();
        if (application == null) {
            return;
        }
        ResXmlAttribute attribute = application
                .searchAttributeByResourceId(ID_debuggable);
        if (debuggable) {
            if (attribute == null) {
                attribute=application.createAndroidAttribute(NAME_debuggable, ID_debuggable);
            }
            attribute.setValueAsBoolean(true);
        } else if (attribute != null) {
            application.removeAttribute(attribute);
        }
    }
    public ResXmlElement getMainActivity() {
        ResXmlElement element = getNamedElement(
                PATH_APPLICATION.element(TAG_activity).alternate(TAG_activity_alias)
                        .element(TAG_intent_filter)
                        .element(TAG_action),
                VALUE_android_intent_action_MAIN);
        if (element != null) {
            return element.getParentElement().getParentElement();
        }
        return null;
    }
    public ResXmlElement getOrCreateMainActivity(String name) {
        ResXmlElement activity = getMainActivity();
        if (activity == null) {
            ResXmlElement action = newChildElement(
                    PATH_APPLICATION.element(TAG_activity)
                    .element(TAG_intent_filter)
                    .element(TAG_action));
            action.getOrCreateAndroidAttribute(NAME_name, ID_name)
                    .setValueAsString(VALUE_android_intent_action_MAIN);

            ResXmlElement intentFilter = action.getParentElement();

            intentFilter.newElement(TAG_category)
                    .getOrCreateAndroidAttribute(NAME_name, ID_name)
                    .setValueAsString("android.intent.category.DEFAULT");
            intentFilter.newElement(TAG_category)
                    .getOrCreateAndroidAttribute(NAME_name, ID_name)
                    .setValueAsString("android.intent.category.LAUNCHER");

            activity = intentFilter.getParentElement();

            activity.getOrCreateAndroidAttribute(NAME_exported, ID_exported)
                    .setValueAsBoolean(true);
        }
        ResXmlAttribute attribute = activity.getOrCreateAndroidAttribute(NAME_name, ID_name);
        attribute.setValueAsString(name);
        return activity;
    }
    public ResXmlElement getOrCreateActivity(String name, boolean activityAlias) {
        ResXmlElement activity = getActivity(name, activityAlias);
        if (activity == null) {
            ResXmlElement application = getOrCreateApplicationElement();
            activity = application.newElement(
                    activityAlias? TAG_activity_alias : TAG_activity);
            ResXmlAttribute attribute = activity
                    .createAndroidAttribute(AndroidManifest.NAME_name, ID_name);
            attribute.setValueAsString(name);
        }
        return activity;
    }
    public ResXmlElement getActivity(String name, boolean activityAlias) {
        name = fullClassName(name);
        Iterator<ResXmlElement> iterator = getActivities(activityAlias);
        while(iterator.hasNext()) {
            ResXmlElement element = iterator.next();
            if (ObjectsUtil.equals(name, fullClassName(getAndroidNameValue(element)))) {
                return element;
            }
        }
        return null;
    }
    @Deprecated
    public List<ResXmlElement> listActivities() {
        ArrayCollection<ResXmlElement> results = new ArrayCollection<>();
        results.addAll(getActivities(true));
        return results;
    }
    public Iterator<ResXmlElement> getActivities(boolean includeAlias) {
        XMLPath xmlPath = PATH_APPLICATION.element(TAG_activity);
        if (includeAlias) {
            xmlPath = xmlPath.alternate(TAG_activity_alias);
        }
        return xmlPath.find(this);
    }
    public List<ResXmlElement> listApplicationElementsByTag(String tag) {
        return PATH_APPLICATION.element(tag).list(this);
    }
    public List<String> getUsesPermissions() {
        Iterator<ResXmlAttribute> iterator = PATH_MANIFEST.element(TAG_uses_permission)
                .attribute(ID_name)
                .find(this);
        return CollectionUtil.toList(ComputeIterator.of(iterator, ResXmlAttribute::getValueAsString));
    }
    public ResXmlElement getUsesPermission(String permissionName) {
        return getNamedElement(PATH_MANIFEST.element(TAG_uses_permission), permissionName);
    }
    public ResXmlElement addUsesPermission(String permissionName) {
        return getOrCreateNamedElement(PATH_MANIFEST.element(TAG_uses_permission), permissionName);
    }
    @Override
    public String getPackageName() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlAttribute attribute = manifest.searchAttributeByName(NAME_PACKAGE);
        if (attribute == null || attribute.getValueType()!=ValueType.STRING) {
            return null;
        }
        return attribute.getValueAsString();
    }

    @Override
    public void setPackageName(String packageName) {
        ResXmlElement manifestElement = getOrCreateManifestElement();
        ResXmlAttribute attribute = manifestElement.getOrCreateAttribute(NAME_PACKAGE, 0);
        attribute.setValueAsString(packageName);
    }
    @Override
    public String getApplicationClassName() {
        ResXmlAttribute attribute = PATH_APPLICATION
                .attribute(ID_name)
                .findFirst(this);
        if (attribute != null) {
            return fullClassName(attribute.getValueAsString());
        }
        return null;
    }
    @Override
    public void setApplicationClassName(String className) {
        ResXmlAttribute attribute = getOrCreateApplicationElement()
                .getOrCreateAndroidAttribute(AndroidManifest.NAME_name, ID_name);
        attribute.setValueAsString(className);
    }
    @Override
    public String getMainActivityClassName() {
        ResXmlElement mainActivity = getMainActivity();
        if (mainActivity != null) {
            ResXmlAttribute attribute = mainActivity
                    .searchAttributeByResourceId(ID_name);
            if (attribute != null) {
                return fullClassName(attribute.getValueAsString());
            }
        }
        return null;
    }
    @Override
    public void setMainActivityClassName(String className) {
        getOrCreateMainActivity(className);
    }

    @Override
    public Integer getVersionCode() {
        return getManifestAttributeInt(ID_versionCode);
    }
    @Override
    public void setVersionCode(int version) {
        setManifestAttributeInt(NAME_versionCode, ID_versionCode, version);
    }
    @Override
    public String getVersionName() {
        return getManifestAttributeString(ID_versionName);
    }
    @Override
    public void setVersionName(String versionName) {
        setManifestAttributeString(NAME_versionName,  ID_versionName, versionName);
    }
    @Override
    public Integer getPlatformBuildVersionCode() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlAttribute attribute = manifest.searchAttributeByName(NAME_platformBuildVersionCode);
        if (attribute == null || attribute.getValueType() != ValueType.DEC) {
            return null;
        }
        return attribute.getData();
    }
    @Override
    public void setPlatformBuildVersionCode(int version) {
        setManifestAttributeInt(NAME_platformBuildVersionCode, 0, version);
    }
    @Override
    public Object getPlatformBuildVersionName() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlAttribute attribute = manifest.searchAttributeByName(NAME_platformBuildVersionName);
        if (attribute == null ) {
            return null;
        }
        if (attribute.getValueType() == ValueType.STRING) {
            return attribute.getValueAsString();
        }
        return attribute.getData();
    }
    @Override
    public void setPlatformBuildVersionName(Object name) {
        Integer versionNumber = null;
        if (name instanceof Integer) {
            versionNumber = (Integer) name;
        } else {
            try{
                versionNumber = Integer.parseInt((String) name);
            }catch (NumberFormatException ignored) {
            }
        }
        if (versionNumber != null) {
            setManifestAttributeInt(NAME_platformBuildVersionName, 0, versionNumber);
        } else {
            setManifestAttributeString(NAME_platformBuildVersionName, 0, (String) name);
        }
    }
    @Override
    public Integer getMinSdkVersion() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlElement usesSdk = manifest.getElement(TAG_uses_sdk);
        if (usesSdk == null) {
            return null;
        }
        ResXmlAttribute attribute = usesSdk.searchAttributeByResourceId(ID_minSdkVersion);
        if (attribute == null || attribute.getValueType()!=ValueType.DEC) {
            return null;
        }
        return attribute.getData();
    }
    @Override
    public void setMinSdkVersion(int version) {
        ResXmlElement manifest = getOrCreateManifestElement();
        ResXmlElement usesSdk = manifest.getElement(TAG_uses_sdk);
        if (usesSdk == null) {
            usesSdk = manifest.newElement(TAG_uses_sdk);
        }
        ResXmlAttribute attribute = usesSdk.getOrCreateAndroidAttribute(NAME_minSdkVersion, ID_minSdkVersion);
        attribute.setTypeAndData(ValueType.DEC, version);
    }
    @Override
    public Integer getTargetSdkVersion() {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlElement usesSdk = manifest.getElement(TAG_uses_sdk);
        if (usesSdk == null) {
            return null;
        }
        ResXmlAttribute attribute = usesSdk.searchAttributeByResourceId(ID_targetSdkVersion);
        if (attribute == null || attribute.getValueType()!=ValueType.DEC) {
            return null;
        }
        return attribute.getData();
    }
    @Override
    public void setTargetSdkVersion(int version) {
        ResXmlElement manifest = getOrCreateManifestElement();
        ResXmlElement usesSdk = manifest.getElement(TAG_uses_sdk);
        if (usesSdk == null) {
            usesSdk = manifest.newElement(TAG_uses_sdk);
        }
        ResXmlAttribute attribute = usesSdk.getOrCreateAndroidAttribute(NAME_targetSdkVersion, ID_targetSdkVersion);
        attribute.setTypeAndData(ValueType.DEC, version);
    }
    @Override
    public Integer getCompileSdkVersion() {
        return getManifestAttributeInt(ID_compileSdkVersion);
    }
    @Override
    public void setCompileSdkVersion(int version) {
        setManifestAttributeInt(NAME_compileSdkVersion, ID_compileSdkVersion, version);
    }
    @Override
    public String getCompileSdkVersionCodename() {
        return getManifestAttributeString(ID_compileSdkVersionCodename);
    }
    @Override
    public void setCompileSdkVersionCodename(String name) {
        setManifestAttributeString(NAME_compileSdkVersionCodename,
                ID_compileSdkVersionCodename, name);
    }
    private String getManifestAttributeString(int resourceId) {
        ResXmlElement manifest = getManifestElement();
        if (manifest == null) {
            return null;
        }
        ResXmlAttribute attribute = manifest.searchAttributeByResourceId(resourceId);
        if (attribute == null || attribute.getValueType()!=ValueType.STRING) {
            return null;
        }
        return attribute.getValueAsString();
    }
    private void setManifestAttributeString(String attributeName, int resourceId, String value) {
        ResXmlElement manifestElement = getOrCreateManifestElement();
        ResXmlAttribute attribute = manifestElement
                .getOrCreateAndroidAttribute(attributeName, resourceId);
        attribute.setValueAsString(value);
    }
    private void setManifestAttributeInt(String attributeName, int resourceId, int value) {
        ResXmlElement manifestElement = getOrCreateManifestElement();
        ResXmlAttribute attribute = manifestElement
                .getOrCreateAndroidAttribute(attributeName, resourceId);
        attribute.setTypeAndData(ValueType.DEC, value);
    }
    private Integer getManifestAttributeInt(int resourceId) {
        ResXmlElement manifestElement = getManifestElement();
        if (manifestElement == null) {
            return null;
        }
        ResXmlAttribute attribute= manifestElement.searchAttributeByResourceId(resourceId);
        if (attribute == null || attribute.getValueType()!=ValueType.DEC) {
            return null;
        }
        return attribute.getData();
    }
    public ResXmlElement getOrCreateApplicationElement() {
        ResXmlElement manifestElement = getOrCreateManifestElement();
        ResXmlElement application = manifestElement.getElement(TAG_application);
        if (application == null) {
            application = manifestElement.newElement(TAG_application);
        }
        return application;
    }
    public ResXmlElement getApplicationElement() {
        ResXmlElement manifestElement = getManifestElement();
        if (manifestElement == null) {
            return null;
        }
        return manifestElement.getElement(TAG_application);
    }
    public ResXmlElement getManifestElement() {
        return getElement(AndroidManifest.TAG_manifest);
    }
    public void ensureFullClassNames() {
        ResXmlElement application = getApplicationElement();
        if (application == null) {
            return;
        }
        Iterator<ResXmlAttribute> iterator = application.recursiveAttributes();
        while (iterator.hasNext()) {
            ResXmlAttribute attribute = iterator.next();
            if (attribute.getNameId() != ID_name ||
                    attribute.getValueType() != ValueType.STRING) {
                continue;
            }
            attribute.setValueAsString(
                    fullClassName(attribute.getValueAsString()));
        }
        application.refresh();
    }
    public String fullClassName(String name) {
        if (name == null || name.length() == 0 || name.charAt(0) != '.') {
            return name;
        }
        String packageName = getPackageName();
        if (packageName == null) {
            return name;
        }
        return packageName + name;
    }
    public String relativeClassName(String name) {
        if (name == null || name.length() == 0 || name.charAt(0) == '.') {
            return name;
        }
        String packageName = getPackageName();
        if (packageName == null || !name.startsWith(packageName)) {
            return name;
        }
        int i = packageName.length();
        if (name.length() == i || name.charAt(i) != '.') {
            return name;
        }
        return name.substring(i);
    }
    private ResXmlElement getOrCreateManifestElement() {
        return getOrCreateElement(AndroidManifest.TAG_manifest);
    }
    public Iterator<ResXmlElement> getAndroidNameElements(String tag, String name) {
        return recursiveElements((element) ->
                element.equalsName(tag) && name.equals(getAndroidNameValue(element)));
    }
    @Override
    public String toString() {
        touchChildNodesForDebug();
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("{");
        builder.append(NAME_PACKAGE).append("=").append(getPackageName());
        builder.append(", ").append(NAME_versionCode).append("=").append(getVersionCode());
        builder.append(", ").append(NAME_versionName).append("=").append(getVersionName());
        builder.append(", ").append(NAME_compileSdkVersion).append("=").append(getCompileSdkVersion());
        builder.append(", ").append(NAME_compileSdkVersionCodename).append("=").append(getCompileSdkVersionCodename());

        List<String> allPermissions= getUsesPermissions();
        builder.append(", PERMISSIONS[");
        boolean appendOnce=false;
        for (String permissions:allPermissions) {
            if (appendOnce) {
                builder.append(", ");
            }
            builder.append(permissions);
            appendOnce=true;
        }
        builder.append("]");
        builder.append("}");
        return builder.toString();
    }
    public static String getAndroidNameValue(ResXmlElement element) {
        ResXmlAttribute attribute = element.searchAttributeByResourceId(AndroidManifestBlock.ID_name);
        if (attribute != null) {
            return attribute.getValueAsString();
        }
        return null;
    }

    public static boolean isAndroidManifestBlock(ResXmlDocument xmlBlock) {
        if (xmlBlock == null) {
            return false;
        }
        return xmlBlock.getElement(AndroidManifest.TAG_manifest) != null;
    }
    public static AndroidManifestBlock load(File file) throws IOException {
        AndroidManifestBlock manifestBlock = new AndroidManifestBlock();
        manifestBlock.readBytes(new BlockReader(file));
        return manifestBlock;
    }
    public static AndroidManifestBlock load(InputStream inputStream) throws IOException {
        AndroidManifestBlock manifestBlock = new AndroidManifestBlock();
        manifestBlock.readBytes(inputStream);
        return manifestBlock;
    }

    public static AndroidManifestBlock empty() {
        AndroidManifestBlock manifestBlock = new AndroidManifestBlock();
        manifestBlock.getOrCreateElement(EMPTY_MANIFEST_TAG);
        return manifestBlock;
    }

    public static final Predicate<ResXmlElement> PREDICATE_FUSED_MODULES = element ->
            element.equalsName(TAG_meta_data) &&
            VALUE_com_android_dynamic_apk_fused_modules.equals(
                    getAndroidNameValue(element));
}
