package com.dotcompliance.limologs.survey;

import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.task.OrderedTask;
import org.researchstack.backbone.ui.ViewTaskActivity;

import java.io.Serializable;
import java.util.List;


public class NavigableOrderedTask extends OrderedTask implements Serializable {
    public NavigableOrderedTask(String identifier, List<Step> steps) {
        super(identifier, steps);
    }

    public NavigableOrderedTask(String identifier, Step... steps) {
        super(identifier, steps);
    }

    @Override
    public Step getStepAfterStep(Step step, TaskResult result) {
        return super.getStepAfterStep(step, result);
    }

    @Override
    public Step getStepBeforeStep(Step step, TaskResult result) {
        return super.getStepBeforeStep(step, result);
    }

    @Override
    public void onViewChange(ViewChangeType type, ViewTaskActivity activity, Step currentStep) {
        super.onViewChange(type, activity, currentStep);

        if (type == ViewChangeType.ActivityCreate) {

        }
        else if (type == ViewChangeType.ActivityStop) {

        }
        else if (type == ViewChangeType.ActivityPause) {
            if (currentStep instanceof ImageCaptureStep) {
                // stop camera and camera preview
            }
        }
        else if (type == ViewChangeType.ActivityResume) {

        }
    }
}
