package com.reandroid.dex.model;

import com.reandroid.dex.common.SectionItem;
import com.reandroid.dex.key.Key;
import com.reandroid.dex.key.TypeKey;
import com.reandroid.dex.sections.Marker;
import com.reandroid.dex.sections.Section;
import com.reandroid.dex.sections.SectionType;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.utils.collection.CombiningIterator;
import com.reandroid.utils.collection.EmptyIterator;
import com.reandroid.utils.collection.SingleIterator;

import java.util.Iterator;
import com.reandroid.utils.function.Predicate;

public interface DexClassModule extends DexClassRepository {

    @Override
    int getVersion();
    @Override
    void setVersion(int version);

    boolean isMultiLayoutEntry();
    int getOffset();

    Iterator<DexClass> getExtendingClasses(TypeKey typeKey);
    Iterator<DexClass> getImplementClasses(TypeKey typeKey);

    @Override
    DexClass getDexClass(TypeKey typeKey);
    DexClass getOrCreateClass(TypeKey key);

    @Override
    Iterator<DexClass> getDexClasses(Predicate<? super TypeKey> filter);

    @Override
    Iterator<DexClass> getDexClassesCloned(Predicate<? super TypeKey> filter);

    @Override
    boolean sort();

    @Override
    default Iterator<DexClass> getSuccessorClasses(TypeKey typeKey){
        return CombiningIterator.two(
                getExtendingClasses(typeKey),
                getImplementClasses(typeKey));
    }

    @Override
    default <T extends SectionItem> Iterator<Section<T>> getSections(SectionType<T> sectionType) {
        return SingleIterator.of(getSection(sectionType));
    }
    <T extends SectionItem> Section<T> getSection(SectionType<T> sectionType);
    <T extends SectionItem> Section<T> getOrCreateSection(SectionType<T> sectionType);

    @Override
    default <T extends SectionItem> T getItem(SectionType<T> sectionType, int id) {
        Section<T> section = getSection(sectionType);
        if (section != null) {
            return section.get(id);
        }
        return null;
    }
    @Override
    default <T extends SectionItem> T getItem(SectionType<T> sectionType, Key key) {
        Section<T> section = getSection(sectionType);
        if (section != null) {
            return section.get(key);
        }
        return null;
    }
    @Override
    default <T extends SectionItem> Iterator<T> getItems(SectionType<T> sectionType) {
        Section<T> section = getSection(sectionType);
        if (section != null) {
            return section.iterator();
        }
        return EmptyIterator.of();
    }
    default int getCount(SectionType<?> sectionType) {
        Section<?> section = getSection(sectionType);
        if (section != null) {
            return section.getCount();
        }
        return 0;
    }

    void addMarker(Marker marker);
    @Override
    Iterator<Marker> getMarkers();
    Iterator<DexSectionInfo> getSectionInfo();

    default DexSectionInfo getSectionInfo(SectionType<?> sectionType) {
        Iterator<DexSectionInfo> iterator = getSectionInfo();
        while (iterator.hasNext()) {
            DexSectionInfo sectionInfo = iterator.next();
            if (ObjectsUtil.equals(sectionType, sectionInfo.getSectionType())) {
                return sectionInfo;
            }
        }
        return null;
    }

    @Override
    <T1 extends SectionItem> boolean removeEntries(SectionType<T1> sectionType, Predicate<T1> filter);

    @Override
    <T1 extends SectionItem> boolean removeEntriesWithKey(SectionType<T1> sectionType, Predicate<? super Key> filter);

    @Override
    <T1 extends SectionItem> boolean removeEntry(SectionType<T1> sectionType, Key key);

    @Override
    boolean removeClasses(Predicate<? super DexClass> filter);

    @Override
    void clearPoolMap();

    @Override
    int shrink();
}
