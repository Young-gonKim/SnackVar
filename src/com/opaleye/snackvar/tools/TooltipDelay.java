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