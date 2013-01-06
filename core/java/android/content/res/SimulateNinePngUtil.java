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

package android.content.res;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.CRC32;

class SimulateNinePngUtil
{
    private static byte[] PNG_HEAD_FORMAT = { -119, 80, 78, 71, 13, 10, 26, 10 };

    private static int computePatchColor(byte[] srcImageData) {
        return 1;
    }

    private static int convertByteToIntByBigEndian(byte[] data, int start) {
        return 0 + ((0xFF & data[(start + 0)]) << 24) +
            ((0xFF & data[(start + 1)]) << 16) + 
                ((0xFF & data[(start + 2)]) << 8) + (0xFF & data[(start + 3)]);
    }

    private static void convertBytesFromIntByBigEndian(byte[] data, int start, int N) {
        data[(start + 0)] = ((byte)(0xFF & N >>> 24));
        data[(start + 1)] = ((byte)(0xFF & N >> 16));
        data[(start + 2)] = ((byte)(0xFF & N >> 8));
        data[(start + 3)] = ((byte)(N & 0xFF));
    }

    public static byte[] convertIntoNinePngData(byte srcData[]) {
        if(srcData == null || srcData.length < 41 || !isPngFormat(srcData))
            return null;

        if(!isNinePngFormat(srcData)) {
            byte ninePatchChunk[] = getNinePatchChunk(srcData);
            byte destData[] = new byte[12 + srcData.length + ninePatchChunk.length];
            for(int i = 0; i < 33; i++)
                destData[i] = srcData[i];

            convertBytesFromIntByBigEndian(destData, 33, ninePatchChunk.length);
            fillNinePngFormatMark(destData);
            for(int j = 0; j < ninePatchChunk.length; j++)
                destData[j + 41] = ninePatchChunk[j];

            int k = 41 + ninePatchChunk.length;
            CRC32 crc32 = new CRC32();
            crc32.update(destData, 37, 4 + ninePatchChunk.length);
            convertBytesFromIntByBigEndian(destData, k, (int)crc32.getValue());
            for(int l = 0; l < -33 + srcData.length; l++)
                destData[l + (k + 4)] = srcData[l + 33];

            srcData = destData;
        }

        return srcData;
    }

    public static InputStream convertIntoNinePngStream(InputStream pngInputStream) {
        NinePathInputStream retStream = null;
        try
        {
            byte[] nineHeader = new byte[41];
            int i = pngInputStream.read(nineHeader);
            if (i > 0)
            {
                byte[] arrayOfByte2 = convertIntoNinePngData(nineHeader);
                if (arrayOfByte2 != null) {
                    retStream = new NinePathInputStream(pngInputStream, arrayOfByte2);
                    if (i < nineHeader.length)
                    {
                        byte[] arrayOfByte1 = Arrays.copyOf(nineHeader, i);
                        nineHeader = arrayOfByte1;
                    }
                }
            }
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }
        return retStream;
    }

    private static void fillNinePngFormatMark(byte[] data)
    {
        data[37] = 110;
        data[38] = 112;
        data[39] = 84;
        data[40] = 99;
    }

    private static byte[] getNinePatchChunk(byte[] srcImageData) {
        int width = convertByteToIntByBigEndian(srcImageData, 16);
        int height = convertByteToIntByBigEndian(srcImageData, 20);
        byte[] chunk = new byte[52];
        chunk[0] = 1;
        chunk[1] = 2;
        chunk[2] = 2;
        chunk[3] = 1;
        convertBytesFromIntByBigEndian(chunk, 36, width);
        convertBytesFromIntByBigEndian(chunk, 44, height);
        convertBytesFromIntByBigEndian(chunk, 48, computePatchColor(srcImageData));
        return chunk;
    }

    private static boolean isNinePngFormat(byte[] data) {
        return ((data != null) && (data.length > 40) && (data[37] == 110) && 
                (data[38] == 112) && (data[39] == 84) && (data[40] == 99));
    }

    private static boolean isPngFormat(byte[] data) {
        if (data.length < PNG_HEAD_FORMAT.length)
            return false;
        for (int i = 0; i < PNG_HEAD_FORMAT.length; i++)
            if (data[i] != PNG_HEAD_FORMAT[i])
                return false;

        return true;
    }

    private static class NinePathInputStream extends InputStream {
        private int mCount = 0;
        private byte[] mExtraHeaderData;
        private InputStream mInputStream;

        public NinePathInputStream(InputStream is, byte[] header) {
            this.mInputStream = is;
            this.mExtraHeaderData = header;
            this.mCount = 0;
        }

        public void close() throws IOException {
            if (this.mInputStream != null)
                this.mInputStream.close();
        }

        public int read() throws IOException {
            int i;
            if(mCount < mExtraHeaderData.length) {
                byte abyte0[] = mExtraHeaderData;
                mCount += 1;
                i = abyte0[mCount];
            } else {
                i = mInputStream.read();
            }
            return i;
        }

        public int read(byte buffer[], int offset, int length) throws IOException {
            Arrays.checkOffsetAndCount(buffer.length, offset, length);
            int readCnt;
            for(readCnt = 0; mCount < mExtraHeaderData.length && readCnt < length; readCnt++) {
                int i1 = offset + readCnt;
                byte abyte1[] = mExtraHeaderData;
                int j1 = mCount;
                mCount = j1 + 1;
                buffer[i1] = abyte1[j1];
            }

            if(readCnt < length)
                readCnt += mInputStream.read(buffer, offset + readCnt, length - readCnt);
            return readCnt;
        }
    }
}

