package com.dotcompliance.limologs.survey;

import org.researchstack.backbone.step.Step;

public class ImageCaptureStep extends Step {

    public ImageCaptureStep(String identifier) {
        super(identifier);
    }

    public ImageCaptureStep(String identifier, String title) {
        super(identifier, title);
    }

    @Override
    public Class getStepLayoutClass()
    {
        return ImageStepLayout.class;
    }
}
