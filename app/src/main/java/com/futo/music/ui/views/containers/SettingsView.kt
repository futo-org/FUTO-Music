package com.futo.music.ui.views.containers

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.futo.music.R
import com.futo.music.constructs.Event0
import com.futo.music.constructs.Event1
import com.futo.music.dp
import com.futo.music.ui.views.general.Toggle
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

class SettingsView: LinearLayout {

    private val _onSettingsViewCreate: ((ISettingsSubView, Setting)->Unit)?

    constructor(context: Context, onSettingsViewCreate: ((ISettingsSubView, Setting)->Unit)? = null): super(context) {
        orientation = VERTICAL;
        _onSettingsViewCreate = onSettingsViewCreate;
    }

    fun setSettingsObject(obj: Any) {
        removeAllViews();
        val props = obj::class.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>()
        val propsReadOnly = obj::class.declaredMemberProperties.filter { it !is KMutableProperty<*> };
        val views = mutableListOf<Pair<View, Int>>();

        val marginTop = 20.dp(resources);
        val marginBot = 10.dp(resources);
        val marginSides = 10.dp(resources);

        val mets = obj::class.declaredFunctions.filter { it.findAnnotation<Setting>() != null }
        for(met in mets) {
            val ann = met.findAnnotation<Setting>();

            val view = SettingsButtonView(context);
            view.setLabel(ann!!.name, ann.description);
            if(ann.icon.isNotBlank())
                view.setIcon(resources.getIdentifier(ann.icon, "drawable", context.packageName));

            if(met.parameters.size == 1) {
                view.onClick.subscribe {
                    met.call(obj);
                }
            }
            else if(met.parameters.size == 2) {
                view.onClick.subscribe {
                    met.call(obj, context);
                }
            }

            views.add(Pair(view, ann.order));
        }

        for(item in props.map { Pair(it, it.findAnnotation<Setting>()) }
                .filter { it.second != null }
                .sortedBy { it.second!!.order }) {
            if(item.second == null)
                continue;

            val type = if(item.second!!.type == SettingType.UNKNOWN)
                settingTypeFromType(item.first.returnType);
            else item.second!!.type;

            val view = when(type) {
                SettingType.TOGGLE -> SettingsToggleView(context);
                SettingType.INFO -> SettingsInfoView(context);
                else -> throw NotImplementedError("Unknown setting type ${type}");
            }

            view.setLabel(item.second!!.name, item.second!!.description);
            view.setValue(item.first.javaGetter!!.invoke(obj));
            view.onValueChanged.subscribe {
                item.first.setter.call(obj, it)
            }

            _onSettingsViewCreate?.invoke(view, item.second!!);
            views.add(Pair(view, item.second!!.order));
        }
        for(item in propsReadOnly.map { Pair(it, it.findAnnotation<Setting>()) }
            .filter { it.second != null }
            .sortedBy { it.second!!.order }) {
            if(item.second == null)
                continue;

            val type = if(item.second!!.type == SettingType.UNKNOWN)
                settingTypeFromType(item.first.returnType);
            else item.second!!.type;

            val view = when(type) {
                SettingType.TOGGLE -> SettingsToggleView(context);
                SettingType.INFO -> SettingsInfoView(context);
                else -> throw NotImplementedError("Unknown setting type ${type}");
            }

            view.setLabel(item.second!!.name, item.second!!.description);
            view.setValue(item.first.javaGetter!!.invoke(obj));

            _onSettingsViewCreate?.invoke(view, item.second!!);
            views.add(Pair(view, item.second!!.order));
        }

        var first = true;
        for(view in views.sortedBy { it.second }) {
            view.first.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                this.setMargins(marginSides, if(first) marginTop else 0, marginSides, marginBot)
            }
            addView(view.first);
            first = false;
        }
    }


    private fun settingTypeFromType(type: Class<*>): SettingType {
        if(type == Boolean::class.java)
            return SettingType.TOGGLE;

        throw NotImplementedError("Unknown type ${type.name}");
    }
    private fun settingTypeFromType(type: KType): SettingType {
        if(type.classifier == Boolean::class)
            return SettingType.TOGGLE;

        throw NotImplementedError("Unknown type ${type}");
    }
}


@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsGroup(val name: String, val order: Int = 999);

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Setting(val name: String, val description: String, val type: SettingType = SettingType.UNKNOWN, val order: Int = 999, val icon: String = "");

enum class SettingType {
    UNKNOWN,
    TOGGLE,
    INFO
}

interface ISettingsSubView {
    val onValueChanged: Event1<Any>;

    fun setLabel(str: String, desc: String? = null);
    fun setValue(obj: Any);
}

class SettingsToggleView: ConstraintLayout, ISettingsSubView {

    val toggle: Toggle;
    val text: TextView;
    val description: TextView;

    override val onValueChanged = Event1<Any>();

    constructor(context: Context): super(context) {
        inflate(context, R.layout.view_settings_toggle, this);

        text = findViewById(R.id.text);
        description = findViewById(R.id.description)
        toggle = findViewById(R.id.toggle);
        toggle.onValueChanged.subscribe(onValueChanged::emit);
    }

    override fun setLabel(str: String, desc: String?) {
        text.text = str;
        description.text = desc;
    }
    override fun setValue(obj: Any) {
        toggle.setValue(obj as Boolean, false, false);
    }
}
class SettingsButtonView: ConstraintLayout {
    val text: TextView;
    val description: TextView;
    val icon: ImageView;

    val onClick = Event0();

    constructor(context: Context): super(context) {
        inflate(context, R.layout.view_settings_button, this);

        text = findViewById(R.id.text);
        description = findViewById(R.id.description);
        icon = findViewById(R.id.icon);

        setOnClickListener {
            onClick.emit();
        }
    }

    fun setLabel(str: String, desc: String?) {
        text.text = str;
        description.text = desc;
    }
    fun setIcon(icon: Int) {
        this.icon.setImageResource(icon);
        this.icon.visibility = VISIBLE;
    }
}
class SettingsInfoView : ConstraintLayout, ISettingsSubView {
    val text: TextView;
    val description: TextView;
    val icon: ImageView;
    val value: TextView;

    val onClick = Event0();

    override val onValueChanged = Event1<Any>();

    constructor(context: Context): super(context) {
        inflate(context, R.layout.view_settings_info, this);

        text = findViewById(R.id.text);
        description = findViewById(R.id.description);
        icon = findViewById(R.id.icon);
        value = findViewById(R.id.value);

        setOnClickListener {
            onClick.emit();
        }
    }

    override fun setLabel(str: String, desc: String?) {
        text.text = str;
        description.text = desc;
    }
    fun setIcon(icon: Int) {
        this.icon.setImageResource(icon);
        this.icon.visibility = VISIBLE;
    }
    override fun setValue(obj: Any) {
        value.text = obj.toString();
    }
}