package com.afollestad.aesthetic;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import static com.afollestad.aesthetic.Rx.distinctToMainThread;
import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

/** @author Aidan Follestad (afollestad) */
final class AestheticEditText extends AppCompatEditText {

  private CompositeSubscription subscriptions;
  private int backgroundResId;
  private int textColorResId;
  private int textColorHintResId;

  public AestheticEditText(Context context) {
    super(context);
  }

  public AestheticEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public AestheticEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    if (attrs != null) {
      int[] attrsArray =
          new int[] {
            android.R.attr.background, android.R.attr.textColor, android.R.attr.textColorHint
          };
      TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
      backgroundResId = ta.getResourceId(0, 0);
      textColorResId = ta.getResourceId(1, 0);
      textColorHintResId = ta.getResourceId(2, 0);
      ta.recycle();
    }
  }

  private void invalidateColors(ColorIsDarkState state) {
    TintHelper.setTintAuto(this, state.color(), true, state.isDark());
    TintHelper.setCursorTint(this, state.color());
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    subscriptions = new CompositeSubscription();
    subscriptions.add(
        Observable.combineLatest(
                ViewUtil.getObservableForResId(
                    getContext(), backgroundResId, Aesthetic.get().colorAccent()),
                Aesthetic.get().isDark(),
                ColorIsDarkState::create)
            .compose(distinctToMainThread())
            .subscribe(this::invalidateColors, onErrorLogAndRethrow()));
    subscriptions.add(
        ViewUtil.getObservableForResId(
                getContext(), textColorResId, Aesthetic.get().textColorPrimary())
            .compose(distinctToMainThread())
            .subscribe(this::setTextColor, onErrorLogAndRethrow()));
    subscriptions.add(
        ViewUtil.getObservableForResId(
                getContext(), textColorHintResId, Aesthetic.get().textColorSecondary())
            .compose(distinctToMainThread())
            .subscribe(this::setTextColor, onErrorLogAndRethrow()));
  }

  @Override
  protected void onDetachedFromWindow() {
    subscriptions.unsubscribe();
    super.onDetachedFromWindow();
  }
}
