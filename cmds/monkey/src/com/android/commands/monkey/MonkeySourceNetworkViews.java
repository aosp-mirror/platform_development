/*
 * Copyright 2011, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.monkey;

import static com.android.commands.monkey.MonkeySourceNetwork.EARG;

import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.graphics.Rect;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.commands.monkey.MonkeySourceNetwork.CommandQueue;
import com.android.commands.monkey.MonkeySourceNetwork.MonkeyCommand;
import com.android.commands.monkey.MonkeySourceNetwork.MonkeyCommandReturn;

import dalvik.system.DexClassLoader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class that enables Monkey to perform view introspection when issued Monkey Network
 * Script commands over the network.
 */
public class MonkeySourceNetworkViews {

    protected static android.app.UiAutomation sUiTestAutomationBridge;

    private static IPackageManager sPm =
            IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    private static Map<String, Class<?>> sClassMap = new HashMap<String, Class<?>>();

    private static final String HANDLER_THREAD_NAME = "UiAutomationHandlerThread";

    private static final String REMOTE_ERROR =
            "Unable to retrieve application info from PackageManager";
    private static final String CLASS_NOT_FOUND = "Error retrieving class information";
    private static final String NO_ACCESSIBILITY_EVENT = "No accessibility event has occured yet";
    private static final String NO_NODE = "Node with given ID does not exist";
    private static final String NO_CONNECTION = "Failed to connect to AccessibilityService, "
                                                + "try restarting Monkey";

    private static final Map<String, ViewIntrospectionCommand> COMMAND_MAP =
            new HashMap<String, ViewIntrospectionCommand>();

    /* Interface for view queries */
    private static interface ViewIntrospectionCommand {
        /**
         * Get the response to the query
         * @return the response to the query
         */
        public MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args);
    }

    static {
        COMMAND_MAP.put("getlocation", new GetLocation());
        COMMAND_MAP.put("gettext", new GetText());
        COMMAND_MAP.put("getclass", new GetClass());
        COMMAND_MAP.put("getchecked", new GetChecked());
        COMMAND_MAP.put("getenabled", new GetEnabled());
        COMMAND_MAP.put("getselected", new GetSelected());
        COMMAND_MAP.put("setselected", new SetSelected());
        COMMAND_MAP.put("getfocused", new GetFocused());
        COMMAND_MAP.put("setfocused", new SetFocused());
        COMMAND_MAP.put("getparent", new GetParent());
        COMMAND_MAP.put("getchildren", new GetChildren());
        COMMAND_MAP.put("getaccessibilityids", new GetAccessibilityIds());
    }

    private static final HandlerThread sHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);

    /**
     * Registers the event listener for AccessibilityEvents.
     * Also sets up a communication connection so we can query the
     * accessibility service.
     */
    public static void setup() {
        sHandlerThread.setDaemon(true);
        sHandlerThread.start();
        sUiTestAutomationBridge = new UiAutomation(sHandlerThread.getLooper(),
                new UiAutomationConnection());
        sUiTestAutomationBridge.connect();
    }

    public static void teardown() {
        sHandlerThread.quit();
    }

    /**
     * Get the ID class for the given package.
     * This will cause issues if people reload a package with different
     * resource identifiers, but don't restart the Monkey server.
     *
     * @param packageName The package that we want to retrieve the ID class for
     * @return The ID class for the given package
     */
    private static Class<?> getIdClass(String packageName, String sourceDir)
            throws ClassNotFoundException {
        // This kind of reflection is expensive, so let's only do it
        // if we need to
        Class<?> klass = sClassMap.get(packageName);
        if (klass == null) {
            DexClassLoader classLoader = new DexClassLoader(
                    sourceDir, "/data/local/tmp",
                    null, ClassLoader.getSystemClassLoader());
            klass = classLoader.loadClass(packageName + ".R$id");
            sClassMap.put(packageName, klass);
        }
        return klass;
    }

    private static AccessibilityNodeInfo getNodeByAccessibilityIds(
            String windowString, String viewString) {
        int windowId = Integer.parseInt(windowString);
        int viewId = Integer.parseInt(viewString);
        int connectionId = sUiTestAutomationBridge.getConnectionId();
        AccessibilityInteractionClient client = AccessibilityInteractionClient.getInstance();
        return client.findAccessibilityNodeInfoByAccessibilityId(connectionId, windowId, viewId,
                false, 0);
    }

    private static AccessibilityNodeInfo getNodeByViewId(String viewId) throws MonkeyViewException {
        int connectionId = sUiTestAutomationBridge.getConnectionId();
        AccessibilityInteractionClient client = AccessibilityInteractionClient.getInstance();
        List<AccessibilityNodeInfo> infos = client.findAccessibilityNodeInfosByViewId(
                connectionId, AccessibilityNodeInfo.ACTIVE_WINDOW_ID,
                AccessibilityNodeInfo.ROOT_NODE_ID, viewId);
        return (!infos.isEmpty()) ? infos.get(0) : null;
    }

    /**
     * Command to list all possible view ids for the given application.
     * This lists all view ids regardless if they are on screen or not.
     */
    public static class ListViewsCommand implements MonkeyCommand {
        //listviews
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            AccessibilityNodeInfo node = sUiTestAutomationBridge.getRootInActiveWindow();
            /* Occasionally the API will generate an event with no source, which is essentially the
             * same as it generating no event at all */
            if (node == null) {
                return new MonkeyCommandReturn(false, NO_ACCESSIBILITY_EVENT);
            }
            String packageName = node.getPackageName().toString();
            try{
                Class<?> klass;
                ApplicationInfo appInfo = sPm.getApplicationInfo(packageName, 0,
                        UserHandle.myUserId());
                klass = getIdClass(packageName, appInfo.sourceDir);
                StringBuilder fieldBuilder = new StringBuilder();
                Field[] fields = klass.getFields();
                for (Field field : fields) {
                    fieldBuilder.append(field.getName() + " ");
                }
                return new MonkeyCommandReturn(true, fieldBuilder.toString());
            } catch (RemoteException e){
                return new MonkeyCommandReturn(false, REMOTE_ERROR);
            } catch (ClassNotFoundException e){
                return new MonkeyCommandReturn(false, CLASS_NOT_FOUND);
            }
        }
    }

    /**
     * A command that allows for querying of views. It takes an id type, the requisite ids,
     * and the command for querying the view.
     */
    public static class QueryViewCommand implements MonkeyCommand {
        //queryview [id type] [id(s)] [command]
        //queryview viewid button1 gettext
        //queryview accessibilityids 12 5 getparent
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() > 2) {
                String idType = command.get(1);
                AccessibilityNodeInfo node;
                String viewQuery;
                List<String> args;
                if ("viewid".equals(idType)) {
                    try {
                        node = getNodeByViewId(command.get(2));
                        viewQuery = command.get(3);
                        args = command.subList(4, command.size());
                    } catch (MonkeyViewException e) {
                        return new MonkeyCommandReturn(false, e.getMessage());
                    }
                } else if (idType.equals("accessibilityids")) {
                    try {
                        node = getNodeByAccessibilityIds(command.get(2), command.get(3));
                        viewQuery = command.get(4);
                        args = command.subList(5, command.size());
                    } catch (NumberFormatException e) {
                        return EARG;
                    }
                } else {
                    return EARG;
                }
                if (node == null) {
                    return new MonkeyCommandReturn(false, NO_NODE);
                }
                ViewIntrospectionCommand getter = COMMAND_MAP.get(viewQuery);
                if (getter != null) {
                    return getter.query(node, args);
                } else {
                    return EARG;
                }
            }
            return EARG;
        }
    }

    /**
     * A command that returns the accessibility ids of the root view.
     */
    public static class GetRootViewCommand implements MonkeyCommand {
        // getrootview
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            AccessibilityNodeInfo node = sUiTestAutomationBridge.getRootInActiveWindow();
            return (new GetAccessibilityIds()).query(node, new ArrayList<String>());
        }
    }

    /**
     * A command that returns the accessibility ids of the views that contain the given text.
     * It takes a string of text and returns the accessibility ids of the nodes that contain the
     * text as a list of integers separated by spaces.
     */
    public static class GetViewsWithTextCommand implements MonkeyCommand {
        // getviewswithtext [text]
        // getviewswithtext "some text here"
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            if (command.size() == 2) {
                String text = command.get(1);
                int connectionId = sUiTestAutomationBridge.getConnectionId();
                List<AccessibilityNodeInfo> nodes = AccessibilityInteractionClient.getInstance()
                    .findAccessibilityNodeInfosByText(connectionId,
                            AccessibilityNodeInfo.ACTIVE_WINDOW_ID,
                            AccessibilityNodeInfo.ROOT_NODE_ID, text);
                ViewIntrospectionCommand idGetter = new GetAccessibilityIds();
                List<String> emptyArgs = new ArrayList<String>();
                StringBuilder ids = new StringBuilder();
                for (AccessibilityNodeInfo node : nodes) {
                    MonkeyCommandReturn result = idGetter.query(node, emptyArgs);
                    if (!result.wasSuccessful()){
                        return result;
                    }
                    ids.append(result.getMessage()).append(" ");
                }
                return new MonkeyCommandReturn(true, ids.toString());
            }
            return EARG;
        }
    }

    /**
     * Command to retrieve the location of the given node.
     * Returns the x, y, width and height of the view, separated by spaces.
     */
    public static class GetLocation implements ViewIntrospectionCommand {
        //queryview [id type] [id] getlocation
        //queryview viewid button1 getlocation
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                Rect nodePosition = new Rect();
                node.getBoundsInScreen(nodePosition);
                StringBuilder positions = new StringBuilder();
                positions.append(nodePosition.left).append(" ").append(nodePosition.top);
                positions.append(" ").append(nodePosition.right-nodePosition.left).append(" ");
                positions.append(nodePosition.bottom-nodePosition.top);
                return new MonkeyCommandReturn(true, positions.toString());
            }
            return EARG;
        }
    }


    /**
     * Command to retrieve the text of the given node
     */
    public static class GetText implements ViewIntrospectionCommand {
        //queryview [id type] [id] gettext
        //queryview viewid button1 gettext
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                if (node.isPassword()){
                    return new MonkeyCommandReturn(false, "Node contains a password");
                }
                /* Occasionally we get a null from the accessibility API, rather than an empty
                 * string */
                if (node.getText() == null) {
                    return new MonkeyCommandReturn(true, "");
                }
                return new MonkeyCommandReturn(true, node.getText().toString());
            }
            return EARG;
        }
    }


    /**
     * Command to retrieve the class name of the given node
     */
    public static class GetClass implements ViewIntrospectionCommand {
        //queryview [id type] [id] getclass
        //queryview viewid button1 getclass
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                return new MonkeyCommandReturn(true, node.getClassName().toString());
            }
            return EARG;
        }
    }
    /**
     * Command to retrieve the checked status of the given node
     */
    public static class GetChecked implements ViewIntrospectionCommand {
        //queryview [id type] [id] getchecked
        //queryview viewid button1 getchecked
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                return new MonkeyCommandReturn(true, Boolean.toString(node.isChecked()));
            }
            return EARG;
        }
    }

    /**
     * Command to retrieve whether the given node is enabled
     */
    public static class GetEnabled implements ViewIntrospectionCommand {
        //queryview [id type] [id] getenabled
        //queryview viewid button1 getenabled
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                return new MonkeyCommandReturn(true, Boolean.toString(node.isEnabled()));
            }
            return EARG;
        }
    }

    /**
     * Command to retrieve whether the given node is selected
     */
    public static class GetSelected implements ViewIntrospectionCommand {
        //queryview [id type] [id] getselected
        //queryview viewid button1 getselected
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                return new MonkeyCommandReturn(true, Boolean.toString(node.isSelected()));
            }
            return EARG;
        }
    }

    /**
     * Command to set the selected status of the given node. Takes a boolean value as its only
     * argument.
     */
    public static class SetSelected implements ViewIntrospectionCommand {
        //queryview [id type] [id] setselected [boolean]
        //queryview viewid button1 setselected true
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 1) {
                boolean actionPerformed;
                if (Boolean.valueOf(args.get(0))) {
                    actionPerformed = node.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                } else if (!Boolean.valueOf(args.get(0))) {
                    actionPerformed =
                            node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION);
                } else {
                    return EARG;
                }
                return new MonkeyCommandReturn(actionPerformed);
            }
            return EARG;
        }
    }

    /**
     * Command to get whether the given node is focused.
     */
    public static class GetFocused implements ViewIntrospectionCommand {
        //queryview [id type] [id] getfocused
        //queryview viewid button1 getfocused
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                return new MonkeyCommandReturn(true, Boolean.toString(node.isFocused()));
            }
            return EARG;
        }
    }

    /**
     * Command to set the focus status of the given node. Takes a boolean value
     * as its only argument.
     */
    public static class SetFocused implements ViewIntrospectionCommand {
        //queryview [id type] [id] setfocused [boolean]
        //queryview viewid button1 setfocused false
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 1) {
                boolean actionPerformed;
                if (Boolean.valueOf(args.get(0))) {
                    actionPerformed = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                } else if (!Boolean.valueOf(args.get(0))) {
                    actionPerformed = node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
                } else {
                    return EARG;
                }
                return new MonkeyCommandReturn(actionPerformed);
            }
            return EARG;
        }
    }

    /**
     * Command to get the accessibility ids of the given node. Returns the accessibility ids as a
     * space separated pair of integers with window id coming first, followed by the accessibility
     * view id.
     */
    public static class GetAccessibilityIds implements ViewIntrospectionCommand {
        //queryview [id type] [id] getaccessibilityids
        //queryview viewid button1 getaccessibilityids
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                int viewId;
                try {
                    Class<?> klass = node.getClass();
                    Field field = klass.getDeclaredField("mAccessibilityViewId");
                    field.setAccessible(true);
                    viewId = ((Integer) field.get(node)).intValue();
                } catch (NoSuchFieldException e) {
                    return new MonkeyCommandReturn(false, NO_NODE);
                } catch (IllegalAccessException e) {
                    return new MonkeyCommandReturn(false, "Access exception");
                }
                String ids = node.getWindowId() + " " + viewId;
                return new MonkeyCommandReturn(true, ids);
            }
            return EARG;
        }
    }

    /**
     * Command to get the accessibility ids of the parent of the given node. Returns the
     * accessibility ids as a space separated pair of integers with window id coming first followed
     * by the accessibility view id.
     */
    public static class GetParent implements ViewIntrospectionCommand {
        //queryview [id type] [id] getparent
        //queryview viewid button1 getparent
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                AccessibilityNodeInfo parent = node.getParent();
                if (parent == null) {
                  return new MonkeyCommandReturn(false, "Given node has no parent");
                }
                return (new GetAccessibilityIds()).query(parent, new ArrayList<String>());
            }
            return EARG;
        }
    }

    /**
     * Command to get the accessibility ids of the children of the given node. Returns the
     * children's ids as a space separated list of integer pairs. Each of the pairs consists of the
     * window id, followed by the accessibility id.
     */
    public static class GetChildren implements ViewIntrospectionCommand {
        //queryview [id type] [id] getchildren
        //queryview viewid button1 getchildren
        public MonkeyCommandReturn query(AccessibilityNodeInfo node,
                                         List<String> args) {
            if (args.size() == 0) {
                ViewIntrospectionCommand idGetter = new GetAccessibilityIds();
                List<String> emptyArgs = new ArrayList<String>();
                StringBuilder ids = new StringBuilder();
                int totalChildren = node.getChildCount();
                for (int i = 0; i < totalChildren; i++) {
                    MonkeyCommandReturn result = idGetter.query(node.getChild(i), emptyArgs);
                    if (!result.wasSuccessful()) {
                        return result;
                    } else {
                        ids.append(result.getMessage()).append(" ");
                    }
                }
                return new MonkeyCommandReturn(true, ids.toString());
            }
            return EARG;
        }
    }
}
