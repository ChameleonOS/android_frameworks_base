/*
 * Copyright (C) 2012 The ChameleonOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cos.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class InputStreamLoader
{
    ByteArrayInputStream mByteArrayInputStream;
    private Context mContext;
    private InputStream mInputStream;
    private String mPath;
    private Uri mUri;
    private ZipFile mZipFile;
    private String mZipPath;

    public InputStreamLoader(Context context, Uri uri)
    {
        if("file".equals(uri.getScheme()))
        {
            mPath = uri.getPath();
        } else
        {
            mContext = context;
            mUri = uri;
        }
    }

    public InputStreamLoader(String path)
    {
        mPath = path;
    }

    public InputStreamLoader(String zipPath, String entry)
    {
        mZipPath = zipPath;
        mPath = entry;
    }

    public InputStreamLoader(byte[] data)
    {
        mByteArrayInputStream = new ByteArrayInputStream(data);
    }

    public void close()
    {
        try
        {
            if (this.mInputStream != null)
                this.mInputStream.close();
            if (this.mZipFile != null)
                this.mZipFile.close();
        }
        catch (IOException localIOException)
        {
        }
        return;
    }

    public InputStream get() throws FileNotFoundException
    {
        close();
        if(mUri != null) {
            mInputStream = mContext.getContentResolver().openInputStream(mUri);
            if(mInputStream != null && !(mInputStream instanceof ByteArrayInputStream))
                mInputStream = new BufferedInputStream(mInputStream, 16384);
        } else {
            try {
                if(mZipPath != null)
                {
                    mZipFile = new ZipFile(mZipPath);
                    mInputStream = mZipFile.getInputStream(mZipFile.getEntry(mPath));
                } else
                if(mPath != null)
                    mInputStream = new FileInputStream(mPath);
                else
                if(mByteArrayInputStream != null)
                {
                    mByteArrayInputStream.reset();
                    mInputStream = mByteArrayInputStream;
                }
            } catch(Exception exception) { }
        }

        return mInputStream;
    }
}
