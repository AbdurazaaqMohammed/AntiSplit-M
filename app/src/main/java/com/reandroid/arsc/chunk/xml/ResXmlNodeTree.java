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
import com.reandroid.arsc.refactor.ResourceMergeOption;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONException;
import com.reandroid.json.JSONObject;
import com.reandroid.utils.collection.*;
import com.reandroid.xml.base.NodeTree;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public abstract class ResXmlNodeTree extends ResXmlNode implements NodeTree<ResXmlNode> {

    // For debugging and testing only, remove latter
    private List<ResXmlNode> CHILD_NODES; private ResXmlNode PARENT_NODE;

    ResXmlNodeTree(Block chunk) {
        super(chunk);
    }

    @Override
    public ResXmlNode get(int i) {
        return getNodeList().get(i);
    }
    @Override
    public int size() {
        return getNodeList().size();
    }
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<ResXmlNode> iterator() {
        return getNodeList().iterator();
    }

    public<T extends ResXmlNode> Iterator<T> recursive(Class<T> instance) {
        return InstanceIterator.of(recursive(), instance);
    }
    public<T extends ResXmlNode> Iterator<T> recursive(Class<T> instance, Predicate<? super T> predicate) {
        return InstanceIterator.of(recursive(), instance, predicate);
    }
    public Iterator<ResXmlNode> recursive() {
        return new IterableIterator<ResXmlNode, ResXmlNode>(iterator()) {
            @Override
            public Iterator<ResXmlNode> iterator(ResXmlNode resXmlNode) {
                if (resXmlNode instanceof ResXmlNodeTree) {
                    return CombiningIterator.singleOne(resXmlNode,
                            ((ResXmlNodeTree) resXmlNode).recursive());
                }
                return SingleIterator.of(resXmlNode);
            }
        };
    }

    public Iterator<ResXmlNode> reversedIterator() {
        return getNodeList().reversedIterator();
    }
    public Iterator<ResXmlNode> reversedRecursive() {
        return new IterableIterator<ResXmlNode, ResXmlNode>(reversedIterator()) {
            @Override
            public Iterator<ResXmlNode> iterator(ResXmlNode resXmlNode) {
                if (resXmlNode instanceof ResXmlNodeTree) {
                    return CombiningIterator.two(
                            ((ResXmlNodeTree) resXmlNode).reversedRecursive(),
                            SingleIterator.of(resXmlNode)
                    );
                }
                return SingleIterator.of(resXmlNode);
            }
        };
    }

    public boolean add(ResXmlNode resXmlNode) {
        return getNodeList().add(resXmlNode);
    }
    public void add(int index, ResXmlNode resXmlNode) {
        getNodeList().add(index, resXmlNode);
    }

    public int indexOf(ResXmlNode resXmlNode) {
        return getNodeList().indexOf(resXmlNode);
    }
    public void moveTo(ResXmlNode resXmlNode, int index) {
        getNodeList().moveTo(resXmlNode, index);
    }
    @Override
    public boolean remove(ResXmlNode resXmlNode) {
        return getNodeList().remove(resXmlNode);
    }
    @Override
    public ResXmlNode remove(int i) {
        return getNodeList().remove(i);
    }
    @Override
    public boolean removeIf(Predicate<? super ResXmlNode> predicate) {
        return getNodeList().removeIf(predicate);
    }
    public int countIf(Predicate<? super ResXmlNode> predicate) {
        return getNodeList().countIf(predicate);
    }
    @Override
    public boolean sort(Comparator<? super ResXmlNode> comparator) {
        return getNodeList().sort(comparator);
    }
    @Override
    public void clear() {
        getNodeList().clearChildes();
    }

    @Override
    public boolean removeSelf() {
        ResXmlNodeTree parentNode = getParentNode();
        if (parentNode != null) {
            return parentNode.remove(this);
        }
        return false;
    }
    @Override
    public ResXmlNodeTree getParentNode() {
        return (ResXmlNodeTree) super.getParentNode();
    }
    @Override
    void onPreRemove() {
        clear();
    }

    abstract ResXmlNodeList getNodeList();

    public abstract ResXmlNode newElement();
    public abstract ResXmlNode newText();
    public abstract ResXmlNode newDocument();

    ResXmlNode newUnknown() {
        return null;
    }
    ResXmlNode newUnknown(ChunkType chunkType) {
        return null;
    }

    @Override
    ResXmlNode newSimilarTo(ResXmlNode otherNode) {
        if (otherNode.isElement()) {
            return newElement();
        }
        if (otherNode.isText() || otherNode.isComment()) {
            return newText();
        }
        if (otherNode.isDocument()) {
            return newDocument();
        }
        if (otherNode.isUnknown()) {
            return newUnknown();
        }
        throw new RuntimeException("Unknown node class: " + otherNode.getClass());
    }

    ResXmlNode newForNodeJson(JSONObject node) {
        String typeName = node.getString(JSON_node_type);
        if (JSON_node_type_unknown.equals(typeName)) {
            return newUnknown(ChunkType.get(
                    (short) node.optInt(JSON_type, 0)));
        }
        return newForNodeTypeName(typeName);
    }
    ResXmlNode newForNodeTypeName(String name) {
        if (JSON_node_type_element.equals(name)) {
            return newElement();
        }
        if (JSON_node_type_document.equals(name)) {
            return newDocument();
        }
        if (JSON_node_type_text.equals(name) || JSON_node_type_comment.equals(name)) {
            return newText();
        }
        throw new JSONException("Unknown node type name: " + name);
    }

    @Override
    public void merge(ResXmlNode xmlNode) {
        if (xmlNode == this) {
            return;
        }
        ResXmlNodeTree comingTree = (ResXmlNodeTree) xmlNode;
        Iterator<ResXmlNode> iterator = comingTree.iterator();
        while (iterator.hasNext()) {
            ResXmlNode coming = iterator.next();
            ResXmlNode child = newSimilarTo(coming);
            child.merge(coming);
        }
    }
    @Override
    public void mergeWithName(ResourceMergeOption mergeOption, ResXmlNode xmlNode) {
        ResXmlNodeTree comingTree = (ResXmlNodeTree) xmlNode;
        Iterator<ResXmlNode> iterator = comingTree.iterator();
        while (iterator.hasNext()) {
            ResXmlNode coming = iterator.next();
            ResXmlNode destination = newSimilarTo(coming);
            destination.mergeWithName(mergeOption, coming);
        }
    }

    void serializeNodes(XmlSerializer serializer, boolean decode) throws IOException {
        Iterator<ResXmlNode> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next().serialize(serializer, decode);
        }
    }

    JSONArray nodesToJson() {
        JSONArray jsonArray = null;
        Iterator<ResXmlNode> iterator = iterator();
        while (iterator.hasNext()) {
            JSONObject jsonObject = iterator.next().toJson();
            if (jsonObject == null) {
                continue;
            }
            if (jsonArray == null) {
                jsonArray = new JSONArray();
            }
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }
    void nodesFromJson(JSONObject json) {
        JSONArray jsonArray = json.optJSONArray(JSON_nodes);
        if (jsonArray == null) {
            return;
        }
        int size = jsonArray.length();
        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (!jsonObject.has(JSON_node_type)) {
                throw new JSONException("Missing: " + JSON_node_type);
            }
            newForNodeJson(jsonObject).fromJson(jsonObject);
        }
    }
    void touchChildNodesForDebug() {
        PARENT_NODE = getParentNode();
        if (this.CHILD_NODES == null) {
            CHILD_NODES = new AbstractList<ResXmlNode>() {
                @Override
                public ResXmlNode get(int i) {
                    return ResXmlNodeTree.this.get(i);
                }
                @Override
                public int size() {
                    return ResXmlNodeTree.this.size();
                }
            };
        }
    }

    @Override
    public String toString() {
        touchChildNodesForDebug();
        return super.toString();
    }
}
