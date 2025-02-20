/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.incallui.incall.protocol;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import com.android.dialer.common.LogUtil;
import com.android.dialer.multimedia.MultimediaData;
import com.google.auto.value.AutoValue;
import java.util.Locale;

/** Information about the primary call. */
@AutoValue
public abstract class PrimaryInfo {
  @Nullable
  public abstract String number();

  @Nullable
  public abstract String name();

  public abstract boolean nameIsNumber();
  // This is from contacts and shows the type of number. For example, "Mobile".
  @Nullable
  public abstract String label();

  @Nullable
  public abstract String location();

  @Nullable
  public abstract Drawable photo();

  @ContactPhotoType
  public abstract int photoType();

  public abstract boolean isSipCall();

  public abstract boolean isContactPhotoShown();

  public abstract boolean isWorkCall();

  public abstract boolean isSpam();

  public abstract boolean isLocalContact();

  //UNISOC: add for bug912215
  public abstract boolean isVoiceMailNumber();

  //UNISOC: add for bug954922
  public abstract boolean isConference();

  public abstract boolean answeringDisconnectsOngoingCall();

  public abstract boolean shouldShowLocation();
  // Used for consistent LetterTile coloring.
  @Nullable
  public abstract String contactInfoLookupKey();

  @Nullable
  public abstract MultimediaData multimediaData();

  public abstract boolean showInCallButtonGrid();

  public abstract int numberPresentation();

  // SPRD Feature Porting: Show fdn list name in incallui feature.
  public abstract int subId();

  // SPRD Feature Porting:mt conference call support
  @Nullable
  public abstract String contactName();

  public static Builder builder() {
    return new AutoValue_PrimaryInfo.Builder();
  }
  /** Builder class for primary call info. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setNumber(String number);

    public abstract Builder setName(String name);

    public abstract Builder setNameIsNumber(boolean nameIsNumber);

    public abstract Builder setLabel(String label);

    public abstract Builder setLocation(String location);

    public abstract Builder setPhoto(Drawable photo);

    public abstract Builder setPhotoType(@ContactPhotoType int photoType);

    public abstract Builder setIsSipCall(boolean isSipCall);

    public abstract Builder setIsContactPhotoShown(boolean isContactPhotoShown);

    public abstract Builder setIsWorkCall(boolean isWorkCall);

    public abstract Builder setIsSpam(boolean isSpam);

    public abstract Builder setIsLocalContact(boolean isLocalContact);

    //UNISOC: add for bug912215
    public abstract Builder setIsVoiceMailNumber(boolean isVoiceMailNumber);

    //UNISOC: add for bug954922
    public abstract Builder setIsConference(boolean isConference);

    public abstract Builder setAnsweringDisconnectsOngoingCall(
        boolean answeringDisconnectsOngoingCall);

    public abstract Builder setShouldShowLocation(boolean shouldShowLocation);

    public abstract Builder setContactInfoLookupKey(String contactInfoLookupKey);

    public abstract Builder setMultimediaData(MultimediaData multimediaData);

    public abstract Builder setShowInCallButtonGrid(boolean showInCallButtonGrid);

    public abstract Builder setNumberPresentation(int numberPresentation);

    public abstract Builder setSubId(int subId);

    public abstract Builder setContactName(String contactName);

    public abstract PrimaryInfo build();
  }

  public static PrimaryInfo empty() {
    return PrimaryInfo.builder()
        .setNameIsNumber(false)
        .setPhotoType(ContactPhotoType.DEFAULT_PLACEHOLDER)
        .setIsSipCall(false)
        .setIsContactPhotoShown(false)
        .setIsWorkCall(false)
        .setIsSpam(false)
        .setIsLocalContact(false)
         //UNISOC: add for bug912215
        .setIsVoiceMailNumber(false)
         //UNISOC: add for bug954922
        .setIsConference(false)
        .setAnsweringDisconnectsOngoingCall(false)
        .setShouldShowLocation(false)
        .setShowInCallButtonGrid(true)
        .setNumberPresentation(-1)
        .setSubId(-1)
        .build();
  }

  @Override
  public String toString() {
    return String.format(
        Locale.US,
        "PrimaryInfo, number: %s, name: %s, location: %s, label: %s, "
            + "photo: %s, photoType: %d, isPhotoVisible: %b, MultimediaData: %s",
        LogUtil.sanitizePhoneNumber(number()),
        LogUtil.sanitizePii(name()),
        LogUtil.sanitizePii(location()),
        label(),
        photo(),
        photoType(),
        isContactPhotoShown(),
        multimediaData());
  }
}
