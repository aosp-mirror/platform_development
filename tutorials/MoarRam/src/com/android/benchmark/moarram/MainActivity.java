package com.android.benchmark.moarram;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.RadioGroup;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.loadLibrary("moarram-32");
        System.loadLibrary("moarram-2M");
        System.loadLibrary("moarram-17_71");
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void add32ByteBlocks(View view) {
        add32ByteBlocksNative();
    }

    public void free32ByteBlocks(View view) {
        free32ByteBlocksNative();
    }

    public void add2MByteBlocks(View view) {
        add2MByteBlocksNative();
    }

    public void free2MByteBlocks(View view) {
        free2MByteBlocksNative();
    }

    public void addVariableSizedBlocks(View view) {

        RadioGroup sizeGroup = (RadioGroup) findViewById(R.id.blockSize);

        int sizeId = sizeGroup.getCheckedRadioButtonId();
        addVariableSizedBlocksNative(sizeId == R.id.radio17 ? 0 : 1);
    }

    public void freeVariableSizedBlocks(View view) {

        RadioGroup sizeGroup = (RadioGroup) findViewById(R.id.blockSize);

        int sizeId = sizeGroup.getCheckedRadioButtonId();
        freeVariableSizedBlocksNative(sizeId == R.id.radio17 ? 0 : 1);
    }

    public native void add32ByteBlocksNative();
    public native void free32ByteBlocksNative();
    public native void add2MByteBlocksNative();
    public native void free2MByteBlocksNative();
    public native void addVariableSizedBlocksNative(int sizeId);
    public native void freeVariableSizedBlocksNative(int sizeId);
}
