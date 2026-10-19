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

import com.reandroid.arsc.pool.ResXmlStringPool;
import com.reandroid.arsc.refactor.ResourceMergeOption;
import com.reandroid.common.Namespace;
import com.reandroid.json.JSONException;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.IterableIterator;
import com.reandroid.utils.collection.SingleIterator;
import com.reandroid.xml.XMLElement;
import com.reandroid.xml.XMLUtil;
import com.reandroid.xml.base.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public class ResXmlElement extends ResXmlDocumentOrElement implements Element<ResXmlNode> {

    public ResXmlElement() {
        super(new ResXmlElementChunk());
    }

    public String getName() {
        return getStartElement().getName();
    }
    @Override
    public String getName(boolean includePrefix) {
        return getStartElement().getName(includePrefix);
    }
    @Override
    public void setName(String name) {
        getChunk().setName(name);
    }
    public boolean equalsName(String name){
        if(name == null){
            return getName() == null;
        }
        name = XMLUtil.splitName(name);
        return name.equals(getName(false));
    }

    @Override
    public ResXmlNamespace getNamespace() {
        return getChunk().getStartElement()
                .getResXmlStartNamespace();
    }

    @Override
    public void setNamespace(Namespace namespace){
        if (namespace != null) {
            setNamespace(namespace.getUri(), namespace.getPrefix());
        }else {
            setNamespace(null, null);
        }
    }
    public void setNamespace(String uri, String prefix) {
        getStartElement().setTagNamespace(uri, prefix);
    }
    @Override
    public String getPrefix() {
        return getStartElement().getPrefix();
    }
    @Override
    public String getUri() {
        return getStartElement().getUri();
    }

    @Override
    ResXmlElementChunk getChunk() {
        return (ResXmlElementChunk) super.getChunk();
    }

    @Override
    ResXmlNodeList getNodeList() {
        return getChunk().getNodeList();
    }

    @Override
    public int getAttributeCount() {
        return getAttributeArray().size();
    }

    @Override
    public ResXmlAttribute getAttributeAt(int i) {
        return getAttributeArray().get(i);
    }
    @Override
    public Iterator<ResXmlAttribute> getAttributes() {
        return getAttributeArray().clonedIterator();
    }
    public Iterator<ResXmlAttribute> getAttributes(Predicate<? super ResXmlAttribute> predicate) {
        return getAttributeArray().iterator(predicate);
    }
    public boolean removeAttribute(ResXmlAttribute attribute) {
        return getAttributeArray().remove(attribute);
    }
    public boolean removeAttributeAt(int index) {
        return getAttributeArray().remove(index) != null;
    }
    public boolean removeAttributeIf(Predicate<? super ResXmlAttribute> predicate) {
        return getAttributeArray().removeIf(predicate);
    }
    public boolean removeAttributesWithId(int resourceId) {
        return removeAttributeIf(attribute -> attribute.equalsNameId(resourceId));
    }
    public boolean removeAttributesWithName(String name) {
        return removeAttributeIf(attribute -> attribute.equalsNameWithNoId(name));
    }
    @Override
    public int getNamespaceCount() {
        return getNamespaceList().size();
    }
    @Override
    public ResXmlNamespace getNamespaceAt(int i) {
        return getNamespaceList().get(i);
    }
    @Override
    public Iterator<ResXmlNamespace> getNamespaces() {
        return getNamespaceList().getNamespaces();
    }
    public boolean removeNamespace(ResXmlNamespace namespace) {
        return getNamespaceList().remove((ResXmlStartNamespace) namespace);
    }
    public boolean removeNamespaceIf(Predicate<? super ResXmlNamespace> predicate) {
        return getNamespaceList().removeIf(predicate);
    }
    public Iterator<ResXmlNamespace> getVisibleNamespaces() {
        return ObjectsUtil.cast(getNamespaceList().getVisibleNamespaces());
    }
    ResXmlNamespace getNamespaceForUriReference(int reference) {
        return getNamespaceList().getForUriReference(reference);
    }
    public ResXmlNamespace getNamespace(String uri, String prefix) {
        return getNamespaceList().get(uri, prefix);
    }
    public ResXmlNamespace getOrCreateNamespace(String uri, String prefix) {
        return getNamespaceList().getOrCreate(uri, prefix);
    }
    public ResXmlNamespace getNamespaceForUri(String uri) {
        return getNamespaceList().getForUri(uri);
    }
    public ResXmlNamespace getNamespaceForPrefix(String prefix) {
        return getNamespaceList().getForPrefix(prefix);
    }
    public ResXmlNamespace getOrCreateNamespaceForPrefix(String prefix) {
        return getNamespaceList().getOrCreateForPrefix(prefix);
    }
    public String getComment() {
        String comment = getStartComment();
        if (StringsUtil.isEmpty(comment)) {
            String end = getEndComment();
            if (end != null) {
                comment = end;
            }
        }
        return comment;
    }
    public void setComment(String comment) {
        setStartComment(comment);
    }
    public String getStartComment() {
        return getStartElement().getComment();
    }
    public void setStartComment(String comment) {
        getStartElement().setComment(comment);
    }
    public String getEndComment() {
        return getEndElement().getComment();
    }
    public void setEndComment(String comment) {
        getEndElement().setComment(comment);
    }

    @Override
    public void setAttributesUnitSize(int size, boolean recursive) {
        getStartElement().getAttributeUnitSize().set(size);
        if (recursive) {
            super.setAttributesUnitSize(size, true);
        }
    }

    @Override
    public ResXmlNamespace newNamespace(String uri, String prefix) {
        return getNamespaceList().createNext(uri, prefix);
    }
    @Override
    public ResXmlAttribute newAttribute() {
        return getAttributeArray().createNext();
    }
    public ResXmlAttribute newAttribute(String name, int resourceId) {
        ResXmlAttribute attribute = newAttribute();
        attribute.setName(name, resourceId);
        return attribute;
    }

    @Override
    void linkStringReferences() {
        getChunk().linkStringReferences();
        super.linkStringReferences();
    }

    @Override
    Iterator<ResXmlEvent> getParserEvents() {
        return CombiningIterator.singleThree(

                ResXmlEvent.startTag(this),

                SingleIterator.of(ResXmlEvent.startComment(this)),

                new IterableIterator<ResXmlNode, ResXmlEvent>(iterator()) {
                    @Override
                    public Iterator<ResXmlEvent> iterator(ResXmlNode node) {
                        return node.getParserEvents();
                    }
                },

                CombiningIterator.singleOne(ResXmlEvent.endComment(this),
                        SingleIterator.of(ResXmlEvent.endTag(this)))
        );
    }

    @Override
    int autoSetLineNumber(int start) {
        start ++;
        Iterator<ResXmlNamespace> namespaces = getNamespaces();
        while (namespaces.hasNext()) {
            namespaces.next().setLineNumber(start);
        }
        getStartElement().setLineNumber(start);
        Iterator<ResXmlNode> iterator = iterator();
        int i = start;
        while (iterator.hasNext()) {
            ResXmlNode node = iterator.next();
            start = node.autoSetLineNumber(start);
        }
        if (i != start) {
            start ++;
        }
        getEndElement().setLineNumber(start);
        return start;
    }
    @Override
    public XMLElement toXml(boolean decode) {
        XMLElement element = new XMLElement();
        element.setName(getName(false));
        Iterator<ResXmlNamespace> namespaces = getNamespaces();
        while (namespaces.hasNext()) {
            ResXmlNamespace namespace = namespaces.next();
            element.addNamespace(namespace.getUri(), namespace.getPrefix());
        }
        element.setNamespace(getNamespace());
        getAttributeArray().toXml(element, decode);
        return element;
    }

    @Override
    public boolean isElement() {
        return true;
    }

    @Override
    public void setLineNumber(int lineNumber) {
        setStartLineNumber(lineNumber);
    }
    public int getStartLineNumber() {
        return getStartElement().getLineNumber();
    }
    public void setStartLineNumber(int lineNumber) {
        getStartElement().setLineNumber(lineNumber);
    }
    public int getEndLineNumber() {
        return getEndElement().getLineNumber();
    }
    public void setEndLineNumber(int lineNumber) {
        getEndElement().setLineNumber(lineNumber);
    }

    public ResXmlElement getParentElement() {
        ResXmlNode parent = getParentNode();
        if (parent instanceof ResXmlElement) {
            return (ResXmlElement) parent;
        }
        return null;
    }
    public ResXmlElement getRootElement() {
        return CollectionUtil.getLast(getParentElementsWithSelf());
    }
    public Iterator<ResXmlElement> getParentElementsWithSelf() {
        return visitParentNodes(ResXmlElement.class, ResXmlDocument.class);
    }
    public Iterator<ResXmlElement> getDescendingParentsWithSelf() {
        return CollectionUtil.reversedOf(getParentElementsWithSelf());
    }

    public ResXmlDocument getParentDocument() {
        return getParentInstance(ResXmlDocument.class);
    }
    public ResXmlStringPool getStringPool() {
        ResXmlDocument document = getParentDocument();
        if (document != null) {
            return document.getStringPool();
        }
        return null;
    }

    public boolean isUndefined() {
        return size() == 0 &&
                this.getAttributeCount() == 0 &&
                this.getNamespaceCount() == 0 &&
                StringsUtil.isEmpty(getName());
    }

    public ResXmlAttribute getOrCreateAndroidAttribute(String name, int resourceId){
        return getAttributeArray().getOrCreateAndroidAttribute(name, resourceId);
    }
    public ResXmlAttribute getOrCreateAttribute(String uri, String prefix, String name, int resourceId) {
        return getAttributeArray().getOrCreateAttribute(uri, prefix, name, resourceId);
    }
    public ResXmlAttribute getOrCreateAttribute(String name, int resourceId) {
        return getAttributeArray().getOrCreateAttribute(name, resourceId);
    }
    public ResXmlAttribute createAndroidAttribute(String name, int resourceId) {
        return getAttributeArray().getOrCreateAndroidAttribute(name, resourceId);
    }
    public ResXmlAttribute createAttribute(String name, int resourceId) {
        return newAttribute(name, resourceId);
    }
    public ResXmlAttribute searchAttributeByName(String name) {
        return getAttributeArray().searchAttributeByName(name);
    }
    public ResXmlAttribute searchAttributeByResourceId(int resourceId) {
        return getAttributeArray().searchAttributeByResourceId(resourceId);
    }
    public ResXmlAttribute searchAttribute(String namespace, String name) {
        return getAttributeArray().searchAttribute(namespace, name);
    }
    public boolean hasAttribute(int resourceId) {
        return getAttributeArray().searchAttributeByResourceId(resourceId) != null;
    }
    public boolean hasAttribute(String name) {
        return getAttributeArray().searchAttributeByName(name) != null;
    }
    public boolean hasAttribute(String namespace, String name) {
        return getAttributeArray().searchAttribute(namespace, name) != null;
    }

    public ResXmlAttribute getIdAttribute() {
        return getStartElement().getIdAttributePosition().getAttribute();
    }
    public ResXmlAttribute getClassAttribute() {
        return getStartElement().getClassAttributePosition().getAttribute();
    }
    public ResXmlAttribute getStyleAttribute() {
        return getStartElement().getStyleAttributePosition().getAttribute();
    }

    private ResXmlAttributeArray getAttributeArray() {
        return getStartElement().getResXmlAttributeArray();
    }
    private ResXmlStartElement getStartElement() {
        return getChunk().getStartElement();
    }
    ResXmlStartNamespaceList getNamespaceList() {
        return getChunk().getStartNamespaceList();
    }
    private ResXmlEndElement getEndElement() {
        return getChunk().getEndElement();
    }

    @Override
    public void merge(ResXmlNode xmlNode) {
        if (xmlNode == this) {
            return;
        }

        ResXmlElement coming = (ResXmlElement) xmlNode;

        setName(coming.getName(false));

        getNamespaceList().merge(coming.getNamespaceList());

        setNamespace(coming.getNamespace());

        getAttributeArray().merge(coming.getAttributeArray());

        setStartComment(coming.getStartComment());
        setEndComment(coming.getEndComment());

        setStartLineNumber(coming.getStartLineNumber());

        super.merge(xmlNode);

        setEndLineNumber(coming.getEndLineNumber());
    }
    @Override
    public void mergeWithName(ResourceMergeOption mergeOption, ResXmlNode xmlNode) {
        if (xmlNode == this) {
            return;
        }

        ResXmlElement coming = (ResXmlElement) xmlNode;

        setName(coming.getName(false));

        getNamespaceList().merge(coming.getNamespaceList());

        setNamespace(coming.getNamespace());

        getAttributeArray().mergeWithName(mergeOption, coming.getAttributeArray());

        setStartComment(coming.getStartComment());
        setEndComment(coming.getEndComment());

        setStartLineNumber(coming.getStartLineNumber());

        super.mergeWithName(mergeOption, xmlNode);

        setEndLineNumber(coming.getEndLineNumber());
    }

    private void setNamespaceFromJson(JSONObject jsonObject) {
        String uri = jsonObject.optString(JSON_uri, null);
        String prefix = jsonObject.optString(JSON_prefix, null);
        if (uri == null && prefix != null) {
            throw new JSONException("Provided " + JSON_prefix + "="
                    + prefix + ", but missing: " + JSON_uri);
        }
        if (prefix == null && uri != null) {
            throw new JSONException("Provided " + JSON_uri + "="
                    + uri + ", but missing: " + JSON_prefix);
        }
        setNamespace(uri, prefix);
    }
    @Override
    String nodeTypeName() {
        return JSON_node_type_element;
    }
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(JSON_node_type, nodeTypeName());

        jsonObject.put(JSON_namespaces, getNamespaceList().toJson());

        jsonObject.put(JSON_name, getName());

        jsonObject.put(JSON_uri, getUri());
        jsonObject.put(JSON_prefix, getPrefix());

        jsonObject.put(JSON_line, getStartLineNumber());
        jsonObject.put(JSON_line_end, getEndLineNumber());

        jsonObject.put(JSON_comment, getComment());

        jsonObject.put(JSON_attributes, getAttributeArray().toJson());

        jsonObject.put(JSON_nodes, nodesToJson());

        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject json) {
        setName(json.getString(JSON_name));
        setStartLineNumber(json.optInt(JSON_line));
        setEndLineNumber(json.optInt(JSON_line));

        getNamespaceList().fromJson(json.optJSONArray(JSON_namespaces));

        setNamespaceFromJson(json);

        setComment(json.optString(JSON_comment, null));

        getAttributeArray().fromJson(json.optJSONArray(JSON_attributes));

        nodesFromJson(json);
    }

    @Override
    public void serialize(XmlSerializer serializer, boolean decode) throws IOException {
        setIndent(serializer, true);
        Iterator<ResXmlNamespace> namespaces = getNamespaces();
        while (namespaces.hasNext()) {
            ResXmlNamespace namespace = namespaces.next();
            serializer.setPrefix(namespace.getPrefix(), namespace.getUri());
        }
        serializeComment(serializer, getStartComment());

        serializer.startTag(getUri(), getName(false));

        getAttributeArray().serialize(serializer, decode);

        serializeNodes(serializer, decode);

        serializer.endTag(getUri(), getName(false));

        serializeComment(serializer, getEndComment());
    }

    @Override
    public void parse(XmlPullParser parser) throws IOException, XmlPullParserException {

        XMLUtil.expectEvent(parser, XmlPullParser.START_TAG);

        setStartLineNumber(parser.getLineNumber());
        getNamespaceList().parse(parser);
        setName(parser.getName());
        setNamespace(parser.getNamespace(), parser.getPrefix());
        getAttributeArray().parse(parser);

        parser.nextToken();
        parseInnerNodes(parser);

        parser.nextToken();
    }

    @Override
    public void removeUnusedNamespaces() {
        removeNamespaceIf(ResXmlNamespace::isUnused);
        super.removeUnusedNamespaces();
    }
    @Override
    public void removeUndefinedAttributes() {
        removeAttributeIf(ResXmlAttribute::isUndefined);
        super.removeUndefinedAttributes();
    }
    @Override
    public void fixNamespaces() {
        Iterator<ResXmlNamespace> namespaces = getNamespaces();
        while (namespaces.hasNext()) {
            ResXmlStartNamespace namespace = (ResXmlStartNamespace) namespaces.next();
            namespace.fixEmpty();
        }
        Iterator<ResXmlAttribute> iterator = getAttributes();
        while (iterator.hasNext()) {
            iterator.next().autoSetNamespace();
        }
        super.fixNamespaces();
    }

    @Override
    void onPreRemove() {
        super.onPreRemove();
        getChunk().onPreRemove();
    }

    @Override
    public String toString() {
        touchChildNodesForDebug();
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(getName(true));
        Iterator<ResXmlNamespace> namespaces = getNamespaces();
        if (namespaces.hasNext()) {
            builder.append(' ');
            builder.append(StringsUtil.join(namespaces, ' '));
        }
        Iterator<ResXmlAttribute> attributes = getAttributes();
        if (attributes.hasNext()) {
            builder.append(' ');
            builder.append(StringsUtil.join(attributes, ' '));
        }
        builder.append('>');
        return builder.toString();
    }
}
