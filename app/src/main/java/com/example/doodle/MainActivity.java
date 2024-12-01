package com.example.doodle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private DoodleView doodle_view;
    private AlertDialog color_popup, line_width_popup;
    private ImageView width_image_view;
    private SeekBar alpha_seek, red_seek, green_seek, blue_seek;
    private View color_preview;
    private boolean eraser_active = false;

    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setup_toolbars();
        doodle_view = findViewById(R.id.view);
    }

    private void setup_toolbars() {
        Toolbar bottom_toolbar = findViewById(R.id.bottom_app_bar);
        setSupportActionBar(bottom_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Toolbar title_toolbar = findViewById(R.id.title);
        title_toolbar.setTitle(R.string.app_name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bottom_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int item_id = item.getItemId();

        if (item_id == R.id.clear_id) {
            doodle_view.clear();
            return true;
        } else if (item_id == R.id.color_id) {
            show_color_picker_popup();
            return true;
        } else if (item_id == R.id.line_width_id) {
            show_line_width_popup();
            return true;
        } else if (item_id == R.id.erase_id) {
            toggle_eraser();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggle_eraser() {
        eraser_active = !eraser_active;
        doodle_view.set_eraser_mode(eraser_active);

        View erase_button = findViewById(R.id.erase_id);
        if (erase_button != null) {
            int background_color = eraser_active
                    ? R.color.background
                    : android.R.color.transparent;
            erase_button.setBackgroundColor(ContextCompat.getColor(this, background_color));
            Toast.makeText(this, eraser_active ? "Eraser On" : "Eraser Off", Toast.LENGTH_SHORT).show();
        }
    }

    private void show_color_picker_popup() {
        AlertDialog.Builder popup_builder = new AlertDialog.Builder(this);
        View popup_view = getLayoutInflater().inflate(R.layout.color_popup, null);

        setup_color_seekbars(popup_view);

        Button set_color_button = popup_view.findViewById(R.id.set_color_button);
        set_color_button.setOnClickListener(v -> {
            doodle_view.set_draw_color(get_selected_color());
            color_popup.dismiss();
            Toast.makeText(this, "Color Updated", Toast.LENGTH_SHORT).show();
        });

        popup_builder.setView(popup_view);
        popup_builder.setTitle("Choose Color");
        color_popup = popup_builder.create();
        color_popup.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_background);
        color_popup.show();
    }

    private void setup_color_seekbars(View popup_view) {
        alpha_seek = popup_view.findViewById(R.id.alpha_seekbar);
        red_seek = popup_view.findViewById(R.id.red_seekbar);
        green_seek = popup_view.findViewById(R.id.green_seekbar);
        blue_seek = popup_view.findViewById(R.id.blue_seekbar);
        color_preview = popup_view.findViewById(R.id.color_view);

        alpha_seek.setOnSeekBarChangeListener(color_seekbar_change_listener);
        red_seek.setOnSeekBarChangeListener(color_seekbar_change_listener);
        green_seek.setOnSeekBarChangeListener(color_seekbar_change_listener);
        blue_seek.setOnSeekBarChangeListener(color_seekbar_change_listener);

        int current_color = doodle_view.get_draw_color();
        alpha_seek.setProgress(Color.alpha(current_color));
        red_seek.setProgress(Color.red(current_color));
        green_seek.setProgress(Color.green(current_color));
        blue_seek.setProgress(Color.blue(current_color));

        update_color_preview();
    }

    private final SeekBar.OnSeekBarChangeListener color_seekbar_change_listener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            update_color_preview();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    private void update_color_preview() {
        int selected_color = get_selected_color();
        color_preview.setBackgroundColor(selected_color);
    }

    private int get_selected_color() {
        return Color.argb(
                alpha_seek.getProgress(),
                red_seek.getProgress(),
                green_seek.getProgress(),
                blue_seek.getProgress()
        );
    }

    private void show_line_width_popup() {
        AlertDialog.Builder popup_builder = new AlertDialog.Builder(this);
        View popup_view = getLayoutInflater().inflate(R.layout.width_popup, null);

        SeekBar width_seekbar = popup_view.findViewById(R.id.width_seek_bar_id);
        width_image_view = popup_view.findViewById(R.id.image_view_id);

        setup_width_seekbar(width_seekbar);

        Button set_width_button = popup_view.findViewById(R.id.width_dialog_btn_id);
        set_width_button.setOnClickListener(v -> {
            doodle_view.set_line_width(width_seekbar.getProgress());
            line_width_popup.dismiss();
            Toast.makeText(this, "Brush Size Set to " + width_seekbar.getProgress(), Toast.LENGTH_SHORT).show();
        });

        popup_builder.setView(popup_view);
        popup_builder.setTitle("Set Brush Size");
        line_width_popup = popup_builder.create();
        line_width_popup.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_background);
        line_width_popup.show();
    }

    private void setup_width_seekbar(SeekBar width_seekbar) {
        width_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            final Bitmap preview_bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888);
            final Canvas preview_canvas = new Canvas(preview_bitmap);

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Paint preview_paint = new Paint();
                preview_paint.setColor(doodle_view.get_draw_color());
                preview_paint.setStrokeCap(Paint.Cap.ROUND);
                preview_paint.setStrokeWidth(progress);

                preview_bitmap.eraseColor(Color.WHITE);
                preview_canvas.drawLine(30, 50, 370, 50, preview_paint);
                width_image_view.setImageBitmap(preview_bitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        width_seekbar.setProgress(doodle_view.get_line_width());
    }
}
