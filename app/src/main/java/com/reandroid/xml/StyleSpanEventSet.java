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

import com.reandroid.utils.CompareUtil;
import com.reandroid.utils.collection.ArrayCollection;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class StyleSpanEventSet {

    private final List<StyleSpanEvent> eventList;

    public StyleSpanEventSet(){
        this.eventList = new ArrayCollection<>();
    }

    public void addChar(char ch){
        add(new StyleSpanEvent(ch));
    }
    public void addStart(Span span){
        add(new StyleSpanEvent(StyleSpanEvent.TYPE_START_TAG, span));
    }
    public void addEnd(Span span){
        add(new StyleSpanEvent(StyleSpanEvent.TYPE_END_TAG, span));
    }
    public void addStartEnd(Span span){
        add(new StyleSpanEvent(StyleSpanEvent.TYPE_START_END, span));
    }
    public void add(StyleSpanEvent styleSpanEvent){
        this.eventList.add(styleSpanEvent);
    }
    private List<StyleSpanEvent> getEventList() {
        List<StyleSpanEvent> eventList = this.eventList;
        java.util.Collections.sort(eventList, CompareUtil.getComparableComparator());
        return eventList;
    }

    public void serialize(XmlSerializer serializer) throws IOException{
        List<StyleSpanEvent> eventList = getEventList();
        int size = eventList.size();
        for(int i = 0; i < size; i++){
            StyleSpanEvent event = eventList.get(i);
            event.serialize(serializer);
        }
    }
    public static StyleDocument serialize(String text, SpanSet<?> spanSet) {
        try{
            StyleSpanEventSet[] holders = create(text, spanSet);
            StyleDocument document = new StyleDocument();
            DocumentSerializer serializer = new DocumentSerializer(document);
            for(StyleSpanEventSet holder : holders){
                holder.serialize(serializer);
            }
            return document;
        }catch (IOException ignored){
            return null;
        }
    }
    private static StyleSpanEventSet[] create(String text, SpanSet<?> spanSet){
        StyleSpanEventSet[] results = create(text);
        fill(results, spanSet);
        return results;
    }
    private static StyleSpanEventSet[] create(String text){
        int length = text.length();
        StyleSpanEventSet[] results = new StyleSpanEventSet[length + 1];
        for(int i = 0; i < length; i++){
            StyleSpanEventSet eventSet = new StyleSpanEventSet();
            eventSet.addChar(text.charAt(i));
            results[i] = eventSet;
        }
        results[length] = new StyleSpanEventSet();
        return results;
    }
    private static void fill(StyleSpanEventSet[] spanEventSets, SpanSet<?> spanSet){
        Iterator<?> iterator = spanSet.getSpans();
        while (iterator.hasNext()){
            Span span = (Span) iterator.next();
            int start = span.getFirstChar();
            if(start >= spanEventSets.length){
                continue;
            }
            int end = span.getLastChar();
            if(end >= spanEventSets.length){
                continue;
            }
            StyleSpanEventSet eventSet = spanEventSets[start];
            if(start >= end){
                eventSet.addStartEnd(span);
                continue;
            }
            eventSet.addStart(span);
            eventSet = spanEventSets[end];
            eventSet.addEnd(span);
        }
    }
}
