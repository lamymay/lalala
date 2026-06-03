package com.arc.s;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_REPLACE_IMAGE = 1;
    private static final int REQUEST_APPEND_IMAGE = 2;
    private static final int ANIMATION_GRID_SPAN = 2;

    private RevealViews revealViews;
    private Bitmap defaultBitmap;
    private int coverColor = android.graphics.Color.BLACK;
    private final List<Bitmap> imageList = new ArrayList<>();
    private int currentImageIndex;
    private RevealAnimation revealAnimation = RevealAnimation.CENTER_OUT_Y;
    private int revealStepPx = RevealStepPreferences.DEFAULT_STEP_PX;
    private int revealLevel;

    private DialogAnimationStyle menuDialogStyle = DialogAnimationStyle.POPUP;
    @Nullable
    private StyledDialog activeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        revealViews = new RevealViews(
                findViewById(R.id.reveal_stage),
                findViewById(R.id.testImageView1),
                findViewById(R.id.mask_top),
                findViewById(R.id.mask_bottom),
                findViewById(R.id.mask_left),
                findViewById(R.id.mask_right),
                findViewById(R.id.mask_center),
                findViewById(R.id.mask_pan),
                findViewById(R.id.mask_full)
        );

        menuDialogStyle = DialogStylePreferences.getMenuDialogStyle(this);
        revealStepPx = RevealStepPreferences.getRevealStepPx(this);

        hideSystemBars();
        initDefaultImage();

        View root = findViewById(R.id.root);
        root.setOnClickListener(v -> onRevealClick());
        root.setOnLongClickListener(v -> {
            showActionMenu();
            return true;
        });
    }

    private void initDefaultImage() {
        defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_money);
        imageList.clear();
        imageList.add(defaultBitmap);
        currentImageIndex = 0;
        resetReveal();
    }

    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        WindowInsetsController insetsController = decorView.getWindowInsetsController();
        if (insetsController == null) {
            return;
        }
        insetsController.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    private void onRevealClick() {
        if (imageList.isEmpty()) {
            return;
        }

        if (revealLevel >= 10000) {
            advanceAfterFullReveal();
            return;
        }

        int w = revealViews.stageWidth();
        int h = revealViews.stageHeight();
        int levelStep = revealAnimation.levelDeltaForStepPx(w, h, revealStepPx);
        revealLevel = Math.min(10000, revealLevel + levelStep);
        applyRevealState();
    }

    private void advanceAfterFullReveal() {
        if (imageList.size() > 1) {
            currentImageIndex = (currentImageIndex + 1) % imageList.size();
        }
        resetReveal();
    }

    private void resetReveal() {
        revealLevel = 0;
        applyRevealState();
    }

    private void applyRevealState() {
        Bitmap bitmap = imageList.get(currentImageIndex);
        coverColor = CoverColorUtil.pickCoverColor(bitmap);
        revealAnimation.apply(revealViews, bitmap, revealLevel, coverColor);
    }

    private DialogAnimationStyle currentMenuDialogStyle() {
        return menuDialogStyle;
    }

    private void showActionMenu() {
        View menuView = getLayoutInflater().inflate(R.layout.dialog_action_menu, null, false);

        TextView appendItem = menuView.findViewById(R.id.menu_item_append);
        if (imageList.size() > 1) {
            appendItem.setText(getString(R.string.menu_append_image) + " (" + imageList.size() + ")");
        }

        updateDialogStyleMenuLabel(menuView.findViewById(R.id.menu_item_dialog_style));
        updateRevealStepMenuLabel(menuView.findViewById(R.id.menu_item_reveal_step));

        menuView.findViewById(R.id.menu_item_exit).setOnClickListener(v -> {
            dismissActiveDialog();
            finish();
        });
        menuView.findViewById(R.id.menu_item_replace).setOnClickListener(v -> {
            dismissActiveDialog();
            pickImage(REQUEST_REPLACE_IMAGE);
        });
        menuView.findViewById(R.id.menu_item_animation).setOnClickListener(v -> {
            dismissActiveDialog();
            showAnimationPicker();
        });
        menuView.findViewById(R.id.menu_item_reveal_step).setOnClickListener(v -> {
            dismissActiveDialog();
            showRevealStepPicker();
        });
        menuView.findViewById(R.id.menu_item_append).setOnClickListener(v -> {
            dismissActiveDialog();
            pickImage(REQUEST_APPEND_IMAGE);
        });
        menuView.findViewById(R.id.menu_item_dialog_style).setOnClickListener(v -> {
            dismissActiveDialog();
            showDialogStylePicker();
        });

        activeDialog = StyledDialog.show(this, menuView, currentMenuDialogStyle());
    }

    private void updateDialogStyleMenuLabel(TextView label) {
        label.setText(getString(R.string.menu_dialog_style_fmt, menuDialogStyle.getLabel()));
    }

    private void updateRevealStepMenuLabel(TextView label) {
        label.setText(getString(R.string.menu_reveal_step_fmt, revealStepPx));
    }

    private void showDialogStylePicker() {
        View pickerView = getLayoutInflater().inflate(R.layout.dialog_animation_picker, null, false);
        TextView title = pickerView.findViewById(R.id.picker_title);
        title.setText(R.string.dialog_pick_menu_style_title);

        RecyclerView grid = pickerView.findViewById(R.id.picker_grid);
        grid.setLayoutManager(new GridLayoutManager(this, 2));

        DialogStyleTileAdapter adapter = new DialogStyleTileAdapter(
                menuDialogStyle.ordinal(),
                style -> {
                    menuDialogStyle = style;
                    DialogStylePreferences.setMenuDialogStyle(this, style);
                    Toast.makeText(
                            this,
                            getString(R.string.toast_menu_style_switched, style.getLabel()),
                            Toast.LENGTH_SHORT
                    ).show();
                    dismissActiveDialog();
                }
        );
        grid.setAdapter(adapter);

        activeDialog = StyledDialog.show(this, pickerView, currentMenuDialogStyle());
    }

    private void showAnimationPicker() {
        View pickerView = getLayoutInflater().inflate(R.layout.dialog_animation_picker, null, false);
        RecyclerView grid = pickerView.findViewById(R.id.picker_grid);
        grid.setLayoutManager(new GridLayoutManager(this, ANIMATION_GRID_SPAN));

        AnimationPickerAdapter adapter = new AnimationPickerAdapter(
                revealAnimation.ordinal(),
                animation -> {
                    revealAnimation = animation;
                    applyRevealState();
                    Toast.makeText(
                            this,
                            getString(R.string.toast_animation_switched, animation.getLabel()),
                            Toast.LENGTH_SHORT
                    ).show();
                    dismissActiveDialog();
                }
        );
        grid.setAdapter(adapter);

        activeDialog = StyledDialog.show(this, pickerView, currentMenuDialogStyle());
    }

    private void showRevealStepPicker() {
        View pickerView = getLayoutInflater().inflate(R.layout.dialog_reveal_step_picker, null, false);

        RecyclerView grid = pickerView.findViewById(R.id.picker_grid);
        grid.setLayoutManager(new GridLayoutManager(this, ANIMATION_GRID_SPAN));

        EditText stepInput = pickerView.findViewById(R.id.reveal_step_input);
        stepInput.setText(String.valueOf(revealStepPx));
        stepInput.setSelection(stepInput.getText().length());

        int[] options = RevealStepPreferences.STEP_OPTIONS_PX;
        int selectedIndex = RevealStepPreferences.indexOfPreset(revealStepPx);

        RevealStepTileAdapter adapter = new RevealStepTileAdapter(
                options,
                selectedIndex,
                stepPx -> {
                    stepInput.setText(String.valueOf(stepPx));
                    applyRevealStepPx(stepPx);
                }
        );
        grid.setAdapter(adapter);

        Runnable applyCustomStep = () -> applyCustomRevealStepFromInput(stepInput);
        pickerView.findViewById(R.id.reveal_step_apply).setOnClickListener(v -> applyCustomStep.run());
        stepInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyCustomStep.run();
                return true;
            }
            return false;
        });

        activeDialog = StyledDialog.show(this, pickerView, currentMenuDialogStyle());
    }

    private void applyCustomRevealStepFromInput(EditText stepInput) {
        String text = stepInput.getText() != null ? stepInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.toast_reveal_step_invalid,
                            RevealStepPreferences.MIN_STEP_PX,
                            RevealStepPreferences.MAX_STEP_PX
                    ),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        try {
            int stepPx = Integer.parseInt(text);
            if (!RevealStepPreferences.isValidStep(stepPx)) {
                Toast.makeText(
                        this,
                        getString(
                                R.string.toast_reveal_step_invalid,
                                RevealStepPreferences.MIN_STEP_PX,
                                RevealStepPreferences.MAX_STEP_PX
                        ),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            applyRevealStepPx(stepPx);
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    getString(
                            R.string.toast_reveal_step_invalid,
                            RevealStepPreferences.MIN_STEP_PX,
                            RevealStepPreferences.MAX_STEP_PX
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void applyRevealStepPx(int stepPx) {
        revealStepPx = stepPx;
        RevealStepPreferences.setRevealStepPx(this, stepPx);
        Toast.makeText(
                this,
                getString(R.string.toast_reveal_step_switched, stepPx),
                Toast.LENGTH_SHORT
        ).show();
        dismissActiveDialog();
    }

    private void dismissActiveDialog() {
        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Bitmap bitmap = loadBitmap(data);
        if (bitmap == null) {
            Toast.makeText(this, R.string.toast_pick_image_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == REQUEST_REPLACE_IMAGE) {
            recycleExcept(bitmap);
            imageList.clear();
            imageList.add(bitmap);
            currentImageIndex = 0;
            resetReveal();
            Toast.makeText(this, R.string.toast_image_replaced, Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_APPEND_IMAGE) {
            imageList.add(bitmap);
            Toast.makeText(
                    this,
                    getString(R.string.toast_image_appended, imageList.size()),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Nullable
    private Bitmap loadBitmap(Intent data) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(data.getData());
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "loadBitmap failed", e);
            return null;
        }
    }

    private void recycleExcept(Bitmap keep) {
        for (Bitmap bitmap : imageList) {
            if (bitmap != keep && bitmap != defaultBitmap) {
                bitmap.recycle();
            }
        }
    }

    @Override
    protected void onDestroy() {
        dismissActiveDialog();
        for (Bitmap bitmap : imageList) {
            if (bitmap != defaultBitmap) {
                bitmap.recycle();
            }
        }
        imageList.clear();
        super.onDestroy();
    }

    /** 动画步长（像素）选项。 */
    private static final class RevealStepTileAdapter
            extends RecyclerView.Adapter<AnimationPickerAdapter.TileViewHolder> {

        interface OnStepSelectedListener {
            void onSelected(int stepPx);
        }

        private final int[] stepsPx;
        private final int selectedIndex;
        private final OnStepSelectedListener listener;

        RevealStepTileAdapter(int[] stepsPx, int selectedIndex, OnStepSelectedListener listener) {
            this.stepsPx = stepsPx;
            this.selectedIndex = selectedIndex;
            this.listener = listener;
        }

        @NonNull
        @Override
        public AnimationPickerAdapter.TileViewHolder onCreateViewHolder(
                @NonNull android.view.ViewGroup parent,
                int viewType
        ) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_animation_tile, parent, false);
            return new AnimationPickerAdapter.TileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AnimationPickerAdapter.TileViewHolder holder, int position) {
            int step = stepsPx[position];
            holder.label.setText(holder.itemView.getContext()
                    .getString(R.string.reveal_step_tile_fmt, step));
            holder.label.setSelected(position == selectedIndex);
            holder.itemView.setOnClickListener(v -> listener.onSelected(step));
        }

        @Override
        public int getItemCount() {
            return stepsPx.length;
        }
    }

    /** 菜单弹出风格：两个方块选项。 */
    private static final class DialogStyleTileAdapter
            extends RecyclerView.Adapter<AnimationPickerAdapter.TileViewHolder> {

        interface OnStyleSelectedListener {
            void onSelected(DialogAnimationStyle style);
        }

        private final DialogAnimationStyle[] styles = DialogAnimationStyle.values();
        private final int selectedOrdinal;
        private final OnStyleSelectedListener listener;

        DialogStyleTileAdapter(int selectedOrdinal, OnStyleSelectedListener listener) {
            this.selectedOrdinal = selectedOrdinal;
            this.listener = listener;
        }

        @NonNull
        @Override
        public AnimationPickerAdapter.TileViewHolder onCreateViewHolder(
                @NonNull android.view.ViewGroup parent,
                int viewType
        ) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_animation_tile, parent, false);
            return new AnimationPickerAdapter.TileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AnimationPickerAdapter.TileViewHolder holder, int position) {
            DialogAnimationStyle style = styles[position];
            holder.label.setText(style.getLabel());
            holder.label.setSelected(position == selectedOrdinal);
            holder.itemView.setOnClickListener(v -> listener.onSelected(style));
        }

        @Override
        public int getItemCount() {
            return styles.length;
        }
    }
}
