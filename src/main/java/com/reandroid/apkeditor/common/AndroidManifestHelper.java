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
package com.reandroid.apkeditor.common;

import com.reandroid.apk.APKLogger;
import com.reandroid.apk.ApkUtil;
import com.reandroid.app.AndroidManifest;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.value.ValueType;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.EmptyList;

import org.apache.commons.collections4.Predicate;

import java.util.List;

public class AndroidManifestHelper {

    public static List<ResXmlElement> listSplitRequired(ResXmlElement parentElement){
        if(parentElement == null){
            return EmptyList.of();
        }
        return CollectionUtil.toList(parentElement.getElements(element -> {
            if(!element.equalsName(AndroidManifest.TAG_meta_data)){
                return false;
            }
            ResXmlAttribute nameAttribute = CollectionUtil.getFirst(element
                    .getAttributes(AndroidManifestHelper.NAME_FILTER));
            if(nameAttribute == null){
                return false;
            }
            String value = nameAttribute.getValueAsString();
            if(value == null){
                return false;
            }
            if (value.equals("com.android.dynamic.apk.fused.modules")){
                ResXmlAttribute attribute = element.searchAttributeByResourceId(AndroidManifest.ID_value);
                    if (attribute != null){
                        String attributeValue = attribute.getValueAsString();
                        if (attributeValue != null && attributeValue.equals(ApkUtil.DEF_MODULE_NAME)) {
                            return true;
                        }
                    }
            }
            return value.startsWith("com.android.vending.")
                    || value.startsWith("com.android.stamp.");
        }));
    }

    public static void removeAttributeFromManifestByName(AndroidManifestBlock androidManifestBlock,
                                                                 String resourceName, APKLogger logger){
        ResXmlElement manifestElement = androidManifestBlock.getManifestElement();
        if(manifestElement == null){
            if(logger != null){
                logger.logMessage("WARN: AndroidManifest don't have <manifest>");
            }
            return;
        }
        boolean removed = manifestElement.removeAttributesWithName(resourceName);
        if (removed && logger != null) {
            logger.logMessage("Removed-attribute : " + resourceName);
        }
    }
    public static void removeAttributeFromManifestById(AndroidManifestBlock androidManifestBlock,
                                                         int resourceId, APKLogger logger){
        ResXmlElement manifestElement = androidManifestBlock.getManifestElement();
        if(manifestElement == null){
            if(logger != null){
                logger.logMessage("WARN: AndroidManifest don't have <manifest>");
            }
            return;
        }
        boolean removed = manifestElement.removeAttributesWithId(resourceId);
        if (removed && logger != null) {
            logger.logMessage("Removed-attribute : " + HexUtil.toHex8("@0x", resourceId));
        }
    }


    public static void removeAttributeFromManifestAndApplication(AndroidManifestBlock androidManifestBlock,
                                                                 int resourceId, APKLogger logger, String nameForLogging){
        if(resourceId == 0){
            return;
        }
        ResXmlElement manifestElement = androidManifestBlock.getManifestElement();
        if(manifestElement == null){
            if(logger != null){
                logger.logMessage("WARN: AndroidManifest don't have <manifest>");
            }
            return;
        }
        boolean removed = manifestElement.removeAttributesWithId(resourceId);
        ResXmlElement applicationElement = manifestElement.getElement(
                AndroidManifest.TAG_application);

        if(removed && logger != null) {
            logger.logMessage("Attributes on <manifest> removed: "
                    + HexUtil.toHex8("0x", resourceId) + " (" + nameForLogging + ")");
        }
        if(applicationElement == null){
            return;
        }
        removed = applicationElement.removeAttributesWithId(resourceId);
        if(removed && logger != null) {
            logger.logMessage("Attributes on <application> removed: "
                    + HexUtil.toHex8("0x", resourceId) + " (" + nameForLogging + ")");
        }
    }
    static boolean isNameResourceId(ResXmlAttribute attribute){
        int resourceId = attribute.getNameId();
        return resourceId == AndroidManifest.ID_name;
    }
    static final Predicate<ResXmlAttribute> NAME_FILTER = attribute -> {
        if(!isNameResourceId(attribute)){
            return false;
        }
        // TODO: could be in reference, use reference resolver
        return attribute.getValueType() == ValueType.STRING;
    };
}
