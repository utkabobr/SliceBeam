package ru.ytkab0bp.slicebeam.components.bed_menu;

import static ru.ytkab0bp.slicebeam.utils.DebugUtils.assertTrue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.message.BasicHeader;
import ru.ytkab0bp.slicebeam.BuildConfig;
import ru.ytkab0bp.slicebeam.MainActivity;
import ru.ytkab0bp.slicebeam.R;
import ru.ytkab0bp.slicebeam.SliceBeam;
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder;
import ru.ytkab0bp.slicebeam.components.UnfoldMenu;
import ru.ytkab0bp.slicebeam.config.ConfigObject;
import ru.ytkab0bp.slicebeam.events.NeedDismissSnackbarEvent;
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent;
import ru.ytkab0bp.slicebeam.fragment.BedFragment;
import ru.ytkab0bp.slicebeam.recycler.SimpleRecyclerItem;
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult;
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer;
import ru.ytkab0bp.slicebeam.slic3r.Slic3rLocalization;
import ru.ytkab0bp.slicebeam.theme.IThemeView;
import ru.ytkab0bp.slicebeam.theme.ThemesRepo;
import ru.ytkab0bp.slicebeam.utils.ViewUtils;
import ru.ytkab0bp.slicebeam.view.DividerView;
import ru.ytkab0bp.slicebeam.view.PositionScrollView;
import ru.ytkab0bp.slicebeam.view.SegmentsView;
import ru.ytkab0bp.slicebeam.view.SnackbarsLayout;

public class SliceMenu extends ListBedMenu {
    private AsyncHttpClient client = new AsyncHttpClient();

    {
        client.setLoggingEnabled(true);
        client.setMaxRetriesAndTimeout(0, 10000);
    }

    private final static List<String> SUPPORTED_SEND = Arrays.asList("octoprint", "klipper");
    private int lastUid;

    @Override
    protected List<SimpleRecyclerItem> onCreateItems(boolean portrait) {
        lastUid = SliceBeam.CONFIG_UID;
        List<SimpleRecyclerItem> items = new ArrayList<>(Arrays.asList(
                new BedMenuItem(R.string.MenuSliceInfo, R.drawable.clock_circle_dashed_outline_24).onClick(v -> fragment.showUnfoldMenu(new InfoMenu(), v)),
                new BedMenuItem(R.string.MenuSliceLayers, R.drawable.square_stack_up_outline_28).onClick(v -> fragment.showUnfoldMenu(new LayersMenu(), v)),
                new BedMenuItem(R.string.MenuSliceExportToFile, R.drawable.folder_simple_arrow_right_outline_28).onClick(v -> {
                    if (fragment.getContext() instanceof Activity) {
                        Activity act = (Activity) fragment.getContext();
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.setType("application/x-gcode");
                        i.putExtra(Intent.EXTRA_TITLE, fragment.getGlView().getRenderer().getGcodeResult().getRecommendedName());
                        act.startActivityForResult(i, MainActivity.REQUEST_CODE_EXPORT_GCODE);
                    }
                }),
                new BedMenuItem(R.string.MenuSliceShare, R.drawable.share_external_28).onClick(v -> {
                    if (fragment.getContext() instanceof Activity) {
                        File f = BedFragment.getTempGCodePath();

                        Activity act = (Activity) fragment.getContext();
                        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        i.setType("application/x-gcode");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            // Simple trick for Samsung to display "1 element" instead of "temp.gcode"
                            // It doesn't actually resolve name from provider and uses path-parsing instead, bruh.
                            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(Collections.singletonList(FileProvider.getUriForFile(act, BuildConfig.APPLICATION_ID + ".provider", f, BedFragment.getTempFileName()))));
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } else {
                            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                        }
                        act.startActivity(Intent.createChooser(i, null));
                    }
                })
        ));
        ConfigObject obj = SliceBeam.CONFIG.findPrinter(SliceBeam.CONFIG.presets.get("printer"));
        assertTrue(obj != null);
        String type = obj.get("host_type");
        if (type == null) type = "octoprint";
        String host = obj.get("print_host");
        String apiKey = obj.get("printhost_apikey");
        if (SUPPORTED_SEND.contains(type) && !TextUtils.isEmpty(host)) {
            String finalType = type;
            items.add(new BedMenuItem(R.string.MenuSliceSendToPrinter, R.drawable.send_outline_28).onClick(v -> upload(finalType, host, apiKey, false)));
            items.add(new BedMenuItem(R.string.MenuSliceSendToPrinterAndPrint, R.drawable.send_28).onClick(v -> upload(finalType, host, apiKey, true)));
        }
        return items;
    }

    private void upload(String type, String host, String apiKey, boolean print) {
        String name = fragment.getGlView().getRenderer().getGcodeResult().getRecommendedName();
        if (!host.startsWith("http://")) {
            host = "http://" + host;
        }
        String tag = UUID.randomUUID().toString();
        SliceBeam.EVENT_BUS.fireEvent(new NeedSnackbarEvent(SnackbarsLayout.Type.LOADING, R.string.MenuSliceSendToPrinterLoading).tag(tag));
        Header[] headers = TextUtils.isEmpty(apiKey) ? new Header[0] : new Header[] {new BasicHeader("X-Api-Key", apiKey)};
        RequestParams params = new RequestParams();
        try {
            params.put("file", new FileInputStream(BedFragment.getTempGCodePath()), name, ContentType.TEXT_PLAIN.getMimeType());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        params.put("select", String.valueOf(print));
        params.put("print", String.valueOf(print));

        String url;
        final boolean isKlipper = "klipper".equals(type);
        switch (type) {
            case "klipper":
                url = host + "/server/files/upload";
                break;
            case "octoprint":
            default:
                url = host + "/api/files/local";
                break;
        }

        client.post(SliceBeam.INSTANCE, url, headers, params, null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject obj = new JSONObject(new String(responseBody));
                    boolean success;
                    if (isKlipper) {
                        // Moonraker: check for "filename" and "path"
                        success = obj.has("filename") && obj.has("path");
                    } else {
                        // OctoPrint: check for "action" or "files"
                        success = obj.has("action") || obj.has("files");
                    }
                    if (!success) {
                        throw new JSONException("Unexpected response: " + obj.toString());
                    }
                    SliceBeam.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(tag));
                    SliceBeam.EVENT_BUS.fireEvent(new NeedSnackbarEvent(print ? SnackbarsLayout.Type.INFO : SnackbarsLayout.Type.DONE, print ? R.string.MenuSliceSendToPrinterPrintStarted : R.string.MenuSliceSendToPrinterOK));
                } catch (JSONException e) {
                    onFailure(statusCode, headers, responseBody, e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                SliceBeam.EVENT_BUS.fireEvent(new NeedDismissSnackbarEvent(tag));
                ViewUtils.postOnMainThread(() -> new BeamAlertDialogBuilder(fragment.getContext())
                        .setTitle(R.string.MenuSliceSendToPrinterFailed)
                        .setMessage(error.toString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show());
            }
        });
    }

    @Override
    public void onViewCreated(View v) {
        super.onViewCreated(v);
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                if (lastUid != SliceBeam.CONFIG_UID) {
                    adapter.setItems(onCreateItems(v.getWidth() < v.getHeight()));
                }
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {}
        });
    }

    private final static class InfoMenu extends UnfoldMenu implements IThemeView {
        private TextView totalView;
        private SegmentsView segmentsView;
        private ExtrusionRoleView[] roleViews = new ExtrusionRoleView[GCodeViewer.EXTRUSION_ROLES_COUNT];
        private static DecimalFormat format = new DecimalFormat("0.##");

        private GCodeViewer getViewer() {
            return fragment.getGlView().getRenderer().getViewer();
        }

        private GCodeProcessorResult getResult() {
            return fragment.getGlView().getRenderer().getGcodeResult();
        }

        @Override
        protected View onCreateView(Context ctx, boolean portrait) {
            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);

            ll.addView(new Space(ctx), new LinearLayout.LayoutParams(0, 0, 1f));

            for (int i = 0; i < GCodeViewer.EXTRUSION_ROLES_COUNT; i++) {
                ll.addView(roleViews[i] = new ExtrusionRoleView(ctx));
            }

            ll.addView(new DividerView(ctx), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(1f)));

            totalView = new TextView(ctx);
            totalView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            totalView.setGravity(Gravity.CENTER);
            ll.addView(totalView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(18)) {{
                topMargin = ViewUtils.dp(8);
            }});

            segmentsView = new SegmentsView(ctx);
            ll.addView(segmentsView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(12)) {{
                leftMargin = rightMargin = ViewUtils.dp(12);
                topMargin = bottomMargin = ViewUtils.dp(8);
            }});

            ll.addView(new DividerView(ctx), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(1f)));
            LinearLayout toolbar = new LinearLayout(ctx);
            toolbar.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            toolbar.setGravity(Gravity.CENTER_VERTICAL);
            toolbar.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 0));
            toolbar.setOnClickListener(v -> dismiss());

            ImageView icon = new ImageView(ctx);
            icon.setImageResource(R.drawable.arrow_left_outline_28);
            icon.setColorFilter(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            toolbar.addView(icon, new LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)));

            TextView title = new TextView(ctx);
            title.setText(R.string.MenuOrientationPositionBack);
            title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            toolbar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
                leftMargin = ViewUtils.dp(12);
            }});

            ll.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
            onApplyTheme();
            return ll;
        }

        @Override
        protected void onCreate() {
            super.onCreate();

            GCodeViewer viewer = getViewer();
            GCodeProcessorResult result = getResult();
            if (viewer != null) {
                for (int i = 0; i < GCodeViewer.EXTRUSION_ROLES_COUNT; i++) {
                    boolean visible = viewer.getEstimatedTime(i) != 0;
                    roleViews[i].setVisibility(visible ? View.VISIBLE : View.GONE);
                    if (visible) {
                        roleViews[i].bind(viewer, result, i);
                    }
                }
            }
            updateValues();
            ViewUtils.postOnMainThread(() -> segmentsView.startAnimation(), 50);
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();

            segmentsView.setNotVisible();
        }

        @Override
        public int getRequestedSize(FrameLayout into, boolean portrait) {
            if (portrait) {
                GCodeViewer viewer = getViewer();
                if (viewer != null) {
                    int visibleCount = 0;
                    for (int i = 0; i < GCodeViewer.EXTRUSION_ROLES_COUNT; i++) {
                        if (viewer.getEstimatedTime(i) != 0) {
                            visibleCount++;
                        }
                    }
                    return ViewUtils.dp(42) * visibleCount + ViewUtils.dp(28) + ViewUtils.dp(52) + ViewUtils.dp(18 + 8);
                }
            }
            return super.getRequestedSize(into, portrait);
        }

        private float getTotalEstimatedTime() {
            GCodeViewer viewer = getViewer();
            if (viewer == null) return 0;
            float total = 0;
            for (int i = 0; i < GCodeViewer.EXTRUSION_ROLES_COUNT; i++) {
                if (viewer.isExtrusionRoleVisible(i)) {
                    total += viewer.getEstimatedTime(i);
                }
            }
            return total;
        }

        @SuppressLint("SetTextI18n")
        private void updateValues() {
            GCodeViewer viewer = getViewer();
            GCodeProcessorResult result = getResult();
            if (viewer == null) return;

            float[] values = new float[2 + GCodeViewer.EXTRUSION_ROLES_COUNT];
            double totalWeight = 0;
            double totalLength = 0;
            values[0] = 0;
            values[values.length - 1] = 1;
            float prev = 0;
            int lastVisible = 0;
            float totalTime = getTotalEstimatedTime();
            for (int i = 0; i < GCodeViewer.EXTRUSION_ROLES_COUNT; i++) {
                if (viewer.isExtrusionRoleVisible(i)) {
                    float percent = viewer.getEstimatedTime(i) / totalTime;
                    values[i + 1] = prev + percent;
                    lastVisible = i;
                    prev = values[i + 1];
                    totalLength += result.getUsedFilamentMM(i);
                    totalWeight += result.getUsedFilamentG(i);
                } else {
                    values[i + 1] = prev;
                }
            }

            values[lastVisible] = 1;

            segmentsView.setValues(values);
            totalView.setText(formatComplex(totalWeight, totalLength, totalTime));
        }

        private static String formatComplex(double weight, double length, float time) {
            StringBuilder sb = new StringBuilder();
            if (weight > 0) {
                sb.append(format.format(weight)).append(" ").append(SliceBeam.INSTANCE.getString(R.string.MenuSliceInfoWeight)).append(" | ");
            }
            sb.append(format.format(length)).append(" ").append(SliceBeam.INSTANCE.getString(R.string.MenuSliceInfoLength)).append(" | ");
            sb.append(formatTime(time));
            return sb.toString();
        }

        private static String formatTime(float time) {
            int secondsTotal = (int) Math.round(Math.ceil(time));
            int seconds = secondsTotal % 60;
            int minutes = ((secondsTotal - seconds) / 60) % 60;
            int hours = ((secondsTotal - seconds) / 60) / 60;

            StringBuilder sb = new StringBuilder();
            if (hours > 0) {
                sb.append(hours).append(" ").append(SliceBeam.INSTANCE.getString(R.string.MenuSliceInfoHour));
            }
            if (minutes > 0) {
                if (sb.length() > 0) sb.append(" ");

                sb.append(minutes).append(" ").append(SliceBeam.INSTANCE.getString(R.string.MenuSliceInfoMinute));
            }
            if (seconds > 0 || sb.length() == 0) {
                if (sb.length() > 0) sb.append(" ");

                sb.append(seconds).append(" ").append(SliceBeam.INSTANCE.getString(R.string.MenuSliceInfoSecond));
            }

            return sb.toString();
        }

        @Override
        public void onApplyTheme() {
            totalView.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
        }

        private final class ExtrusionRoleView extends LinearLayout implements IThemeView {
            private MaterialCheckBox checkBox;
            private TextView titleView;
            private TextView timeView;

            private Runnable invalidateGl;

            public ExtrusionRoleView(Context context) {
                super(context);
                setOrientation(HORIZONTAL);
                setGravity(Gravity.CENTER_VERTICAL);
                setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(16), 0);

                checkBox = new MaterialCheckBox(getContext()) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent event) {
                        return false;
                    }
                };
                addView(checkBox, new LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)));

                titleView = new TextView(context);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
                    leftMargin = ViewUtils.dp(12);
                    rightMargin = ViewUtils.dp(8);
                }});

                timeView = new TextView(context);
                timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                addView(timeView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(42)));
                onApplyTheme();
            }

            public void bind(GCodeViewer viewer, GCodeProcessorResult result, @GCodeViewer.ExtrusionRole int role) {
                switch (role) {
                    case GCodeViewer.EXTRUSION_ROLE_NONE:
                        titleView.setText(Slic3rLocalization.getString("Unknown"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_PERIMETER:
                        titleView.setText(Slic3rLocalization.getString("Perimeter"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_EXTERNAL_PERIMETER:
                        titleView.setText(Slic3rLocalization.getString("External perimeter"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_OVERHANG_PERIMETER:
                        titleView.setText(Slic3rLocalization.getString("Overhang perimeter"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_INTERNAL_INFILL:
                        titleView.setText(Slic3rLocalization.getString("Internal infill"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_SOLID_INFILL:
                        titleView.setText(Slic3rLocalization.getString("Solid infill"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_TOP_SOLID_INFILL:
                        titleView.setText(Slic3rLocalization.getString("Top solid infill"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_IRONING:
                        titleView.setText(Slic3rLocalization.getString("Ironing"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_BRIDGE_INFILL:
                        titleView.setText(Slic3rLocalization.getString("Bridge infill"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_GAP_FILL:
                        titleView.setText(Slic3rLocalization.getString("Gap fill"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_SKIRT:
                        titleView.setText(Slic3rLocalization.getString("Skirt/Brim"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_SUPPORT_MATERIAL:
                        titleView.setText(Slic3rLocalization.getString("Support material"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_SUPPORT_MATERIAL_INTERFACE:
                        titleView.setText(Slic3rLocalization.getString("Support material interface"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_WIPE_TOWER:
                        titleView.setText(Slic3rLocalization.getString("Wipe tower"));
                        break;
                    case GCodeViewer.EXTRUSION_ROLE_CUSTOM:
                        titleView.setText(Slic3rLocalization.getString("Custom"));
                        break;
                }

                timeView.setText(formatComplex(result.getUsedFilamentG(role), result.getUsedFilamentMM(role), viewer.getEstimatedTime(role)));

                checkBox.setChecked(viewer.isExtrusionRoleVisible(role));
                checkBox.setButtonTintList(ColorStateList.valueOf(SegmentsView.mapColor(role)));
                setOnClickListener(v -> {
                    if (getTotalEstimatedTime() == viewer.getEstimatedTime(role)) {
                        return;
                    }

                    viewer.toggleExtrusionRoleVisible(role);
                    checkBox.setChecked(!checkBox.isChecked());
                    updateValues();
                    if (invalidateGl != null) ViewUtils.removeCallbacks(invalidateGl);
                    ViewUtils.postOnMainThread(invalidateGl = () -> {
                        Pair<Long, Long> p = viewer.getLayersViewRange();
                        viewer.setLayersViewRange(p.first, p.second);
                        fragment.getGlView().requestRender();
                    }, 250);
                });
            }

            @Override
            public void onApplyTheme() {
                titleView.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
                timeView.setTextColor(ThemesRepo.getColor(android.R.attr.textColorSecondary));
                setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 12));
            }
        }
    }

    private final static class LayersMenu extends UnfoldMenu {
        private PositionScrollView fromTrack, toTrack;
        private TextView title;

        private Runnable applyCallback;

        private GCodeViewer getViewer() {
            return fragment.getGlView().getRenderer().getViewer();
        }

        private void applyView(int from, int to) {
            if (applyCallback != null) ViewUtils.removeCallbacks(applyCallback);

            GCodeViewer viewer = getViewer();
            if (viewer == null) {
                return;
            }
            viewer.setLayersViewRange(from - 1, to - 1);
            fragment.getGlView().requestRender();

            title.setText(fragment.getContext().getString(R.string.MenuSliceInfoLayers, from, to));
        }

        @Override
        protected View onCreateView(Context ctx, boolean portrait) {
            LinearLayout ll = new LinearLayout(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);

            ll.addView(new Space(ctx), new LinearLayout.LayoutParams(0, 0, 1f));

            title = new TextView(ctx);
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            title.setTextColor(ThemesRepo.getColor(android.R.attr.colorAccent));
            title.setGravity(Gravity.CENTER);
            ll.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(20)) {{
                bottomMargin = ViewUtils.dp(4);
            }});

            fromTrack = new PositionScrollView(ctx);
            fromTrack.setProgressListener(integer -> {
                if (getViewer() == null) return;
                toTrack.setMinMax(integer, (int) getViewer().getLayersCount());
                if (toTrack.getCurrentPosition() < integer) {
                    toTrack.setCurrentPosition(integer);
                }
                title.setText(fragment.getContext().getString(R.string.MenuSliceInfoLayers, fromTrack.getCurrentPosition(), integer));

                ViewUtils.removeCallbacks(applyCallback);
                ViewUtils.postOnMainThread(applyCallback = ()-> applyView(integer, toTrack.getCurrentPosition()), 50);
            });
            fromTrack.setListener(integer -> applyView(integer, toTrack.getCurrentPosition()));
            ll.addView(fromTrack, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(80)));

            toTrack = new PositionScrollView(ctx);
            toTrack.setProgressListener(integer -> {
                title.setText(fragment.getContext().getString(R.string.MenuSliceInfoLayers, fromTrack.getCurrentPosition(), integer));

                ViewUtils.removeCallbacks(applyCallback);
                ViewUtils.postOnMainThread(applyCallback = ()-> applyView(fromTrack.getCurrentPosition(), integer), 50);
            });
            toTrack.setListener(integer -> applyView(fromTrack.getCurrentPosition(), integer));
            ll.addView(toTrack, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(80)));

            ll.addView(new DividerView(ctx), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(1f)));
            LinearLayout toolbar = new LinearLayout(ctx);
            toolbar.setPadding(ViewUtils.dp(12), 0, ViewUtils.dp(12), 0);
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            toolbar.setGravity(Gravity.CENTER_VERTICAL);
            toolbar.setBackground(ViewUtils.createRipple(ThemesRepo.getColor(android.R.attr.colorControlHighlight), 0));
            toolbar.setOnClickListener(v -> dismiss());

            ImageView icon = new ImageView(ctx);
            icon.setImageResource(R.drawable.arrow_left_outline_28);
            icon.setColorFilter(ThemesRepo.getColor(android.R.attr.textColorSecondary));
            toolbar.addView(icon, new LinearLayout.LayoutParams(ViewUtils.dp(28), ViewUtils.dp(28)));

            TextView title = new TextView(ctx);
            title.setText(R.string.MenuOrientationPositionBack);
            title.setTypeface(ViewUtils.getTypeface(ViewUtils.ROBOTO_MEDIUM));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            title.setTextColor(ThemesRepo.getColor(android.R.attr.textColorPrimary));
            toolbar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) {{
                leftMargin = ViewUtils.dp(12);
            }});

            ll.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewUtils.dp(52)));
            return ll;
        }

        @Override
        public int getRequestedSize(FrameLayout into, boolean portrait) {
            return portrait ? ViewUtils.dp(80) * 2 + ViewUtils.dp(24) + ViewUtils.dp(52) + ViewUtils.dp(12) : super.getRequestedSize(into, false);
        }

        @Override
        protected void onCreate() {
            super.onCreate();

            GCodeViewer viewer = getViewer();
            if (viewer == null) return;
            long max = viewer.getLayersCount();
            fromTrack.setMinMax(1, (int) max);
            toTrack.setMinMax(1, (int) max);

            Pair<Long, Long> range = viewer.getLayersViewRange();
            // TODO: Support long instead of int in PositionScrollView
            fromTrack.setCurrentPosition(Math.min(range.first.intValue() + 1, range.second.intValue() + 1));
            toTrack.setCurrentPosition(Math.max(range.first.intValue() + 1, range.second.intValue() + 1));

            title.setText(fragment.getContext().getString(R.string.MenuSliceInfoLayers, fromTrack.getCurrentPosition(), toTrack.getCurrentPosition()));
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();

            fromTrack.stopScroll();
            toTrack.stopScroll();
        }
    }
}
