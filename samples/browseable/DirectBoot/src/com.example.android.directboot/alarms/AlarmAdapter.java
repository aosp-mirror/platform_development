/*
* Copyright 2016 The Android Open Source Project
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

package com.example.android.directboot.alarms;

import com.example.android.directboot.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter responsible for interactions between the {@link RecyclerView} and the
 * scheduled alarms.
 */
public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private SortedList<Alarm> mAlarmList;
    private AlarmStorage mAlarmStorage;
    private AlarmUtil mAlarmUtil;
    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;
    private Context mContext;

    public AlarmAdapter(Context context, Set<Alarm> alarms) {
        mAlarmList = new SortedList<>(Alarm.class, new SortedListCallback());
        mAlarmList.addAll(alarms);
        mAlarmStorage = new AlarmStorage(context);
        mContext = context;
        mAlarmUtil = new AlarmUtil(context);
        mDateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        mTimeFormat = new SimpleDateFormat("kk:mm", Locale.getDefault());
    }

    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alarm_row, parent, false);
        return new AlarmViewHolder(v);
    }

    @Override
    public void onBindViewHolder(AlarmViewHolder holder, final int position) {
        Alarm alarm = mAlarmList.get(position);
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.MONTH, alarm.month);
        alarmTime.set(Calendar.DATE, alarm.date);
        alarmTime.set(Calendar.HOUR_OF_DAY, alarm.hour);
        alarmTime.set(Calendar.MINUTE, alarm.minute);
        holder.mAlarmTimeTextView
                .setText(mTimeFormat.format(alarmTime.getTime()));
        holder.mAlarmDateTextView
                .setText(mDateFormat.format(alarmTime.getTime()));
        holder.mDeleteImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Alarm toBeDeleted = mAlarmList.get(position);
                mAlarmList.removeItemAt(position);
                mAlarmStorage.deleteAlarm(toBeDeleted);
                mAlarmUtil.cancelAlarm(toBeDeleted);
                notifyDataSetChanged();
                Toast.makeText(mContext, mContext.getString(R.string.alarm_deleted,
                        toBeDeleted.hour, toBeDeleted.minute), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAlarmList.size();
    }

    public void addAlarm(Alarm alarm) {
        mAlarmList.add(alarm);
        notifyDataSetChanged();
    }

    public void deleteAlarm(Alarm alarm) {
        mAlarmList.remove(alarm);
        notifyDataSetChanged();
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {

        private TextView mAlarmTimeTextView;
        private TextView mAlarmDateTextView;
        private ImageView mDeleteImageView;

        public AlarmViewHolder(View itemView) {
            super(itemView);
            mAlarmTimeTextView = (TextView) itemView.findViewById(R.id.text_alarm_time);
            mAlarmDateTextView = (TextView) itemView.findViewById(R.id.text_alarm_date);
            mDeleteImageView = (ImageView) itemView.findViewById(R.id.image_delete_alarm);
        }
    }


    private static class SortedListCallback extends SortedList.Callback<Alarm> {

        @Override
        public int compare(Alarm o1, Alarm o2) {
            return o1.compareTo(o2);
        }

        @Override
        public void onInserted(int position, int count) {
            //No op
        }

        @Override
        public void onRemoved(int position, int count) {
            //No op
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            //No op
        }

        @Override
        public void onChanged(int position, int count) {
            //No op
        }

        @Override
        public boolean areContentsTheSame(Alarm oldItem, Alarm newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Alarm item1, Alarm item2) {
            return item1.equals(item2);
        }
    }

    /**
     * ItemDecoration that draws an divider between items in a RecyclerView.
     */
    public static class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private Drawable mDivider;

        public DividerItemDecoration(Context context) {
            mDivider = context.getDrawable(R.drawable.divider);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                        .getLayoutParams();
                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}
