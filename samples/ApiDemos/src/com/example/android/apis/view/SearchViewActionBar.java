/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.apis.view;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.List;

/**
 * This demonstrates the usage of SearchView in an ActionBar as a menu item.
 * It sets a SearchableInfo on the SearchView for suggestions and submitting queries to.
 */
public class SearchViewActionBar extends Activity implements SearchView.OnQueryChangeListener,
        SearchView.OnCloseListener, Button.OnClickListener {

    private SearchView mSearchView;
    private Button mOpenButton;
    private Button mCloseButton;
    private TextView mStatusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        setContentView(R.layout.searchview_actionbar);

        mStatusView = (TextView) findViewById(R.id.status_text);
        mOpenButton = (Button) findViewById(R.id.open_button);
        mCloseButton = (Button) findViewById(R.id.close_button);
        mOpenButton.setOnClickListener(this);
        mCloseButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.searchview_in_menu, menu);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        setupSearchView();

        return true;
    }

    private void setupSearchView() {

        mSearchView.setIconifiedByDefault(true);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            List<SearchableInfo> searchables = searchManager.getSearchablesInGlobalSearch();

            // Try to use the "applications" global search provider
            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            for (SearchableInfo inf : searchables) {
                if (inf.getSuggestAuthority() != null
                        && inf.getSuggestAuthority().startsWith("applications")) {
                    info = inf;
                }
            }
            mSearchView.setSearchableInfo(info);
        }

        mSearchView.setOnQueryChangeListener(this);
        mSearchView.setOnCloseListener(this);
    }

    public boolean onQueryTextChanged(String newText) {
        mStatusView.setText("Query = " + newText);
        return false;
    }

    public boolean onSubmitQuery(String query) {
        mStatusView.setText("Query = " + query + " : submitted");
        return false;
    }

    public boolean onClose() {
        mStatusView.setText("Closed!");
        return false;
    }

    public void onClick(View view) {
        if (view == mCloseButton) {
            mSearchView.setIconified(true);
        } else if (view == mOpenButton) {
            mSearchView.setIconified(false);
        }
    }
}
