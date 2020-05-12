/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opaleye.snackvar.tools;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.lang.reflect.Field;

/**
 * Title : TooltipDelay
 * A class for implementing Tooltip
 * @author Young-gon Kim
 *2018.6
 */
public class TooltipDelay {

	/**
	 * Used for instant activation of a tooltip
	 * @param tooltip
	 */
    public static void activateTooltipInstantly(Tooltip tooltip) {
        hackTooltipTiming( tooltip, 0, "activationTimer" );
    }

	/**
	 * Used to prevent a tooltip from hiding
	 * @param tooltip
	 */
    public static void holdTooltip( Tooltip tooltip) {
        hackTooltipTiming( tooltip, Integer.MAX_VALUE, "hideTimer" );
    }

    private static void hackTooltipTiming( Tooltip tooltip, int delay, String property ) {
        try {
            Field fieldBehavior = tooltip.getClass().getDeclaredField( "BEHAVIOR" );
            fieldBehavior.setAccessible( true );
            Object objBehavior = fieldBehavior.get( tooltip );

            Field fieldTimer = objBehavior.getClass().getDeclaredField( property );
            fieldTimer.setAccessible( true );
            Timeline objTimer = (Timeline) fieldTimer.get( objBehavior );

            objTimer.getKeyFrames().clear();
            objTimer.getKeyFrames().add( new KeyFrame( new Duration( delay ) ) );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}