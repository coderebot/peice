package com.peice;

import com.peice.QuestionAdapter.OnAnswerChanged;
import com.peice.model.TestQuestion;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.EditText;

public class VacancyQuestionAdapter extends QuestionAdapter {
	
	static class VacancyField {
		CheckedTextView id;
		EditText        text;
	}
	
	VacancyField[] mVacancyFields;
	
	class VacancyTextWatcher implements TextWatcher {
		int mIndex;
		
		public VacancyTextWatcher(int index) {
			mIndex = index;
		}

		@Override
		public void afterTextChanged(Editable arg0) {
			// TODO Auto-generated method stub
			updateAnswer();
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub
			CheckedTextView id = mVacancyFields[mIndex].id;
			id.setChecked(mVacancyFields[mIndex].text.getText().length() > 0);
		}
		
	}
	
	
	public VacancyQuestionAdapter(TestQuestion question, OnAnswerChanged onAnswerChanged) {
		super(question, onAnswerChanged);
	}

	@Override
	public void getBranchView(ViewGroup parent, LayoutInflater inflate) {
		// TODO Auto-generated method stub
		if(mQuestion == null)
			return ;
		
		int branchCount = mQuestion.getVacancyCount();
		
		if(branchCount <= 0)
			return ;
		
		mVacancyFields = new VacancyField[branchCount];
		
		for(int i = 0; i < branchCount; i ++) {
			View view = inflate.inflate(getResId(), null);
			
			mVacancyFields[i] = new VacancyField();
			mVacancyFields[i].id = (CheckedTextView)view.findViewById(R.id.branch_id);
			mVacancyFields[i].text = (EditText)view.findViewById(R.id.branch_content);
			mVacancyFields[i].text.addTextChangedListener(new VacancyTextWatcher(i));
			
			mVacancyFields[i].id.setText(IdxToAnswer(i));
			
			view.setVisibility(View.VISIBLE);
			parent.addView(view);
		}
	
		onAnswerUpdated();
	}

	@Override
	protected int getResId() {
		// TODO Auto-generated method stub
		return R.layout.vacancy_question;
	}

	@Override
	public void onAnswerUpdated() {
		// TODO Auto-generated method stub
		if(mQuestion.getVacancyCount() <= 0 || getAnswer() == null)
			return;
		
		String[] answers = getAnswer().split("\n");
		
		for(int i = 0; i < answers.length; i++) {
			mVacancyFields[i].text.setText(answers[i]);
		}
		
	}
	
	void updateAnswer() {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < mVacancyFields.length; i ++) {
			String str = mVacancyFields[i].text.getText().toString();
			builder.append(str);
			builder.append("\n");
		}
		setAnswer(builder.toString());
		onAnswerChanged();
	}

}
