/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.server.wm;

class Task {
//    private final String TAG = "TaskGroup";
    TaskStack mStack;
    final AppTokenList mAppTokens = new AppTokenList();
    final int taskId;
    final int mUserId;

    Task(AppWindowToken wtoken, TaskStack stack, int userId) {
        taskId = wtoken.groupId;
        mAppTokens.add(wtoken);
        mStack = stack;
        mUserId = userId;
    }

    DisplayContent getDisplayContent() {
        return mStack.getDisplayContent();
    }

    void addAppToken(int addPos, AppWindowToken wtoken) {
        mAppTokens.add(addPos, wtoken);
    }

    boolean removeAppToken(AppWindowToken wtoken) {
        mAppTokens.remove(wtoken);
        if (mAppTokens.size() == 0) {
            mStack.removeTask(this);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{taskId=" + taskId + " appTokens=" + mAppTokens + "}";
    }
}
