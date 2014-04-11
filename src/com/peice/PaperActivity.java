package com.peice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.peice.model.Candidate;
import com.peice.model.DataManager;
import com.peice.model.Question;
import com.peice.model.QuestionGroup;
import com.peice.model.Test;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

public class PaperActivity extends Activity implements 
		ViewPager.OnPageChangeListener, 
		QuestionCard.Listner,
		AnswerCard.Listener{
	
	static final String TAG="PaperActivity";
	
	static public final String TEST_ID = "course_id";
	
	TextView mLeftTimes;
	TextView mTitle;
	
	Candidate mCandidate;
	Test      mTest;
	
	int      mCurrentQuestion;
	ProgressBar mAnswerProgress;
	ImageButton mShowAnswerCard;
	
	QuestionCard mFrontQuestionCard;
	QuestionCard mBackQuestionCard;
	MyPagerAdapter mPagerAdapter;
	ViewPager    mContentView;
	CheckBox     mMark;
	Handler      mHandler;
	
	class GotoQuestionRunable implements Runnable{
		public int nextId;

		@Override
		public void run() {
			gotoQuestion(nextId);
		}
		
	};
	GotoQuestionRunable mGotoQuestion = new GotoQuestionRunable();
	
	static class PageViewManager<V extends View> {
		List<V> mCached;
		Map<Integer, V>  mUsing;
		V getCached() {
			if(mCached != null && mCached.size() > 0) {
				V v = mCached.get(0);
				mCached.remove(0);
				return v;
			}
			return null;
		}
		void putUsing(int pos, V v) {
			if(mUsing == null) {
				mUsing = new HashMap<Integer, V>();
			}
			mUsing.put(pos, v);
		}
		V remove(int pos) {
			if(mUsing == null)
				return null;
			V v = mUsing.get(pos);
			mUsing.remove(pos);
			if(mCached == null)
				mCached = new ArrayList<V>();
			mCached.add(v);
			return v;
		}
	}
	
	class MyPagerAdapter extends PagerAdapter {

		PageViewManager<QuestionCard>  mQuestionCardManager = new PageViewManager<QuestionCard>();
		PageViewManager<AnswerCard>    mAnswerCardManager = new PageViewManager<AnswerCard>();
		
		private QuestionCard  getQuestionCard(Question q, int poistion) {
			QuestionCard card = mQuestionCardManager.getCached();
			if(card == null) {
				card = new QuestionCard(PaperActivity.this, null);
			}
			
			card.init(mCandidate, mTest, PaperActivity.this);
			card.setQuestion(q, poistion);
			mQuestionCardManager.putUsing(poistion, card);
			
			return card;
		}
		
		private AnswerCard getAnswerCard(int postion) {
			AnswerCard card = mAnswerCardManager.getCached();
			if(card == null) {
				card = new AnswerCard(PaperActivity.this, null);
			}
			card.setTest(mTest);
			card.setListener(PaperActivity.this);
			mAnswerCardManager.putUsing(postion, card);
			return card;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mTest != null ? mTest.getQuestionCount() + 1 :  0;
		}

		@Override
		public boolean isViewFromObject(View view, Object data) {
			// TODO Auto-generated method stub
			if(view instanceof QuestionCard && ((QuestionCard)view).getQuestion() == data) {
				return true;
			}
			else if(view instanceof AnswerCard && data instanceof Test) {
				return true;
			}
			return false;
		}
		
		@Override
		public void destroyItem(View container, int position, Object object) {
			View view = null;
			if(position == mTest.getQuestionCount()) //answer
			{
				view = mAnswerCardManager.remove(position);
			}
			else {
				QuestionCard card = mQuestionCardManager.remove(position);
				view = card;
			}
			((ViewGroup)container).removeView(view);
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if(position == mTest.getQuestionCount()) //answer
			{
				AnswerCard card = getAnswerCard(position);
				container.addView(card);
				return mTest;
			}
			else {
				Question tq = mTest.getQuestion(position);
				QuestionCard card = getQuestionCard(tq, position);
				container.addView(card);
				return tq;
			}
		}
		
	}
	
	
	int     mLeftSeconds;
	int     mTotalSeconds;
	long    mLastTimeMillise;
	int     mTimerState;
	static final int TIMER_IDLE = 0;
	static final int TIMER_RUNNING = 0;
	static final int TIMER_PAUSED = 0;
	static final int TIMER_FINISH = 0;
	Handler mTimerHandler = new Handler();
	Runnable mTimerRunable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(mTimerState != TIMER_RUNNING)
				return;
			
			if(mLastTimeMillise <= 0)  {
				mLastTimeMillise = System.currentTimeMillis();
			}
			else {
				long cur = System.currentTimeMillis();
				int seconds = (int)((cur - mLastTimeMillise) / 1000);
				mLastTimeMillise = cur;
				mLeftSeconds -= seconds;
				if(mLeftSeconds <= 0) {
					//时间到
					timeCome();
					mTimerState = TIMER_FINISH;
				}
			}
			
			updateTime();
			
			mTimerHandler.postDelayed(this, 1000);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		
		Intent intent = getIntent();
		
		String testId = intent.getStringExtra(TEST_ID);
		mCandidate = DataManager.getInstance().getCandidate();

		if(mCandidate == null) {
			Log.e(TAG, "Cannot Get the Candidate");
			return;
		}
		

		mCurrentQuestion = 0;
		
		setContentView(R.layout.paper_frame);
		mContentView = (ViewPager)findViewById(R.id.viewpager);
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what != DataManager.MSG_QUERY_TEST) {
					return;
				}
				
				mTest = (Test)msg.obj;
				if (mTest == null) {
					Log.e("PapaerAcitivty", "Invalidate Test");
					finish();
					return ;
				}
		
				mPagerAdapter = new MyPagerAdapter();
				mContentView.setAdapter(mPagerAdapter);
				mContentView.setOnPageChangeListener(PaperActivity.this);
		
				mLeftSeconds = 0;
				
				onPageSelected(0);
				setTime(mTest.getTimeLength() * 60);
				startTimer();
			}
		};
		
		DataManager.getInstance().queryTest(testId, mHandler);
		
	}
	
	@Override
	public void onStart() {
		super.onStart();
		LayoutInflater inflater = LayoutInflater.from(this);
		View view =  inflater.inflate(R.layout.paper_nav, null);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(view);
		
		final ImageButton backbtn = (ImageButton)actionBar.getCustomView().findViewById(android.R.id.home);
		backbtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
		
		mTitle = (TextView)view.findViewById(R.id.title);
		mLeftTimes = (TextView)view.findViewById(R.id.time_left);
		mMark = (CheckBox)view.findViewById(R.id.mark);
		mMark.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onMarkChanged(mMark.isChecked());
			}
		});
		mShowAnswerCard = (ImageButton)view.findViewById(R.id.questions);
		
		mShowAnswerCard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mContentView.setCurrentItem(mTest.getQuestionCount()+1);
			}
		});
		
		mTitle.setText(getCaption());
		
	}
	
    private String getCaption() {
    	return "【"+  mCandidate.getName() + "】" + mCandidate.getProjectName();
    }
	
	
	private void onMarkChanged(boolean checked) {
		Log.i("==DJJ", "onMarkChanged, currentQuestion="+mCurrentQuestion+",checked="+checked);
		if(mCurrentQuestion >= 0 && mCurrentQuestion < mTest.getQuestionCount()) {
			Question tq = mTest.getQuestion(mCurrentQuestion);
			mTest.setQuestionMark(tq.getId(), checked);
		}
	}
	
	public void setTime(int seconds) {
		stopTimer();
		mTotalSeconds = seconds;
	}
	
	public void stopTimer() {
		mTimerState = TIMER_FINISH;
	}
	
	public void pauseTimer() {
		mTimerState = TIMER_PAUSED;
	}
	
	public void resumeTimer() {
		if(mTimerState == TIMER_PAUSED || mTimerState == TIMER_IDLE);
		mTimerState = TIMER_RUNNING;
		mTimerHandler.post(mTimerRunable);
		mLastTimeMillise = System.currentTimeMillis();
	}
	
	public void startTimer() {
		mTimerState = TIMER_IDLE;
		mLeftSeconds = mTotalSeconds;
		resumeTimer();
	}
	
	
	private void timeCome() {
		//时间到
		
	}
	
	private void updateTime() {
		if(mLeftTimes != null)
			mLeftTimes.setText( (mLeftSeconds/60) + ":" + (mLeftSeconds%60));
	}
	

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	
	private void enableToolButtons(boolean b) {
		mShowAnswerCard.setEnabled(b);
		mMark.setEnabled(b);
	}

	@Override
	public void onPageSelected(int index) {
		mCurrentQuestion = index;
		enableToolButtons(index >= 0 && index < mTest.getQuestionCount());
		if(index < mTest.getQuestionCount()) //questions
		{
			Question tq = mTest.getQuestion(index);
			if(tq != null) {
				mMark.setChecked(mTest.isQuestionMark(tq.getId()));
			}
		}
		else {
			mMark.setChecked(false);
		}
			
	}

	@Override
	public void onAnswer(Question tq, int idx) {
		//mAnswerProgress.setProgress(mPaper.getAnswerCount());
	}

	@Override
	public void gotoQuestion(int index) {
		if(mTest == null)
			return;
		mContentView.setCurrentItem(index);
	}

	@Override
	public void submitAnswer() {
		// TODO submit the answer
		
	}

	@Override
	public void onAnswerFinished(Question tq, int idx) {
		mGotoQuestion.nextId = idx + 1;
		getWindow().getDecorView().postDelayed(mGotoQuestion, 500);
	}
	
	
}
