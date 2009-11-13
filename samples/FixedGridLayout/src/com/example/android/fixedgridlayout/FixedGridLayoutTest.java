package com.example.android.fixedgridlayout;

import android.app.Activity;
import android.os.Bundle;

public class FixedGridLayoutTest extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        FixedGridLayout grid = (FixedGridLayout)findViewById(R.id.grid);
        grid.setCellWidth(80);
        grid.setCellHeight(80);
    }
}
