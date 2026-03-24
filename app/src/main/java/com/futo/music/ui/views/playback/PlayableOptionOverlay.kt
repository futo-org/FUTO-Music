package com.futo.music.ui.views.playback

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.buttons.IconButton

class PlayableOptionOverlay: ConstraintLayout {

    private var _currentPlayable: IPlayable? = null;

    private val _root: ConstraintLayout;

    private val _viewBackground: View;

    private val _overlayContainer: ConstraintLayout;
    private val _buttonPlay: IconButton;
    private val _buttonPlayNext: IconButton;
    private val _buttonQueueAdd: IconButton;
    private val _buttonPlaylistAdd: IconButton;

    private val _textTitle: TextView;

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_playable_options_overlay, this);
        _root = findViewById<ConstraintLayout>(R.id.root);
        _buttonPlay = findViewById(R.id.button_play);
        _buttonPlayNext = findViewById(R.id.button_play_next);
        _buttonQueueAdd = findViewById(R.id.button_queue_add);
        _buttonPlaylistAdd = findViewById(R.id.button_add_playlist);
        _viewBackground = findViewById(R.id.view_background);
        _overlayContainer = findViewById(R.id.overlay_container);
        _textTitle = findViewById(R.id.text_title);

        //this.translationY = 1f;
        this.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        //this.alpha = 0f;

        //visibility = INVISIBLE;

        _viewBackground.setOnClickListener {
            hide();
        }

        _buttonPlay.onClick.subscribe {
            _currentPlayable?.let {
                StateApp.instance.activity()?.navigate<PlaybackFragment>(it);
            }
            hide();
        }
        _buttonPlayNext.onClick.subscribe {
            _currentPlayable?.let {
                StateQueue.instance.setQueuePlayNext(context, it);
            }
            hide();
        }
        _buttonPlaylistAdd.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack) {
                    UIDialogs.showAddToPlaylistDialog(context, findViewTreeLifecycleOwner()?.lifecycleScope ?: StateApp.instance.scopeOrNull!!, "Add to playlist", it);
                }
            }
            hide();
        }
    }

    fun show(playable: IPlayable) {
        _currentPlayable = playable;

        setButtons(playable);

        _viewBackground.alpha = 0.0f;
        _overlayContainer.translationY = 1f;
        visibility = VISIBLE;

        _textTitle.text = playable.name;
        if(_textTitle.text.isEmpty())
            _textTitle.isVisible = false;
        else
            _textTitle.isVisible = true;

        val animations = arrayListOf<Animator>();
        animations.add(ObjectAnimator.ofFloat(_viewBackground, "alpha", 0.0f, 1.0f).setDuration(200L));
        animations.add(ObjectAnimator.ofFloat(_overlayContainer, "translationY", _overlayContainer.measuredHeight.toFloat(), 0.0f)
            .setDuration(200L).apply {
                this.interpolator = AccelerateDecelerateInterpolator()
            })

        val animatorSet = AnimatorSet();
        animatorSet.playTogether(animations);
        animatorSet.start();
    }
    fun hide() {
        val animations = arrayListOf<Animator>();
        animations.add(ObjectAnimator.ofFloat(_viewBackground, "alpha", 1.0f, 0f).setDuration(200L));
        animations.add(ObjectAnimator.ofFloat(_overlayContainer, "translationY", 0.0f, _overlayContainer.measuredHeight.toFloat())
            .setDuration(200L).apply {
                this.interpolator = AccelerateDecelerateInterpolator()
            })

        val animatorSet = AnimatorSet();
        animatorSet.playTogether(animations);
        animatorSet.start();

        animatorSet.addListener(object: Animator.AnimatorListener {
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationEnd(p0: Animator) {
                _currentPlayable = null;
                visibility = GONE;
            }
            override fun onAnimationRepeat(p0: Animator) {}
            override fun onAnimationStart(p0: Animator) {}
        })
    }


    fun setButtons(playable: IPlayable) {
        return;
        if(playable is DBTrack) {
            _buttonPlaylistAdd.isVisible = true;
            _buttonPlayNext.isVisible = true;
        }
        else {
            _buttonPlaylistAdd.isVisible = false;
            _buttonPlayNext.isVisible = false;
        }
    }


}