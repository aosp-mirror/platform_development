/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.quiz;

import static com.example.android.wearable.quiz.Constants.ANSWERS;
import static com.example.android.wearable.quiz.Constants.CHOSEN_ANSWER_CORRECT;
import static com.example.android.wearable.quiz.Constants.CORRECT_ANSWER_INDEX;
import static com.example.android.wearable.quiz.Constants.NUM_CORRECT;
import static com.example.android.wearable.quiz.Constants.NUM_INCORRECT;
import static com.example.android.wearable.quiz.Constants.NUM_SKIPPED;
import static com.example.android.wearable.quiz.Constants.QUESTION;
import static com.example.android.wearable.quiz.Constants.QUESTION_INDEX;
import static com.example.android.wearable.quiz.Constants.QUESTION_WAS_ANSWERED;
import static com.example.android.wearable.quiz.Constants.QUESTION_WAS_DELETED;
import static com.example.android.wearable.quiz.Constants.QUIZ_ENDED_PATH;
import static com.example.android.wearable.quiz.Constants.QUIZ_EXITED_PATH;
import static com.example.android.wearable.quiz.Constants.RESET_QUIZ_PATH;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Allows the user to create questions, which will be put as notifications on the watch's stream.
 * The status of questions will be updated on the phone when the user answers them.
 */
public class MainActivity extends Activity implements DataApi.DataListener,
        MessageApi.MessageListener, ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ExampleQuizApp";
    private static final String QUIZ_JSON_FILE = "Quiz.json";

    // Various UI components.
    private EditText questionEditText;
    private EditText choiceAEditText;
    private EditText choiceBEditText;
    private EditText choiceCEditText;
    private EditText choiceDEditText;
    private RadioGroup choicesRadioGroup;
    private TextView quizStatus;
    private LinearLayout quizButtons;
    private LinearLayout questionsContainer;
    private Button readQuizFromFileButton;
    private Button resetQuizButton;

    private GoogleApiClient mGoogleApiClient;
    private PriorityQueue<Question> mFutureQuestions;
    private int mQuestionIndex = 0;
    private boolean mHasQuestionBeenAsked = false;

    // Data to display in end report.
    private int mNumCorrect = 0;
    private int mNumIncorrect = 0;
    private int mNumSkipped = 0;

    private static final Map<Integer, Integer> radioIdToIndex;

    static {
        Map<Integer, Integer> temp = new HashMap<Integer, Integer>(4);
        temp.put(R.id.choice_a_radio, 0);
        temp.put(R.id.choice_b_radio, 1);
        temp.put(R.id.choice_c_radio, 2);
        temp.put(R.id.choice_d_radio, 3);
        radioIdToIndex = Collections.unmodifiableMap(temp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mFutureQuestions = new PriorityQueue<Question>(10);

        // Find UI components to be used later.
        questionEditText = (EditText) findViewById(R.id.question_text);
        choiceAEditText = (EditText) findViewById(R.id.choice_a_text);
        choiceBEditText = (EditText) findViewById(R.id.choice_b_text);
        choiceCEditText = (EditText) findViewById(R.id.choice_c_text);
        choiceDEditText = (EditText) findViewById(R.id.choice_d_text);
        choicesRadioGroup = (RadioGroup) findViewById(R.id.choices_radio_group);
        quizStatus = (TextView) findViewById(R.id.quiz_status);
        quizButtons = (LinearLayout) findViewById(R.id.quiz_buttons);
        questionsContainer = (LinearLayout) findViewById(R.id.questions_container);
        readQuizFromFileButton = (Button) findViewById(R.id.read_quiz_from_file_button);
        resetQuizButton = (Button) findViewById(R.id.reset_quiz_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);

        // Tell the wearable to end the quiz (counting unanswered questions as skipped), and then
        // disconnect mGoogleApiClient.
        DataMap dataMap = new DataMap();
        dataMap.putInt(NUM_CORRECT, mNumCorrect);
        dataMap.putInt(NUM_INCORRECT, mNumIncorrect);
        if (mHasQuestionBeenAsked) {
            mNumSkipped += 1;
        }
        mNumSkipped += mFutureQuestions.size();
        dataMap.putInt(NUM_SKIPPED, mNumSkipped);
        if (mNumCorrect + mNumIncorrect + mNumSkipped > 0) {
            sendMessageToWearable(QUIZ_EXITED_PATH, dataMap.toByteArray());
        }

        clearQuizStatus();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Ignore
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Failed to connect to Google Play Services");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(RESET_QUIZ_PATH)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resetQuiz(null);
                }
            });
        }
    }

    /**
     * Used to ensure questions with smaller indexes come before questions with larger
     * indexes. For example, question0 should come before question1.
     */
    private static class Question implements Comparable<Question> {

        private String question;
        private int questionIndex;
        private String[] answers;
        private int correctAnswerIndex;

        public Question(String question, int questionIndex, String[] answers,
                int correctAnswerIndex) {
            this.question = question;
            this.questionIndex = questionIndex;
            this.answers = answers;
            this.correctAnswerIndex = correctAnswerIndex;
        }

        public static Question fromJson(JSONObject questionObject, int questionIndex)
                throws JSONException {
            String question = questionObject.getString(JsonUtils.JSON_FIELD_QUESTION);
            JSONArray answersJsonArray = questionObject.getJSONArray(JsonUtils.JSON_FIELD_ANSWERS);
            String[] answers = new String[JsonUtils.NUM_ANSWER_CHOICES];
            for (int j = 0; j < answersJsonArray.length(); j++) {
                answers[j] = answersJsonArray.getString(j);
            }
            int correctIndex = questionObject.getInt(JsonUtils.JSON_FIELD_CORRECT_INDEX);
            return new Question(question, questionIndex, answers, correctIndex);
        }

        @Override
        public int compareTo(Question that) {
            return this.questionIndex - that.questionIndex;
        }

        public PutDataRequest toPutDataRequest() {
            PutDataMapRequest request = PutDataMapRequest.create("/question/" + questionIndex);
            DataMap dataMap = request.getDataMap();
            dataMap.putString(QUESTION, question);
            dataMap.putInt(QUESTION_INDEX, questionIndex);
            dataMap.putStringArray(ANSWERS, answers);
            dataMap.putInt(CORRECT_ANSWER_INDEX, correctAnswerIndex);
            return request.asPutDataRequest();
        }
    }

    /**
     * Create a quiz, as defined in Quiz.json, when the user clicks on "Read quiz from file."
     *
     * @throws IOException
     */
    public void readQuizFromFile(View view) throws IOException, JSONException {
        clearQuizStatus();
        JSONObject jsonObject = JsonUtils.loadJsonFile(this, QUIZ_JSON_FILE);
        JSONArray jsonArray = jsonObject.getJSONArray(JsonUtils.JSON_FIELD_QUESTIONS);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject questionObject = jsonArray.getJSONObject(i);
            Question question = Question.fromJson(questionObject, mQuestionIndex++);
            addQuestionDataItem(question);
            setNewQuestionStatus(question.question);
        }
    }

    /**
     * Adds a question (with answer choices) when user clicks on "Add Question."
     */
    public void addQuestion(View view) {
        // Retrieve the question and answers supplied by the user.
        String question = questionEditText.getText().toString();
        String[] answers = new String[4];
        answers[0] = choiceAEditText.getText().toString();
        answers[1] = choiceBEditText.getText().toString();
        answers[2] = choiceCEditText.getText().toString();
        answers[3] = choiceDEditText.getText().toString();
        int correctAnswerIndex = radioIdToIndex.get(choicesRadioGroup.getCheckedRadioButtonId());

        addQuestionDataItem(new Question(question, mQuestionIndex++, answers, correctAnswerIndex));
        setNewQuestionStatus(question);

        // Clear the edit boxes to let the user input a new question.
        questionEditText.setText("");
        choiceAEditText.setText("");
        choiceBEditText.setText("");
        choiceCEditText.setText("");
        choiceDEditText.setText("");
    }

    /**
     * Adds the questions (and answers) to the wearable's stream by creating a Data Item
     * that will be received on the wearable, which will create corresponding notifications.
     */
    private void addQuestionDataItem(Question question) {
        if (!mHasQuestionBeenAsked) {
            // Ask the question now.
            Wearable.DataApi.putDataItem(mGoogleApiClient, question.toPutDataRequest());
            setHasQuestionBeenAsked(true);
        } else {
            // Enqueue the question to be asked in the future.
            mFutureQuestions.add(question);
        }
    }

    /**
     * Sets the question's status to be the default "unanswered." This will be updated when the
     * user chooses an answer for the question on the wearable.
     */
    private void setNewQuestionStatus(String question) {
        quizStatus.setVisibility(View.VISIBLE);
        quizButtons.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        View questionStatusElem = inflater.inflate(R.layout.question_status_element, null, false);
        ((TextView) questionStatusElem.findViewById(R.id.question)).setText(question);
        ((TextView) questionStatusElem.findViewById(R.id.status))
                .setText(R.string.question_unanswered);
        questionsContainer.addView(questionStatusElem);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        // Need to freeze the dataEvents so they will exist later on the UI thread
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (DataEvent event : events) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem())
                                .getDataMap();
                        boolean questionWasAnswered = dataMap.getBoolean(QUESTION_WAS_ANSWERED);
                        boolean questionWasDeleted = dataMap.getBoolean(QUESTION_WAS_DELETED);
                        if (questionWasAnswered) {
                            // Update the answered question's status.
                            int questionIndex = dataMap.getInt(QUESTION_INDEX);
                            boolean questionCorrect = dataMap.getBoolean(CHOSEN_ANSWER_CORRECT);
                            updateQuestionStatus(questionIndex, questionCorrect);
                            askNextQuestionIfExists();
                        } else if (questionWasDeleted) {
                            // Update the deleted question's status by marking it as left blank.
                            int questionIndex = dataMap.getInt(QUESTION_INDEX);
                            markQuestionLeftBlank(questionIndex);
                            askNextQuestionIfExists();
                        }
                    }
                }
            }
        });
    }

    /**
     * Updates the given question based on whether it was answered correctly or not.
     * This involves changing the question's text color and changing the status text for it.
     */
    public void updateQuestionStatus(int questionIndex, boolean questionCorrect) {
        LinearLayout questionStatusElement = (LinearLayout)
                questionsContainer.getChildAt(questionIndex);
        TextView questionText = (TextView) questionStatusElement.findViewById(R.id.question);
        TextView questionStatus = (TextView) questionStatusElement.findViewById(R.id.status);
        if (questionCorrect) {
            questionText.setTextColor(Color.GREEN);
            questionStatus.setText(R.string.question_correct);
            mNumCorrect++;
        } else {
            questionText.setTextColor(Color.RED);
            questionStatus.setText(R.string.question_incorrect);
            mNumIncorrect++;
        }
    }

    /**
     * Marks a question as "left blank" when its corresponding question notification is deleted.
     */
    private void markQuestionLeftBlank(int index) {
        LinearLayout questionStatusElement = (LinearLayout) questionsContainer.getChildAt(index);
        if (questionStatusElement != null) {
            TextView questionText = (TextView) questionStatusElement.findViewById(R.id.question);
            TextView questionStatus = (TextView) questionStatusElement.findViewById(R.id.status);
            if (questionStatus.getText().equals(getString(R.string.question_unanswered))) {
                questionText.setTextColor(Color.YELLOW);
                questionStatus.setText(R.string.question_left_blank);
                mNumSkipped++;
            }
        }
    }

    /**
     * Asks the next enqueued question if it exists, otherwise ends the quiz.
     */
    private void askNextQuestionIfExists() {
        if (mFutureQuestions.isEmpty()) {
            // Quiz has been completed - send message to wearable to display end report.
            DataMap dataMap = new DataMap();
            dataMap.putInt(NUM_CORRECT, mNumCorrect);
            dataMap.putInt(NUM_INCORRECT, mNumIncorrect);
            dataMap.putInt(NUM_SKIPPED, mNumSkipped);
            sendMessageToWearable(QUIZ_ENDED_PATH, dataMap.toByteArray());
            setHasQuestionBeenAsked(false);
        } else {
            // Ask next question by putting a DataItem that will be received on the wearable.
            Wearable.DataApi.putDataItem(mGoogleApiClient,
                    mFutureQuestions.remove().toPutDataRequest());
            setHasQuestionBeenAsked(true);
        }
    }

    private void sendMessageToWearable(final String path, final byte[] data) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            Wearable.MessageApi
                                    .sendMessage(mGoogleApiClient, node.getId(), path, data);
                        }

                        if (path.equals(QUIZ_EXITED_PATH) && mGoogleApiClient.isConnected()) {
                            mGoogleApiClient.disconnect();
                        }
                    }
                });
    }

    /**
     * Resets the current quiz when Reset Quiz is pressed.
     */
    public void resetQuiz(View view) {
        // Reset quiz status in phone layout.
        for (int i = 0; i < questionsContainer.getChildCount(); i++) {
            LinearLayout questionStatusElement = (LinearLayout) questionsContainer.getChildAt(i);
            TextView questionText = (TextView) questionStatusElement.findViewById(R.id.question);
            TextView questionStatus = (TextView) questionStatusElement.findViewById(R.id.status);
            questionText.setTextColor(Color.WHITE);
            questionStatus.setText(R.string.question_unanswered);
        }
        // Reset data items and notifications on wearable.
        if (mGoogleApiClient.isConnected()) {
            Wearable.DataApi.getDataItems(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
                        @Override
                        public void onResult(DataItemBuffer result) {
                            try {
                                if (result.getStatus().isSuccess()) {
                                    resetDataItems(result);
                                } else {
                                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                                        Log.d(TAG, "Reset quiz: failed to get Data Items to reset");
                                    }
                                }
                            } finally {
                                result.release();
                            }
                        }
                    });
        } else {
            Log.e(TAG, "Failed to reset data items because client is disconnected from "
                    + "Google Play Services");
        }
        setHasQuestionBeenAsked(false);
        mNumCorrect = 0;
        mNumIncorrect = 0;
        mNumSkipped = 0;
    }

    private void resetDataItems(DataItemBuffer dataItemList) {
        if (mGoogleApiClient.isConnected()) {
            for (final DataItem dataItem : dataItemList) {
                final Uri dataItemUri = dataItem.getUri();
                Wearable.DataApi.getDataItem(mGoogleApiClient, dataItemUri)
                        .setResultCallback(new ResetDataItemCallback());
            }
        } else {
            Log.e(TAG, "Failed to reset data items because client is disconnected from "
                    + "Google Play Services");
        }
    }

    /**
     * Callback that marks a DataItem, which represents a question, as unanswered and not deleted.
     */
    private class ResetDataItemCallback implements ResultCallback<DataApi.DataItemResult> {

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                PutDataMapRequest request = PutDataMapRequest.createFromDataMapItem(
                        DataMapItem.fromDataItem(dataItemResult.getDataItem()));
                DataMap dataMap = request.getDataMap();
                dataMap.putBoolean(QUESTION_WAS_ANSWERED, false);
                dataMap.putBoolean(QUESTION_WAS_DELETED, false);
                if (!mHasQuestionBeenAsked && dataMap.getInt(QUESTION_INDEX) == 0) {
                    // Ask the first question now.
                    Wearable.DataApi.putDataItem(mGoogleApiClient, request.asPutDataRequest());
                    setHasQuestionBeenAsked(true);
                } else {
                    // Enqueue future questions.
                    mFutureQuestions.add(new Question(dataMap.getString(QUESTION),
                            dataMap.getInt(QUESTION_INDEX), dataMap.getStringArray(ANSWERS),
                            dataMap.getInt(CORRECT_ANSWER_INDEX)));
                }
            } else {
                Log.e(TAG, "Failed to reset data item " + dataItemResult.getDataItem().getUri());
            }
        }
    }

    /**
     * Clears the current quiz when user clicks on "New Quiz."
     * On this end, this involves clearing the quiz status layout and deleting all DataItems. The
     * wearable will then remove any outstanding question notifications upon receiving this change.
     */
    public void newQuiz(View view) {
        clearQuizStatus();
        if (mGoogleApiClient.isConnected()) {
            Wearable.DataApi.getDataItems(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
                        @Override
                        public void onResult(DataItemBuffer result) {
                            try {
                                if (result.getStatus().isSuccess()) {
                                    List<Uri> dataItemUriList = new ArrayList<Uri>();
                                    for (final DataItem dataItem : result) {
                                        dataItemUriList.add(dataItem.getUri());
                                    }
                                    deleteDataItems(dataItemUriList);
                                } else {
                                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                                        Log.d(TAG, "Clear quiz: failed to get Data Items for "
                                                + "deletion");

                                    }
                                }
                            } finally {
                                result.release();
                            }
                        }
                    });
        } else {
            Log.e(TAG, "Failed to delete data items because client is disconnected from "
                    + "Google Play Services");
        }
    }

    /**
     * Removes quiz status views (i.e. the views describing the status of each question).
     */
    private void clearQuizStatus() {
        questionsContainer.removeAllViews();
        quizStatus.setVisibility(View.INVISIBLE);
        quizButtons.setVisibility(View.INVISIBLE);
        setHasQuestionBeenAsked(false);
        mFutureQuestions.clear();
        mQuestionIndex = 0;
        mNumCorrect = 0;
        mNumIncorrect = 0;
        mNumSkipped = 0;
    }

    private void deleteDataItems(List<Uri> dataItemUriList) {
        if (mGoogleApiClient.isConnected()) {
            for (final Uri dataItemUri : dataItemUriList) {
                Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItemUri)
                        .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                            @Override
                            public void onResult(DataApi.DeleteDataItemsResult deleteResult) {
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    if (deleteResult.getStatus().isSuccess()) {
                                        Log.d(TAG, "Successfully deleted data item " + dataItemUri);
                                    } else {
                                        Log.d(TAG, "Failed to delete data item " + dataItemUri);
                                    }
                                }
                            }
                        });
            }
        } else {
            Log.e(TAG, "Failed to delete data items because client is disconnected from "
                    + "Google Play Services");
        }
    }

    private void setHasQuestionBeenAsked(boolean b) {
        mHasQuestionBeenAsked = b;
        // Only let user click on Reset or Read from file if they have answered all the questions.
        readQuizFromFileButton.setEnabled(!mHasQuestionBeenAsked);
        resetQuizButton.setEnabled(!mHasQuestionBeenAsked);
    }
}
