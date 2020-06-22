package com.pspdfkit.cordova.event;

import java.util.List;

import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.cordova.event.EventDispatcher;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.cordova.event.AnnotationSelectedListener;

import android.R;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;

public class OpenAssetModalListener implements OnContextualToolbarLifecycleListener {

    // private final AnnotationSelectedListener annotationSelectedListener;

    public OpenAssetModalListener() {
    }

    @Override
    public void onPrepareContextualToolbar(@NonNull ContextualToolbar toolbar) {

    }

    @Override
    public void onDisplayContextualToolbar(ContextualToolbar toolbar) {
        // squash;
    }

    @Override
    public void onRemoveContextualToolbar(ContextualToolbar toolbar) {
        //squash;
    }

}