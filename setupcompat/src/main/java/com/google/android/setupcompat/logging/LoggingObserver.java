/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.android.setupcompat.logging;

import android.view.View;

/**
 * An abstract class which can be attached to a Setupcompat layout and provides methods for logging
 * impressions and interactions of its views and buttons.
 */
public interface LoggingObserver {
  void log(SetupCompatUiEvent event);

  abstract class SetupCompatUiEvent {
    private SetupCompatUiEvent() {}

    public static final class LayoutInflatedEvent extends SetupCompatUiEvent {
      private final View view;

      public LayoutInflatedEvent(View view) {
        this.view = view;
      }

      public View getView() {
        return view;
      }
    }

    public static final class LayoutShownEvent extends SetupCompatUiEvent {
      private final View view;

      public LayoutShownEvent(View view) {
        this.view = view;
      }

      public View getView() {
        return view;
      }
    }

    public static final class ButtonInflatedEvent extends SetupCompatUiEvent {
      private final View view;
      private final ButtonType buttonType;

      public ButtonInflatedEvent(View view, ButtonType buttonType) {
        this.view = view;
        this.buttonType = buttonType;
      }

      public View getView() {
        return view;
      }

      public ButtonType getButtonType() {
        return buttonType;
      }
    }

    public static final class ButtonShownEvent extends SetupCompatUiEvent {
      private final View view;
      private final ButtonType buttonType;

      public ButtonShownEvent(View view, ButtonType buttonType) {
        this.view = view;
        this.buttonType = buttonType;
      }

      public View getView() {
        return view;
      }

      public ButtonType getButtonType() {
        return buttonType;
      }
    }

    public static final class ButtonInteractionEvent extends SetupCompatUiEvent {
      private final View view;
      private final InteractionType interactionType;

      public ButtonInteractionEvent(View view, InteractionType interactionType) {
        this.view = view;
        this.interactionType = interactionType;
      }

      public View getView() {
        return view;
      }

      public InteractionType getInteractionType() {
        return interactionType;
      }
    }
  }

  enum ButtonType {
    UNKNOWN,
    PRIMARY,
    SECONDARY
  }

  enum InteractionType {
    UNKNOWN,
    TAP,
    LONG_PRESS,
    DOUBLE_TAP
  }
}