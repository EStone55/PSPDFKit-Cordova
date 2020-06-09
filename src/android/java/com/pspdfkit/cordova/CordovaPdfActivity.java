package com.pspdfkit.cordova;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pspdfkit.cordova.event.EventDispatcher;
import com.pspdfkit.cordova.event.OpenAssetModalListener;
import com.pspdfkit.cordova.event.AnnotationSelectedListener;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.listeners.DocumentListener;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.toolbar.ContextualToolbar;

import java.util.List;

import javax.activation.DataContentHandler;

import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;

import android.R;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.pspdfkit.cordova.Utilities.checkArgumentNotNull;

public class CordovaPdfActivity extends PdfActivity implements OnContextualToolbarLifecycleListener{

  public static final String LOG_TAG = "CordovaPdfActivity";

  /**
   * For communication with the JavaScript context, we keep a static reference to
   * the current activity.
   */
  private static CordovaPdfActivity currentActivity;
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();

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

  @NonNull
  private final static AnnotationSelectedListener annotationSelectedListener = new AnnotationSelectedListener();

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
      final ContextualToolbarMenuItem customItem = ContextualToolbarMenuItem.createSingleItem(
        this,
        8237456, )
        ContextCompat.getDrawable(this, R.drawable.ic_menu_add), 
        "Title", 
        Color.WHITE,
        Color.WHITE, 
        ContextualToolbarMenuItem.Position.END, 
        false
        );

      // Add the custom item to our toolbar.
      menuItems.add(customItem);
      toolbar.setMenuItems(menuItems);
      JSONObject data = new JSONObject();
      data.put("icon", customItem);

      EventDispatcher.getInstance().sendEvent("onGenericEvent", data);
      // Add a click listener to handle clicks on the custom item.
      toolbar.setOnMenuItemClickListener((toolbar1, menuItem) -> {
        if (menuItem.getId() == 8237456) {
          // EventDispatcher.getInstance().sendEvent("onGenericEvent", new JSONObject());
          EventDispatcher.getInstance().sendEvent("onOpenAssetActionModal", annotationSelectedListener.getAnnotation());
          return true;
        }
        return false;
      });
    }
  }

  @Override
  public void onDisplayContextualToolbar(ContextualToolbar toolbar) {
    // squash;
  }

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

    pdfFragment.addDocumentListener(listener);
    pdfFragment.addOnAnnotationSelectedListener(annotationSelectedListener);
    this.setOnContextualToolbarLifecycleListener(this);

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
