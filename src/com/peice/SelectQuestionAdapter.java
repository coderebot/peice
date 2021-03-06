package com.peice;

import java.util.List;

import com.peice.common.SelectView;
import com.peice.model.TestQuestion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

public class SelectQuestionAdapter extends QuestionAdapter{

	protected SelectView[] mBtnBranches;
	
	public SelectQuestionAdapter(TestQuestion tq, OnAnswerChanged onAnswerChanged) {
		super(tq, onAnswerChanged);
	}

	@Override
	public void getBranchView(ViewGroup parent, LayoutInflater inflater) {
		if(mQuestion == null)
			return ;
		List<String> branches = mQuestion.getBranches();
		
		if(branches == null || branches.size() <= 0)
			return ;
		
		mBtnBranches = new SelectView[branches.size()];
		
		for(int i = 0; i < branches.size(); i ++) {
			View view = inflater.inflate(getResId(), null);
			if(view == null)
				continue;
			
				
			//add listener
			mBtnBranches[i] = (SelectView)view;
			
			mBtnBranches[i].setContent(branches.get(i));
			mBtnBranches[i].setId(IdxToAnswer(i));
			
			mBtnBranches[i].setOnCheckedChangeListener(mOnCheckedChanged);
			android.util.Log.i("==DJJ", "setupUI getBranchView SelectQuestion view="+view);
			view.setVisibility(View.VISIBLE);
			parent.addView(view);
		}
		onAnswerUpdated();
	} 
	
	@Override
	public void onAnswerUpdated() {
		if(mBtnBranches == null || getAnswer() == null)
			return;
		char [] answers = getAnswer().toCharArray();
		if(answers == null || answers.length == 0)
			return;
		
		for(int i = 0; i < answers.length; i++) {
			int idx = answers[i] - 'A';
			if(idx >= 0 && idx <= mBtnBranches.length && mBtnBranches[idx] != null) {
				mBtnBranches[idx].setChecked(true);
			}
		}
	}
	
	@Override
	protected int getResId() {
		return R.layout.multi_select_item;
	}
	
	private SelectView.OnCheckedChangeListener mOnCheckedChanged= new SelectView.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(SelectView btn, boolean checked) {
			onBrancheSelectChanged(btn, checked);
		}
		
	};
	
	protected void onBrancheSelectChanged(SelectView btn, boolean checked) {
		StringBuilder answer = new StringBuilder();
		for(int i = 0; i < mBtnBranches.length; i++) {
			if(mBtnBranches[i].isChecked()) {
				answer.append(IdxToAnswer(i));
			}
		}
		setAnswer(answer.toString());
		onAnswerChanged();
	}
	
	
}
