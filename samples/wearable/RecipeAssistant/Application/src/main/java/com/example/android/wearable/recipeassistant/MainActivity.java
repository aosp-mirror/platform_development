
package com.example.android.wearable.recipeassistant;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class MainActivity extends ListActivity {

    private static final String TAG = "RecipeAssistant";
    private RecipeListAdapter mAdapter;

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG , "onListItemClick " + position);
        }
        String itemName = mAdapter.getItemName(position);
        Intent intent = new Intent(getApplicationContext(), RecipeActivity.class);
        intent.putExtra(Constants.RECIPE_NAME_TO_LOAD, itemName);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(android.R.layout.list_content);

        mAdapter = new RecipeListAdapter(this);
        setListAdapter(mAdapter);
    }
}
