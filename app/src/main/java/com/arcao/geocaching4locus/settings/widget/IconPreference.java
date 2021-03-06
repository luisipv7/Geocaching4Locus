package com.arcao.geocaching4locus.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.arcao.geocaching4locus.R;

public class IconPreference extends Preference {
    private Drawable iconDrawable;

    public IconPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setLayoutResource(R.layout.preference_icon);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.IconPreference, defStyle, 0);
        iconDrawable = a.getDrawable(R.styleable.IconPreference_icon);
        a.recycle();
    }

    @Override
    public void onBindView(@NonNull View view) {
        super.onBindView(view);

        ImageView imageView = view.findViewById(R.id.icon);
        if (imageView != null && iconDrawable != null) {
            imageView.setImageDrawable(iconDrawable);
        }
    }

    @Override
    public void setIcon(Drawable icon) {
        if ((icon == null && iconDrawable != null) || (icon != null && !icon.equals(iconDrawable))) {
            iconDrawable = icon;
            notifyChanged();
        }
    }

    @Override
    public Drawable getIcon() {
        return iconDrawable;
    }
}