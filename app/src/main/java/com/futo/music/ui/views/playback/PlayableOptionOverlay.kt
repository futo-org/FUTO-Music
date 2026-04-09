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
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateQueue
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.buttons.IconButton
import com.futo.music.ui.buttons.ListButton
import kotlinx.coroutines.CoroutineScope

class PlayableOptionOverlay: ConstraintLayout {

    private var _currentPlayable: IPlayable? = null;

    private val _root: ConstraintLayout;

    private val _viewBackground: View;

    private val _overlayContainer: ConstraintLayout;
    private val _buttonPlay: IconButton;
    private val _buttonPlayNext: IconButton;
    private val _buttonQueueAdd: IconButton;

    private val _buttonPlaylistAdd: ListButton;
    private val _buttonArtist: ListButton;
    private val _buttonRate: ListButton;

    private val _textTitle: TextView;

    constructor(context: Context, attrs: AttributeSet? = null): super(context, attrs) {
        inflate(context, R.layout.view_playable_options_overlay, this);
        _root = findViewById<ConstraintLayout>(R.id.root);
        _buttonPlay = findViewById(R.id.button_play);
        _buttonPlayNext = findViewById(R.id.button_play_next);
        _buttonQueueAdd = findViewById(R.id.button_queue_add);
        _viewBackground = findViewById(R.id.view_background);
        _overlayContainer = findViewById(R.id.overlay_container);
        _textTitle = findViewById(R.id.text_title);


        _buttonPlaylistAdd = findViewById(R.id.button_add_playlist);
        _buttonArtist = findViewById(R.id.button_artist);
        _buttonRate = findViewById(R.id.button_rate);

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
                if(it is DBTrack)
                    StateQueue.instance.setQueuePlayNext(context, it);
            }
            hide();
        }
        _buttonPlaylistAdd.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack) {
                    UIDialogs.showAddToPlaylistDialog(context, getScope(), "Add to playlist", it);
                }
            }
            hide();
        }
        _buttonRate.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack)
                    UIDialogs.showRatingDialog(context, getScope(), it, null, null, null);
                else if(it is DBPlaylist)
                    UIDialogs.showRatingDialog(context, getScope(), null, it, null, null);
                else if(it is DBAlbum)
                    UIDialogs.showRatingDialog(context, getScope(), null, null, it, null);
                else if(it is DBArtist)
                    UIDialogs.showRatingDialog(context, getScope(), null, null, null, it);
            }
            hide();
        }
        _buttonArtist.onClick.subscribe {
            _currentPlayable?.let {
                if(it is DBTrack) {
                    val artist = StateDatabase.instance.getArtist(it.artistId ?: return@let);
                    //
                }
                else if(it is DBArtist) {

                }
            }
        }
    }

    fun getScope(): CoroutineScope {
        return findViewTreeLifecycleOwner()?.lifecycleScope ?: StateApp.instance.scopeOrNull!!
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
        if(playable is DBTrack) {
            _buttonPlaylistAdd.isVisible = true;
            _buttonPlayNext.isVisible = true;
            _buttonQueueAdd.isVisible = true;
        }
        else {
            _buttonPlaylistAdd.isVisible = false;
            _buttonPlayNext.isVisible = false;
            _buttonQueueAdd.isVisible = false;
        }

        if(playable is DBTrack || playable is DBPlaylist || playable is DBAlbum || playable is DBArtist)
            _buttonRate.isVisible = true;
        else
            _buttonRate.isVisible = false;

        if(playable is DBTrack || playable is DBArtist)
            _buttonArtist.isVisible = true;
        else
            _buttonArtist.isVisible = false;
    }


}