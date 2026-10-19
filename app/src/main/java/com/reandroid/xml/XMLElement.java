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
package com.reandroid.xml;

import com.reandroid.common.Namespace;
import com.reandroid.utils.StringsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.IndexIterator;
import com.reandroid.utils.collection.SizedSupplier;
import com.reandroid.xml.base.Attribute;
import com.reandroid.xml.base.Element;
import com.reandroid.xml.kxml2.KXmlParser;
import com.reandroid.xml.kxml2.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import com.reandroid.utils.function.Predicate;

public class XMLElement extends XMLNodeTree implements Element<XMLNode> {

    private ArrayCollection<XMLAttribute> mAttributes;
    private String mName;
    private XMLNamespace mNamespace;
    private ArrayCollection<XMLNamespace> mNamespaceList;

    private boolean mVoidHtml;

    public XMLElement() {
        super();
        mAttributes = EMPTY_ATTRIBUTES;
        mNamespaceList = EMPTY_NAMESPACES;
    }
    public XMLElement(String tagName) {
        this();
        setName(tagName);
    }
    public void addText(String text) {
        newText(text);
    }
    public XMLAttribute getAttributeAt(int index) {
        return mAttributes.get(index);
    }
    public Iterator<? extends XMLAttribute> getAttributes() {
        return new IndexIterator<>(new SizedSupplier<XMLAttribute>() {
            @Override
            public int size() {
                return getAttributeCount();
            }
            @Override
            public XMLAttribute get(int index) {
                return getAttributeAt(index);
            }
        });
    }
    public String getUri() {
        XMLNamespace namespace = getNamespace();
        if (namespace != null) {
            return namespace.getUri();
        }
        return null;
    }
    public String getPrefix() {
        XMLNamespace namespace = getNamespace();
        if (namespace != null) {
            return namespace.getPrefix();
        }
        return null;
    }
    public XMLNamespace getNamespace() {
        return mNamespace;
    }
    public void setNamespace(XMLNamespace namespace) {
        this.mNamespace = namespace;
    }
    public void setNamespace(Namespace namespace) {
        if (namespace == null) {
            setNamespace(null, null);
        } else {
            setNamespace(namespace.getUri(), namespace.getPrefix());
        }
    }
    public void setNamespace(String uri, String prefix) {
        setNamespace(getOrCreateXMLNamespace(uri, prefix));
    }

    public int getNamespaceCount() {
        return mNamespaceList.size();
    }
    public XMLNamespace getNamespaceAt(int index) {
        return mNamespaceList.get(index);
    }

    @Override
    public Iterator<? extends XMLNamespace> getNamespaces() {
        return mNamespaceList.iterator();
    }

    @Override
    public XMLNamespace newNamespace(String uri, String prefix) {
        XMLNamespace namespace = new XMLNamespace(uri, prefix);
        addNamespace(namespace);
        return namespace;
    }
    @Override
    public XMLElement newElement() {
        XMLElement element = new XMLElement();
        add(element);
        return element;
    }
    @Override
    public XMLText newText() {
        XMLText xmlText = new XMLText();
        add(xmlText);
        return xmlText;
    }

    @Override
    public XMLComment newComment() {
        XMLComment comment = new XMLComment();
        add(comment);
        return comment;
    }
    public XMLAttribute newAttribute() {
        XMLAttribute attribute = new XMLAttribute();
        addAttribute(attribute);
        return attribute;
    }

    public void addNamespace(String uri, String prefix) {
        if (uri == null || prefix == null) {
            return;
        }
        newNamespace(uri, prefix);
    }
    public void addNamespace(XMLNamespace namespace) {
        if (namespace != null && !mNamespaceList.contains(namespace)) {
            if (mNamespaceList == EMPTY_NAMESPACES) {
                mNamespaceList = new ArrayCollection<>();
            } else if (mNamespaceList.contains(namespace)) {
                return;
            }
            mNamespaceList.add(namespace);
        }
    }
    public XMLNamespace getOrCreateXMLNamespace(String uri, String prefix) {
        if (uri == null || prefix == null) {
            return null;
        }
        XMLNamespace namespace = getXMLNamespace(uri, prefix);
        if (namespace != null) {
            return namespace;
        }
        namespace = getRootElement().newNamespace(uri, prefix);
        return namespace;
    }
    public XMLNamespace getXMLNamespace(String uri, String prefix) {
        if (uri == null || prefix == null) {
            return null;
        }
        int count = getNamespaceCount();
        for (int i = 0; i < count; i++) {
            XMLNamespace namespace = getNamespaceAt(i);
            if (namespace.isEqual(uri, prefix)) {
                return namespace;
            }
        }
        XMLElement parent = getParentElement();
        if (parent != null) {
            return parent.getXMLNamespace(uri, prefix);
        }
        return null;
    }
    public XMLNamespace getXMLNamespaceByUri(String uri) {
        if (uri == null) {
            return null;
        }
        int count = getNamespaceCount();
        for (int i = 0; i < count; i++) {
            XMLNamespace namespace = getNamespaceAt(i);
            if (uri.equals(namespace.getUri())) {
                return namespace;
            }
        }
        XMLElement parent = getParentElement();
        if (parent != null) {
            return parent.getXMLNamespaceByUri(uri);
        }
        return null;
    }
    public XMLNamespace getXMLNamespaceByPrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        int count = getNamespaceCount();
        for (int i = 0; i < count; i++) {
            XMLNamespace namespace = getNamespaceAt(i);
            if (prefix.equals(namespace.getPrefix())) {
                return namespace;
            }
        }
        XMLElement parent = getParentElement();
        if (parent != null) {
            return parent.getXMLNamespaceByPrefix(prefix);
        }
        return null;
    }
    public Collection<XMLAttribute> listAttributes() {
        return mAttributes;
    }
    public int getChildElementsCount() {
        return super.countNodeWithType(XMLElement.class);
    }
    public List<XMLElement> getChildElementList() {
        return CollectionUtil.toList(iterator(XMLElement.class));
    }
    public Iterator<XMLElement> getElements(Predicate<XMLElement> filter) {
        return iterator(XMLElement.class, filter);
    }
    public int getAttributeCount() {
        return mAttributes.size();
    }
    public String getAttributeValue(String name) {
        XMLAttribute attribute = getAttribute(name);
        if (attribute != null) {
            return attribute.getValueAsString();
        }
        return null;
    }
    public XMLAttribute getAttribute(String name) {
        if (name == null) {
            return null;
        }
        int count = getAttributeCount();
        for (int i = 0; i < count; i++) {
            XMLAttribute attribute = getAttributeAt(i);
            if (attribute.equalsName(name)) {
                return attribute;
            }
        }
        return null;
    }
    public void clearAttributes() {
        if (mAttributes.size() == 0) {
            return;
        }
        for (int i = 0; i < mAttributes.size(); i++) {
            XMLAttribute attribute = mAttributes.get(i);
            attribute.setParentNode(null);
        }
        mAttributes.clear();
        mAttributes.trimToSize();
    }
    public XMLAttribute removeAttribute(String name) {
        return removeAttribute(getAttribute(name));
    }
    public XMLAttribute removeAttribute(XMLAttribute attribute) {
        if ( mAttributes.remove(attribute) && attribute != null) {
            attribute.setParentNode(null);
        }
        return attribute;
    }
    public XMLAttribute removeAttributeAt(int index) {
        XMLAttribute attribute = mAttributes.remove(index);
        if (attribute != null) {
            attribute.setParentNode(null);
        }
        return attribute;
    }
    public XMLAttribute setAttribute(String name, String value) {
        if (StringsUtil.isEmpty(name)) {
            return null;
        }
        XMLAttribute xmlAttribute = getAttribute(name);
        if (xmlAttribute == null) {
            if (XMLNamespace.looksNamespace(name, value)) {
                newNamespace(value, XMLUtil.splitName(name));
            } else {
                newAttribute().set(name,value);
            }
        } else {
            xmlAttribute.setValue(value);
        }
        return xmlAttribute;
    }
    public XMLElement addAttribute(String name, String value) {
        if (!StringsUtil.isEmpty(name)) {
            if (XMLNamespace.looksNamespace(name, value)) {
                newNamespace(value, XMLUtil.splitName(name));
            } else {
                newAttribute().set(name,value);
            }
        }
        return this;
    }
    public XMLElement addAttribute(String uri, String prefix, String name, String value) {
        if (!StringsUtil.isEmpty(name)) {
            if (XMLNamespace.looksNamespace(name, value)) {
                newNamespace(value, XMLUtil.splitName(name));
            } else {
                XMLAttribute attribute = new XMLAttribute();
                addAttribute(attribute);
                attribute.setName(uri, prefix, name);
                attribute.setValue(value);
            }
        }
        return this;
    }
    public void addAttribute(Attribute attribute) {
        if (attribute == null) {
            return;
        }
        XMLAttribute xmlAttribute = (XMLAttribute) attribute;
        if (mAttributes == EMPTY_ATTRIBUTES) {
            mAttributes = new ArrayCollection<>();
        }
        mAttributes.add(xmlAttribute);
        xmlAttribute.setParentNode(this);
    }
    public void addAttribute(int i, Attribute attribute) {
        if (attribute == null) {
            return;
        }
        XMLAttribute xmlAttribute = (XMLAttribute) attribute;
        if (mAttributes == EMPTY_ATTRIBUTES) {
            mAttributes = new ArrayCollection<>();
        }
        mAttributes.add(i, xmlAttribute);
        xmlAttribute.setParentNode(this);
    }
    public XMLElement getParentElement() {
        XMLNode parent = getParentNode();
        if (parent instanceof XMLElement) {
            return (XMLElement) parent;
        }
        return null;
    }
    public XMLElement getRootElement() {
        XMLElement parent = getParentElement();
        if (parent != null) {
            return parent.getRootElement();
        }
        return this;
    }
    public XMLDocument getParentDocument() {
        XMLElement root = getRootElement();
        XMLNode parent = root.getParentNode();
        if (parent instanceof XMLDocument) {
            return (XMLDocument) parent;
        }
        return null;
    }
    public int getDepth() {
        int result = 1;
        XMLElement parent = getParentElement();
        while (parent != null) {
            result ++;
            parent = parent.getParentElement();
        }
        return result;
    }
    public boolean equalsName(String name) {
        if (name == null) {
            return getName() == null;
        }
        String prefix = XMLUtil.splitPrefix(name);
        if (prefix != null && !prefix.equals(getPrefix())) {
            return false;
        }
        return name.equals(getName());
    }
    public String getName() {
        return mName;
    }
    public String getName(boolean includePrefix) {
        String name = getName();
        if (!includePrefix) {
            return name;
        }
        String prefix = getPrefix();
        if (prefix != null) {
            name = prefix + ":" + name;
        }
        return name;
    }
    public void setName(String name) {
        setName(null, null, name);
    }
    public void setName(String uri, String name) {
        setName(uri, null, name);
    }
    public void setName(String uri, String prefix, String name) {
        mName = XMLUtil.splitName(name);
        if (prefix == null) {
            prefix = XMLUtil.splitPrefix(name);
        }
        if (XMLUtil.isEmpty(uri)) {
            uri = null;
        }
        if (uri == null && prefix == null) {
            return;
        }
        XMLNamespace namespace;
        if (uri == null) {
            namespace = getXMLNamespaceByPrefix(prefix);
            if (namespace == null) {
                throw new IllegalArgumentException("Namespace not found for prefix: " + prefix);
            }
        } else {
            namespace = getXMLNamespaceByUri(uri);
            if (namespace == null) {
                throw new IllegalArgumentException("Namespace not found for uri: " + uri);
            }
        }
        setNamespace(namespace);
    }
    public String getTextContent() {
        return getTextContent(false);
    }
    public String getTextContent(boolean escapeXmlText) {
        StringWriter writer = new StringWriter();
        try {
            Iterator<XMLNode> iterator = iterator();
            while (iterator.hasNext()) {
                XMLNode child = iterator.next();
                child.write(writer, true, escapeXmlText);
            }
            writer.flush();
            writer.close();
        } catch (IOException ignored) {
        }
        return writer.toString();
    }
    public boolean hasAttribute(String name) {
        return getAttribute(name) != null;
    }
    public void setTextContent(String text, boolean escape) {
        super.clear();
        if (escape) {
            text = XMLUtil.escapeXmlChars(text);
        }
        addText(text);
    }

    public boolean isVoidHtml() {
        return mVoidHtml;
    }
    public void setVoidHtml(boolean voidHtml) {
        this.mVoidHtml = voidHtml;
        if (voidHtml) {
            transferChildrenToParent();
        }
    }
    private void transferChildrenToParent() {
        XMLNodeTree parentNode = (XMLNodeTree) getParentNode();
        if (parentNode != null) {
            int index = parentNode.indexOfExact(this);
            List<XMLNode> nodeList = CollectionUtil.toList(iterator());
            clear();
            for (XMLNode node : nodeList) {
                index ++;
                parentNode.add(index, node);
            }
        }
    }

    @Override
    void startSerialize(XmlSerializer serializer) throws IOException {
        int count = getNamespaceCount();
        for (int i = 0; i < count; i++) {
            XMLNamespace namespace = getNamespaceAt(i);
            serializer.setPrefix(namespace.getPrefix(),
                    namespace.getUri());
        }
        serializer.startTag(getUri(), getName(false));
        count = getAttributeCount();
        for (int i = 0; i < count; i++) {
            getAttributeAt(i).serialize(serializer);
        }
    }
    @Override
    void endSerialize(XmlSerializer serializer) throws IOException {
        if (!isVoidHtml()) {
            serializer.endTag(getUri(), getName(false));
        } else {
            KXmlSerializer kXmlSerializer = XMLUtil.getKXmlSerializer(serializer);
            if (kXmlSerializer != null) {
                kXmlSerializer.endTag(true, getUri(), getName(false));
            }
        }
    }

    @Override
    protected void onStartParse(XmlPullParser parser) throws XmlPullParserException, IOException {
        XMLUtil.expectEvent(parser, XmlPullParser.START_TAG);
        parseNamespaces(parser);
        setName(parser.getNamespace(), parser.getPrefix(), parser.getName());
        parseAttributes(parser);
        parser.nextToken();
    }

    @Override
    protected void onEndParse(XmlPullParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        if (event == XmlPullParser.END_TAG) {
            if (parser instanceof KXmlParser && size() == 0) {
                KXmlParser kXmlParser = (KXmlParser) parser;
                if (kXmlParser.isClosedWithTag()) {
                    newText("");
                }
            }
            // if not equals this name (i.e unclosed tag present). it is a responsibility
            // of parser to throw exceptions.
            // it could be parsing HTML and may have feature '... doc/features.html#relaxed'
            if (equalsName(parser.getName())) {
                parser.nextToken();
            } else if (XMLUtil.hasFeatureRelaxed(parser)) {
                setVoidHtml(true);
            }
        } else if (event == XmlPullParser.END_DOCUMENT) {
            setVoidHtml(true);
        }
    }
    private void parseNamespaces(XmlPullParser parser) throws XmlPullParserException {
        int count = parser.getNamespaceCount(getDepth());
        for (int i = 0; i < count; i++) {
            addNamespace(parser.getNamespaceUri(i),
                    parser.getNamespacePrefix(i));
        }
        count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (XMLNamespace.looksNamespace(name, value)) {
                addNamespace(value, XMLUtil.splitName(name));
            }
        }
    }
    public void parseAttributes(XmlPullParser parser) {
        boolean processNamespaces = parser.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (!XMLNamespace.looksNamespace(name, value)) {
                String uri = parser.getAttributeNamespace(i);
                String prefix = parser.getAttributePrefix(i);
                if (processNamespaces) {
                    name = XMLUtil.splitName(name);
                }
                addAttribute(uri, prefix, name, value);
            }
        }
    }
    @Override
    void write(Appendable appendable, boolean xml, boolean escapeXmlText) throws IOException {
        appendable.append('<');
        appendable.append(getName());
        appendAttributes(appendable, xml, escapeXmlText);
        boolean haveChildes = false;
        Iterator<XMLNode> iterator = iterator();
        while (iterator.hasNext()) {
            if (!haveChildes) {
                appendable.append(">");
            }
            XMLNode child = iterator.next();
            child.write(appendable, xml, escapeXmlText);
            haveChildes = true;
        }
        if (!isVoidHtml()) {
            if (haveChildes) {
                appendable.append("</");
                appendable.append(getName());
                appendable.append('>');
            } else {
                appendable.append(" />");
            }
        }
    }
    void appendAttributes(Appendable appendable, boolean xml, boolean escapeXmlText) throws IOException {
        char separator;
        if (xml) {
            separator = ' ';
        } else {
            separator = ';';
        }
        Iterator<? extends XMLAttribute> iterator = getAttributes();
        while (iterator.hasNext()) {
            appendable.append(separator);
            iterator.next().write(appendable, xml, escapeXmlText);
        }
    }

    public static XMLElement parseElement(XmlPullParser parser) throws IOException, XmlPullParserException {
        XMLElement element = new XMLElement();
        element.parse(parser);
        return element;
    }

    private static final ArrayCollection<XMLAttribute> EMPTY_ATTRIBUTES = ArrayCollection.empty();
    private static final ArrayCollection<XMLNamespace> EMPTY_NAMESPACES = ArrayCollection.empty();
}
