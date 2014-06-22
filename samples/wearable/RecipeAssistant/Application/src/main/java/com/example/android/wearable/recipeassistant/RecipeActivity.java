package com.example.android.wearable.recipeassistant;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class RecipeActivity extends Activity {
    private static final String TAG = "RecipeAssistant";
    private String mRecipeName;
    private Recipe mRecipe;
    private ImageView mImageView;
    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private TextView mIngredientsTextView;
    private LinearLayout mStepsLayout;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        mRecipeName = intent.getStringExtra(Constants.RECIPE_NAME_TO_LOAD);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Intent: " + intent.toString() + " " + mRecipeName);
        }
        loadRecipe();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recipe);
        mTitleTextView = (TextView) findViewById(R.id.recipeTextTitle);
        mSummaryTextView = (TextView) findViewById(R.id.recipeTextSummary);
        mImageView = (ImageView) findViewById(R.id.recipeImageView);
        mIngredientsTextView = (TextView) findViewById(R.id.textIngredients);
        mStepsLayout = (LinearLayout) findViewById(R.id.layoutSteps);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_cook:
                startCooking();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadRecipe() {
        JSONObject jsonObject = AssetUtils.loadJSONAsset(this, mRecipeName);
        if (jsonObject != null) {
            mRecipe = Recipe.fromJson(this, jsonObject);
            if (mRecipe != null) {
                displayRecipe(mRecipe);
            }
        }
    }

    private void displayRecipe(Recipe recipe) {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        mTitleTextView.setAnimation(fadeIn);
        mTitleTextView.setText(recipe.titleText);
        mSummaryTextView.setText(recipe.summaryText);
        if (recipe.recipeImage != null) {
            mImageView.setAnimation(fadeIn);
            Bitmap recipeImage = AssetUtils.loadBitmapAsset(this, recipe.recipeImage);
            mImageView.setImageBitmap(recipeImage);
        }
        mIngredientsTextView.setText(recipe.ingredientsText);

        findViewById(R.id.ingredientsHeader).setAnimation(fadeIn);
        findViewById(R.id.ingredientsHeader).setVisibility(View.VISIBLE);
        findViewById(R.id.stepsHeader).setAnimation(fadeIn);

        findViewById(R.id.stepsHeader).setVisibility(View.VISIBLE);

        LayoutInflater inf = LayoutInflater.from(this);
        mStepsLayout.removeAllViews();
        int stepNumber = 1;
        for (Recipe.RecipeStep step : recipe.recipeSteps) {
            View view = inf.inflate(R.layout.step_item, null);
            ImageView iv = (ImageView) view.findViewById(R.id.stepImageView);
            if (step.stepImage == null) {
                iv.setVisibility(View.GONE);
            } else {
                Bitmap stepImage = AssetUtils.loadBitmapAsset(this, step.stepImage);
                iv.setImageBitmap(stepImage);
            }
            ((TextView) view.findViewById(R.id.textStep)).setText(
                    (stepNumber++) + ". " + step.stepText);
            mStepsLayout.addView(view);
        }
    }

    private void startCooking() {
        Intent intent = new Intent(this, RecipeService.class);
        intent.setAction(Constants.ACTION_START_COOKING);
        intent.putExtra(Constants.EXTRA_RECIPE, mRecipe.toBundle());
        startService(intent);
    }
}
