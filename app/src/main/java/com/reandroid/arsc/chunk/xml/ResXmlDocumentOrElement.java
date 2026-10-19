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

import com.reandroid.arsc.base.Block;
import com.reandroid.arsc.chunk.ChunkType;
import com.reandroid.utils.collection.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

abstract class ResXmlDocumentOrElement extends ResXmlNodeTree {

    ResXmlDocumentOrElement(Block chunk) {
        super(chunk);
    }

    public Iterator<ResXmlElement> getElements() {
        return iterator(ResXmlElement.class);
    }
    public Iterator<ResXmlTextNode> getTexts() {
        return iterator(ResXmlTextNode.class);
    }

    public int getElementsCount() {
        return countNodeWithType(ResXmlElement.class);
    }
    public int getElementsCount(String name) {
        return CollectionUtil.count(getElements(name));
    }
    public int getTextsCount() {
        return countNodeWithType(ResXmlTextNode.class);
    }
    public boolean hasElement() {
        return containsNodeWithType(ResXmlElement.class);
    }
    public boolean hasText() {
        return containsNodeWithType(ResXmlTextNode.class);
    }
    public ResXmlElement getElement(String name) {
        return CollectionUtil.getFirst(getElements(name));
    }
    public ResXmlElement getOrCreateElement(String name){
        ResXmlElement element = getElement(name);
        if (element == null) {
            element = newElement(name);
        }
        return element;
    }
    public Iterator<ResXmlElement> getElements(String name) {
        return iterator(ResXmlElement.class, element -> element.equalsName(name));
    }
    public Iterator<ResXmlElement> getElementsWithChild(String ... childNames) {
        if (childNames == null || childNames.length == 0) {
            return EmptyIterator.of();
        }
        return getElementsWithChild(childNames, 0);
    }
    Iterator<ResXmlElement> getElementsWithChild(String[] childNames, int start) {
        int last = childNames.length - 1;
        if (start > last) {
            return EmptyIterator.of();
        }
        Iterator<ResXmlElement> iterator = getElements(childNames[start]);
        if (start == last || !iterator.hasNext()) {
            return iterator;
        }
        final int next = start + 1;
        return new IterableIterator<ResXmlElement, ResXmlElement>(iterator) {
            @Override
            public Iterator<ResXmlElement> iterator(ResXmlElement element) {
                return element.getElementsWithChild(childNames, next);
            }
        };
    }
    public boolean removeElementsIf(Predicate<? super ResXmlElement> predicate) {
        return removeIf(xmlNode -> (xmlNode instanceof ResXmlElement)
                && predicate.test((ResXmlElement) xmlNode));
    }
    public void setAttributesUnitSize(int size, boolean recursive) {
        Iterator<ResXmlElement> iterator = getElements();
        while (iterator.hasNext()) {
            ResXmlElement element = iterator.next();
            element.setAttributesUnitSize(size, recursive);
        }
    }
    public int lastIndexOf(String name) {
        Iterator<ResXmlElement> iterator = getElements(name);
        ResXmlElement element = CollectionUtil.getLast(iterator);
        if (element != null) {
            return element.getIndex();
        }
        return -1;
    }
    @Deprecated
    public List<ResXmlElement> listElements(String name) {
        return CollectionUtil.toList(getElements(name));
    }
    public Iterator<ResXmlElement> getElements(Predicate<? super ResXmlElement> predicate) {
        return iterator(ResXmlElement.class, predicate);
    }
    public Iterator<ResXmlElement> recursiveElements() {
        return recursive(ResXmlElement.class);
    }
    public Iterator<ResXmlElement> recursiveElements(Predicate<? super ResXmlElement> predicate) {
        return recursive(ResXmlElement.class, predicate);
    }
    public Iterator<ResXmlAttribute> recursiveAttributes(Predicate<? super ResXmlAttribute> predicate) {
        return FilterIterator.of(recursiveAttributes(), predicate);
    }
    public Iterator<ResXmlAttribute> recursiveAttributes() {
        Iterator<ResXmlAttribute> iterator = new IterableIterator<ResXmlElement, ResXmlAttribute>(recursiveElements()) {
            @Override
            public Iterator<ResXmlAttribute> iterator(ResXmlElement element) {
                return element.getAttributes();
            }
        };
        if (this instanceof ResXmlElement) {
            iterator = CombiningIterator.two(
                    ((ResXmlElement) this).getAttributes(), iterator);
        }
        return iterator;
    }

    @Override
    public ResXmlElement newElement() {
        return newElement(null);
    }
    public ResXmlElement newElement(String name) {
        ResXmlElement element = new ResXmlElement();
        add(element);
        if (name != null) {
            element.setName(name);
        }
        return element;
    }
    public ResXmlElement newElementAt(int position) {
        return newElementAt(position, null);
    }
    public ResXmlElement newElementAt(int position, String name) {
        ResXmlElement element = new ResXmlElement();
        add(position, element);
        if (name != null) {
            element.setName(name);
        }
        return element;
    }
    @Override
    public ResXmlTextNode newText() {
        ResXmlTextNode textNode = new ResXmlTextNode();
        add(textNode);
        return textNode;
    }
    @Override
    public ResXmlDocument newDocument() {
        ResXmlDocument document = new ResXmlDocument();
        add(document);
        return document;
    }
    @Override
    public UnknownResXmlNode newUnknown() {
        return newUnknown(ChunkType.NULL);
    }
    @Override
    public UnknownResXmlNode newUnknown(ChunkType chunkType) {
        UnknownResXmlNode xmlNode;
        if (UnexpectedResXmlNode.isSet(chunkType)) {
            xmlNode = new UnexpectedResXmlNode(chunkType);
        } else {
            xmlNode = new UnknownResXmlNode();
        }
        add(xmlNode);
        return xmlNode;
    }
    public ResXmlTextNode getOrCreateLastText() {
        ResXmlTextNode textNode = null;
        int size = size();
        if (size != 0) {
            ResXmlNode node = get(size - 1);
            if (node instanceof ResXmlTextNode) {
                textNode = (ResXmlTextNode) node;
                if (textNode.isComment()) {
                    textNode = null;
                }
            }
        }
        if (textNode == null) {
            textNode = newText();
        }
        return textNode;
    }

    public void removeUnusedNamespaces() {
        Iterator<ResXmlElement> iterator = getElements();
        while (iterator.hasNext()) {
            iterator.next().removeUnusedNamespaces();
        }
    }
    public void removeUndefinedAttributes() {
        Iterator<ResXmlElement> iterator = getElements();
        while (iterator.hasNext()) {
            iterator.next().removeUndefinedAttributes();
        }
    }
    public void fixNamespaces() {
        Iterator<ResXmlElement> iterator = getElements();
        while (iterator.hasNext()) {
            iterator.next().fixNamespaces();
        }
    }
    public void fixAttributeNames() {
        Iterator<ResXmlAttribute> iterator = recursiveAttributes();
        while (iterator.hasNext()) {
            iterator.next().autoSetName();
        }
    }

    @Override
    void linkStringReferences() {
        Iterator<ResXmlNode> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next().linkStringReferences();
        }
    }
    public boolean removeNullElements() {
        boolean result = false;
        if (removeElementsIf(ResXmlElement::isUndefined)) {
            result = true;
        }
        Iterator<ResXmlElement> iterator = getElements();
        while (iterator.hasNext()) {
            if (iterator.next().removeNullElements()) {
                result = true;
            }
        }
        return result;
    }
    public boolean autoSetAttributeNames() {
        boolean result = false;
        Iterator<ResXmlAttribute> iterator = recursiveAttributes();
        while (iterator.hasNext()) {
            ResXmlAttribute attribute = iterator.next();
            if (attribute.autoSetName()) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public void parse(XmlPullParser parser) throws IOException, XmlPullParserException {
        parseInnerNodes(parser);
    }
    void parseInnerNodes(XmlPullParser parser) throws IOException, XmlPullParserException {
        int event;
        while (!isEndEvent(event = parser.getEventType())) {
            ResXmlNode node = createForEvent(event);
            node.parse(parser);
        }
    }
    private ResXmlNode createForEvent(int event) {
        if (event == XmlPullParser.START_TAG) {
            return newElement();
        } else if (isTextEvent(event)) {
            return getOrCreateLastText();
        } else if (event == XmlPullParser.COMMENT) {
            return newText();
        } else if (event == XmlPullParser.START_DOCUMENT) {
            return newDocument();
        }
        throw new RuntimeException("Unexpected event " + event);
    }
    private boolean isEndEvent(int event){
        return event == XmlPullParser.END_TAG && isElement() ||
                event == XmlPullParser.END_DOCUMENT && isDocument();
    }
}
