package com.pspdfkit.cordova;

import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

import androidx.annotation.NonNull;

import com.pspdfkit.datastructures.Range;

import com.pspdfkit.cordova.event.EventDispatcher;
import com.pspdfkit.cordova.event.OpenAssetModalListener;
import com.pspdfkit.cordova.event.AnnotationSelectedListener;
import com.pspdfkit.cordova.event.AnnotationUpdatedListener;
import com.pspdfkit.document.sharing.DocumentSharingManager;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.document.sharing.ShareAction;
import com.pspdfkit.annotations.StampAnnotation;

import com.pspdfkit.listeners.DocumentListener;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.toolbar.ContextualToolbar;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.Integer;

import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.grouping.presets.AnnotationEditingToolbarGroupingRule;
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem;

import com.pspdfkit.annotations.stamps.StampPickerItem;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.configuration.StampAnnotationConfiguration;
import com.pspdfkit.annotations.stamps.CustomStampAppearanceStreamGenerator;
import com.pspdfkit.annotations.appearance.AssetAppearanceStreamGenerator;
import com.pspdfkit.annotations.LinkAnnotation;
import com.pspdfkit.annotations.AnnotationProvider;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.actions.UriAction;


import com.pspdfkit.ui.dialog.DocumentSharingDialog;
import com.pspdfkit.ui.dialog.DocumentSharingDialogConfiguration;
import com.pspdfkit.document.sharing.SharingOptions;
import com.pspdfkit.ui.dialog.BaseDocumentSharingDialog;

import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.pspdfkit.cordova.Utilities.checkArgumentNotNull;

import org.apache.cordova.CordovaInterface;
import com.pspdfkit.cordova.PSPDFKitPlugin;

/**
 * Implementation of OnContextualToolbarLifecycleListener is specific to
 * masterlibraryu It is used to customize the annotation editing toolbar to
 * select the menu to add fields to the asset
 */
public class CordovaPdfActivity extends PdfActivity implements OnContextualToolbarLifecycleListener {

  public static final String LOG_TAG = "CordovaPdfActivity";
  @NonNull
  private final String CUSTOM_AP_STREAM_SUBJECT = "CustomApStream";
  /**
   * For communication with the JavaScript context, we keep a static reference to
   * the current activity.
   */
  private static CordovaPdfActivity currentActivity;
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();
  @NonNull
  private final CustomStampAppearanceStreamGenerator customStampAppearanceStreamGenerator = new CustomStampAppearanceStreamGenerator();
  private List<String> stampTitles = new ArrayList<>(); 
  /**
   * Nested class. Code specific to ML, not in default plugin. Description:
   * Specificies the rules for placement of custom items in toolbars
   */
  public class CustomAnnotationEditingToolbarGroupingRule extends AnnotationEditingToolbarGroupingRule {
    public CustomAnnotationEditingToolbarGroupingRule(@NonNull Context context) {
      super(context);
    }

    @NonNull
    @Override
    public List<MenuItem> getGroupPreset(int capacity, int itemsCount) {
      // Copy preset from default grouping rule while making room for additional
      // custom item.
      List<com.pspdfkit.ui.toolbar.grouping.presets.MenuItem> groupPreset = new ArrayList<>(
          super.getGroupPreset(capacity - 1, itemsCount - 1));

      // Add our custom item to the grouping preset.
      groupPreset.add(new com.pspdfkit.ui.toolbar.grouping.presets.MenuItem(
          currentActivity.getResources().getIdentifier("custom_button_id", "id", currentActivity.getPackageName())));

      return groupPreset;
    }
  }

  public static class CustomSharingDialogListener implements DocumentSharingDialog.SharingDialogListener {

    private boolean shouldAddLinks;

    public CustomSharingDialogListener() {

    }

    @Override
    public void onAccept(@NonNull SharingOptions shareOptions) {
      if (this.shouldAddLinks) {
        // add link annotations on top of stamps here
        // PdfDocument document =  currentActivity.getDocument();
        AnnotationProvider annotationProvider = currentActivity.getDocument().getAnnotationProvider();
        List<Annotation> annotations = annotationProvider.getAnnotations(0);
        List<LinkAnnotation> linkAnnotations = new ArrayList<>();

        Log.i(LOG_TAG, "OG annotations");
        for (Annotation annotation : annotations) {
            Log.i(LOG_TAG, annotation.getName());
        } 

        for (Annotation annotation : annotations) {
            AnnotationType annotationType = annotation.getType();
            if (annotationType == AnnotationType.STAMP) {
                StampAnnotation stampAnnotation = (StampAnnotation) annotation;
                Log.i(LOG_TAG, stampAnnotation.getTitle());
                if (currentActivity.stampTitles.contains(stampAnnotation.getTitle())) {
                    RectF bbox = stampAnnotation.getBoundingBox();
                    LinkAnnotation linkAnnotation = new LinkAnnotation(0);
                    linkAnnotation.setBoundingBox(bbox);
                    linkAnnotation.setAction(new UriAction("https://github.com/"));
                    linkAnnotations.add(linkAnnotation);
                    annotationProvider.addAnnotationToPage(linkAnnotation);
                }
            }
        }

        try {
            currentActivity.getCurrentActivity().saveDocument();
        } catch (IOException ex) {
            Log.i(LOG_TAG, "DOCUMENT NOT SAVED");
        }

        List<Annotation> newAnnotations = annotationProvider.getAnnotations(0);
        Log.i(LOG_TAG, "New annotations");
        for (Annotation annotation : newAnnotations) {
            Log.i(LOG_TAG, annotation.getName());
        } 

        DocumentSharingManager.shareDocument(currentActivity, currentActivity.getDocument(), ShareAction.SEND, shareOptions);
        for (LinkAnnotation linkAnnotation : linkAnnotations) {
            annotationProvider.removeAnnotationFromPage(linkAnnotation);
        }

        try {
            currentActivity.getCurrentActivity().saveDocument();
        } catch (IOException ex) {
            Log.i(LOG_TAG, "DOCUMENT NOT SAVED");
        }

        List<Annotation> removedAnnotations = annotationProvider.getAnnotations(0);
        Log.i(LOG_TAG, "Removed annotations");
        for (Annotation annotation : removedAnnotations) {
            Log.i(LOG_TAG, annotation.getName());
        } 

      } else {
        DocumentSharingManager.shareDocument(currentActivity, currentActivity.getDocument(), ShareAction.SEND, shareOptions);
      }
    }

    @Override
    public void onDismiss() {

    }

    public void setShouldAddLinks(boolean shouldAddLinks) {
        this.shouldAddLinks = shouldAddLinks;
    }

  }

  public static class CustomSharingDialog extends BaseDocumentSharingDialog {
    
    private DialogLayout dialogLayout;
    private static CustomSharingDialogListener customSharingDialogListener = new CustomSharingDialogListener();

    @Override
    public DocumentSharingDialog.SharingDialogListener getListener() {
        return customSharingDialogListener;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialogLayout = new DialogLayout(getConfiguration(), getContext());

        dialogLayout.positiveButton.setOnClickListener(v -> {
            if (getListener() != null) {
                CustomSharingDialogListener listener = (CustomSharingDialogListener) getListener();
                listener.setShouldAddLinks(shouldAddLinks());
                listener.onAccept(getSharingOptions());
                dismiss();
            }
        });
        return dialogLayout.createDialog();
    }

    private SharingOptions getSharingOptions() {
        PdfProcessorTask.AnnotationProcessingMode annotationProcessingMode = PdfProcessorTask.AnnotationProcessingMode.KEEP;
        if (dialogLayout.addLinks.isChecked()) {
            annotationProcessingMode = PdfProcessorTask.AnnotationProcessingMode.KEEP;
        }
        return new SharingOptions(
            annotationProcessingMode,
            Collections.singletonList(new Range(0, currentActivity.getDocument().getPageCount())),
            dialogLayout.documentNameEditText.getText().toString()
        );
    }

    private boolean shouldAddLinks() {
        return dialogLayout.addLinks.isChecked();
    }

  }

  static class DialogLayout {
    DocumentSharingDialogConfiguration configuration;
    Context context;

    View root;
    EditText documentNameEditText;
    TextView positiveButton;
    CheckBox addLinks;

    DialogLayout(DocumentSharingDialogConfiguration configuration, Context context) {
        this.configuration = configuration;
        this.context = context;

        int layoutId = currentActivity.getResources().getIdentifier("custom_document_sharing_dialog", "layout", currentActivity.getPackageName());
        root = View.inflate(context, layoutId, null);

        int shareDialogDocumentNameId = currentActivity.getResources().getIdentifier("share_dialog_document_name", "id", currentActivity.getPackageName());
        documentNameEditText = root.findViewById(shareDialogDocumentNameId);
        documentNameEditText.setText(configuration.getInitialDocumentName());
        documentNameEditText.clearFocus();

        int shareDialogAddLinksId = currentActivity.getResources().getIdentifier("share_dialog_add_links", "id", currentActivity.getPackageName());
        addLinks = root.findViewById(shareDialogAddLinksId);

        int shareButtonId = currentActivity.getResources().getIdentifier("share_button", "id", currentActivity.getPackageName());
        positiveButton = root.findViewById(shareButtonId);
        positiveButton.setText(configuration.getPositiveButtonText());
    }

    Dialog createDialog() {
        return new AlertDialog.Builder(context)
            .setCancelable(true)
            .setTitle(configuration.getDialogTitle())
            .setView(root)
            .create();
    }

  }

  @NonNull
  private final static DocumentListener listener = new SimpleDocumentListener() {
    @Override
    public void onDocumentSaved(@NonNull PdfDocument document) {
      EventDispatcher.getInstance().sendEvent("onDocumentSaved", new JSONObject());
    }

    @Override
    public void onDocumentSaveFailed(@NonNull PdfDocument document, @NonNull Throwable exception) {
      try {
        final JSONObject data = new JSONObject();
        data.put("message", exception.getMessage());
        EventDispatcher.getInstance().sendEvent("onDocumentSaveFailed", data);
      } catch (JSONException e) {
        Log.e(LOG_TAG, "Error while creating JSON payload for 'onDocumentSaveFailed' event.", e);
      }
    }
  };

  // Make an instance of AnnotationSelectedListener
  @NonNull
  private final static AnnotationSelectedListener annotationSelectedListener = new AnnotationSelectedListener();

  // Make an instance of AnnotationUpdatedListener
  @NonNull
  private final static AnnotationUpdatedListener annotationUpdatedListener = new AnnotationUpdatedListener();

  @Override
  public void onDocumentLoaded(@NonNull PdfDocument document) {
    super.onDocumentLoaded(document);

    document.getAnnotationProvider().addAppearanceStreamGenerator(customStampAppearanceStreamGenerator);
    if (currentActivity.getPdfFragment() != null) {

      final List<StampPickerItem> items = createCustomApStampItem();

      currentActivity.getPdfFragment().getAnnotationConfiguration().put(AnnotationType.STAMP,
          StampAnnotationConfiguration.builder(this).setAvailableStampPickerItems(items).build());
    }
  }

  @NonNull
  private List<StampPickerItem> createCustomApStampItem() {
    AssetAppearanceStreamGenerator appearanceStreamGen1 = new AssetAppearanceStreamGenerator(
        "images/air-conditioning-unit.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen2 = new AssetAppearanceStreamGenerator(
        "images/air-handler-unit.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen3 = new AssetAppearanceStreamGenerator(
        "images/backup-generator.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen4 = new AssetAppearanceStreamGenerator("images/blue-light.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen5 = new AssetAppearanceStreamGenerator("images/boiler.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen6 = new AssetAppearanceStreamGenerator("images/chiller.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen7 = new AssetAppearanceStreamGenerator("images/co-detector.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen8 = new AssetAppearanceStreamGenerator("images/condensor.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen9 = new AssetAppearanceStreamGenerator("images/defibrilator.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen10 = new AssetAppearanceStreamGenerator(
        "images/diffuser-heating-cooling.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen11 = new AssetAppearanceStreamGenerator(
        "images/drinking-fountain.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen12 = new AssetAppearanceStreamGenerator(
        "images/electric-meter.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen13 = new AssetAppearanceStreamGenerator(
        "images/electric-motor.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen14 = new AssetAppearanceStreamGenerator(
        "images/emergency-recovery-ventilation.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen15 = new AssetAppearanceStreamGenerator(
        "images/entrance-door-designator.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen16 = new AssetAppearanceStreamGenerator("images/exhaust-fan.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen17 = new AssetAppearanceStreamGenerator(
        "images/expansion-tank.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen18 = new AssetAppearanceStreamGenerator(
        "images/eye-wash-station.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen19 = new AssetAppearanceStreamGenerator("images/fire-alarm.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen20 = new AssetAppearanceStreamGenerator(
        "images/fire-alarm-control-panel.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen21 = new AssetAppearanceStreamGenerator(
        "images/fire-extinguisher.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen22 = new AssetAppearanceStreamGenerator(
        "images/fire-hydrant.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen23 = new AssetAppearanceStreamGenerator(
        "images/first-aid-station.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen24 = new AssetAppearanceStreamGenerator("images/gas-meter.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen25 = new AssetAppearanceStreamGenerator("images/glycol-tank.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen26 = new AssetAppearanceStreamGenerator("images/grease-trap.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen27 = new AssetAppearanceStreamGenerator(
        "images/handicap-accessibility.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen28 = new AssetAppearanceStreamGenerator(
        "images/main-distribution-panel.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen29 = new AssetAppearanceStreamGenerator("images/makeup-air.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen30 = new AssetAppearanceStreamGenerator("images/manhole.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen31 = new AssetAppearanceStreamGenerator(
        "images/panel-electric.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen32 = new AssetAppearanceStreamGenerator("images/pump-hvac.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen33 = new AssetAppearanceStreamGenerator(
        "images/pump-potable.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen34 = new AssetAppearanceStreamGenerator("images/return-air.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen35 = new AssetAppearanceStreamGenerator("images/roof-access.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen36 = new AssetAppearanceStreamGenerator(
        "images/roof-top-unit.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen37 = new AssetAppearanceStreamGenerator("images/unit-heater.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen38 = new AssetAppearanceStreamGenerator(
        "images/unit-ventilator-cabinet-type.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen39 = new AssetAppearanceStreamGenerator(
        "images/unit-ventilator-wall-type.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen40 = new AssetAppearanceStreamGenerator(
        "images/valve-generic.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen41 = new AssetAppearanceStreamGenerator("images/water-meter.pdf");
    AssetAppearanceStreamGenerator appearanceStreamGen42 = new AssetAppearanceStreamGenerator(
        "images/water-softening-tank.pdf");
    StampPickerItem stampPickerItem1 = StampPickerItem.fromTitle(this, "air-conditioning-unit")
        .withSize(113, 54)
        .withAppearanceStreamGenerator(appearanceStreamGen1).build();
    StampPickerItem stampPickerItem2 = StampPickerItem.fromTitle(this, "air-handler-unit")
        .withSize(115, 56)
        .withAppearanceStreamGenerator(appearanceStreamGen2).build();
    StampPickerItem stampPickerItem3 = StampPickerItem.fromTitle(this, "backup-generator")
        .withSize(93, 35)
        .withAppearanceStreamGenerator(appearanceStreamGen3).build();
    StampPickerItem stampPickerItem4 = StampPickerItem.fromTitle(this, "blue-light")
        .withSize(34, 35)
        .withAppearanceStreamGenerator(appearanceStreamGen4).build();
    StampPickerItem stampPickerItem5 = StampPickerItem.fromTitle(this, "boiler")
        .withSize(79, 80)
        .withAppearanceStreamGenerator(appearanceStreamGen5).build();
    StampPickerItem stampPickerItem6 = StampPickerItem.fromTitle(this, "chiller")
        .withSize(142, 48)
        .withAppearanceStreamGenerator(appearanceStreamGen6).build();
    StampPickerItem stampPickerItem7 = StampPickerItem.fromTitle(this, "co-detector")
        .withSize(32, 32)
        .withAppearanceStreamGenerator(appearanceStreamGen7).build();
    StampPickerItem stampPickerItem8 = StampPickerItem.fromTitle(this, "condensor")
        .withSize(96, 73)
        .withAppearanceStreamGenerator(appearanceStreamGen8).build();
    StampPickerItem stampPickerItem9 = StampPickerItem.fromTitle(this, "defibrilator")
        .withSize(34, 33)
        .withAppearanceStreamGenerator(appearanceStreamGen9).build();
    StampPickerItem stampPickerItem10 = StampPickerItem.fromTitle(this, "diffuser-heating-cooling")
        .withSize(77, 72)
        .withAppearanceStreamGenerator(appearanceStreamGen10).build();
    StampPickerItem stampPickerItem11 = StampPickerItem.fromTitle(this, "drinking-fountain")
        .withSize(29, 32)
        .withAppearanceStreamGenerator(appearanceStreamGen11).build();
    StampPickerItem stampPickerItem12 = StampPickerItem.fromTitle(this, "electric-meter")
        .withSize(66, 34)
        .withAppearanceStreamGenerator(appearanceStreamGen12).build();
    StampPickerItem stampPickerItem13 = StampPickerItem.fromTitle(this, "electric-motor")
        .withSize(31, 25)
        .withAppearanceStreamGenerator(appearanceStreamGen13).build();
    StampPickerItem stampPickerItem14 = StampPickerItem.fromTitle(this, "emergency-recovery-ventilation")
        .withSize(64, 51)
        .withAppearanceStreamGenerator(appearanceStreamGen14).build();
    StampPickerItem stampPickerItem15 = StampPickerItem.fromTitle(this, "entrance-door-designator")
        .withSize(46, 24)
        .withAppearanceStreamGenerator(appearanceStreamGen15).build();
    StampPickerItem stampPickerItem16 = StampPickerItem.fromTitle(this, "exhaust-fan")
        .withSize(44, 42)
        .withAppearanceStreamGenerator(appearanceStreamGen16).build();
    StampPickerItem stampPickerItem17 = StampPickerItem.fromTitle(this, "expansion-tank")
        .withSize(42, 38)
        .withAppearanceStreamGenerator(appearanceStreamGen17).build();
    StampPickerItem stampPickerItem18 = StampPickerItem.fromTitle(this, "eye-wash-station")
        .withSize(33, 32)
        .withAppearanceStreamGenerator(appearanceStreamGen18).build();
    StampPickerItem stampPickerItem19 = StampPickerItem.fromTitle(this, "fire-alarm")
        .withSize(35, 32)
        .withAppearanceStreamGenerator(appearanceStreamGen19).build();
    StampPickerItem stampPickerItem20 = StampPickerItem.fromTitle(this, "fire-alarm-control-panel")
        .withSize(46, 32)
        .withAppearanceStreamGenerator(appearanceStreamGen20).build();
    StampPickerItem stampPickerItem21 = StampPickerItem.fromTitle(this, "fire-extinguisher")
        .withSize(33, 32)
        .withAppearanceStreamGenerator(appearanceStreamGen21).build();
    StampPickerItem stampPickerItem22 = StampPickerItem.fromTitle(this, "fire-hydrant")
        .withSize(42, 46)
        .withAppearanceStreamGenerator(appearanceStreamGen22).build();
    StampPickerItem stampPickerItem23 = StampPickerItem.fromTitle(this, "first-aid-station")
        .withSize(33, 31)
        .withAppearanceStreamGenerator(appearanceStreamGen23).build();
    StampPickerItem stampPickerItem24 = StampPickerItem.fromTitle(this, "gas-meter")
        .withSize(69, 26)
        .withAppearanceStreamGenerator(appearanceStreamGen24).build();
    StampPickerItem stampPickerItem25 = StampPickerItem.fromTitle(this, "glycol-tank")
        .withSize(44, 41)
        .withAppearanceStreamGenerator(appearanceStreamGen25).build();
    StampPickerItem stampPickerItem26 = StampPickerItem.fromTitle(this, "grease-trap")
        .withSize(36, 36)
        .withAppearanceStreamGenerator(appearanceStreamGen26).build();
    StampPickerItem stampPickerItem27 = StampPickerItem.fromTitle(this, "handicap-accessibility")
        .withSize(55, 81)
        .withAppearanceStreamGenerator(appearanceStreamGen27).build();
    StampPickerItem stampPickerItem28 = StampPickerItem.fromTitle(this, "main-distribution-panel")
        .withSize(61, 31)
        .withAppearanceStreamGenerator(appearanceStreamGen28).build();
    StampPickerItem stampPickerItem29 = StampPickerItem.fromTitle(this, "makeup-air")
        .withSize(73, 58)
        .withAppearanceStreamGenerator(appearanceStreamGen29).build();
    StampPickerItem stampPickerItem30 = StampPickerItem.fromTitle(this, "manhole")
        .withSize(38, 36)
        .withAppearanceStreamGenerator(appearanceStreamGen30).build();
    StampPickerItem stampPickerItem31 = StampPickerItem.fromTitle(this, "panel-electric")
        .withSize(50, 23)
        .withAppearanceStreamGenerator(appearanceStreamGen31).build();
    StampPickerItem stampPickerItem32 = StampPickerItem.fromTitle(this, "pump-hvac")
        .withSize(25, 19)
        .withAppearanceStreamGenerator(appearanceStreamGen32).build();
    StampPickerItem stampPickerItem33 = StampPickerItem.fromTitle(this, "pump-potable")
        .withSize(21, 20)
        .withAppearanceStreamGenerator(appearanceStreamGen33).build();
    StampPickerItem stampPickerItem34 = StampPickerItem.fromTitle(this, "return-air")
        .withSize(67, 67)
        .withAppearanceStreamGenerator(appearanceStreamGen34).build();
    StampPickerItem stampPickerItem35 = StampPickerItem.fromTitle(this, "roof-access")
        .withSize(51, 37)
        .withAppearanceStreamGenerator(appearanceStreamGen35).build();
    StampPickerItem stampPickerItem36 = StampPickerItem.fromTitle(this, "roof-top-unit")
        .withSize(114, 56)
        .withAppearanceStreamGenerator(appearanceStreamGen36).build();
    StampPickerItem stampPickerItem37 = StampPickerItem.fromTitle(this, "unit-heater")
        .withSize(52, 42)
        .withAppearanceStreamGenerator(appearanceStreamGen37).build();
    StampPickerItem stampPickerItem38 = StampPickerItem.fromTitle(this, "unit-ventilator-cabinet-type")
        .withSize(57, 18)
        .withAppearanceStreamGenerator(appearanceStreamGen38).build();
    StampPickerItem stampPickerItem39 = StampPickerItem.fromTitle(this, "unit-ventilator-wall-type")
        .withSize(41, 27)
        .withAppearanceStreamGenerator(appearanceStreamGen39).build();
    StampPickerItem stampPickerItem40 = StampPickerItem.fromTitle(this, "valve-generic")
        .withSize(33, 22)
        .withAppearanceStreamGenerator(appearanceStreamGen40).build();
    StampPickerItem stampPickerItem41 = StampPickerItem.fromTitle(this, "water-meter")
        .withSize(66, 25)
        .withAppearanceStreamGenerator(appearanceStreamGen41).build();
    StampPickerItem stampPickerItem42 = StampPickerItem.fromTitle(this, "water-softening-tank")
        .withSize(40, 40)
        .withAppearanceStreamGenerator(appearanceStreamGen42).build();

    final List<StampPickerItem> items = new ArrayList<>();

    items.add(stampPickerItem1);
    items.add(stampPickerItem2);
    items.add(stampPickerItem3);
    items.add(stampPickerItem4);
    items.add(stampPickerItem5);
    items.add(stampPickerItem6);
    items.add(stampPickerItem7);
    items.add(stampPickerItem8);
    items.add(stampPickerItem9);
    items.add(stampPickerItem10);
    items.add(stampPickerItem11);
    items.add(stampPickerItem12);
    items.add(stampPickerItem13);
    items.add(stampPickerItem14);
    items.add(stampPickerItem15);
    items.add(stampPickerItem16);
    items.add(stampPickerItem17);
    items.add(stampPickerItem18);
    items.add(stampPickerItem19);
    items.add(stampPickerItem20);
    items.add(stampPickerItem21);
    items.add(stampPickerItem22);
    items.add(stampPickerItem23);
    items.add(stampPickerItem24);
    items.add(stampPickerItem25);
    items.add(stampPickerItem26);
    items.add(stampPickerItem27);
    items.add(stampPickerItem28);
    items.add(stampPickerItem29);
    items.add(stampPickerItem30);
    items.add(stampPickerItem31);
    items.add(stampPickerItem32);
    items.add(stampPickerItem33);
    items.add(stampPickerItem34);
    items.add(stampPickerItem35);
    items.add(stampPickerItem36);
    items.add(stampPickerItem37);
    items.add(stampPickerItem38);
    items.add(stampPickerItem39);
    items.add(stampPickerItem40);
    items.add(stampPickerItem41);
    items.add(stampPickerItem42);

    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("air-conditioning-unit", appearanceStreamGen1);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("air-handler-unit", appearanceStreamGen2);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("backup-generator", appearanceStreamGen3);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("blue-light", appearanceStreamGen4);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("boiler", appearanceStreamGen5);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("chiller", appearanceStreamGen6);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("co-detector", appearanceStreamGen7);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("condensor", appearanceStreamGen8);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("defibrilator", appearanceStreamGen9);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("diffuser-heating-cooling", appearanceStreamGen10);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("drinking-fountain", appearanceStreamGen11);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("electric-meter", appearanceStreamGen12);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("electric-motor", appearanceStreamGen13);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("emergency-recovery-ventilation", appearanceStreamGen14);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("entrance-door-designator", appearanceStreamGen15);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("exhaust-fan", appearanceStreamGen16);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("expansion-tank", appearanceStreamGen17);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("eye-wash-station", appearanceStreamGen18);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("fire-alarm", appearanceStreamGen19);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("fire-alarm-control-panel", appearanceStreamGen20);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("fire-extinguisher", appearanceStreamGen21);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("fire-hydrant", appearanceStreamGen22);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("first-aid-station", appearanceStreamGen23);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("gas-meter", appearanceStreamGen24);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("glycol-tank", appearanceStreamGen25);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("grease-trap", appearanceStreamGen26);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("handicap-accessibility", appearanceStreamGen27);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("main-distribution-panel", appearanceStreamGen28);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("makeup-air", appearanceStreamGen29);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("manhole", appearanceStreamGen30);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("panel-electric", appearanceStreamGen31);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("pump-hvac", appearanceStreamGen32);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("pump-potable", appearanceStreamGen33);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("return-air", appearanceStreamGen34);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("roof-access", appearanceStreamGen35);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("roof-top-unit", appearanceStreamGen36);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("unit-heater", appearanceStreamGen37);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("unit-ventilator-cabinet-type", appearanceStreamGen38);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("unit-ventilator-wall-type", appearanceStreamGen39);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("valve-generic", appearanceStreamGen40);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("water-meter", appearanceStreamGen41);
    customStampAppearanceStreamGenerator.addAppearanceStreamGenerator("water-softening-tank", appearanceStreamGen42);

    return items;
  }

  /**
   * Method that needs to be implemented for OnContextualToolbarLifecycleListener
   * Specific to ML, not in default plugin
   * 
   * @param toolbar
   */
  @Override
  public void onPrepareContextualToolbar(@NonNull ContextualToolbar toolbar) {
    // This is called whenever toolbar is getting displayed
    if (toolbar instanceof AnnotationEditingToolbar) {
      // Sanity check to make sure there is only 1 selected annotation.
      if (currentActivity.getPdfFragment().getSelectedAnnotations().size() != 1)
        return;

      // Get the existing menu items to add new items later.
      final List<ContextualToolbarMenuItem> menuItems = ((AnnotationEditingToolbar) toolbar).getMenuItems();

      // Create custom menu item.
      final ContextualToolbarMenuItem customItem = ContextualToolbarMenuItem.createSingleItem(this,
          this.getResources().getIdentifier("custom_button_id", "id", this.getPackageName()),
          this.getResources()
              .getDrawable(this.getResources().getIdentifier("ic_edit", "drawable", this.getPackageName()), null),
          "Title", Color.WHITE, Color.WHITE, ContextualToolbarMenuItem.Position.END, false);

      // Add the custom item to our toolbar.
      menuItems.add(customItem);
      toolbar.setMenuItems(menuItems);
      toolbar.setMenuItemGroupingRule(new CustomAnnotationEditingToolbarGroupingRule(this));
      // Add a click listener to handle clicks on the custom item.
      toolbar.setOnMenuItemClickListener((toolbar1, menuItem) -> {
        if (menuItem.getId() == this.getResources().getIdentifier("custom_button_id", "id", this.getPackageName())) {
          // EventDispatcher.getInstance().sendEvent("onGenericEvent", new JSONObject());
          EventDispatcher.getInstance().sendEvent("onOpenAssetActionModal", annotationSelectedListener.getData());
          return true;
        }
        return false;
      });
    }
  }

  /**
   * Method that needs to be implemented for OnContextualToolbarLifecycleListener
   * Specific to ML, not in default plugin
   * 
   * @param toolbar
   */
  @Override
  public void onDisplayContextualToolbar(ContextualToolbar toolbar) {
    // squash;
  }

  /**
   * Method that needs to be implemented for OnContextualToolbarLifecycleListener
   * Specific to ML, not in default plugin
   * 
   * @param toolbar
   */
  @Override
  public void onRemoveContextualToolbar(ContextualToolbar toolbar) {
    // squash;
  }

  public static CordovaPdfActivity getCurrentActivity() {
    return currentActivity;
  }

  private void bindActivity(@NonNull final CordovaPdfActivity activity) {
    checkArgumentNotNull(activity, "activity");

    final CordovaPdfActivity oldActivity = currentActivity;
    if (oldActivity != null) {
      releaseActivity();
      oldActivity.disposeSubscriptions();
    }

    currentActivity = activity;
    final PdfFragment pdfFragment = currentActivity.getPdfFragment();
    if (pdfFragment == null) {
      throw new IllegalStateException(
          "EventDispatcher only supports binding to activities that have a fragment instance.");
    }

    Log.d("WTF", "listener during create = " + listener);

    this.stampTitles.add("air-conditioning-unit");
    this.stampTitles.add("air-handler-unit");
    this.stampTitles.add("backup-generator");
    this.stampTitles.add("blue-light");
    this.stampTitles.add("boiler");
    this.stampTitles.add("chiller");
    this.stampTitles.add("co-detector");
    this.stampTitles.add("condensor");
    this.stampTitles.add("defibrilator");
    this.stampTitles.add("diffuser-heating-cooling");
    this.stampTitles.add("drinking-fountain");
    this.stampTitles.add("electric-meter");
    this.stampTitles.add("electric-motor");
    this.stampTitles.add("emergency-recovery-ventilation");
    this.stampTitles.add("entrance-door-designator");
    this.stampTitles.add("exhaust-fan");
    this.stampTitles.add("expansion-tank");
    this.stampTitles.add("eye-wash-station");
    this.stampTitles.add("fire-alarm");
    this.stampTitles.add("fire-alarm-control-panel");
    this.stampTitles.add("fire-extinguisher");
    this.stampTitles.add("fire-hydrant");
    this.stampTitles.add("first-aid-station");
    this.stampTitles.add("gas-meter");
    this.stampTitles.add("glycol-tank");
    this.stampTitles.add("grease-trap");
    this.stampTitles.add("handicap-accessibility");
    this.stampTitles.add("main-distribution-panel");
    this.stampTitles.add("makeup-air");
    this.stampTitles.add("manhole");
    this.stampTitles.add("panel-electric");
    this.stampTitles.add("pump-hvac");
    this.stampTitles.add("pump-potable");
    this.stampTitles.add("return-air");
    this.stampTitles.add("roof-access");
    this.stampTitles.add("roof-top-unit");
    this.stampTitles.add("unit-heater");
    this.stampTitles.add("unit-ventilator-cabinet-type");
    this.stampTitles.add("unit-ventilator-wall-type");
    this.stampTitles.add("valve-generic");
    this.stampTitles.add("water-meter");
    this.stampTitles.add("water-softening-tank");

    pdfFragment.addDocumentListener(listener);
    pdfFragment.addOnAnnotationSelectedListener(annotationSelectedListener); // register the AnnotationSelectedListener
    pdfFragment.addOnAnnotationUpdatedListener(annotationUpdatedListener);
    this.setOnContextualToolbarLifecycleListener(this); // Register this CordovaPdfActivity to listen for Toolbar
                                                        // lifecycles
  }

  private void releaseActivity() {
    // Release the current activity instance. However, we intentionally don't
    // unregister the
    // document listener, so that asynchronous save events from saving the document
    // after the
    // activity was destroyed, can still be relayed to the JavaScript interface.
    // Since the listener is static, we also don't leak the activity nor fragment.
    currentActivity = null;
  }

  /**
   * Adds given {@link Disposable} to disposable collection which will be
   * automatically disposed as a part of onDestroy during activity finishing
   * process.
   *
   * @param disposable to add to the disposable collection.
   */
  public void addSubscription(Disposable disposable) {
    compositeDisposable.add(disposable);
  }

  private void disposeSubscriptions() {
    compositeDisposable.dispose();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setDocumentSharingDialogFactory(CustomSharingDialog::new);
    
    setSharingOptionsProvider((document, currentPage) -> {
        return null;
    });
    bindActivity(this);
  }

  @Override
  protected void onDestroy() {
    if (currentActivity.equals(this)) {
      releaseActivity();

      if (isFinishing()) {
        disposeSubscriptions();
      }
    }

    super.onDestroy();
  }

  @Override
  public void finish() {
    super.finish();
    // Notify JavaScript listeners that the activity was dismissed.
    EventDispatcher.getInstance().notifyActivityDismissed();
  }

  /**
   * Dismisses this PDF activity.
   */
  public void dismiss() {
    finish();
  }

  public boolean saveDocument() throws IOException {
    final PdfDocument document = getDocument();
    if (document != null) {
      boolean modified = document.saveIfModified();
      Log.d("WTF", "wasModified  + " + modified);
      Log.d("WTF", "listener during save = " + listener);
      return modified;
    } else {
      return false;
    }
  }
}
