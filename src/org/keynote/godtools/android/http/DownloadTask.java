package org.keynote.godtools.android.http;

import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.conn.ConnectTimeoutException;
import org.keynote.godtools.android.business.GTPackage;
import org.keynote.godtools.android.business.GTPackageReader;
import org.keynote.godtools.android.dao.DBAdapter;
import org.keynote.godtools.android.snuffy.Decompress;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class DownloadTask extends AsyncTask<Object, Void, Boolean>{

    private DownloadTaskHandler mTaskHandler;
    private Context mContext;
    private String url, filePath, tag;

    public static interface  DownloadTaskHandler {
        void downloadTaskComplete(String url, String filePath, String tag);
        void downloadTaskFailure(String url, String filePath, String tag);
    }

    public DownloadTask(Context context, DownloadTaskHandler taskHandler) {
        this.mTaskHandler = taskHandler;
        this.mContext = context;
    }

    @Override
    protected Boolean doInBackground(Object... params) {

        url = params[0].toString();
        filePath = params[1].toString();
        tag = params[2].toString();

        try {
            URL mURL = new URL(url);
            URLConnection c = mURL.openConnection();
            c.setConnectTimeout(10000);
            c.addRequestProperty("authorization", "a");
            c.addRequestProperty("interpreter", "1");

            InputStream is = c.getInputStream();
            DataInputStream dis = new DataInputStream(is);

            File zipfile = new File(filePath);
            String parentDir = zipfile.getParent();
            File unzipDir = new File(parentDir);
            unzipDir.mkdirs();

            byte[] buffer = new byte[2048];
            int length;

            FileOutputStream fout = new FileOutputStream(zipfile);
            BufferedOutputStream bufferOut = new BufferedOutputStream(fout, buffer.length);

            while ((length = dis.read(buffer, 0, buffer.length)) != -1)
                bufferOut.write(buffer, 0, length);

            bufferOut.flush();
            bufferOut.close();

            dis.close();
            fout.close();

            // unzip package.zip
            new Decompress().unzip(zipfile, unzipDir);

            // parse content.xml
            String content = unzipDir + "/contents.xml";
            File contentFile = new File(content);
            List<GTPackage> packageList = GTPackageReader.processContentFile(contentFile);

            // save the parsed packages to database
            DBAdapter adapter = DBAdapter.getInstance(mContext);
            adapter.open();
            for (GTPackage gtp : packageList) {
                adapter.updateGTPackage(gtp);
            }
            adapter.close();

            // delete package.zip and contents.xml
            zipfile.delete();
            contentFile.delete();

            // move files to main directory
            String mainDir = unzipDir.getParent();
            FileInputStream inputStream;
            FileOutputStream outputStream;

            File[] fileList = unzipDir.listFiles();
            File oldFile;
            for (int i = 0; i < fileList.length; i++){
                oldFile = fileList[i];
                inputStream = new FileInputStream(oldFile);
                outputStream = new FileOutputStream(mainDir + File.separator + oldFile.getName());
                copyFile(inputStream, outputStream);

                inputStream.close();
                inputStream = null;
                outputStream.flush();
                outputStream.close();
                outputStream = null;
                oldFile.delete();
            }

            // delete unzip directory
            unzipDir.delete();

            return true;

        } catch (ConnectTimeoutException e){
            e.printStackTrace();
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    protected void onPostExecute(Boolean isSuccessful) {

        if (isSuccessful)
            mTaskHandler.downloadTaskComplete(url, filePath, tag);
        else
            mTaskHandler.downloadTaskFailure(url, filePath, tag);

    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
