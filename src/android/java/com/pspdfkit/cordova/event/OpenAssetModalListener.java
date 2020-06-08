package com.pspdfkit.cordova.event;

import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.cordova.event.EventDispatcher;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.cordova.event.AnnotationSelectedListener;

import android.R;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

public class OpenAssetModalListener implements OnContextualToolbarLifecycleListener {

    private final AnnotationSelectedListener annotationSelectedListener;

    public OpenAssetModalListener(AnnotationSelectedListener annotationSelectedListener) {
        this.annotationSelectedListener = annotationSelectedListener;
    }

    @Override
    public void onPrepareContextualToolbar(@NonNull ContextualToolbar toolbar) {
        // This is called whenever toolbar is getting displayed
        if (toolbar instanceof AnnotationEditingToolbar) {
            // Sanity check to make sure there is only 1 selected annotation.
            if (this.getCurrentActivity().getPdfFragment().getSelectedAnnotations().size() != 1)
                return;

            // Get the existing menu items to add new items later.
            final List<ContextualToolbarMenuItem> menuItems = ((AnnotationEditingToolbar) toolbar).getMenuItems();

            // Create custom menu item.
            final ContextualToolbarMenuItem openForm = ContextualToolbarMenuItem.createSingleItem(this,
                    R.id.pspdf_menu_custom, ContextCompat.getDrawable(this, R.drawable.ic_input_add), "Title",
                    Color.WHITE, Color.WHITE, ContextualToolbarMenuItem.Position.END, false);

            // Add the custom item to our toolbar.
            menuItems.add(openForm);
            toolbar.setMenuItems(menuItems);

            // Add a click listener to handle clicks on the custom item.
            toolbar.setOnMenuItemClickListener((toolbar1, menuItem) -> {
                if (menuItem.getId() == R.id.pspdf_menu_custom) {
                    EventDispatcher.getInstance().sendEvent("onOpenAssetActionModal", annotationSelectedListener.getAnnotation());
                    return true;
                }
                return false;
            });
        }
    }

}