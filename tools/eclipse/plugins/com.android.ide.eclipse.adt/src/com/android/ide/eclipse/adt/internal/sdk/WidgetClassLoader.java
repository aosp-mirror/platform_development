/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.sdk;

import com.android.ide.eclipse.adt.AndroidConstants;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.management.InvalidAttributeValueException;

/**
 * Parser for the text file containing the list of widgets, layouts and layout params.
 * <p/>
 * The file is a straight text file containing one class per line.<br>
 * Each line is in the following format<br>
 * <code>[code][class name] [super class name] [super class name]...</code>
 * where code is a single letter (W for widget, L for layout, P for layout params), and class names
 * are the fully qualified name of the classes.
 */
public final class WidgetClassLoader implements IAndroidClassLoader {
    
    /**
     * Basic class containing the class descriptions found in the text file. 
     */
    private final static class ClassDescriptor implements IClassDescriptor {
        
        private String mName;
        private String mSimpleName;
        private ClassDescriptor mSuperClass;
        private ClassDescriptor mEnclosingClass;
        private final ArrayList<IClassDescriptor> mDeclaredClasses =
                new ArrayList<IClassDescriptor>();
        private boolean mIsInstantiable = false;

        ClassDescriptor(String fqcn) {
            mName = fqcn;
            mSimpleName = getSimpleName(fqcn);
        }

        public String getCanonicalName() {
            return mName;
        }

        public String getSimpleName() {
            return mSimpleName;
        }

        public IClassDescriptor[] getDeclaredClasses() {
            return mDeclaredClasses.toArray(new IClassDescriptor[mDeclaredClasses.size()]);
        }

        private void addDeclaredClass(ClassDescriptor declaredClass) {
            mDeclaredClasses.add(declaredClass);
        }

        public IClassDescriptor getEnclosingClass() {
            return mEnclosingClass;
        }
        
        void setEnclosingClass(ClassDescriptor enclosingClass) {
            // set the enclosing class.
            mEnclosingClass = enclosingClass;
            
            // add this to the list of declared class in the enclosing class.
            mEnclosingClass.addDeclaredClass(this);
            
            // finally change the name of declared class to make sure it uses the
            // convention: package.enclosing$declared instead of package.enclosing.declared
            mName = enclosingClass.mName + "$" + mName.substring(enclosingClass.mName.length() + 1);
        }

        public IClassDescriptor getSuperclass() {
            return mSuperClass;
        }
        
        void setSuperClass(ClassDescriptor superClass) {
            mSuperClass = superClass;
        }
        
        @Override
        public boolean equals(Object clazz) {
            if (clazz instanceof ClassDescriptor) {
                return mName.equals(((ClassDescriptor)clazz).mName);
            }
            return super.equals(clazz);
        }
        
        @Override
        public int hashCode() {
            return mName.hashCode();
        }
        
        public boolean isInstantiable() {
            return mIsInstantiable;
        }
        
        void setInstantiable(boolean state) {
            mIsInstantiable = state;
        }
        
        private String getSimpleName(String fqcn) {
            String[] segments = fqcn.split("\\.");
            return segments[segments.length-1];
        }
    }

    private BufferedReader mReader;

    /** Output map of FQCN => descriptor on all classes */
    private final Map<String, ClassDescriptor> mMap = new TreeMap<String, ClassDescriptor>();
    /** Output map of FQCN => descriptor on View classes */
    private final Map<String, ClassDescriptor> mWidgetMap = new TreeMap<String, ClassDescriptor>();
    /** Output map of FQCN => descriptor on ViewGroup classes */
    private final Map<String, ClassDescriptor> mLayoutMap = new TreeMap<String, ClassDescriptor>();
    /** Output map of FQCN => descriptor on LayoutParams classes */
    private final Map<String, ClassDescriptor> mLayoutParamsMap =
        new HashMap<String, ClassDescriptor>();
    /** File path of the source text file */
    private String mOsFilePath;

    /**
     * Creates a loader with a given file path.
     * @param osFilePath the OS path of the file to load.
     * @throws FileNotFoundException if the file is not found.
     */
    WidgetClassLoader(String osFilePath) throws FileNotFoundException {
        mOsFilePath = osFilePath;
        mReader = new BufferedReader(new FileReader(osFilePath));
    }

    public String getSource() {
        return mOsFilePath;
    }
    
    /**
     * Parses the text file and return true if the file was successfully parsed.
     * @param monitor
     */
    boolean parseWidgetList(IProgressMonitor monitor) {
        try {
            String line;
            while ((line = mReader.readLine()) != null) {
                if (line.length() > 0) {
                    char prefix = line.charAt(0);
                    String[] classes = null;
                    ClassDescriptor clazz = null;
                    switch (prefix) {
                        case 'W':
                            classes = line.substring(1).split(" ");
                            clazz = processClass(classes, 0, null /* map */);
                            if (clazz != null) {
                                clazz.setInstantiable(true);
                                mWidgetMap.put(classes[0], clazz);
                            }
                            break;
                        case 'L':
                            classes = line.substring(1).split(" ");
                            clazz = processClass(classes, 0, null /* map */);
                            if (clazz != null) {
                                clazz.setInstantiable(true);
                                mLayoutMap.put(classes[0], clazz);
                            }
                            break;
                        case 'P':
                            classes = line.substring(1).split(" ");
                            clazz = processClass(classes, 0, mLayoutParamsMap);
                            if (clazz != null) {
                                clazz.setInstantiable(true);
                            }
                            break;
                        case '#':
                            // comment, do nothing
                            break;
                        default:
                                throw new IllegalArgumentException();
                    }
                }
            }
            
            // reconciliate the layout and their layout params
            postProcess();
            
            return true;
        } catch (IOException e) {
        } finally {
            try {
                mReader.close();
            } catch (IOException e) {
            }
        }
        
        return false;
    }
    
    /**
     * Parses a View class and adds a ViewClassInfo for it in mWidgetMap.
     * It calls itself recursively to handle super classes which are also Views.
     * @param classes the inheritance list of the class to process.
     * @param index the index of the class to process in the <code>classes</code> array.
     * @param map an optional map in which to put every {@link ClassDescriptor} created.
     */
    private ClassDescriptor processClass(String[] classes, int index,
            Map<String, ClassDescriptor> map) {
        if (index >= classes.length) {
            return null;
        }
        
        String fqcn = classes[index];
        
        if ("java.lang.Object".equals(fqcn)) { //$NON-NLS-1$
            return null;
        }

        // check if the ViewInfoClass has not yet been created.
        if (mMap.containsKey(fqcn)) {
            return mMap.get(fqcn);
        }

        // create the custom class.
        ClassDescriptor clazz = new ClassDescriptor(fqcn);
        mMap.put(fqcn, clazz);
        if (map != null) {
            map.put(fqcn, clazz);
        }
        
        // get the super class
        ClassDescriptor superClass = processClass(classes, index+1, map);
        if (superClass != null) {
            clazz.setSuperClass(superClass);
        }
        
        return clazz;
    }
    
    /**
     * Goes through the layout params and look for the enclosed class. If the layout params
     * has no known enclosed type it is dropped.
     */
    private void postProcess() {
        Collection<ClassDescriptor> params = mLayoutParamsMap.values();

        for (ClassDescriptor param : params) {
            String fqcn = param.getCanonicalName();
            
            // get the enclosed name.
            String enclosed = getEnclosedName(fqcn);
            
            // look for a match in the layouts. We don't use the layout map as it only contains the
            // end classes, but in this case we also need to process the layout params for the base
            // layout classes.
            ClassDescriptor enclosingType = mMap.get(enclosed);
            if (enclosingType != null) {
                param.setEnclosingClass(enclosingType);
                
                // remove the class from the map, and put it back with the fixed name
                mMap.remove(fqcn);
                mMap.put(param.getCanonicalName(), param);
            }
        }
    }

    private String getEnclosedName(String fqcn) {
        int index = fqcn.lastIndexOf('.');
        return fqcn.substring(0, index);
    }

    /**
     * Finds and loads all classes that derive from a given set of super classes.
     * 
     * @param rootPackage Root package of classes to find. Use an empty string to find everyting.
     * @param superClasses The super classes of all the classes to find. 
     * @return An hash map which keys are the super classes looked for and which values are
     *         ArrayList of the classes found. The array lists are always created for all the
     *         valid keys, they are simply empty if no deriving class is found for a given
     *         super class. 
     * @throws IOException
     * @throws InvalidAttributeValueException
     * @throws ClassFormatError
     */
    public HashMap<String, ArrayList<IClassDescriptor>> findClassesDerivingFrom(String rootPackage,
            String[] superClasses) throws IOException, InvalidAttributeValueException,
            ClassFormatError {
        HashMap<String, ArrayList<IClassDescriptor>> map =
                new HashMap<String, ArrayList<IClassDescriptor>>();
        
        ArrayList<IClassDescriptor> list = new ArrayList<IClassDescriptor>();
        list.addAll(mWidgetMap.values());
        map.put(AndroidConstants.CLASS_VIEW, list);
        
        list = new ArrayList<IClassDescriptor>();
        list.addAll(mLayoutMap.values());
        map.put(AndroidConstants.CLASS_VIEWGROUP, list);

        list = new ArrayList<IClassDescriptor>();
        list.addAll(mLayoutParamsMap.values());
        map.put(AndroidConstants.CLASS_VIEWGROUP_LAYOUTPARAMS, list);

        return map;
    }

    /**
     * Returns a {@link IAndroidClassLoader.IClassDescriptor} by its fully-qualified name.
     * @param className the fully-qualified name of the class to return.
     * @throws ClassNotFoundException
     */
    public IClassDescriptor getClass(String className) throws ClassNotFoundException {
        return mMap.get(className);
    }

}
