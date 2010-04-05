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

package com.example.android.tictactoe.library;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.tictactoe.library.GameView.ICellListener;
import com.example.android.tictactoe.library.GameView.State;


public class GameActivity extends Activity {

    /** Start player. Must be 1 or 2. Default is 1. */
    public static final String EXTRA_START_PLAYER =
        "com.example.android.tictactoe.library.GameActivity.EXTRA_START_PLAYER";

    private static final int MSG_COMPUTER_TURN = 1;
    private static final long COMPUTER_DELAY_MS = 500;

    private Handler mHandler = new Handler(new MyHandlerCallback());
    private Random mRnd = new Random();
    private GameView mGameView;
    private TextView mInfoView;
    private Button mButtonNext;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        /*
         * IMPORTANT: all resource IDs from this library will eventually be merged
         * with the resources from the main project that will use the library.
         *
         * If the main project and the libraries define the same resource IDs,
         * the application project will always have priority and override library resources
         * and IDs defined in multiple libraries are resolved based on the libraries priority
         * defined in the main project.
         *
         * An intentional consequence is that the main project can override some resources
         * from the library.
         * (TODO insert example).
         *
         * To avoid potential conflicts, it is suggested to add a prefix to the
         * library resource names.
         */
        setContentView(R.layout.lib_game);

        mGameView = (GameView) findViewById(R.id.game_view);
        mInfoView = (TextView) findViewById(R.id.info_turn);
        mButtonNext = (Button) findViewById(R.id.next_turn);

        mGameView.setFocusable(true);
        mGameView.setFocusableInTouchMode(true);
        mGameView.setCellListener(new MyCellListener());

        mButtonNext.setOnClickListener(new MyButtonListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        State player = mGameView.getCurrentPlayer();
        if (player == State.UNKNOWN) {
            player = State.fromInt(getIntent().getIntExtra(EXTRA_START_PLAYER, 1));
            if (!checkGameFinished(player)) {
                selectTurn(player);
            }
        }
        if (player == State.PLAYER2) {
            mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
        }
        if (player == State.WIN) {
            setWinState(mGameView.getWinner());
        }
    }


    private State selectTurn(State player) {
        mGameView.setCurrentPlayer(player);
        mButtonNext.setEnabled(false);

        if (player == State.PLAYER1) {
            mInfoView.setText(R.string.player1_turn);
            mGameView.setEnabled(true);

        } else if (player == State.PLAYER2) {
            mInfoView.setText(R.string.player2_turn);
            mGameView.setEnabled(false);
        }

        return player;
    }

    private class MyCellListener implements ICellListener {
        public void onCellSelected() {
            if (mGameView.getCurrentPlayer() == State.PLAYER1) {
                int cell = mGameView.getSelection();
                mButtonNext.setEnabled(cell >= 0);
            }
        }
    }

    private class MyButtonListener implements OnClickListener {

        public void onClick(View v) {
            State player = mGameView.getCurrentPlayer();

            if (player == State.WIN) {
                GameActivity.this.finish();

            } else if (player == State.PLAYER1) {
                int cell = mGameView.getSelection();
                if (cell >= 0) {
                    mGameView.stopBlink();
                    mGameView.setCell(cell, player);
                    finishTurn();
                }
            }
        }
    }

    private class MyHandlerCallback implements Callback {
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_COMPUTER_TURN) {

                // Pick a non-used cell at random. That's about all the AI you need for this game.
                State[] data = mGameView.getData();
                int used = 0;
                while (used != 0x1F) {
                    int index = mRnd.nextInt(9);
                    if (((used >> index) & 1) == 0) {
                        used |= 1 << index;
                        if (data[index] == State.EMPTY) {
                            mGameView.setCell(index, mGameView.getCurrentPlayer());
                            break;
                        }
                    }
                }

                finishTurn();
                return true;
            }
            return false;
        }
    }

    private State getOtherPlayer(State player) {
        return player == State.PLAYER1 ? State.PLAYER2 : State.PLAYER1;
    }

    private void finishTurn() {
        State player = mGameView.getCurrentPlayer();
        if (!checkGameFinished(player)) {
            player = selectTurn(getOtherPlayer(player));
            if (player == State.PLAYER2) {
                mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
            }
        }
    }

    public boolean checkGameFinished(State player) {
        State[] data = mGameView.getData();
        boolean full = true;

        int col = -1;
        int row = -1;
        int diag = -1;

        // check rows
        for (int j = 0, k = 0; j < 3; j++, k += 3) {
            if (data[k] != State.EMPTY && data[k] == data[k+1] && data[k] == data[k+2]) {
                row = j;
            }
            if (full && (data[k] == State.EMPTY ||
                         data[k+1] == State.EMPTY ||
                         data[k+2] == State.EMPTY)) {
                full = false;
            }
        }

        // check columns
        for (int i = 0; i < 3; i++) {
            if (data[i] != State.EMPTY && data[i] == data[i+3] && data[i] == data[i+6]) {
                col = i;
            }
        }

        // check diagonals
        if (data[0] != State.EMPTY && data[0] == data[1+3] && data[0] == data[2+6]) {
            diag = 0;
        } else  if (data[2] != State.EMPTY && data[2] == data[1+3] && data[2] == data[0+6]) {
            diag = 1;
        }

        if (col != -1 || row != -1 || diag != -1) {
            setFinished(player, col, row, diag);
            return true;
        }

        // if we get here, there's no winner but the board is full.
        if (full) {
            setFinished(State.EMPTY, -1, -1, -1);
            return true;
        }
        return false;
    }

    private void setFinished(State player, int col, int row, int diagonal) {

        mGameView.setCurrentPlayer(State.WIN);
        mGameView.setWinner(player);
        mGameView.setEnabled(false);
        mGameView.setFinished(col, row, diagonal);

        setWinState(player);
    }

    private void setWinState(State player) {
        mButtonNext.setEnabled(true);
        mButtonNext.setText("Back");

        String text;

        if (player == State.EMPTY) {
            text = getString(R.string.tie);
        } else if (player == State.PLAYER1) {
            text = getString(R.string.player1_win);
        } else {
            text = getString(R.string.player2_win);
        }
        mInfoView.setText(text);
    }
}
