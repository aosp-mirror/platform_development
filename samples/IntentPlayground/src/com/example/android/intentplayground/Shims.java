/** TODO: http://go/java-style#javadoc */
package com.example.android.intentplayground;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.util.List;
import java.util.Map;

class TestBase {
    static final String EXPECTED_HIERARCHY = "";
    enum LaunchStyle { TASK_STACK_BUILDER, COMMAND_LINE, LAUNCH_FORWARD }
    TestBase(Context context, Node hierarchy) {}
    void setupActivities(LaunchStyle style) {}
    Node computeExpected(Intent intent) { return null; }
    static Node describeTaskHierarchy(Context context) { return null; }

}
class TreeFragment extends Fragment {
    static final String TREE_NODE = "";
    static final String FRAGMENT_TITLE = "";
}
class CurrentTaskFragment extends Fragment {}
class IntentFragment extends Fragment {}
class IntentBuilderFragment extends Fragment implements View.OnClickListener {
    ComponentName mActivityToLaunch;
    void selectLaunchMode(View view) {}
    public void onClick(View view) {}
    void clearFlags() {}
    void selectFlags(List<String> flags) {}
}
class BuildConfig {
    static final boolean DEBUG = true;
}
class Node implements Parcelable, Comparable<Node> {
    ComponentName name;
    boolean isTaskNode;
    int taskId;
    static final String TAG = "";
    public static final Creator<Node> CREATOR = new  Creator<Node>() {
        @Override
        public Node createFromParcel(Parcel in) {
            return new Node(in);
        }

        @Override
        public Node[] newArray(int size) {
            return new Node[size];
        }
    };;
    List<Node> children;
    Node(ComponentName data) {}
    Node(int taskId) {}
    Node(Node other) {}
    Node(Parcel in) {}
    Node addChild(Node child) { return null; }
    boolean equals(Node other) { return false; }
    public int compareTo(Node o) {return 0;}
    @Override
    public void writeToParcel(Parcel dest, int flags) {}
    @Override
    public int describeContents() { return 0; }
}
class FlagUtils {
    static List<String> discoverFlags(Intent intent) { return null; }
    static List<String> intentFlags() { return null; }
    static Map<String, List<String>> intentFlagsByCategory() { return null; }
    static int value(String flagName) { return 0; }
    static List<String> discoverActivityFlags() { return null; }
    static String camelify(String snake) { return null; }
}