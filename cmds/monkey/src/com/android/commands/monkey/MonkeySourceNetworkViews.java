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

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.accessibilityservice.IEventListener;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityManager;
import android.view.accessibility.AccessibilityEvent;

import dalvik.system.DexClassLoader;

import com.android.commands.monkey.MonkeySourceNetwork.CommandQueue;
import com.android.commands.monkey.MonkeySourceNetwork.MonkeyCommand;
import com.android.commands.monkey.MonkeySourceNetwork.MonkeyCommandReturn;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


/**
 * Utility class that enables Monkey to perform view introspection when issued Monkey Network
 * Script commands over the network.
 */
public class MonkeySourceNetworkViews {
    private static final String TAG = "MonkeyViews";

    private static final int TIMEOUT_REGISTER_EVENT_LISTENER = 2000;

    private static final int NO_ID = -1;

    private static volatile AtomicReference<AccessibilityEvent> sLastAccessibilityEvent
            = new AtomicReference<AccessibilityEvent>();
    protected static int sConnectionId;
    private static IPackageManager sPm =
            IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    private static Map<String, Class<?>> sClassMap = new HashMap<String, Class<?>>();

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

    private static int getConnection() throws RemoteException {
        if (sConnectionId != NO_ID) {
            return sConnectionId;
        }
        IEventListener listener = new IEventListener.Stub() {
            public void setConnection(IAccessibilityServiceConnection connection,
                    int connectionId) {
                sConnectionId = connectionId;
                if (connection != null) {
                    AccessibilityInteractionClient.getInstance().addConnection(connectionId,
                            connection);
                } else {
                    AccessibilityInteractionClient.getInstance().removeConnection(connectionId);
                }
                synchronized (MonkeySourceNetworkViews.class) {
                    notifyAll();
                }
            }

            public void onInterrupt() {}

            public void onAccessibilityEvent(AccessibilityEvent event) {
                Log.d(TAG, "Accessibility Event");
                sLastAccessibilityEvent.set(AccessibilityEvent.obtain(event));
                synchronized (MonkeySourceNetworkViews.class) {
                    notifyAll();
                }
            }
        };

        IAccessibilityManager manager = IAccessibilityManager.Stub.asInterface(
                ServiceManager.getService(Context.ACCESSIBILITY_SERVICE));

        final long beginTime = SystemClock.uptimeMillis();
        synchronized (MonkeySourceNetworkViews.class) {
            manager.registerEventListener(listener);
            while (true) {
                if (sConnectionId != NO_ID) {
                    return sConnectionId;
                }
                final long elapsedTime = (SystemClock.uptimeMillis() - beginTime);
                final long remainingTime = TIMEOUT_REGISTER_EVENT_LISTENER - elapsedTime;
                if (remainingTime <= 0) {
                    if (sConnectionId == NO_ID) {
                        throw new IllegalStateException("Cound not register IEventListener.");
                    }
                    return sConnectionId;
                }
                try {
                    MonkeySourceNetworkViews.class.wait(remainingTime);
                } catch (InterruptedException ie) {
                    /* ignore */
                }
            }
        }
    }

    /**
     * Registers the event listener for AccessibilityEvents.
     * Also sets up a communication connection so we can query the
     * accessibility service.
     */
    public static void setup() {
        try {
            sConnectionId = getConnection();
        } catch (RemoteException re) {
            Log.e(TAG,"Remote Exception encountered when"
                  + " attempting to connect to Accessibility Service");
        }
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
            throws RemoteException, ClassNotFoundException {
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

    private static String getPositionFromNode(AccessibilityNodeInfo node) {
        Rect nodePosition = new Rect();
        node.getBoundsInScreen(nodePosition);
        StringBuilder positions = new StringBuilder();
        positions.append(nodePosition.left).append(" ").append(nodePosition.top);
        positions.append(" ").append(nodePosition.right-nodePosition.left).append(" ");
        positions.append(nodePosition.bottom-nodePosition.top);
        return positions.toString();
    }


    /**
     * Converts a resource identifier into it's generated integer ID
     *
     * @param stringId the string identifier
     * @return the generated integer identifier.
     */
    private static int getId(String stringId, AccessibilityEvent event)
            throws MonkeyViewException {
        try {
            AccessibilityNodeInfo node = event.getSource();
            String packageName = node.getPackageName().toString();
            ApplicationInfo appInfo = sPm.getApplicationInfo(packageName, 0);
            Class<?> klass;
            klass = getIdClass(packageName, appInfo.sourceDir);
            return klass.getField(stringId).getInt(null);
        } catch (RemoteException e) {
            throw new MonkeyViewException(REMOTE_ERROR);
        } catch (ClassNotFoundException e){
            throw new MonkeyViewException(e.getMessage());
        } catch (NoSuchFieldException e){
            throw new MonkeyViewException("No such node with given id");
        } catch (IllegalAccessException e){
            throw new MonkeyViewException("Private identifier");
        } catch (NullPointerException e) {
            // AccessibilityServiceConnection throws a NullPointerException if you hand it
            // an ID that doesn't exist onscreen
            throw new MonkeyViewException("No node with given id exists onscreen");
        }
    }

    private static AccessibilityNodeInfo getNodeByAccessibilityIds(
            String windowString, String viewString) {
        int windowId = Integer.parseInt(windowString);
        int viewId = Integer.parseInt(viewString);
        return AccessibilityInteractionClient.getInstance()
            .findAccessibilityNodeInfoByAccessibilityId(sConnectionId, windowId, viewId);
    }

    private static AccessibilityNodeInfo getNodeByViewId(String viewId, AccessibilityEvent event)
            throws MonkeyViewException {
        int id = getId(viewId, event);
        return AccessibilityInteractionClient.getInstance()
            .findAccessibilityNodeInfoByViewIdInActiveWindow(sConnectionId, id);
    }

    /**
     * Command to list all possible view ids for the given application.
     * This lists all view ids regardless if they are on screen or not.
     */
    public static class ListViewsCommand implements MonkeyCommand {
        //listviews
        public MonkeyCommandReturn translateCommand(List<String> command,
                                                    CommandQueue queue) {
            AccessibilityEvent lastEvent = sLastAccessibilityEvent.get();
            if (lastEvent == null) {
                return new MonkeyCommandReturn(false, NO_ACCESSIBILITY_EVENT);
            }
            lastEvent.setSealed(true);
            AccessibilityNodeInfo node = lastEvent.getSource();
            /* Occasionally the API will generate an event with no source, which is essentially the
             * same as it generating no event at all */
            if (node == null) {
                return new MonkeyCommandReturn(false, NO_ACCESSIBILITY_EVENT);
            }
            String packageName = node.getPackageName().toString();
            try{
                Class<?> klass;
                ApplicationInfo appInfo = sPm.getApplicationInfo(packageName, 0);
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
                if (sConnectionId < 0) {
                    return new MonkeyCommandReturn(false, NO_CONNECTION);
                }
                AccessibilityEvent lastEvent = sLastAccessibilityEvent.get();
                if (lastEvent == null) {
                    return new MonkeyCommandReturn(false, NO_ACCESSIBILITY_EVENT);
                }
                lastEvent.setSealed(true);
                String idType = command.get(1);
                AccessibilityNodeInfo node;
                String viewQuery;
                List<String> args;
                if ("viewid".equals(idType)) {
                    try {
                        node = getNodeByViewId(command.get(2), lastEvent);
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
            AccessibilityEvent lastEvent = sLastAccessibilityEvent.get();
            if (lastEvent == null) {
                return new MonkeyCommandReturn(false, NO_ACCESSIBILITY_EVENT);
            }
            lastEvent.setSealed(true);
            AccessibilityNodeInfo node = lastEvent.getSource();
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
            if (sConnectionId < 0) {
                return new MonkeyCommandReturn(false, NO_CONNECTION);
            }
            if (command.size() == 2) {
                String text = command.get(1);
                List<AccessibilityNodeInfo> nodes = AccessibilityInteractionClient.getInstance()
                    .findAccessibilityNodeInfosByViewTextInActiveWindow(sConnectionId, text);
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
            node.setSealed(true);
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
                    Class klass = node.getClass();
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
