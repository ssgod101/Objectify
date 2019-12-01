package com.teamelephant.bkmc.objectify;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class AnalyzerFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private MainActivity mActivity;
    public AnalyzerFactory(Application application, MainActivity activity){
        mApplication = application;
        mActivity = activity;
    }
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass){
        return (T) new ObjectifyAnalyzer(mActivity);
    }
}
