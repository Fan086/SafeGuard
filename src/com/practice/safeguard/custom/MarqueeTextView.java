package com.practice.safeguard.custom;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

public class MarqueeTextView extends TextView {

	public MarqueeTextView(Context context) {
		this(context, null);
	}

	public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		//设置跑马灯效果
		setEllipsize(TruncateAt.MARQUEE);
		setSingleLine(true);
		setMarqueeRepeatLimit(2);
	}

	@Override
	public boolean isFocused() {
		return true;
	}
	public MarqueeTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
}
