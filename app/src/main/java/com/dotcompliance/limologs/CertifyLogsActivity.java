package com.dotcompliance.limologs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dotcompliance.limologs.adapter.CertifyLogItemRecyleAdapter;
import com.dotcompliance.limologs.data.CertifylogsModel;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import static com.dotcompliance.limologs.adapter.CertifyLogItemRecyleAdapter.strselected;


public class CertifyLogsActivity extends LimoBaseActivity implements View.OnClickListener {

    private RecyclerView lvCertifyLog;
    private ArrayList<CertifylogsModel> certify_List = new ArrayList<CertifylogsModel>();
    private LinearLayout ll_CertifyLayout;
    private View footerView;
    private Button btn_agree, btn_notready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.certify_logs);

        findViewById(R.id.btn_certifythem).setOnClickListener(this);
        findViewById(R.id.btn_skipfornow).setOnClickListener(this);
        lvCertifyLog = (RecyclerView) findViewById(R.id.lv_CertifyLog);
        ll_CertifyLayout = (LinearLayout) findViewById(R.id.ll_CertifyLayout);
        footerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.certify_header, null, false);
       // lvCertifyLog.addFooterView(footerView);
        btn_agree = (Button) footerView.findViewById(R.id.btn_agree);
        btn_notready = (Button) footerView.findViewById(R.id.btn_notready);
        btn_agree.setOnClickListener(this);
        btn_notready.setOnClickListener(this);

        //  onMenuItemRight();


    }

    @Override
    protected void onMenuItemRight() {
        super.onMenuItemRight();
        //  Toast.makeText(mContext, strselected, Toast.LENGTH_SHORT).show();

        if (strselected.equals("")) {
            Toast.makeText(mContext, "Select Atleast One Log", Toast.LENGTH_SHORT).show();
        } else {
            saveCertification();
        }



    }


    @Override
    protected void onMenuItemLeft() {
        super.onMenuItemLeft();
        if (Preferences.isCertify) {
            startActivity(new Intent(CertifyLogsActivity.this, WelcomeActivity.class));
            CertifyLogsActivity.this.finish();
        } else {
            Preferences.clearSession(mContext);
            Preferences.mDriverLogs.clear();
            Preferences.mVehicleList.clear();
            Preferences.isConnected = false;
            stopTimer();
            stopService(new Intent(mContext, LocationUpdateService.class));
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_certifythem:

                lvCertifyLog.setVisibility(View.VISIBLE);
                ll_CertifyLayout.setVisibility(View.GONE);
                getCertification();
                //TODO implement
                break;
            case R.id.btn_skipfornow:
                //TODO implement
                if (Preferences.isCertify) {
                    startActivity(new Intent(CertifyLogsActivity.this, WelcomeActivity.class));
                    CertifyLogsActivity.this.finish();
                } else {
                    Preferences.clearSession(mContext);
                    Preferences.mDriverLogs.clear();
                    Preferences.mVehicleList.clear();
                    Preferences.isConnected = false;
                    stopTimer();
                    stopService(new Intent(mContext, LocationUpdateService.class));
                    startActivity(new Intent(mContext, LoginActivity.class));
                    finish();
                }

                break;

            case R.id.btn_agree:

                if (strselected.equals("")) {
                    Toast.makeText(mContext, "Select Atleast One Log", Toast.LENGTH_SHORT).show();
                } else {
                    saveCertification();
                }
                break;


            case R.id.btn_notready:
                if (Preferences.isCertify) {
                    startActivity(new Intent(CertifyLogsActivity.this, WelcomeActivity.class));
                    CertifyLogsActivity.this.finish();
                } else {
                    Preferences.clearSession(mContext);
                    Preferences.mDriverLogs.clear();
                    Preferences.mVehicleList.clear();
                    Preferences.isConnected = false;
                    stopTimer();
                    stopService(new Intent(mContext, LocationUpdateService.class));
                    startActivity(new Intent(mContext, LoginActivity.class));
                    finish();
                }
                break;

        }
    }

    public void getCertification() {

        DataManager.getInstance().showProgressMessage(CertifyLogsActivity.this);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getCertificatioLog("/log/get_certification"), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (response != null) {
                    try {
                        int error = response.optInt("error");
                        if (error == 0) {
                            JSONArray response_Array = response.optJSONArray("certificate_logs");
                            for (int i = 0; i < response_Array.length(); i++) {
                                JSONObject responseObj = response_Array.optJSONObject(i);
                                CertifylogsModel certifylogsModel = new CertifylogsModel();
                                certifylogsModel.setDriverlog_id(responseObj.optString("driverlog_id"));
                                certifylogsModel.setLog_date(responseObj.optString("log_date"));
                                certify_List.add(certifylogsModel);
                            }



                            CertifyLogItemRecyleAdapter adapter = new CertifyLogItemRecyleAdapter(certify_List);
                            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                            lvCertifyLog.setLayoutManager(mLayoutManager);
                            lvCertifyLog.setItemAnimator(new DefaultItemAnimator());
                            lvCertifyLog.setAdapter(adapter);
                            DataManager.getInstance().hideProgressMessage();
                            setRightMenuItem("Agree");
                            setLeftMenuItem("Not Ready");
                            setLeftMenuItemFont(13);
                            setRightMenuItemFont(13);

                            btn_agree.setVisibility(View.GONE);
                            btn_notready.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                DataManager.getInstance().hideProgressMessage();


            }
        });


    }


    public void saveCertification() {

        DataManager.getInstance().showProgressMessage(CertifyLogsActivity.this);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.saveCertification("/log/save_certification", strselected), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (response != null) {
                    //  Toast.makeText(CertifyLogsActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                    try {
                        int error = response.optInt("error");
                        String message = response.optString("message");
                        if (message.equalsIgnoreCase("success")) {
                            DataManager.getInstance().hideProgressMessage();

                            if (Preferences.isCertify) {
                                startActivity(new Intent(CertifyLogsActivity.this, WelcomeActivity.class));
                                CertifyLogsActivity.this.finish();
                            } else {
                                Preferences.clearSession(mContext);
                                Preferences.mDriverLogs.clear();
                                Preferences.mVehicleList.clear();
                                Preferences.isConnected = false;
                                stopTimer();
                                stopService(new Intent(mContext, LocationUpdateService.class));
                                startActivity(new Intent(mContext, LoginActivity.class));
                                finish();
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                DataManager.getInstance().hideProgressMessage();


            }
        });


    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        findViewById(R.id.btn_skipfornow).performClick();
    }
}
