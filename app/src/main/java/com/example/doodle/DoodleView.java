package com.example.doodle;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.util.HashMap;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.net.Uri;
import android.os.Build;
import java.io.OutputStream;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class DoodleView extends View {
    private static final float TOUCH_TOLERANCE = 10f;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int DEFAULT_STROKE_WIDTH = 10;
    private static final int ERASER_STROKE_WIDTH = 100;

    private Bitmap bitmap;
    private Canvas bitmap_canvas;
    private final Paint paint_screen = new Paint();
    private final Paint paint_draw = new Paint();
    private final Paint paint_eraser = new Paint();
    private final HashMap<Integer, Path> path_map = new HashMap<>();
    private final HashMap<Integer, Point> previous_point_map = new HashMap<>();
    private boolean is_erasing = false;

    public DoodleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint_draw.setAntiAlias(true);
        paint_draw.setColor(DEFAULT_COLOR);
        paint_draw.setStyle(Paint.Style.STROKE);
        paint_draw.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        paint_draw.setStrokeCap(Paint.Cap.ROUND);

        paint_eraser.setAntiAlias(true);
        paint_eraser.setColor(Color.WHITE);
        paint_eraser.setStyle(Paint.Style.STROKE);
        paint_eraser.setStrokeWidth(ERASER_STROKE_WIDTH);
        paint_eraser.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap_canvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, paint_screen);
        for (Path path : path_map.values()) {
            canvas.drawPath(path, is_erasing ? paint_eraser : paint_draw);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int action_index = event.getActionIndex();
        int pointer_id = event.getPointerId(action_index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                touch_start(event.getX(action_index), event.getY(action_index), pointer_id);
                break;

            case MotionEvent.ACTION_MOVE:
                touch_move(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                touch_stop(pointer_id);
                break;
        }

        invalidate();
        return true;
    }

    private void touch_start(float x, float y, int pointer_id) {
        Path path = path_map.computeIfAbsent(pointer_id, id -> new Path());
        Point point = previous_point_map.computeIfAbsent(pointer_id, id -> new Point());

        path.moveTo(x, y);
        point.set((int) x, (int) y);
    }

    private void touch_move(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointer_id = event.getPointerId(i);
            int pointer_index = event.findPointerIndex(pointer_id);

            if (path_map.containsKey(pointer_id)) {
                float new_x = event.getX(pointer_index);
                float new_y = event.getY(pointer_index);

                Point point = previous_point_map.get(pointer_id);
                Path path = path_map.get(pointer_id);

                float delta_x = Math.abs(new_x - point.x);
                float delta_y = Math.abs(new_y - point.y);

                if (delta_x >= TOUCH_TOLERANCE || delta_y >= TOUCH_TOLERANCE) {
                    path.quadTo(point.x, point.y, (new_x + point.x) / 2, (new_y + point.y) / 2);
                    point.set((int) new_x, (int) new_y);
                }
            }
        }
    }

    private void touch_stop(int pointer_id) {
        Path path = path_map.get(pointer_id);
        if (path != null) {
            bitmap_canvas.drawPath(path, is_erasing ? paint_eraser : paint_draw);
            path.reset();
        }
    }

    public void clear() {
        path_map.clear();
        previous_point_map.clear();
        bitmap.eraseColor(Color.WHITE);
        invalidate();
    }

    public void save() {
        String f_name = "Doodle_" + System.currentTimeMillis() + ".png";
        String mime_type = "image/jpeg";
        String relative_path = "Download/DoodleApp";

        try {
            ContentResolver resolver = getContext().getContentResolver();
            ContentValues content_values = new ContentValues();
            content_values.put(MediaStore.MediaColumns.DISPLAY_NAME, f_name);
            content_values.put(MediaStore.MediaColumns.MIME_TYPE, mime_type);
            content_values.put(MediaStore.MediaColumns.RELATIVE_PATH, relative_path);

            Uri image_uri = null;

            // Check for Android Q or later
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                image_uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, content_values);
            }

            if (image_uri != null) {
                try (OutputStream output_stream = resolver.openOutputStream(image_uri)) {
                    if (output_stream != null && bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output_stream);
                        Toast.makeText(getContext(), "Doodle saved :D", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                throw new IOException("Failed to create URI");
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error with Doodle save", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            clear();
        }
    }

    public void set_eraser_mode(boolean erasing) {
        is_erasing = erasing;
    }

    public void set_draw_color(int color) {
        paint_draw.setColor(color);
    }

    public int get_draw_color() {
        return paint_draw.getColor();
    }

    public void set_line_width(int width) {
        paint_draw.setStrokeWidth(width);
    }

    public int get_line_width() {
        return (int) paint_draw.getStrokeWidth();
    }
}
