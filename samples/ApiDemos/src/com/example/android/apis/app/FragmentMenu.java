package com.example.android.apis.app;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Demonstrates how fragments can participate in the options menu.
 */
public class FragmentMenu extends Activity {
    Fragment mFragment1;
    Fragment mFragment2;
    CheckBox mCheckBox1;
    CheckBox mCheckBox2;
    
    // Update fragment visibility when check boxes are changed.
    final OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            updateFragmentVisibility();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_menu);
        
        // Make sure the two menu fragments are created.
        FragmentTransaction ft = openFragmentTransaction();
        mFragment1 = findFragmentByTag("f1");
        if (mFragment1 == null) {
            mFragment1 = new Menu1Fragment();
            ft.add(mFragment1, "f1");
        }
        mFragment2 = findFragmentByTag("f2");
        if (mFragment2 == null) {
            mFragment2 = new Menu2Fragment();
            ft.add(mFragment2, "f2");
        }
        ft.commit();
        
        // Watch check box clicks.
        mCheckBox1 = (CheckBox)findViewById(R.id.menu1);
        mCheckBox1.setOnClickListener(mClickListener);
        mCheckBox2 = (CheckBox)findViewById(R.id.menu2);
        mCheckBox2.setOnClickListener(mClickListener);
        
        // Make sure fragments start out with correct visibility.
        updateFragmentVisibility();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Make sure fragments are updated after check box view state is restored.
        updateFragmentVisibility();
    }

    // Update fragment visibility based on current check box state.
    void updateFragmentVisibility() {
        FragmentTransaction ft = openFragmentTransaction();
        if (mCheckBox1.isChecked()) ft.show(mFragment1);
        else ft.hide(mFragment1);
        if (mCheckBox2.isChecked()) ft.show(mFragment2);
        else ft.hide(mFragment2);
        ft.commit();
    }
    
    /**
     * A fragment that displays a menu.  This fragment happens to not
     * have a UI (it does not implement onCreateView), but it could also
     * have one if it wanted.
     */
    public static class Menu1Fragment extends Fragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add("Menu 1a");
            menu.add("Menu 1b");
        }
    }

    /**
     * Second fragment with a menu.
     */
    public static class Menu2Fragment extends Fragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add("Menu 2");
        }
    }
}
