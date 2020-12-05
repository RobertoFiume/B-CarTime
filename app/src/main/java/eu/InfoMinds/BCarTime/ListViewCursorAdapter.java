package eu.InfoMinds.BCarTime;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.shapes.Shape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

public class ListViewCursorAdapter extends CursorAdapter {
    private LayoutInflater cursorInflater;

    public ListViewCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);

        cursorInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return cursorInflater.inflate(R.layout.listrow, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tvTitle = (TextView)view.findViewById(R.id.textTitle);
        TextView tvFromTime = (TextView)view.findViewById(R.id.textFromTime);
        TextView tvToTime = (TextView)view.findViewById(R.id.textToTime);
        TextView tvTotalTime = (TextView)view.findViewById(R.id.textTotalTime);
        LinearLayout lySynchronized = (LinearLayout) view.findViewById(R.id.shapeLayout);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date date = null;
        String dateInfo = null;
        try {
            String fromTime = cursor.getString(cursor.getColumnIndexOrThrow("FROMTIME"));
            date = dateFormat.parse(fromTime);

           dateInfo = dateFormat.getDateInstance(DateFormat.FULL).format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String fromTime = cursor.getString(cursor.getColumnIndexOrThrow("FROM_TIME"));
        String toTime = cursor.getString(cursor.getColumnIndexOrThrow("TO_TIME"));
        String totalTime = cursor.getString(cursor.getColumnIndexOrThrow("TOTAL_TIME"));

        // Populate fields with extracted properties
        tvTitle.setText(dateInfo);
        tvFromTime.setText(fromTime);
        tvToTime.setText(toTime);
        tvTotalTime.setText(totalTime);

        Boolean hasSynchronized = (cursor.getInt(cursor.getColumnIndexOrThrow("SYNCHRONIZED")) == 1);
        if (hasSynchronized) {
            lySynchronized.setBackgroundColor(Color.GRAY);
        }
        else {
            lySynchronized.setBackgroundColor(Color.RED);
        }
    }
}
