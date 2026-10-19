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

import com.reandroid.utils.collection.*;
import com.reandroid.xml.base.NodeTree;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public abstract class XMLNodeTree extends XMLNode implements
        NodeTree<XMLNode>, Iterable<XMLNode>, SizedSupplier<XMLNode> {

    private ArrayCollection<XMLNode> mNodeList;

    private int lastTrimSize;

    public XMLNodeTree() {
        super();
        this.mNodeList = EMPTY;
    }

    public XMLNode getLast() {
        int size = size();
        if (size == 0) {
            return null;
        }
        return mNodeList.get(size - 1);
    }
    public XMLElement getLastElement() {
        return CollectionUtil.getFirst(getElementsReversed());
    }
    @Override
    public void clear() {
        if (size() == 0) {
            return;
        }
        synchronized (this) {
            mNodeList.clear();
            mNodeList.trimToSize();
            lastTrimSize = 0;
        }
    }
    public Iterator<XMLNode> iterator(Predicate<? super XMLNode> filter) {
        return new IndexIterator<>(this, filter);
    }
    @Override
    public Iterator<XMLNode> iterator() {
        return new IndexIterator<>(this);
    }
    public Iterator<XMLNode> reversedIterator() {
        int size = size();
        int start = size - 1;
        return new ReversedIterator<XMLNode>(start, size) {
            @Override
            public XMLNode get(int i) {
                return XMLNodeTree.this.get(i);
            }
        };
    }
    public Iterator<XMLNode> recursiveNodes() {
        return RecursiveIterator.of(this, XMLNode::iterator);
    }
    @Override
    public int size() {
        return mNodeList.size();
    }
    @Override
    public XMLNode get(int index) {
        synchronized (this) {
            return mNodeList.get(index);
        }
    }
    public void addAll(Iterable<? extends XMLNode> iterable) {
        Iterator<? extends XMLNode> itr = iterable.iterator();
        while (itr.hasNext()) {
            add(itr.next());
        }
    }
    public boolean add(XMLNode xmlNode) {
        if (xmlNode == null || xmlNode == this) {
            return false;
        }
        synchronized (this) {
            if (mNodeList == EMPTY) {
                mNodeList = new ArrayCollection<>();
            }
            if (mNodeList.containsExact(xmlNode)) {
                throw new IllegalArgumentException("Duplicate node: " + xmlNode);
            }
            boolean added = mNodeList.add(xmlNode);
            xmlNode.setParentNode(this);
            if (mNodeList.size() - lastTrimSize > TRIM_INTERVAL) {
                mNodeList.trimToSize();
                lastTrimSize = mNodeList.size();
            }
            return added;
        }
    }
    public void add(int i, XMLNode xmlNode) {
        if (xmlNode == null || xmlNode == this) {
            return;
        }
        synchronized (this) {
            if (mNodeList == EMPTY) {
                mNodeList = new ArrayCollection<>();
            }
            if (mNodeList.containsExact(xmlNode)) {
                throw new IllegalArgumentException("Duplicate node: " + xmlNode);
            }
            mNodeList.add(i, xmlNode);
            xmlNode.setParentNode(this);
            if (mNodeList.size() - lastTrimSize > TRIM_INTERVAL) {
                mNodeList.trimToSize();
                lastTrimSize = mNodeList.size();
            }
        }
    }
    public int indexOf(XMLNode node) {
        return mNodeList.indexOf(node);
    }
    public int indexOfExact(XMLNode node) {
        return mNodeList.indexOfExact(node);
    }
    public boolean remove(XMLNode xmlNode) {
        synchronized (this) {
            if (xmlNode != null && mNodeList.remove(xmlNode)) {
                xmlNode.setParentNode(null);
                return true;
            }
            return false;
        }
    }
    public XMLNode remove(int i) {
        synchronized (this) {
            XMLNode xmlNode = mNodeList.remove(i);
            if (xmlNode != null) {
                xmlNode.setParentNode(null);
            }
            return xmlNode;
        }
    }
    public void move(Object xmlNode, int to) {
        mNodeList.move(xmlNode, to);
    }
    /**
     * Use removeIf
     * */
    @Deprecated
    public boolean remove(Predicate<? super XMLNode> filter) {
        throw new RuntimeException("Depreciated method");
    }
    @Override
    public boolean removeIf(Predicate<? super XMLNode> filter) {
        synchronized (this) {
            return mNodeList.removeIf(filter);
        }
    }
    @Override
    public boolean sort(Comparator<? super XMLNode> comparator) {
        synchronized (this) {
            return mNodeList.sortItems(comparator);
        }
    }
    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        startSerialize(serializer);
        serializeChildes(serializer);
        endSerialize(serializer);
    }
    abstract void startSerialize(XmlSerializer serializer) throws IOException;
    private void serializeChildes(XmlSerializer serializer) throws IOException {
        Iterator<XMLNode> itr = iterator();
        while (itr.hasNext()) {
            itr.next().serialize(serializer);
        }
    }
    abstract void endSerialize(XmlSerializer serializer) throws IOException;

    public abstract XMLElement newElement();
    public abstract XMLText newText();
    public XMLText newText(String text) {
        XMLText xmlText = newText();
        xmlText.setText(text);
        return xmlText;
    }
    public XMLComment newComment() {
        XMLComment comment = new XMLComment();
        add(comment);
        return comment;
    }
    public XMLNamespace newNamespace(String uri, String prefix) {
        return new XMLNamespace(uri, prefix);
    }
    public XMLElement newElement(String name) {
        XMLElement element = newElement();
        element.setName(name);
        return element;
    }
    public XMLDocType newDocType() {
        XMLDocType node = new XMLDocType();
        add(node);
        return node;
    }
    public XMLDocType newDocType(String type) {
        XMLDocType node = newDocType();
        node.setName(type);
        return node;
    }
    public XMLProcessingInstruction newProcessingInstruction() {
        XMLProcessingInstruction node = new XMLProcessingInstruction();
        add(node);
        return node;
    }
    public XMLCDSect newCDSect() {
        XMLCDSect node = new XMLCDSect();
        add(node);
        return node;
    }

    public XMLElement getOrCreateElement(String name) {
        XMLElement element = getElement(name);
        if (element == null) {
            element = newElement(name);
        }
        return element;
    }
    public XMLText getOrCreateFirstText(String text) {
        XMLText xmlText = null;
        if (size() != 0) {
            XMLNode node = get(0);
            if (node instanceof XMLText) {
                xmlText = (XMLText) node;
                xmlText.setText(text);
            }
        }
        if (xmlText == null) {
            xmlText = newText(text);
        }
        return xmlText;
    }
    public XMLText getOrCreateLastText() {
        XMLNode node = getLast();
        if (node instanceof XMLText) {
            return (XMLText) node;
        }
        return newText();
    }
    public XMLNode createForEvent(int event) {
        if (event == XmlPullParser.START_TAG) {
            return newElement();
        }
        if (event == XmlPullParser.DOCDECL) {
            return newDocType();
        }
        if (XMLText.isTextEvent(event)) {
            return getOrCreateLastText();
        }
        if (event == XmlPullParser.COMMENT) {
            return newComment();
        }
        if (event == XmlPullParser.PROCESSING_INSTRUCTION) {
            return newProcessingInstruction();
        }
        if (event == XmlPullParser.CDSECT) {
            return newCDSect();
        }
        return null;
    }
    public XMLElement getElement(String name) {
        return CollectionUtil.getFirst(getElements(name));
    }
    public Iterator<XMLElement> getElements(String name) {
        return InstanceIterator.of(iterator(), XMLElement.class, 
                element -> element.equalsName(name));
    }
    public Iterator<? extends XMLElement> getElements() {
        return iterator(XMLElement.class);
    }
    public Iterator<? extends XMLElement> getElementsReversed() {
        return InstanceIterator.of(reversedIterator(), XMLElement.class);
    }
    public Iterator<? extends XMLText> getTexts() {
        return iterator(XMLText.class);
    }

    public boolean hasChildElements() {
        return !CollectionUtil.isEmpty(getElements());
    }
    public boolean hasTextNode() {
        return !CollectionUtil.isEmpty(getTexts());
    }

    @Override
    public void parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        onStartParse(parser);
        parseInner(parser);
        onEndParse(parser);
    }
    protected void onStartParse(XmlPullParser parser) throws XmlPullParserException, IOException {
    }

    public void parseInner(XmlPullParser parser) throws XmlPullParserException, IOException {
        int endEvent = getEndEvent();
        int event = parser.getEventType();
        while (event != endEvent && event != XmlPullParser.END_DOCUMENT) {
            XMLNode node = createForEvent(event);
            if (node != null) {
                node.parse(parser);
            } else {
                onUnknownEvent(parser);
            }
            event = parser.getEventType();
        }
    }
    protected void onEndParse(XmlPullParser parser) throws XmlPullParserException, IOException {
    }
    protected void onUnknownEvent(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.nextToken();
    }
    private int getEndEvent() {
        if (this instanceof XMLElement) {
            return XmlPullParser.END_TAG;
        }
        return XmlPullParser.END_DOCUMENT;
    }
    private static final int TRIM_INTERVAL = 1000;
    private static final ArrayCollection<XMLNode> EMPTY = ArrayCollection.empty();
}
