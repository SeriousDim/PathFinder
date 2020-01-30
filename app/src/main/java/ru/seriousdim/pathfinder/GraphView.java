package ru.seriousdim.pathfinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * This class is not used.
 */
@Deprecated
public class GraphView extends View {

    final int numToShow = 230;
    final int color = 0x18473Aff;
    final int thick = 3;
    final int delims = 10;

    ArrayList<Float> values;
    float max = -1e10f, min = 1e10f;

    public GraphView(Context context) {
        super(context);
        values = new ArrayList<>();
    }

    public GraphView(Context ctx, AttributeSet set){
        super(ctx, set);
    }

    public void addValue(float val){
        if (values.size() == numToShow) {
            if (values.get(0) == max){
                for (int i = 1; i < numToShow; i++){
                    max = Math.max(max, values.get(i));
                }
            } else if (values.get(0) == min){
                for (int i = 1; i < numToShow; i++){
                    min = Math.min(min, values.get(i));
                }
            }
            values.remove(0);
        }
        values.add(val);
        max = Math.max(max, val);
        min = Math.min(min, val);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float distance = (max-min)/delims;

    }

}
