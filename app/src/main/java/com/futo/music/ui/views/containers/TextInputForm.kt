package com.futo.music.ui.views.containers

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.futo.music.R
import com.futo.music.ui.buttons.StandardButton
import com.futo.music.ui.views.general.EditText

class TextInputForm: LinearLayout {

    val title: TextView;
    val description: TextView;
    val input: AppCompatEditText;
    val buttonCancel: StandardButton;
    val buttonSubmit: StandardButton;

    private var submitAction: ((String)->Unit)? = null;


    constructor(context: Context): super(context) {
        inflate(context, R.layout.text_input_form, this);

        title = findViewById(R.id.title);
        description = findViewById(R.id.description);
        input = findViewById(R.id.input_text);
        buttonCancel = findViewById(R.id.button_cancel);
        buttonSubmit = findViewById(R.id.button_submit);
        input.addTextChangedListener {
            if(it?.toString()?.isNotBlank() ?: false){
                buttonSubmit.isEnabled = true;
                buttonSubmit.alpha = 1f;
            }
            else {
                buttonSubmit.isEnabled = false;
                buttonSubmit.alpha = 0.5f;
            }
            if(it?.lastOrNull() == '\n') {
                val trimmed = it?.toString()?.trim()
                input.setText(trimmed);
                submitAction?.invoke(trimmed ?: return@addTextChangedListener);
            }
        }
        buttonSubmit.isEnabled = false;
    }

    fun setData(title: String, desc: String, placeholder: String, submitName: String, handleSubmit: ((String)->Unit), handleCancel: (()->Unit)? = null) {
        this.title.text = title;
        this.description.text = desc;
        this.input.hint = placeholder;
        this.buttonCancel.onClick.clear();
        if(handleCancel == null)
            this.buttonCancel.isVisible = false;
        else {
            this.buttonCancel.isVisible = true;
            this.buttonCancel.onClick.subscribe(handleCancel);
        }
        this.buttonSubmit.text.text = submitName;
        this.buttonSubmit.onClick.clear();
        submitAction = handleSubmit;
        this.buttonSubmit.onClick.subscribe {
            handleSubmit(this.input.text?.toString() ?: "");
        }
    }
}