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
package com.reandroid.dex.refactor;

import com.reandroid.dex.key.KeyPair;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliParser;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.utils.collection.FilterIterator;
import com.reandroid.utils.collection.InstanceIterator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.reandroid.utils.function.Predicate;

public class RenameBatch implements SmaliFormat, SmaliParser, Iterable<Rename<?>> {

    private final ArrayCollection<Rename<?>> renameList;

    public RenameBatch() {
        this.renameList = new ArrayCollection<>();
    }
    public boolean isEmpty() {
        for (Rename<?> rename : this) {
            if (!rename.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    public int size() {
        return renameList.size();
    }
    public Rename<?> get(int i) {
        return renameList.get(i);
    }
    public void add(Rename<?> rename) {
        if (rename != null && !renameList.containsExact(rename)) {
            renameList.add(rename);
        }
    }
    public void addAll(Iterable<? extends Rename<?>> iterable) {
        if (iterable != null && iterable != this) {
            addAll(iterable.iterator());
        }
    }
    public void addAll(Iterator<? extends Rename<?>> iterator) {
        while (iterator.hasNext()) {
            add(iterator.next());
        }
    }
    @Override
    public Iterator<Rename<?>> iterator() {
        return renameList.clonedIterator();
    }
    public Iterator<Rename<?>> iterator(SmaliDirective directive) {
        return FilterIterator.of(iterator(), rename -> rename.getSmaliDirective() == directive);
    }
    public<T extends Rename<?>> Iterator<T> iterator(Class<T> instance) {
        return InstanceIterator.of(iterator(), instance);
    }
    public<T extends Rename<?>> T getFirst(Class<T> instance) {
        return CollectionUtil.getFirst(iterator(instance));
    }
    public void removeIf(Predicate<Rename<?>> predicate) {
        renameList.removeIf(predicate);
    }
    public void removeEmptyRenames() {
        removeIf(Rename::isEmpty);
    }
    public int totalSize() {
        int result = 0;
        for (Rename<?> rename : this) {
            result += rename.size();
        }
        return result;
    }
    public int totalLockedSize() {
        int result = 0;
        for (Rename<?> rename : this) {
            result += rename.lockedSize();
        }
        return result;
    }
    public int sizeOf(SmaliDirective directive) {
        int result = 0;
        Iterator<Rename<?>> iterator = iterator(directive);
        while (iterator.hasNext()) {
            result += iterator.next().size();
        }
        return result;
    }
    public int sizeOfLocked(SmaliDirective directive) {
        int result = 0;
        Iterator<Rename<?>> iterator = iterator(directive);
        while (iterator.hasNext()) {
            result += iterator.next().lockedSize();
        }
        return result;
    }
    public RenameBatch merge() {
        RenameBatch result = new RenameBatch();
        result.add(getMerged(SmaliDirective.CLASS));
        result.add(getMerged(SmaliDirective.FIELD));
        result.add(getMerged(SmaliDirective.METHOD));
        result.removeEmptyRenames();
        return result;
    }
    public RenameBatch flip() {
        RenameBatch result = new RenameBatch();
        for (Rename<?> rename : this) {
            rename = rename.flip();
            result.apply(rename);
            result.add(rename);
        }
        return result.merge();
    }
    private Rename<?> getMerged(SmaliDirective directive) {
        Rename<?> result = RenameFactory.DEFAULT_FACTORY.createRename(directive);
        Iterator<Rename<?>> iterator = iterator(directive);
        while (iterator.hasNext()) {
            Rename<?> rename = iterator.next();
            List<? extends KeyPair<?, ?>> list = rename.toList();
            for (KeyPair<?, ?> keyPair : list) {
                KeyPair<?, ?> merged = getMerged(directive, keyPair);
                if (!result.contains(merged)) {
                    result.add(ObjectsUtil.cast(merged));
                }
            }
        }
        return result;
    }
    private KeyPair<?, ?> getMerged(SmaliDirective directive, KeyPair<?, ?> keyPair) {
        for (Rename<?> rename : this) {
            SmaliDirective renameDirective = rename.getSmaliDirective();
            if (directive != SmaliDirective.CLASS && renameDirective == SmaliDirective.CLASS) {
                keyPair = rename.apply(keyPair);
            }
            if (renameDirective == directive) {
                keyPair = rename.mergeRename(ObjectsUtil.cast(keyPair));
            }
        }
        return keyPair;
    }

    @Override
    public void append(SmaliWriter writer) throws IOException {
        if (isEmpty()) {
            return;
        }
        writer.appendComment("types = " + sizeOf(SmaliDirective.CLASS) + "/"
                + sizeOfLocked(SmaliDirective.CLASS));
        writer.appendComment("fields = " + sizeOf(SmaliDirective.FIELD) + "/"
                + sizeOfLocked(SmaliDirective.FIELD), false);
        writer.appendComment("methods = " + sizeOf(SmaliDirective.METHOD) + "/"
                + sizeOfLocked(SmaliDirective.METHOD), false);
        writer.newLine();
        for (Rename<?> rename : this) {
            if (!rename.isEmpty()) {
                rename.append(writer);
                writer.newLine();
            }
        }
    }

    public int apply(DexClassRepository classRepository) {
        int result = 0;
        for (Rename<?> rename : this) {
            result += rename.apply(classRepository);
        }
        return result;
    }
    public int apply(Rename<?> rename) {
        if (this.isEmpty() || rename.isEmpty()) {
            return 0;
        }
        int result = 0;
        for (Rename<?> r : this) {
            result += r.apply(rename);
        }
        return result;
    }
    public KeyPair<?, ?> apply(KeyPair<?, ?> keyPair) {
        for (Rename<?> r : this) {
            keyPair = r.apply(keyPair);
        }
        return keyPair;
    }
    @Override
    public void parse(SmaliReader reader) throws IOException {
        parse(null, null, reader);
    }
    public void parse(DexClassRepository classRepository, SmaliReader reader) throws IOException {
        parse(null, classRepository, reader);
    }
    public void parse(RenameFactory renameFactory,
                      DexClassRepository classRepository,
                      SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        while (!reader.finished()) {
            add(Rename.read(renameFactory, classRepository, reader));
            reader.skipWhitespacesOrComment();
        }
    }
    public void writeSmali(File file) throws IOException {
        SmaliWriter writer = new SmaliWriter();
        writer.setWriter(file);
        append(writer);
        writer.close();
    }

    public String toSmaliString() {
        return SmaliWriter.toStringSafe(this);
    }
    @Override
    public String toString() {
        return "size=" + size() +
                ", types=" + sizeOf(SmaliDirective.CLASS) + "/" + sizeOfLocked(SmaliDirective.CLASS) +
                ", fields=" + sizeOf(SmaliDirective.FIELD) + "/" + sizeOfLocked(SmaliDirective.FIELD) +
                ", methods=" + sizeOf(SmaliDirective.METHOD) + "/" + sizeOfLocked(SmaliDirective.METHOD);
    }
}
