package com.dotcompliance.limologs.survey;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;
import org.researchstack.backbone.answerformat.AnswerFormat;
import org.researchstack.backbone.result.StepResult;
import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.ui.ViewTaskActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

public class AccidentSurveyController extends Object {
    public Context mContext;

    // define identifiers
    public static final String SURVEY_ID = "accident_survey";


    public AccidentSurveyController(Context c) {
        mContext = c;
    }

    public Intent createSurveyIntent() {
        // create a task
        AccidentSurveyTask task = new AccidentSurveyTask(SURVEY_ID);


        // Create an activity using the task and set a delegate.
        Intent intent = ViewTaskActivity.newIntent(mContext, task);
        return intent;
    }

    public static void processSurveyResult(TaskResult taskResult, final RestTask.TaskCallbackInterface callbackInterface) {
        RequestParams params = new RequestParams();
        params.put("vehicle_id", Preferences.getCurrentVehicle().vehicle_id);
        params.put("location", Preferences.mCurrentLocation);

        Map<String, StepResult> results = taskResult.getResults();

        for (Map.Entry<String, StepResult> entry: results.entrySet()) {
            StepResult result = entry.getValue();
            if (result == null) continue;

            switch (entry.getKey()) {
                case AccidentSurveyTask.kStepOtherDriverLicensePicture:
                case AccidentSurveyTask.kStepOtherLicensePlatePicture:
                case AccidentSurveyTask.kStepOtherInsuranceCard: {
                    File file = new File((String) result.getResult());
                    try {
                        params.put(entry.getKey(), file);
                    } catch (FileNotFoundException e) {

                    }
                    break;
                }

                case AccidentSurveyTask.kStepBasicInfo:
                case AccidentSurveyTask.kStepOtherVehicleForm:
                case AccidentSurveyTask.kStepPoliceCalledWhy:
                case AccidentSurveyTask.kStepPoliceForm: {
                    Map<String, StepResult> formResults = result.getResults();
                    for (Map.Entry<String, StepResult> ent2 : formResults.entrySet()) {
                        params.put(ent2.getKey(), getAnswerStringFromQuestionResult(ent2.getValue()));
                    }
                    break;
                }
                default:
                    if (entry.getKey().contains(AccidentSurveyTask.kStepPassengerForm)) {
                        Map<String, StepResult> formResults = result.getResults();
                        for (Map.Entry<String, StepResult> ent2 : formResults.entrySet()) {
                            params.put(ent2.getKey(), getAnswerStringFromQuestionResult(ent2.getValue()));
                        }
                    }
                    else if (entry.getKey().contains(AccidentSurveyTask.kStepAllPicture)) {
                        File file = new File((String) result.getResult());
                        try {
                            params.put(entry.getKey(), file);
                        }
                        catch (FileNotFoundException e) {

                        }
                    }
                    else {
                        params.put(entry.getKey(), getAnswerStringFromQuestionResult(result));
                    }
                    break;
            }
        }

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/limo/accident_report"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        callbackInterface.onTaskCompleted(true, "Successfully submitted!");
                    }
                    else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("check_update", "unexpected response");
                    if (callbackInterface != callbackInterface)
                        callbackInterface.onTaskCompleted(false, "Unexpected Response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.e("Network error", " " +  throwable.getMessage());
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Timeout");
                } else {
                    try {
                        Log.d("network error", errorResponse.toString(4));
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    private static String getAnswerStringFromQuestionResult(StepResult result) {
        if (result == null || result.getResult() == null) {
            return "";
        }

        if (result.getAnswerFormat().getQuestionType() == AnswerFormat.Type.Boolean) {
            return (Boolean) result.getResult() ? "Yes" : "No";
        }
        else if (result.getAnswerFormat().getQuestionType() == AnswerFormat.Type.Text) {
            return (String) result.getResult();
        }
        else if (result.getAnswerFormat().getQuestionType() == AnswerFormat.Type.Integer) {
            return result.getResult().toString();
        }

        return "";
    }
}
