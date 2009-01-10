/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.sdk;

import com.android.ide.eclipse.common.AndroidConstants;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.management.InvalidAttributeValueException;

/**
 * Custom class loader able to load a class from the SDK jar file.
 */
public class AndroidJarLoader extends ClassLoader implements IAndroidClassLoader {
    
    /**
     * Wrapper around a {@link Class} to provide the methods of
     * {@link IAndroidClassLoader.IClassDescriptor}.
     */
    public final static class ClassWrapper implements IClassDescriptor {
        private Class<?> mClass;

        public ClassWrapper(Class<?> clazz) {
            mClass = clazz;
        }

        public String getCanonicalName() {
            return mClass.getCanonicalName();
        }

        public IClassDescriptor[] getDeclaredClasses() {
            Class<?>[] classes = mClass.getDeclaredClasses();
            IClassDescriptor[] iclasses = new IClassDescriptor[classes.length];
            for (int i = 0 ; i < classes.length ; i++) {
                iclasses[i] = new ClassWrapper(classes[i]);
            }

            return iclasses;
        }

        public IClassDescriptor getEnclosingClass() {
            return new ClassWrapper(mClass.getEnclosingClass());
        }

        public String getSimpleName() {
            return mClass.getSimpleName();
        }

        public IClassDescriptor getSuperclass() {
            return new ClassWrapper(mClass.getSuperclass());
        }
        
        @Override
        public boolean equals(Object clazz) {
            if (clazz instanceof ClassWrapper) {
                return mClass.equals(((ClassWrapper)clazz).mClass);
            }
            return super.equals(clazz);
        }
        
        @Override
        public int hashCode() {
            return mClass.hashCode();
        }


        public boolean isInstantiable() {
            int modifiers = mClass.getModifiers();
            return Modifier.isAbstract(modifiers) == false && Modifier.isPublic(modifiers) == true;
        }

        public Class<?> wrappedClass() {
            return mClass;
        }

    }
    
    private String mOsFrameworkLocation;
    
    /** A cache for binary data extracted from the zip */
    private final HashMap<String, byte[]> mEntryCache = new HashMap<String, byte[]>();
    /** A cache for already defined Classes */
    private final HashMap<String, Class<?> > mClassCache = new HashMap<String, Class<?> >();
    
    /**
     * Creates the class loader by providing the os path to the framework jar archive
     * 
     * @param osFrameworkLocation OS Path of the framework JAR file
     */
    public AndroidJarLoader(String osFrameworkLocation) {
        super();
        mOsFrameworkLocation = osFrameworkLocation;
    }
    
    public String getSource() {
        return mOsFrameworkLocation;
    }
    
    /**
     * Pre-loads all class binary data that belong to the given package by reading the archive
     * once and caching them internally.
     * <p/>
     * This does not actually preload "classes", it just reads the unzipped bytes for a given
     * class. To obtain a class, one must call {@link #findClass(String)} later.
     * <p/>
     * All classes which package name starts with "packageFilter" will be included and can be
     * found later.
     * <p/>
     * May throw some exceptions if the framework JAR cannot be read.
     * 
     * @param packageFilter The package that contains all the class data to preload, using a fully
     *                    qualified binary name (.e.g "com.my.package."). The matching algorithm
     *                    is simple "startsWith". Use an empty string to include everything.
     * @param taskLabel An optional task name for the sub monitor. Can be null.
     * @param monitor A progress monitor. Can be null. Caller is responsible for calling done.
     * @throws IOException
     * @throws InvalidAttributeValueException
     * @throws ClassFormatError
     */
    public void preLoadClasses(String packageFilter, String taskLabel, IProgressMonitor monitor)
        throws IOException, InvalidAttributeValueException, ClassFormatError {
        // Transform the package name into a zip entry path
        String pathFilter = packageFilter.replaceAll("\\.", "/"); //$NON-NLS-1$ //$NON-NLS-2$
        
        SubMonitor progress = SubMonitor.convert(monitor, taskLabel == null ? "" : taskLabel, 100);
        
        // create streams to read the intermediary archive
        FileInputStream fis = new FileInputStream(mOsFrameworkLocation);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry;       
        while ((entry = zis.getNextEntry()) != null) {
            // get the name of the entry.
            String entryPath = entry.getName();
            
            if (!entryPath.endsWith(AndroidConstants.DOT_CLASS)) {
                // only accept class files
                continue;
            }

            // check if it is part of the package to preload
            if (pathFilter.length() > 0 && !entryPath.startsWith(pathFilter)) {
                continue;
            }
            String className = entryPathToClassName(entryPath);

            if (!mEntryCache.containsKey(className)) {
                long entrySize = entry.getSize();
                if (entrySize > Integer.MAX_VALUE) {
                    throw new InvalidAttributeValueException();
                }
                byte[] data = readZipData(zis, (int)entrySize);
                mEntryCache.put(className, data);
            }

            // advance 5% of whatever is allocated on the progress bar
            progress.setWorkRemaining(100);
            progress.worked(5);
            progress.subTask(String.format("Preload %1$s", className));
        }
    }

    /**
     * Finds and loads all classes that derive from a given set of super classes.
     * <p/>
     * As a side-effect this will load and cache most, if not all, classes in the input JAR file.
     * 
     * @param packageFilter Base name of package of classes to find.
     *                      Use an empty string to find everyting.
     * @param superClasses The super classes of all the classes to find. 
     * @return An hash map which keys are the super classes looked for and which values are
     *         ArrayList of the classes found. The array lists are always created for all the
     *         valid keys, they are simply empty if no deriving class is found for a given
     *         super class. 
     * @throws IOException
     * @throws InvalidAttributeValueException
     * @throws ClassFormatError
     */
    public HashMap<String, ArrayList<IClassDescriptor>> findClassesDerivingFrom(
            String packageFilter,
            String[] superClasses)
            throws IOException, InvalidAttributeValueException, ClassFormatError {

        packageFilter = packageFilter.replaceAll("\\.", "/"); //$NON-NLS-1$ //$NON-NLS-2$

        HashMap<String, ArrayList<IClassDescriptor>> mClassesFound =
                new HashMap<String, ArrayList<IClassDescriptor>>();

        for (String className : superClasses) {
            mClassesFound.put(className, new ArrayList<IClassDescriptor>());
        }

        // create streams to read the intermediary archive
        FileInputStream fis = new FileInputStream(mOsFrameworkLocation);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            // get the name of the entry and convert to a class binary name
            String entryPath = entry.getName();
            if (!entryPath.endsWith(AndroidConstants.DOT_CLASS)) {
                // only accept class files
                continue;
            }
            if (packageFilter.length() > 0 && !entryPath.startsWith(packageFilter)) {
                // only accept stuff from the requested root package.
                continue;
            }
            String className = entryPathToClassName(entryPath);
      
            Class<?> loaded_class = mClassCache.get(className);
            if (loaded_class == null) {
                byte[] data = mEntryCache.get(className);
                if (data == null) {    
                    // Get the class and cache it
                    long entrySize = entry.getSize();
                    if (entrySize > Integer.MAX_VALUE) {
                        throw new InvalidAttributeValueException();
                    }
                    data = readZipData(zis, (int)entrySize);
                }
                loaded_class = defineAndCacheClass(className, data);
            }

            for (Class<?> superClass = loaded_class.getSuperclass();
                    superClass != null;
                    superClass = superClass.getSuperclass()) {
                String superName = superClass.getCanonicalName();
                if (mClassesFound.containsKey(superName)) {
                    mClassesFound.get(superName).add(new ClassWrapper(loaded_class));
                    break;
                }
            }
        }

        return mClassesFound;
    }

    /** Helper method that converts a Zip entry path into a corresponding
     *  Java full qualified binary class name.
     *  <p/>
     *  F.ex, this converts "com/my/package/Foo.class" into "com.my.package.Foo".
     */
    private String entryPathToClassName(String entryPath) {
        return entryPath.replaceFirst("\\.class$", "").replaceAll("[/\\\\]", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Finds the class with the specified binary name.
     * 
     * {@inheritDoc}
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // try to find the class in the cache
            Class<?> cached_class = mClassCache.get(name);
            if (cached_class == ClassNotFoundException.class) {
                // we already know we can't find this class, don't try again
                throw new ClassNotFoundException(name);
            } else if (cached_class != null) {
                return cached_class;
            }
            
            // if not found, look it up and cache it
            byte[] data = loadClassData(name);
            if (data != null) {
                return defineAndCacheClass(name, data);
            } else {
                // if the class can't be found, record a CNFE class in the map so
                // that we don't try to reload it next time
                mClassCache.put(name, ClassNotFoundException.class);
                throw new ClassNotFoundException(name);
            }
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ClassNotFoundException(e.getMessage()); 
        }
    }

    /**
     * Defines a class based on its binary data and caches the resulting class object.
     * 
     * @param name The binary name of the class (i.e. package.class1$class2)
     * @param data The binary data from the loader.
     * @return The class defined
     * @throws ClassFormatError if defineClass failed.
     */
    private Class<?> defineAndCacheClass(String name, byte[] data) throws ClassFormatError {
        Class<?> cached_class;
        cached_class = defineClass(null, data, 0, data.length);

        if (cached_class != null) {
            // Add new class to the cache class and remove it from the zip entry data cache
            mClassCache.put(name, cached_class);
            mEntryCache.remove(name);
        }
        return cached_class;
    }
    
    /**
     * Loads a class data from its binary name.
     * <p/>
     * This uses the class binary data that has been preloaded earlier by the preLoadClasses()
     * method if possible.
     * 
     * @param className the binary name
     * @return an array of bytes representing the class data or null if not found
     * @throws InvalidAttributeValueException 
     * @throws IOException 
     */
    private synchronized byte[] loadClassData(String className)
            throws InvalidAttributeValueException, IOException {

        byte[] data = mEntryCache.get(className);
        if (data != null) {
            return data;
        }
        
        // The name is a binary name. Something like "android.R", or "android.R$id".
        // Make a path out of it.
        String entryName = className.replaceAll("\\.", "/") + AndroidConstants.DOT_CLASS; //$NON-NLS-1$ //$NON-NLS-2$

       // create streams to read the intermediary archive
        FileInputStream fis = new FileInputStream(mOsFrameworkLocation);
        ZipInputStream zis = new ZipInputStream(fis);
        
        // loop on the entries of the intermediary package and put them in the final package.
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
            // get the name of the entry.
            String currEntryName = entry.getName();
            
            if (currEntryName.equals(entryName)) {
                long entrySize = entry.getSize();
                if (entrySize > Integer.MAX_VALUE) {
                    throw new InvalidAttributeValueException();
                }

                data = readZipData(zis, (int)entrySize);
                return data;
            }
        }

        return null;
    }

    /**
     * Reads data for the <em>current</em> entry from the zip input stream.
     * 
     * @param zis The Zip input stream
     * @param entrySize The entry size. -1 if unknown.
     * @return The new data for the <em>current</em> entry.
     * @throws IOException If ZipInputStream.read() fails.
     */
    private byte[] readZipData(ZipInputStream zis, int entrySize) throws IOException {
        int block_size = 1024;
        int data_size = entrySize < 1 ? block_size : entrySize; 
        int offset = 0;
        byte[] data = new byte[data_size];
        
        while(zis.available() != 0) {
            int count = zis.read(data, offset, data_size - offset);
            if (count < 0) {  // read data is done
                break;
            }
            offset += count;
            
            if (entrySize >= 1 && offset >= entrySize) {  // we know the size and we're done
                break;
            }

            // if we don't know the entry size and we're not done reading,
            // expand the data buffer some more.
            if (offset >= data_size) {
                byte[] temp = new byte[data_size + block_size];
                System.arraycopy(data, 0, temp, 0, data_size);
                data_size += block_size;
                data = temp;
                block_size *= 2;
            }
        }
        
        if (offset < data_size) {
            // buffer was allocated too large, trim it
            byte[] temp = new byte[offset];
            if (offset > 0) {
                System.arraycopy(data, 0, temp, 0, offset);
            }
            data = temp;
        }
        
        return data;
    }

    /**
     * Returns a {@link IAndroidClassLoader.IClassDescriptor} by its fully-qualified name.
     * @param className the fully-qualified name of the class to return.
     * @throws ClassNotFoundException
     */
    public IClassDescriptor getClass(String className) throws ClassNotFoundException {
        try {
            return new ClassWrapper(loadClass(className));
        } catch (ClassNotFoundException e) {
            throw e;  // useful for debugging
        }
    }
}
