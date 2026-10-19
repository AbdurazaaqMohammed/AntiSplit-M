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

import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.KeyPair;
import com.reandroid.dex.model.DexClassRepository;
import com.reandroid.dex.smali.SmaliDirective;
import com.reandroid.dex.smali.SmaliFormat;
import com.reandroid.dex.smali.SmaliParseException;
import com.reandroid.dex.smali.SmaliParser;
import com.reandroid.dex.smali.SmaliReader;
import com.reandroid.dex.smali.SmaliWriter;
import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.ArrayCollection;
import com.reandroid.utils.collection.CollectionUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Rename<T extends Key>
        implements SmaliFormat, SmaliParser {

    private final Map<KeyPair<?, ?>, KeyPair<T, T>> keyPairMap;
    private final Map<KeyPair<?, ?>, KeyPair<T, T>> flippedKeyMap;
    private final Set<KeyPair<?, ?>> lockedKeys;
    private final Set<KeyPair<?, ?>> lockedFlippedKeys;

    public Rename() {
        this.keyPairMap = new HashMap<>();
        this.flippedKeyMap = new HashMap<>();
        this.lockedKeys = new HashSet<>();
        this.lockedFlippedKeys = new HashSet<>();
    }

    public abstract void add(DexClassRepository classRepository, KeyPair<T, T> keyPair);

    public void add(DexClassRepository classRepository, T search, T replace) {
        add(classRepository, new KeyPair<>(search, replace));
    }
    public void add(T search, T replace) {
        add(new KeyPair<>(search, replace));
    }
    public void add(KeyPair<T, T> keyPair) {
        addToSet(keyPair);
    }
    public void addAll(Collection<KeyPair<T, T>> keyPairs) {
        this.addAll(keyPairs.iterator());
    }
    public void addAll(Iterator<KeyPair<T, T>> iterator) {
        while (iterator.hasNext()) {
            add(iterator.next());
        }
    }
    public void addAll(DexClassRepository classRepository, Collection<KeyPair<T, T>> keyPairs) {
        if (keyPairs != null) {
            addAll(classRepository, keyPairs.iterator());
        }
    }
    public void addAll(DexClassRepository classRepository, Iterator<KeyPair<T, T>> iterator) {
        while (iterator.hasNext()) {
            add(classRepository, iterator.next());
        }
    }
    private void addToSet(KeyPair<T, T> keyPair) {
        if (keyPair == null || !keyPair.isValid()) {
            return;
        }
        boolean bad = false;
        if (lockedKeys.contains(keyPair) || lockedFlippedKeys.contains(keyPair)) {
            bad = true;
        }
        KeyPair<T, T> flip = keyPair.flip();
        if (lockedFlippedKeys.contains(flip) || lockedKeys.contains(flip)) {
            if (bad) {
                return;
            }
            bad = true;
        }
        if (!bad) {
            KeyPair<T, T> exist = keyPairMap.get(keyPair);
            if (exist == null) {
                exist = flippedKeyMap.get(flip);
                if (exist != null) {
                    bad = true;
                }
            } else {
                if (keyPair.equalsBoth(exist)) {
                    return;
                }
                bad = true;
            }
        }
        if (bad) {
            lockKey(keyPair, flip);
        } else {
            keyPairMap.put(keyPair, keyPair);
            flippedKeyMap.put(flip, keyPair);
        }
        onChanged();
    }
    private void lockKey(KeyPair<T, T> keyPair, KeyPair<T, T> flip) {
        lockedKeys.add(keyPair);
        lockedFlippedKeys.add(flip);
        KeyPair<T, T> p1 = keyPairMap.remove(keyPair);
        if (p1 == null) {
            p1 = keyPairMap.remove(flip);
        }
        KeyPair<T, T> p2 = flippedKeyMap.remove(flip);
        if (p2 == null) {
            p2 = flippedKeyMap.remove(keyPair);
        }
        if (p1 != null && !p1.equalsBoth(keyPair)) {
            lockKey(p1, p1.flip());
        }
        if (p2 != null && !p2.equalsBoth(keyPair)) {
            lockKey(p2, p2.flip());
        }
    }
    public boolean isLocked(KeyPair<?, ?> keyPair) {
        if (keyPair != null) {
            KeyPair<?, ?> flip = keyPair.flip();
            return lockedKeys.contains(keyPair) ||
                    lockedFlippedKeys.contains(keyPair) ||
                    lockedKeys.contains(flip) ||
                    lockedFlippedKeys.contains(flip);
        }
        return false;
    }
    public void lock(KeyPair<T, T> keyPair) {
        if (keyPair != null && keyPair.isValid()) {
            lockKey(keyPair, keyPair.flip());
        }
    }
    public void lockAll(Iterable<? extends KeyPair<T, T>> iterable) {
        if (iterable != null) {
            for (KeyPair<T, T> keyPair : iterable) {
                lock(keyPair);
            }
        }
    }
    public void unlock(KeyPair<T, T> keyPair) {
        if (keyPair != null) {
            KeyPair<T, T> flip = keyPair.flip();
            lockedKeys.remove(keyPair);
            lockedFlippedKeys.remove(flip);
            add(keyPair);
        }
    }

    protected void onChanged() {
    }
    public void close() {
        keyPairMap.clear();
        flippedKeyMap.clear();
        lockedKeys.clear();
        lockedFlippedKeys.clear();
    }

    public boolean isEmpty() {
        return size() == 0 && lockedSize() == 0;
    }
    public int size() {
        return keyPairMap.size();
    }
    public int lockedSize() {
        return lockedKeys.size();
    }

    public boolean contains(KeyPair<?, ?> keyPair) {
        if (keyPair != null) {
            return contains(keyPair.getFirst()) ||
                    contains(keyPair.getSecond()) ||
                    isLocked(keyPair);
        }
        return false;
    }
    public boolean contains(Key key) {
        return find(key) != null;
    }
    public KeyPair<T, T> get(Key search) {
        return keyPairMap.get(new KeyPair<>(search, null));
    }
    public KeyPair<T, T> getFlipped(Key replace) {
        return flippedKeyMap.get(new KeyPair<>(replace, null));
    }
    public KeyPair<T, T> find(Key key) {
        KeyPair<T, T> result = get(key);
        if (result == null) {
            result = getFlipped(key);
        }
        return result;
    }
    public KeyPair<T, T> removeKey(Key key) {
        return remove(find(key));
    }
    public KeyPair<T, T> remove(KeyPair<T, T> keyPair) {
        if (keyPair == null) {
            return null;
        }
        KeyPair<T, T> flipped = keyPair.flip();
        KeyPair<T, T> result = keyPairMap.remove(keyPair);
        KeyPair<T, T> p = keyPairMap.remove(flipped);
        if (result == null) {
            result = p;
        }
        p = flippedKeyMap.remove(flipped);
        if (result == null) {
            result = p;
        }
        p = flippedKeyMap.remove(keyPair);
        if (result == null) {
            result = p;
        }
        lockedKeys.remove(keyPair);
        lockedKeys.remove(flipped);
        lockedFlippedKeys.remove(keyPair);
        lockedFlippedKeys.remove(flipped);
        return result;
    }
    public T getReplace(Key search) {
        KeyPair<T, T> keyPair = get(search);
        if (keyPair != null) {
            return keyPair.getSecond();
        }
        return null;
    }
    public<E extends Key> E replaceInKey(E key) {
        Key replace = getReplace(key);
        if (replace != null) {
            if (key.getClass() == replace.getClass()) {
                key = ObjectsUtil.cast(replace);
            }
            return key;
        }
        Key result = key;
        Iterator<? extends Key> iterator = key.contents();
        while (iterator.hasNext()) {
            Key search = iterator.next();
            replace = getReplace(search);
            if (replace != null) {
                result = replaceKey(result, search, replace);
            }
        }
        return ObjectsUtil.cast(result);
    }
    public Key replaceKey(Key key, Key search, Key replace) {
        return key.replaceKey(search, replace);
    }
    public List<KeyPair<T, T>> toList() {
        return toList(CompareUtil.getComparableComparator());
    }
    public List<KeyPair<T, T>> toList(Comparator<KeyPair<? super T, ? super T>> comparator) {
        List<KeyPair<T, T>> results = new ArrayCollection<>(getKeyPairSet());
        if (comparator != null) {
            java.util.Collections.sort(results, comparator);
        }
        return results;
    }
    public List<KeyPair<T, T>> listLocked() {
        return ObjectsUtil.cast(new ArrayCollection<>(lockedKeys));
    }

    public void validate(DexClassRepository classRepository) {
        List<KeyPair<T, T>> list = toList();
        for (KeyPair<T, T> keyPair : list) {
            if (containsDeclaration(classRepository, keyPair.getSecond())) {
                lock(keyPair);
            }
        }
    }
    protected boolean containsDeclaration(DexClassRepository classRepository, T replaceKey) {
        return classRepository.getDexDeclaration(replaceKey) != null;
    }

    public abstract int apply(DexClassRepository classRepository);
    public int apply(Rename<?> rename) {
        if (this.isEmpty() || rename.isEmpty()) {
            return 0;
        }
        List<? extends KeyPair<?, ?>> list = rename.toList();
        int result = 0;
        for (KeyPair<?, ?> keyPair : list) {
            KeyPair<?, ?> renamedPair = apply(keyPair);
            if (renamedPair != keyPair) {
                rename.replace(keyPair, renamedPair);
                result ++;
            }
        }
        return result;
    }
    public KeyPair<?, ?> apply(KeyPair<?, ?> keyPair) {
        Key first = keyPair.getFirst();
        Key first2 = this.renameKey(first);
        Key second = keyPair.getSecond();
        Key second2 = this.renameKey(second);
        if (first != first2 || second != second2) {
            keyPair = new KeyPair<>(first2, second2);
        }
        return keyPair;
    }
    private void replace(KeyPair<?, ?> keyPair, KeyPair<?, ?> replace) {
        if (keyPairMap.remove(keyPair) != null) {
            flippedKeyMap.remove(keyPair.flip());
            add(ObjectsUtil.cast(replace));
        }
    }
    protected Key renameKey(Key key) {
        Iterator<? extends Key> iterator = CollectionUtil.uniqueOf(key.contents());
        while (iterator.hasNext()) {
            Key mentioned = iterator.next();
            Key replace = getReplace(mentioned);
            if (replace != null) {
                key = replaceKey(key, mentioned, replace);
            }
        }
        return key;
    }
    public KeyPair<T, T> mergeRename(KeyPair<T, T> lower) {
        KeyPair<T, T> renamed = get(lower.getSecond());
        if (renamed != null && !renamed.equalsBoth(lower)) {
            return new KeyPair<>(lower.getFirst(), renamed.getSecond());
        }
        return lower;
    }

    public Set<KeyPair<T, T>> getKeyPairSet() {
        return ObjectsUtil.cast(keyPairMap.keySet());
    }

    public abstract SmaliDirective getSmaliDirective();

    @Override
    public void append(SmaliWriter writer) throws IOException {
        SmaliDirective directive = getSmaliDirective();
        directive.append(writer);
        writer.appendComment("size = " + size());
        int locked = lockedSize();
        if (locked != 0) {
            writer.appendComment("locked = " + locked, false);
        }
        writer.indentPlus();
        List<KeyPair<T, T>> keyPairList = toList();
        for (KeyPair<T, T> keyPair : keyPairList) {
            writer.newLine();
            keyPair.append(writer);
        }
        List<KeyPair<T, T>> lockedKeyPairList = listLocked();
        for (KeyPair<T, T> keyPair : lockedKeyPairList) {
            writer.newLine();
            writer.appendComment(keyPair.toString());
        }
        writer.indentMinus();
        directive.appendEnd(writer);
        writer.newLine();
    }

    @Override
    public void parse(SmaliReader reader) throws IOException {
        parse(null, reader);
    }
    public void parse(DexClassRepository classRepository, SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        if (reader.finished()) {
            return;
        }
        SmaliDirective directive = getSmaliDirective();
        if (directive.isEnd(reader)) {
            directive.skipEnd(reader);
            return;
        }
        SmaliParseException.expect(reader, directive);
        reader.skipWhitespacesOrComment();
        while (!directive.isEnd(reader)) {
            KeyPair<T, T> keyPair = KeyPair.read(directive, reader);
            if (classRepository == null) {
                add(keyPair);
            } else {
                add(classRepository, keyPair);
            }
            reader.skipWhitespacesOrComment();
        }
        SmaliParseException.expect(reader, directive, true);
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
    public Rename<T> flip() {
        Rename<T> result = RenameFactory.DEFAULT_FACTORY.createRename(getSmaliDirective());
        for (KeyPair<T, T> keyPair : getKeyPairSet()) {
            result.add(keyPair.flip());
        }
        return result;
    }
    @Override
    public String toString() {
        return size() + "/" + lockedSize();
    }

    public static Rename<?> read(SmaliReader reader) throws IOException {
        return read(null, null, reader);
    }
    public static Rename<?> read(RenameFactory renameFactory,  DexClassRepository classRepository, SmaliReader reader) throws IOException {
        if (renameFactory == null) {
            renameFactory = RenameFactory.DEFAULT_FACTORY;
        }
        reader.skipWhitespacesOrComment();
        SmaliDirective directive = SmaliDirective.parse(reader, false);
        if (directive == null) {
            throw new SmaliParseException(
                    "Expecting rename directives (.class, .field, .method ...)", reader);
        }
        if (directive.isEnd(reader)) {
            throw new SmaliParseException("Unexpected end", reader);
        }
        Rename<?> rename = renameFactory.createRename(directive);
        if (rename == null) {
            throw new SmaliParseException("Unknown rename directive", reader);
        }
        rename.parse(classRepository, reader);
        return rename;
    }
}
