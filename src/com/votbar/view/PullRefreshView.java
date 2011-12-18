package com.votbar.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * 
 */

/**
 * @author sanping.li@alipay.com
 * 下拉刷新View
 *
 */
public class PullRefreshView extends FrameLayout implements OnGestureListener,
		AnimationListener {
	private static final int MAX_MARGIN = 200;
	
	private static final byte STATE_CLOSE = 0;
	private static final byte STATE_OPEN = STATE_CLOSE+1;
	private static final byte STATE_OVER = STATE_OPEN+1;
	private static final byte STATE_OPEN_RELEASE = STATE_OVER+1;
	private static final byte STATE_OVER_RELEASE = STATE_OPEN_RELEASE+1;
	private static final byte STATE_REFRESH = STATE_OVER_RELEASE+1;
	private static final byte STATE_REFRESH_RELEASE = STATE_REFRESH+1;
	private byte mState;
	
	private GestureDetector mGestureDetector;
	private Flinger mFlinger;

	private Animation mReverseFlipAnimation;
	private Animation mFlipAnimation;
	private boolean mIndicatorUp;
	
	private OnRefreshListener mRefreshListener;
	
	private View mRefreshView;
	private View mLoadingView;
	
	private int mLastY;

	public PullRefreshView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mGestureDetector = new GestureDetector(this);
		mFlinger = new Flinger();
		
		init();
		loadVariables();
	}

	private void loadVariables() {
        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setAnimationListener(this);
        mReverseFlipAnimation = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setAnimationListener(this);
        mIndicatorUp = false;
	}

	private void init() {
		View mOverView = inflate(getContext(), R.layout.pull_refresh_header, null);
		addView(mOverView);
		
		mRefreshView = mOverView.findViewById(R.id.refresh);
		mLoadingView = mOverView.findViewById(R.id.loading);
	}

	@Override
	public void onAnimationEnd(Animation anim) {
		ImageView indicator = (ImageView) getChildAt(0).findViewById(R.id.refresh_indicator);
		if(anim==mFlipAnimation){
			indicator.setImageResource(R.drawable.pull_arrow_up);
		}else{
			indicator.setImageResource(R.drawable.pull_arrow_down);
		}
	}
	
	@Override
	public void onAnimationRepeat(Animation arg0) {

	}

	@Override
	public void onAnimationStart(Animation arg0) {

	}

	@Override
	public boolean onDown(MotionEvent evn) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float disX,
			float disY) {
		View child = getChildAt(1);
		if(child instanceof AdapterView<?>){
			if(((AdapterView<?>)child).getFirstVisiblePosition()!=0||
					(((AdapterView<?>)child).getFirstVisiblePosition()==0&&((AdapterView<?>)child).getChildAt(0).getTop()<0))
				return false;
		}
		if(child.getTop()<=0&&disY>0){
			return false;
		}
		if(mState==STATE_OPEN_RELEASE||mState==STATE_OVER_RELEASE||mState==STATE_REFRESH_RELEASE||mState==STATE_REFRESH)
			return false;
		boolean bool = moveDown(mLastY, true);
		mLastY = (int) -disY;
		return bool;
	}

	private void release(int dis) {
		View head = getChildAt(0);
		if(mRefreshListener!=null&&dis>head.getMeasuredHeight()){
			mState = STATE_OVER_RELEASE;
			mFlinger.recover(dis-head.getMeasuredHeight());
		}else{
			mState = STATE_OPEN_RELEASE;
			mFlinger.recover(dis);
		}
	}

	@Override
	public void onShowPress(MotionEvent arg0) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		return false;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(!mFlinger.isFinished())
			return false;
		View head = getChildAt(0);
		if(ev.getAction()==MotionEvent.ACTION_UP){
			if(head.getBottom()>0){
				if(mState==STATE_REFRESH&&head.getBottom()>head.getMeasuredHeight()){
					release(head.getBottom()-head.getMeasuredHeight());
				}else if(mState!=STATE_REFRESH){
					release(head.getBottom());
				}
				return false;
			}
		}
		boolean bool = mGestureDetector.onTouchEvent(ev);
		
		if((bool||(mState!=STATE_CLOSE&&mState!=STATE_REFRESH))&&head.getBottom()!=0){
			ev.setAction(MotionEvent.ACTION_CANCEL);
			return super.dispatchTouchEvent(ev);
		}

		if(bool)
			return true;
		else
			return super.dispatchTouchEvent(ev);
	}

	/**
	 * 自动滚动
	 *
	 */
	private class Flinger implements Runnable{
		private Scroller mScroller;
		private int mLastY;
		private boolean mIsFinished;
		
		public Flinger() {
			mScroller = new Scroller(getContext());
			mIsFinished = true;
		}

		@Override
		public void run() {
			boolean b = mScroller.computeScrollOffset();
			if(b){
				moveDown(mLastY-mScroller.getCurrY(),false);
				mLastY = mScroller.getCurrY();
				post(this);
			}else{
				mIsFinished = true;
				removeCallbacks(this);
			}
		}
		
		public void recover(int dis){
			if(dis<=0)return;
			removeCallbacks(this);
			mLastY = 0;
			mIsFinished = false;
			mScroller.startScroll(0, 0, 0, dis,300);
			post(this);
		}
		
		public boolean isFinished(){
			return mIsFinished;
		}
	}
	

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if(changed){
			View head = getChildAt(0);
			View child = getChildAt(1);
			if(mState==STATE_REFRESH){
				head.layout(left, top, right, top+head.getMeasuredHeight());
				child.layout(left, top+head.getMeasuredHeight(), right, bottom); 
			}else{
				head.layout(left, top-head.getMeasuredHeight(), right, top);
				child.layout(left, top, right, bottom); 
			}
		}
//		super.onLayout(changed, left, top, right, bottom);
	}

	public boolean moveDown(int dis,boolean changeState) {
		View head = getChildAt(0);
		View child = getChildAt(1);
		int childTop = child.getTop()+dis;
		if(childTop<=0){
			mState = STATE_CLOSE;
			head.offsetTopAndBottom(dis);
			child.offsetTopAndBottom(dis);
		}else if(childTop<=head.getMeasuredHeight()){
			if(mIndicatorUp){
				playAnim(false);
			}
			head.offsetTopAndBottom(dis);
			child.offsetTopAndBottom(dis);
			if(changeState)
				mState = STATE_OPEN;
			else if(dis==0&&mState==STATE_OVER_RELEASE){
				refresh();
			}
		}else{
			if(!mIndicatorUp){
				playAnim(true);
			}
			if(changeState)
				mState = STATE_OVER;
			if(childTop>=MAX_MARGIN){
				release(head.getBottom());
				return false;
			}
			head.offsetTopAndBottom(dis);
			child.offsetTopAndBottom(dis);
		}
		invalidate();
		return true;
	}

	/**
	 * 刷新数据
	 */
	private void refresh() {
		if(mRefreshListener!=null){
			mState = STATE_REFRESH;
			mRefreshView.setVisibility(INVISIBLE);
			mLoadingView.setVisibility(VISIBLE);
			mRefreshListener.onRefresh();
		}
	}

	/**
	 * 刷新结束
	 */
	public void refreshFinishd() {
		View head = getChildAt(0);
		mState = STATE_REFRESH_RELEASE;
		mRefreshView.setVisibility(VISIBLE);
		mLoadingView.setVisibility(INVISIBLE);
		release(head.getBottom());
	}

	private void playAnim(boolean bool) {
		ImageView indicator = (ImageView) getChildAt(0).findViewById(R.id.refresh_indicator);
		TextView textView = (TextView) getChildAt(0).findViewById(R.id.refresh_text);
		indicator.clearAnimation();
		if(bool){
			mIndicatorUp = true;
			textView.setText(R.string.release_refresh);
			indicator.startAnimation(mFlipAnimation);
		}else{
			mIndicatorUp = false;
			textView.setText(R.string.pull_refresh);
			indicator.startAnimation(mReverseFlipAnimation);
		}
		
	}
	
	
	/**
	 * 设置刷新回调接口
	 */
	public void setRefreshListener(OnRefreshListener refreshListener) {
		mRefreshListener = refreshListener;
	}

	/**
	* 刷新回调接口
	*/
    public interface OnRefreshListener {
        /**
        * 刷新
        */
        public void onRefresh();
    }
	
}
