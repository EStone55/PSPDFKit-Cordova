package com.pspdfkit.cordova.event;

import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.ui.special_mode.manager.AnnotationManager.OnAnnotationSelectedListener;
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController;
import com.pspdfkit.cordova.event.EventDispatcher;
import org.json.JSONObject;

public class AnnotationSelectedListener implements OnAnnotationSelectedListener {

    public AnnotationSelectedListener() {

    }

    public void onAnnotationSelected(Annotation annotation, boolean annotationCreated) {
        EventDispatcher.getInstance().sendEvent("onAnnotationSelected", new JSONObject(annotation.toInstantJson()));
    }

    public boolean onPrepareAnnotationSelection(AnnotationSelectionController controller, Annotation annotation,
            boolean annotationCreated) {
        return true;
    }

}