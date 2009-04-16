/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package data;

import org.w3c.dom.css.Rect;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 */
public class TestTemplateClass<T extends InputStream, U> {

    private final Map<T, U> mMap_T_U = null;
    
    public Map<ArrayList<T>, Map<String, ArrayList<U>>> mMap_T_S_U = null;
    
    public TestTemplateClass() {
    }
    
    public Map<T, U> getMap_T_U() {
        return mMap_T_U;
    }
    
    public Map<ArrayList<T>, Map<String, ArrayList<U>>> getMap_T_S_U() {
        return mMap_T_S_U;
    }

    public void draw(List<? extends Rect> shape) {
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
    }

    public <X extends T, Y> void getMap(List<T> list, Map<T, U> tu, Map<X, Set<? super Y>> xy) {
    }
}
