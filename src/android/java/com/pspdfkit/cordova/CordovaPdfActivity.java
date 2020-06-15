package com.pspdfkit.cordova;

import android.os.Bundle;
import android.util.Log;
import android.content.Context;

import androidx.annotation.NonNull;

import com.pspdfkit.cordova.event.EventDispatcher;
import com.pspdfkit.cordova.event.OpenAssetModalListener;
import com.pspdfkit.cordova.event.AnnotationSelectedListener;
import com.pspdfkit.cordova.event.AnnotationUpdatedListener;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.listeners.DocumentListener;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.toolbar.ContextualToolbar;

import java.util.List;
import java.util.ArrayList;

import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.grouping.presets.AnnotationEditingToolbarGroupingRule;
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem;

import com.pspdfkit.annotations.stamps.StampPickerItem;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.configuration.StampAnnotationConfiguration;

import android.graphics.Color;
import android.graphics.BitmapFactory;

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

  /**
   * For communication with the JavaScript context, we keep a static reference to
   * the current activity.
   */
  private static CordovaPdfActivity currentActivity;
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();

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
          EventDispatcher.getInstance().sendEvent("onOpenAssetActionModal", annotationSelectedListener.getAnnotation());
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

    final List<StampPickerItem> items = new ArrayList<>();
    items.add(StampPickerItem
        .fromBitmap(BitmapFactory.decodeResources(this.getResources(),
            this.getResources()
                .getDrawable(this.getResource().getIdentifier("ac_unit", "drawable", this.getPackageName()))))
        .build());
    pdfFragment.getAnnotationConfiguration().put(AnnotationType.STAMP,
        StampAnnotationConfiguration.builder(this).setAvailableStampPickerItems(items).build());

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
