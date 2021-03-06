/*
 * Copyright (C) 2014 Vlad Mihalachi
 *
 * This file is part of Turbo Editor.
 *
 * Turbo Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sharedcode.turboeditor.task;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.widget.Toast;

import com.spazedog.lib.rootfw4.RootFW;
import com.spazedog.lib.rootfw4.utils.File;

import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.activity.MainActivity;
import sharedcode.turboeditor.util.Device;
import sharedcode.turboeditor.util.GreatUri;

public class SaveFileTask extends AsyncTask<Void, Void, Void> {

    private final MainActivity activity;
    private final GreatUri uri;
    private final String newContent;
    private final String encoding;
    private String message;
    private String positiveMessage;
    private SaveFileInterface mCompletionHandler;

    public SaveFileTask(MainActivity activity, GreatUri uri, String newContent, String encoding, SaveFileInterface mCompletionHandler) {
        this.activity = activity;
        this.uri = uri;
        this.newContent = newContent;
        this.encoding = encoding;
        this.mCompletionHandler = mCompletionHandler;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        positiveMessage = String.format(activity.getString(R.string.file_saved_with_success), uri.getFileName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void doInBackground(final Void... voids) {

        try {
            String filePath = uri.getFilePath();
            // if the uri has no path
            if (TextUtils.isEmpty(filePath)) {
               writeUri(uri.getUri(), newContent, encoding);
            } else {
                if (uri.isRootRequired()) {
                    if (RootFW.connect()) {
                        File file = RootFW.getFile(filePath);
                        file.write(newContent);
                    }
                }
                // if we can read the file associated with the uri
                else {
                    if (Device.hasKitKatApi())
                        writeUri(uri.getUri(), newContent, encoding);
                    else {
                        FileUtils.write(new java.io.File(filePath),
                                newContent,
                                encoding);
                    }
                }
            }

            message = positiveMessage;
        } catch (Exception e) {
            message = e.getMessage();
        }
        return null;
    }

    private void writeUri(Uri uri, String newContent, String encoding) throws IOException {
        ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "w");
        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
        fileOutputStream.write(newContent.getBytes(Charset.forName(encoding)));
        fileOutputStream.close();
        pfd.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(final Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

        if (mCompletionHandler != null)
            mCompletionHandler.fileSaved(message.equals(positiveMessage));
    }

    public interface SaveFileInterface {
        public void fileSaved(Boolean success);
    }
}