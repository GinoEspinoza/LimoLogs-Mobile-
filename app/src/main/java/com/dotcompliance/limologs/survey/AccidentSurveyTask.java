package com.dotcompliance.limologs.survey;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

import com.dotcompliance.limologs.data.Preferences;

import org.researchstack.backbone.answerformat.BooleanAnswerFormat;
import org.researchstack.backbone.answerformat.IntegerAnswerFormat;
import org.researchstack.backbone.answerformat.TextAnswerFormat;
import org.researchstack.backbone.result.StepResult;
import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.step.FormStep;
import org.researchstack.backbone.step.InstructionStep;
import org.researchstack.backbone.step.QuestionStep;
import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.ui.ViewTaskActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AccidentSurveyTask extends NavigableOrderedTask {
    public static final int maxDamagePicturesCount = 3;
    public static final int maxPassengersCount = 5;

    // define identifiers
    public static final String kStepBasicInfo = "basic_form_step";
    public static final String kItemLicenseNumberIdentifier = "license_number",    // 1
            kItemDamageIdentifier = "vehicle_damage",           // 2

    kStepAllPicture = "vehicle_picture_",
            kStepMorePicture = "more_vehicle_picture",

    kStepAreYouCDL = "is_cdl",                          // 4
            kStepVehicleTowed = "vehicle_towed",                // 5
            kStepReceiveMedicare = "receive_medicare",          // 6
            kStepLeaveMedicare = "leave_medicare",              // 7
            kStepReceiveTicket = "receive_ticket",              // 8
            kStepAnyoneKilled = "anyone_killed",                // 9
            kStepHavePassengers = "have_passengers",            // 10
            kStepDescribeAccident = "describe_accident",        // 11
            kStepOthersInvolved = "other_vehicles_involved",    // 12

    kStepOtherVehicleForm = "other_vehicle_form",
            kItemOtherDriverIdentifier = "other_drivername",
            kItemOtherAddressIdentifier = "other_address",
            kItemOtherPhoneIdentifier = "other_phone",
            kItemOtherLicenseIdentifier = "other_license_state",
            kItemOtherVMakeIdentifier = "other_vehicle_make",
            kItemOtherVModelIdentifier = "other_vehicle_model",
            kItemOtherVYearIdentifier = "other_vehicle_year",
            kItemOtherVVinIdentifier = "other_vehicle_vin",

    // other driver's insurance information
    kItemOtherInsuranceIdentifier = "other_insurance_company",
            kItemOtherInsPolicyIdentifier = "other_insurance_policy",
            kItemOtherInsAgentIdentifier = "other_insurance_agent",
            kItemOtherVehicleDamageIdentifier = "other_vehicle_damage",
            kItemOtherPassengerCountIdentifier = "passenger_count_other",
            kItemOtherPassengersPhonesIdentifier = "passenger_names_phones",

    kStepOtherDriverLicensePicture = "other_driver_license_picture",
            kStepOtherLicensePlatePicture = "other_license_plate_picture",
            kStepOtherInsuranceCard = "other_insurance_card_picture",

    kStepAdditionalsInvolved = "additional_vehicles_involved",

    kStepPassengersVehicle = "passengers_in_vehicle",
            kStepPassengerForm = "passenger_form_",
            kItemPassengerNameIdentifier = "passenger_name_",
            kItemPassengerPhoneIdentifier = "passenger_phone_",
            kItemPassengerInjuredIdentifier = "passenger_injured_",

    kStepAnyOtherPassenger = "any_other_passenger",

    kStepPoliceCalledWhy = "police_called_form",
            kItemPoliceCalledIdentifier = "police_called",
            kItemPoliceCalledWhyIdentifier = "police_not_called_why",

    kStepPoliceComeScene = "police_come_scene",
            kStepPoliceForm = "police_form",
            kItemPoliceDepartmentIdentifier = "police_department",
            kItemPoliceNameIdentifier = "police_name",
            kItemPoliceBadgeIdentifier = "police_badge",
            kItemPoliceArrestIdentifier = "arrested_by_police",
            kItemPoliceGivenIdentifier = "info_by_police",

    kStepWitness = "witness";


    public AccidentSurveyTask(String identifier) {
        super(identifier);

        ArrayList<Step> stepList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:MM:SS", Locale.US);
        sdf.setTimeZone(Preferences.getDriverTimezone());

        String driverName = Preferences.mDriver.firstname + " " + Preferences.mDriver.lastname;
        String vehicleNo = Preferences.getCurrentVehicle() != null ? Preferences.getCurrentVehicle().vehicle_no : "";

        InstructionStep instructionStep = new InstructionStep("identifier",
                "Accident Survey",
                sdf.format(new Date()) + "<br>" +
                        driverName + "<br>" +
                        vehicleNo + "<br>" +
                        Preferences.mCurrentLocation);
        stepList.add(instructionStep);

        // step: Driver License & Damages to your vehicle
        FormStep form_step = new FormStep(kStepBasicInfo, "", "");
        ArrayList<QuestionStep> formItems = new ArrayList<>();

        TextAnswerFormat textAnswerFormat = new TextAnswerFormat();
        textAnswerFormat.setIsMultipleLines(false);

        TextAnswerFormat multilineTextFormat = new TextAnswerFormat();
        multilineTextFormat.setIsMultipleLines(true);

        formItems.add(new QuestionStep(kItemLicenseNumberIdentifier, "Your Driver's License Number", textAnswerFormat));
        formItems.add(new QuestionStep(kItemDamageIdentifier, "Describe damage to your vehicle", multilineTextFormat));
        form_step.setFormSteps(formItems);

        stepList.add(form_step);

        // step: take pictures of all vehicles involved
        InstructionStep step = new InstructionStep("identifier0", "Vehicle Pictures", "Attach Pictures of All Vehicles With or Without Damage");
        stepList.add(step);

        for (int i = 0; i < maxDamagePicturesCount; i++) {
            ImageCaptureStep imageStep = new ImageCaptureStep(kStepAllPicture + i, "Take a Picture");
            stepList.add(imageStep);
        }


        // step: Are you a CDL?
        stepList.add(new QuestionStep(kStepAreYouCDL, "Are you a CDL?", new BooleanAnswerFormat("Yes", "No")));

        // step: vehicle towed
        stepList.add(new QuestionStep(kStepVehicleTowed, "Were any vehicles towed from the scene?", new BooleanAnswerFormat("Yes", "No")));

        stepList.add(new QuestionStep(kStepReceiveMedicare, "Did any parties involved receive medical treatment on scene?", new BooleanAnswerFormat("Yes", "No")));
        stepList.add(new QuestionStep(kStepLeaveMedicare, "Did any parties involved leave the scene for medical treatment?", new BooleanAnswerFormat("Yes", "No")));

        // step: receive ticket, yes/no
        stepList.add(new QuestionStep(kStepReceiveTicket, "Did you receive a ticket?", new BooleanAnswerFormat("Yes", "No")));

        // step: anyone killed, yes/no
        stepList.add(new QuestionStep(kStepAnyoneKilled, "Was anyone killed?", new BooleanAnswerFormat("Yes", "No")));

        // step: have passengers, yes/no
        stepList.add(new QuestionStep(kStepHavePassengers, "Did you have passengers on board vehicle?", new BooleanAnswerFormat("Yes", "No")));

        // step: describe accident
        QuestionStep describeAccidentStep = new QuestionStep(kStepDescribeAccident, "Describe Accident", multilineTextFormat);
        describeAccidentStep.setText("*Example - Driver ran stop sign, and collided with passenger side front fender.");
        stepList.add(describeAccidentStep);

        QuestionStep stepOthersInvolved = new QuestionStep(kStepOthersInvolved, "Were other vehicles involved?", new BooleanAnswerFormat("Yes", "No"));
        stepOthersInvolved.setOptional(false);
        stepList.add(stepOthersInvolved);

        // step: other vehicle info if prior step is yes
        form_step = new FormStep(kStepOtherVehicleForm, "Other Vehicle", "Other Driver IF VEHICLE DID NOT HAVE INSURANCE CALL POLICE IMMEDIATELY");
        formItems = new ArrayList<>();

        formItems.add(new QuestionStep(kItemOtherDriverIdentifier, "Drivers Name", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherAddressIdentifier, "Address", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherPhoneIdentifier, "Phone Number", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherLicenseIdentifier, "License Number and State", textAnswerFormat));

        formItems.add(new QuestionStep(kItemOtherVMakeIdentifier, "Other Vehicle Make", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherVModelIdentifier, "Model", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherVYearIdentifier, "Year", new IntegerAnswerFormat(1900, 2050)));
        formItems.add(new QuestionStep(kItemOtherVVinIdentifier, "VIN", textAnswerFormat));

        formItems.add(new QuestionStep(kItemOtherInsuranceIdentifier, "Insurance Company", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherInsPolicyIdentifier, "Policy #", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherInsAgentIdentifier, "Agents Name", textAnswerFormat));
        formItems.add(new QuestionStep(kItemOtherVehicleDamageIdentifier, "Describe other vehicle's damage", multilineTextFormat));
        formItems.add(new QuestionStep(kItemOtherPassengerCountIdentifier, "How many passengers were in other vehicle", new IntegerAnswerFormat(0, 99)));
        formItems.add(new QuestionStep(kItemOtherPassengersPhonesIdentifier, "Passenger Names and Phone numbers", multilineTextFormat));

        form_step.setFormSteps(formItems);
        stepList.add(form_step);

        // step: other vehicle picture
        stepList.add(new ImageCaptureStep(kStepOtherDriverLicensePicture, "Other drivers operators license"));

        stepList.add(new ImageCaptureStep(kStepOtherLicensePlatePicture, "Other drivers license plate"));

        stepList.add(new ImageCaptureStep(kStepOtherInsuranceCard, "Other drivers insurance card"));


        // step: any passengers in your vehicle
        QuestionStep stepPassengers = new QuestionStep(kStepPassengersVehicle, "Any passengers in your vehicle?", new BooleanAnswerFormat("Yes", "No"));
        stepPassengers.setOptional(false);
        stepList.add(stepPassengers);

        // step: passenger info if prior answer yes
        for (int i = 0; i < maxPassengersCount; i++) {
            form_step = new FormStep(kStepPassengerForm + i, "Your Passegner Information", "");
            formItems = new ArrayList<>();

            QuestionStep passengerNameItem = new QuestionStep(kItemPassengerNameIdentifier + i, "Name", textAnswerFormat);
            passengerNameItem.setOptional(false);
            formItems.add(passengerNameItem);

            formItems.add(new QuestionStep(kItemPassengerPhoneIdentifier + i, "Phone Number", textAnswerFormat));
            formItems.add(new QuestionStep(kItemPassengerInjuredIdentifier + i, "Injured?", new BooleanAnswerFormat("Yes", "No")));

            form_step.setFormSteps(formItems);
            form_step.setOptional(false);

            stepList.add(form_step);

            QuestionStep stepOtherPassengers = new QuestionStep(kStepAnyOtherPassenger + i, "Other passengers?", new BooleanAnswerFormat("Yes", "No"));
            stepOtherPassengers.setOptional(false);
            stepList.add(stepOtherPassengers);
        }

        // step: police called and why
        form_step = new FormStep(kStepPoliceCalledWhy, "", "");
        formItems = new ArrayList<>();

        formItems.add(new QuestionStep(kItemPoliceCalledIdentifier, "Were Police Called?", new BooleanAnswerFormat("Yes", "No")));

        formItems.add(new QuestionStep(kItemPoliceCalledWhyIdentifier, "If no WHY", multilineTextFormat));

        form_step.setFormSteps(formItems);
        stepList.add(form_step);

        // step: police come to scene
        stepList.add(new QuestionStep(kStepPoliceComeScene, "Did police come to scene?", new BooleanAnswerFormat("Yes", "No")));

        // step: police info
        form_step = new FormStep(kStepPoliceForm, "Police Information", "");
        formItems = new ArrayList<>();

        formItems.add(new QuestionStep(kItemPoliceDepartmentIdentifier, "Department Name", textAnswerFormat));
        formItems.add(new QuestionStep(kItemPoliceNameIdentifier, "Officers Name", textAnswerFormat));
        formItems.add(new QuestionStep(kItemPoliceBadgeIdentifier, "Officers Badge #", textAnswerFormat));
        formItems.add(new QuestionStep(kItemPoliceArrestIdentifier, "Was anyone cited or arrested? If so who and why?", multilineTextFormat));
        formItems.add(new QuestionStep(kItemPoliceGivenIdentifier, "Any information given to you by police officer?", multilineTextFormat));

        form_step.setFormSteps(formItems);
        stepList.add(form_step);

        // step: witness
        QuestionStep witnessStep = new QuestionStep(kStepWitness, "Witnesses", multilineTextFormat);
        witnessStep.setText("List all witness");
        stepList.add(witnessStep);

        this.steps = stepList;
    }

    @Override
    public Step getStepAfterStep(Step step, TaskResult result) {
        if (step != null) {
            if (step.getIdentifier().equals(kStepOthersInvolved)) {
                StepResult res = result.getStepResult(kStepOthersInvolved);
                if (res != null && !((Boolean) res.getResult())) {
                    // go to kStepPassengersVehicle
                    Step next_step = super.getStepAfterStep(step, result);
                    while (next_step != null) {
                        if (next_step.getIdentifier().equals(kStepPassengersVehicle)) {
                            return next_step;
                        }
                        next_step = super.getStepAfterStep(next_step, result);
                    }
                }
            }
            else if (step.getIdentifier().equals(kStepPassengersVehicle)) {
                StepResult res = result.getStepResult(kStepPassengersVehicle);
                if (res != null && !((Boolean) res.getResult())) {
                    // go to kStepPassengersVehicle
                    Step next_step = super.getStepAfterStep(step, result);
                    while (next_step != null) {
                        if (next_step.getIdentifier().equals(kStepPoliceCalledWhy)) {
                            return next_step;
                        }
                        next_step = super.getStepAfterStep(next_step, result);
                    }
                }
            }
            else if (step.getIdentifier().equals(kStepPoliceComeScene)) {
                StepResult res = result.getStepResult(kStepPoliceComeScene);
                if (res != null && !((Boolean) res.getResult())) {
                    // go to kStepPassengersVehicle
                    Step next_step = super.getStepAfterStep(step, result);
                    while (next_step != null) {
                        if (next_step.getIdentifier().equals(kStepWitness)) {
                            return next_step;
                        }
                        next_step = super.getStepAfterStep(next_step, result);
                    }
                }
            }
            else if (step.getIdentifier().contains(kStepAllPicture)) {
                StepResult res = result.getStepResult(step.getIdentifier());
                if (res == null) {
                    // go to kStepPassengersVehicle
                    Step next_step = super.getStepAfterStep(step, result);
                    while (next_step != null) {
                        if (next_step.getIdentifier().equals(kStepAreYouCDL)) {
                            return next_step;
                        }
                        next_step = super.getStepAfterStep(next_step, result);
                    }
                }
            }
            else if (step.getIdentifier().contains(kStepAnyOtherPassenger)) {
                StepResult res = result.getStepResult(step.getIdentifier());
                if (res != null && !((Boolean) res.getResult())) {
                    // go to kStepPassengersVehicle
                    Step next_step = super.getStepAfterStep(step, result);
                    while (next_step != null) {
                        if (next_step.getIdentifier().equals(kStepPoliceCalledWhy)) {
                            return next_step;
                        }
                        next_step = super.getStepAfterStep(next_step, result);
                    }
                }
            }
        }
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

            }
        }
        else if (type == ViewChangeType.ActivityResume) {

        }
        else if (type == ViewChangeType.StepChanged) {

        }
    }
}
