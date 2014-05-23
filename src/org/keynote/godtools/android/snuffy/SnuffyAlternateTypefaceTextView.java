package org.keynote.godtools.android.snuffy;

import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Encapsulates an Android TextView with additional logic.
 *
 * It exposes one method to set an alternate typeface if a valid typeface is passed.
 *
 * Created by ryancarlson on 4/17/14.
 */
public class SnuffyAlternateTypefaceTextView
{
	TextView textView;

	public SnuffyAlternateTypefaceTextView(TextView textView)
	{
		this.textView = textView;
	}

	/**
	 * Will set the typeface on underlying TextView if the @param alternateTypeface is not null.
	 *
	 * @param alternateTypeface
	 * @return
	 */
	public SnuffyAlternateTypefaceTextView setAlternateTypeface(Typeface alternateTypeface)
	{
		if(alternateTypeface != null)
		{
			textView.setTypeface(alternateTypeface);
		}
		return this;
	}

	/**
	 * Get the underlying Android TextView
	 *
	 * @return
	 */
	public TextView get()
	{
		return textView;
	}
}
