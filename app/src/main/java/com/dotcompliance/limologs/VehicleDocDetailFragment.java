package com.dotcompliance.limologs;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;

/**
 * A fragment representing a single VehicleDoc detail screen.
 * This fragment is either contained in a {@link VehicleDocListActivity}
 * in two-pane mode (on tablets) or a {@link VehicleDocumentDetailActivity}
 * on handsets.
 */
public class VehicleDocDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_URL = "item_url";

    /**
     * The dummy content this fragment is presenting.
     */
    private String mUrl = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public VehicleDocDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.vehicledoc_detail, container, false);

        PDFView pdfView = (PDFView) rootView.findViewById(R.id.pdf_view);

        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/limologs", "temp.pdf");

        pdfView.fromFile(file)
                .scrollHandle(new DefaultScrollHandle(getContext()))
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        Log.d("PDF", nbPages + " pages");
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Log.d("PDF", "error");
                        t.printStackTrace();
                    }
                })
                .load();

        return rootView;
    }
}
