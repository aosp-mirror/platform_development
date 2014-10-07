package com.android.tools.rmtypedefs;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import junit.framework.TestCase;

import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Permission;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static java.io.File.separatorChar;

@SuppressWarnings("SpellCheckingInspection")
public class RmTypeDefsTest extends TestCase {
    public void test() throws IOException {
        // Creates a test class containing various typedefs, as well as the @IntDef annotation
        // itself (to make the test case independent of the SDK), and compiles this using
        // ECJ. It then runs the RmTypeDefs tool on the resulting output directory, and
        // finally verifies that the tool exits with a 0 exit code.

        File dir = Files.createTempDir();
        String testClass = ""
            + "package test.pkg;\n"
            + "\n"
            + "import android.annotation.IntDef;\n"
            + "\n"
            + "import java.lang.annotation.Retention;\n"
            + "import java.lang.annotation.RetentionPolicy;\n"
            + "\n"
            + "@SuppressWarnings({\"UnusedDeclaration\",\"JavaDoc\"})\n"
            + "public class TestClass {\n"
            + "    /** @hide */\n"
            + "    @Retention(RetentionPolicy.SOURCE)\n"
            + "    @IntDef(flag = true,\n"
            + "            value = {\n"
            + "                    DISPLAY_USE_LOGO,\n"
            + "                    DISPLAY_SHOW_HOME,\n"
            + "                    DISPLAY_HOME_AS_UP,\n"
            + "                    DISPLAY_SHOW_TITLE,\n"
            + "                    DISPLAY_SHOW_CUSTOM,\n"
            + "                    DISPLAY_TITLE_MULTIPLE_LINES\n"
            + "            })\n"
            + "    public @interface DisplayOptions {}\n"
            + "\n"
            + "    public static final int DISPLAY_USE_LOGO = 0x1;\n"
            + "    public static final int DISPLAY_SHOW_HOME = 0x2;\n"
            + "    public static final int DISPLAY_HOME_AS_UP = 0x4;\n"
            + "    public static final int DISPLAY_SHOW_TITLE = 0x8;\n"
            + "    public static final int DISPLAY_SHOW_CUSTOM = 0x10;\n"
            + "    public static final int DISPLAY_TITLE_MULTIPLE_LINES = 0x20;\n"
            + "\n"
            + "    public void setDisplayOptions(@DisplayOptions int options) {\n"
            + "        System.out.println(\"setDisplayOptions \" + options);\n"
            + "    }\n"
            + "    public void setDisplayOptions(@DisplayOptions int options, @DisplayOptions int mask) {\n"
            + "        System.out.println(\"setDisplayOptions \" + options + \", mask=\" + mask);\n"
            + "    }\n"
            + "\n"
            + "    public static class StaticInnerClass {\n"
            + "        int mViewFlags = 0;\n"
            + "        static final int VISIBILITY_MASK = 0x0000000C;\n"
            + "\n"
            + "        /** @hide */\n"
            + "        @IntDef({VISIBLE, INVISIBLE, GONE})\n"
            + "        @Retention(RetentionPolicy.SOURCE)\n"
            + "        public @interface Visibility {}\n"
            + "\n"
            + "        public static final int VISIBLE = 0x00000000;\n"
            + "        public static final int INVISIBLE = 0x00000004;\n"
            + "        public static final int GONE = 0x00000008;\n"
            + "\n"
            + "        @Visibility\n"
            + "        public int getVisibility() {\n"
            + "            return mViewFlags & VISIBILITY_MASK;\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public static class Inherits extends StaticInnerClass {\n"
            + "        @Override\n"
            + "        @Visibility\n"
            + "        public int getVisibility() {\n"
            + "            return 0;\n"
            + "        }\n"
            + "    }\n"
            + "}\n";
        String intdef = ""
            + "package android.annotation;\n"
            + "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n"
            + "@java.lang.annotation.Target({java.lang.annotation.ElementType.ANNOTATION_TYPE})\n"
            + "public @interface IntDef {\n"
            + "    long[] value() default {};\n"
            + "    boolean flag() default false;\n"
            + "}";

        File srcDir = new File(dir, "test" + File.separator + "pkg");
        boolean mkdirs = srcDir.mkdirs();
        assertTrue(mkdirs);
        File srcFile1 = new File(srcDir, "TestClass.java");
        Files.write(testClass, srcFile1, Charsets.UTF_8);

        srcDir = new File(dir, "android" + File.separator + "annotation");
        mkdirs = srcDir.mkdirs();
        assertTrue(mkdirs);
        File srcFile2 = new File(srcDir, "IntDef.java");
        Files.write(intdef, srcFile2, Charsets.UTF_8);

        boolean compileSuccessful = BatchCompiler.compile(srcFile1 + " " + srcFile2 +
                        " -source 1.6 -target 1.6 -nowarn",
                new PrintWriter(System.out),
                new PrintWriter(System.err), null);
        assertTrue(compileSuccessful);

        assertEquals(""
            + "testDir/\n"
            + "    testDir/android/\n"
            + "        testDir/android/annotation/\n"
            + "            testDir/android/annotation/IntDef.class\n"
            + "            testDir/android/annotation/IntDef.java\n"
            + "    testDir/test/\n"
            + "        testDir/test/pkg/\n"
            + "            testDir/test/pkg/TestClass$DisplayOptions.class\n"
            + "            testDir/test/pkg/TestClass$Inherits.class\n"
            + "            testDir/test/pkg/TestClass$StaticInnerClass$Visibility.class\n"
            + "            testDir/test/pkg/TestClass$StaticInnerClass.class\n"
            + "            testDir/test/pkg/TestClass.class\n"
            + "            testDir/test/pkg/TestClass.java\n",
            getDirectoryContents(dir));

        // Trap System.exit calls:
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
            }
            @Override
            public void checkPermission(Permission perm, Object context) {
            }
            @Override
            public void checkExit(int status) {
                throw new ExitException(status);
            }
        });
        try {
            RmTypeDefs.main(new String[]{"--verbose", dir.getPath()});
        } catch (ExitException e) {
            assertEquals(0, e.getStatus());
        }
        System.setSecurityManager(null);

        // TODO: check that the classes are identical
        // BEFORE removal

        assertEquals(""
                + "testDir/\n"
                + "    testDir/android/\n"
                + "        testDir/android/annotation/\n"
                + "            testDir/android/annotation/IntDef.class\n"
                + "            testDir/android/annotation/IntDef.java\n"
                + "    testDir/test/\n"
                + "        testDir/test/pkg/\n"
                + "            testDir/test/pkg/TestClass$Inherits.class\n"
                + "            testDir/test/pkg/TestClass$StaticInnerClass.class\n"
                + "            testDir/test/pkg/TestClass.class\n"
                + "            testDir/test/pkg/TestClass.java\n",
                getDirectoryContents(dir));

        // Make sure the Visibility symbol is completely gone from the outer class
        assertDoesNotContainBytes(new File(dir,
                "test/pkg/TestClass$StaticInnerClass.class".replace('/', separatorChar)),
                "Visibility");

        deleteDir(dir);
    }

    private void assertDoesNotContainBytes(File file, String sub) throws IOException {
        byte[] contents = Files.toByteArray(file);
        // Like the strings command, look for 4 or more consecutive printable characters
        for (int i = 0, n = contents.length; i < n; i++) {
            if (Character.isJavaIdentifierStart(contents[i])) {
                for (int j = i + 1; j < n; j++) {
                    if (!Character.isJavaIdentifierPart(contents[j])) {
                        if (j > i + 4) {
                            int length = j - i - 1;
                            if (length == sub.length()) {
                                String symbol = new String(contents, i, length, UTF_8);
                                assertFalse("Found " + sub + " in class file " + file,
                                        sub.equals(symbol));
                            }
                        }
                        i = j;
                        break;
                    }
                }
            }
        }
    }

    String getDirectoryContents(File root) {
        StringBuilder sb = new StringBuilder();
        list(sb, root, "", 0, "testDir");
        return sb.toString();
    }

    private void list(StringBuilder sb, File file, String prefix, int depth, String rootName) {
        for (int i = 0; i < depth; i++) {
            sb.append("    ");
        }

        if (!prefix.isEmpty()) {
            sb.append(prefix);
        }
        String fileName = file.getName();
        if (depth == 0 && rootName != null) { // avoid temp-name
            fileName = rootName;
        }
        sb.append(fileName);
        if (file.isDirectory()) {
            sb.append('/');
            sb.append('\n');
            File[] files = file.listFiles();
            if (files != null) {
                List<File> children = Lists.newArrayList();
                Collections.addAll(children, files);
                Collections.sort(children, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                prefix = prefix + fileName + "/";
                for (File child : children) {
                    list(sb, child, prefix, depth + 1, rootName);
                }
            }
        } else {
            sb.append('\n');
        }
    }

    /**
     * Recursive delete directory. Mostly for fake SDKs.
     *
     * @param root directory to delete
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteDir(File root) {
        if (root.exists()) {
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            root.delete();
        }
    }

    private static class ExitException extends SecurityException {
        private static final long serialVersionUID = 1L;

        private final int mStatus;

        public ExitException(int status) {
            super("Unit test");
            mStatus = status;
        }

        public int getStatus() {
            return mStatus;
        }
    }
}
