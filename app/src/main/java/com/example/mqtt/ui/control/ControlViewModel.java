package com.example.mqtt.ui.control;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ControlViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ControlViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is manager fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}