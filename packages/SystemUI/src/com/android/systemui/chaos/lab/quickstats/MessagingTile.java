/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
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

package com.android.systemui.chaos.lab.quickstats;

import android.annotation.ChaosLab;
import android.annotation.ChaosLab.Classification;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class MessagingTile extends QuickStatsTile {

    public MessagingTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mDrawable = R.drawable.ic_qstats_messaging;
        mQSC.registerObservedContent(Telephony.Sms.Inbox.CONTENT_URI, this);
        mQSC.registerObservedContent(Telephony.Sms.Sent.CONTENT_URI, this);
        mQSC.registerObservedContent(Telephony.Mms.Inbox.CONTENT_URI, this);
        mQSC.registerObservedContent(Telephony.Mms.Sent.CONTENT_URI, this);
        updateResources();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateResources();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        int received = 0;
        int sent = 0;
        final ContentResolver resolver = mContext.getContentResolver();
        // get received sms count
        Cursor cursor = resolver.query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            received += cursor.getCount();
            cursor.close();
        }
        // get received mms count
        cursor = resolver.query(Telephony.Mms.Inbox.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            received += cursor.getCount();
            cursor.close();
        }

        // get sent sms count
        cursor = resolver.query(Telephony.Sms.Sent.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            sent += cursor.getCount();
            cursor.close();
        }
        // get sent mms count
        cursor = resolver.query(Telephony.Mms.Sent.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            sent += cursor.getCount();
            cursor.close();
        }
        mLabel = mContext.getString(R.string.quick_stats_messaging_label, sent, received);
    }
}
