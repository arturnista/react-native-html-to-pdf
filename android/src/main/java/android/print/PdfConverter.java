/*
 * Created on 11/15/17.
 * Written by Islam Salah with assistance from members of Blink22.com
 */

package android.print;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import android.util.Base64;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

/**
 * Converts HTML to PDF.
 * <p>
 * Can convert only one task at a time, any requests to do more conversions before
 * ending the current task are ignored.
 */
public class PdfConverter implements Runnable {

    private static final String TAG = "PdfConverter";
    private static PdfConverter sInstance;

    private Context mContext;
    private String mHtmlString;
    private File mPdfFile;
    private PrintAttributes mPdfPrintAttrs;
    private boolean mIsCurrentlyConverting;
    private WebView mWebView;
    private boolean mShouldEncode;
    private WritableMap mResultMap;
    private Promise mPromise;
    private String mBaseURL;
    private PrintAttributes.MediaSize mMediaSize;

    private PdfConverter() {
    }

    public static synchronized PdfConverter getInstance() {
        if (sInstance == null)
            sInstance = new PdfConverter();

        return sInstance;
    }

    @Override
    public void run() {
        mWebView = new WebView(mContext);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                    throw new RuntimeException("call requires API level 19");
                else {
                    PrintDocumentAdapter documentAdapter = mWebView.createPrintDocumentAdapter();
                    documentAdapter.onLayout(null, getPdfPrintAttrs(), null, new PrintDocumentAdapter.LayoutResultCallback() {
                    }, null);
                    documentAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, getOutputFileDescriptor(), null, new PrintDocumentAdapter.WriteResultCallback() {
                        @Override
                        public void onWriteFinished(PageRange[] pages) {
                            try {
                                String base64 = "";
                                if (mShouldEncode) {
                                    base64 = encodeFromFile(mPdfFile);
                                }
                                mResultMap.putString("filePath", mPdfFile.getAbsolutePath());
                                mResultMap.putString("base64", base64);
                                mPromise.resolve(mResultMap);
                            } catch (IOException e) {
                                mPromise.reject(e.getMessage());
                            } finally {
                                destroy();
                            }
                        }
                    });
                }
            }
        });
        mWebView.loadDataWithBaseURL(mBaseURL, mHtmlString, "text/HTML", "UTF-8", null);
    }

    public PrintAttributes getPdfPrintAttrs() {
        return mPdfPrintAttrs != null ? mPdfPrintAttrs : getDefaultPrintAttrs();
    }

    public void setPdfPrintAttrs(PrintAttributes printAttrs) {
        this.mPdfPrintAttrs = printAttrs;
    }

    public void convert(Context context, String htmlString, File file, boolean shouldEncode, WritableMap resultMap,
            Promise promise, String baseURL, String mediaSize) {
        if (context == null)
            throw new IllegalArgumentException("context can't be null");
        if (htmlString == null)
            throw new IllegalArgumentException("htmlString can't be null");
        if (file == null)
            throw new IllegalArgumentException("file can't be null");

        if (mIsCurrentlyConverting)
            return;

        mContext = context;
        mHtmlString = htmlString;
        mPdfFile = file;
        mIsCurrentlyConverting = true;
        mShouldEncode = shouldEncode;
        mResultMap = resultMap;
        mPromise = promise;
        mBaseURL = baseURL;
        setMediaSize(mediaSize);
        runOnUiThread(this);
    }

    private ParcelFileDescriptor getOutputFileDescriptor() {
        try {
            mPdfFile.createNewFile();
            return ParcelFileDescriptor.open(mPdfFile, ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (Exception e) {
            Log.d(TAG, "Failed to open ParcelFileDescriptor", e);
        }
        return null;
    }

    private PrintAttributes getDefaultPrintAttrs() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return null;

        return new PrintAttributes.Builder()
                .setMediaSize(mMediaSize)
                .setResolution(new PrintAttributes.Resolution("RESOLUTION_ID", "RESOLUTION_ID", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

    }

    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(mContext.getMainLooper());
        handler.post(runnable);
    }

    private void destroy() {
        mContext = null;
        mHtmlString = null;
        mPdfFile = null;
        mPdfPrintAttrs = null;
        mIsCurrentlyConverting = false;
        mWebView = null;
        mShouldEncode = false;
        mResultMap = null;
        mPromise = null;
    }

    private String encodeFromFile(File file) throws IOException{
      RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
      byte[] fileBytes = new byte[(int)randomAccessFile.length()];
      randomAccessFile.readFully(fileBytes);
      return Base64.encodeToString(fileBytes, Base64.DEFAULT);
    }

    private void setMediaSize(String mediaSize) {
        if(mediaSize == null) {
            mMediaSize = PrintAttributes.MediaSize.NA_GOVT_LETTER;
        } else if(mediaSize.equals("ISO_A0")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A0;
        } else if(mediaSize.equals("ISO_A1")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A1;
        } else if(mediaSize.equals("ISO_A10")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A10;
        } else if(mediaSize.equals("ISO_A2")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A2;
        } else if(mediaSize.equals("ISO_A3")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A3;
        } else if(mediaSize.equals("ISO_A4")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A4;
        } else if(mediaSize.equals("ISO_A5")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A5;
        } else if(mediaSize.equals("ISO_A6")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A6;
        } else if(mediaSize.equals("ISO_A7")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A7;
        } else if(mediaSize.equals("ISO_A8")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A8;
        } else if(mediaSize.equals("ISO_A9")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_A9;
        } else if(mediaSize.equals("ISO_B0")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B0;
        } else if(mediaSize.equals("ISO_B1")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B1;
        } else if(mediaSize.equals("ISO_B10")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B10;
        } else if(mediaSize.equals("ISO_B2")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B2;
        } else if(mediaSize.equals("ISO_B3")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B3;
        } else if(mediaSize.equals("ISO_B4")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B4;
        } else if(mediaSize.equals("ISO_B5")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B5;
        } else if(mediaSize.equals("ISO_B6")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B6;
        } else if(mediaSize.equals("ISO_B7")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B7;
        } else if(mediaSize.equals("ISO_B8")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B8;
        } else if(mediaSize.equals("ISO_B9")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_B9;
        } else if(mediaSize.equals("ISO_C0")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C0;
        } else if(mediaSize.equals("ISO_C1")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C1;
        } else if(mediaSize.equals("ISO_C10")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C10;
        } else if(mediaSize.equals("ISO_C2")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C2;
        } else if(mediaSize.equals("ISO_C3")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C3;
        } else if(mediaSize.equals("ISO_C4")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C4;
        } else if(mediaSize.equals("ISO_C5")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C5;
        } else if(mediaSize.equals("ISO_C6")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C6;
        } else if(mediaSize.equals("ISO_C7")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C7;
        } else if(mediaSize.equals("ISO_C8")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C8;
        } else if(mediaSize.equals("ISO_C9")) {
            mMediaSize = PrintAttributes.MediaSize.ISO_C9;
        } else if(mediaSize.equals("JIS_B0")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B0;
        } else if(mediaSize.equals("JIS_B1")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B1;
        } else if(mediaSize.equals("JIS_B10")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B10;
        } else if(mediaSize.equals("JIS_B2")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B2;
        } else if(mediaSize.equals("JIS_B3")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B3;
        } else if(mediaSize.equals("JIS_B4")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B4;
        } else if(mediaSize.equals("JIS_B5")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B5;
        } else if(mediaSize.equals("JIS_B6")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B6;
        } else if(mediaSize.equals("JIS_B7")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B7;
        } else if(mediaSize.equals("JIS_B8")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B8;
        } else if(mediaSize.equals("JIS_B9")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_B9;
        } else if(mediaSize.equals("JIS_EXEC")) {
            mMediaSize = PrintAttributes.MediaSize.JIS_EXEC;
        } else if(mediaSize.equals("JPN_CHOU2")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_CHOU2;
        } else if(mediaSize.equals("JPN_CHOU3")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_CHOU3;
        } else if(mediaSize.equals("JPN_CHOU4")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_CHOU4;
        } else if(mediaSize.equals("JPN_HAGAKI")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_HAGAKI;
        } else if(mediaSize.equals("JPN_KAHU")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_KAHU;
        } else if(mediaSize.equals("JPN_KAKU2")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_KAKU2;
        } else if(mediaSize.equals("JPN_OUFUKU")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_OUFUKU;
        } else if(mediaSize.equals("JPN_YOU4")) {
            mMediaSize = PrintAttributes.MediaSize.JPN_YOU4;
        } else if(mediaSize.equals("NA_FOOLSCAP")) {
            mMediaSize = PrintAttributes.MediaSize.NA_FOOLSCAP;
        } else if(mediaSize.equals("NA_GOVT_LETTER")) {
            mMediaSize = PrintAttributes.MediaSize.NA_GOVT_LETTER;
        } else if(mediaSize.equals("NA_INDEX_3X5")) {
            mMediaSize = PrintAttributes.MediaSize.NA_INDEX_3X5;
        } else if(mediaSize.equals("NA_INDEX_4X6")) {
            mMediaSize = PrintAttributes.MediaSize.NA_INDEX_4X6;
        } else if(mediaSize.equals("NA_INDEX_5X8")) {
            mMediaSize = PrintAttributes.MediaSize.NA_INDEX_5X8;
        } else if(mediaSize.equals("NA_JUNIOR_LEGAL")) {
            mMediaSize = PrintAttributes.MediaSize.NA_JUNIOR_LEGAL;
        } else if(mediaSize.equals("NA_LEDGER")) {
            mMediaSize = PrintAttributes.MediaSize.NA_LEDGER;
        } else if(mediaSize.equals("NA_LEGAL")) {
            mMediaSize = PrintAttributes.MediaSize.NA_LEGAL;
        } else if(mediaSize.equals("NA_LETTER")) {
            mMediaSize = PrintAttributes.MediaSize.NA_LETTER;
        } else if(mediaSize.equals("NA_MONARCH")) {
            mMediaSize = PrintAttributes.MediaSize.NA_MONARCH;
        } else if(mediaSize.equals("NA_QUARTO")) {
            mMediaSize = PrintAttributes.MediaSize.NA_QUARTO;
        } else if(mediaSize.equals("NA_TABLOID")) {
            mMediaSize = PrintAttributes.MediaSize.NA_TABLOID;
        } else if(mediaSize.equals("OM_DAI_PA_KAI")) {
            mMediaSize = PrintAttributes.MediaSize.OM_DAI_PA_KAI;
        } else if(mediaSize.equals("OM_JUURO_KU_KAI")) {
            mMediaSize = PrintAttributes.MediaSize.OM_JUURO_KU_KAI;
        } else if(mediaSize.equals("OM_PA_KAI")) {
            mMediaSize = PrintAttributes.MediaSize.OM_PA_KAI;
        } else if(mediaSize.equals("PRC_1")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_1;
        } else if(mediaSize.equals("PRC_10")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_10;
        } else if(mediaSize.equals("PRC_16K")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_16K;
        } else if(mediaSize.equals("PRC_2")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_2;
        } else if(mediaSize.equals("PRC_3")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_3;
        } else if(mediaSize.equals("PRC_4")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_4;
        } else if(mediaSize.equals("PRC_5")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_5;
        } else if(mediaSize.equals("PRC_6")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_6;
        } else if(mediaSize.equals("PRC_7")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_7;
        } else if(mediaSize.equals("PRC_8")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_8;
        } else if(mediaSize.equals("PRC_9")) {
            mMediaSize = PrintAttributes.MediaSize.PRC_9;
        } else if(mediaSize.equals("ROC_16K")) {
            mMediaSize = PrintAttributes.MediaSize.ROC_16K;
        } else if(mediaSize.equals("ROC_8K")) {
            mMediaSize = PrintAttributes.MediaSize.ROC_8K;
        } else {
            mMediaSize = PrintAttributes.MediaSize.NA_GOVT_LETTER;
        }
    }
}
