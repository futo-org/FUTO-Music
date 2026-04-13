package com.futo.music.activities

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.window.OnBackInvokedDispatcher
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.futo.music.R
import com.futo.music.RootInsetsController
import com.futo.music.UIDialogs
import com.futo.music.UIDialogs.ActionStyle
import com.futo.music.constructs.Event1
import com.futo.music.fragments.bottom.MenuBottomBarFragment
import com.futo.music.fragments.main.ArtistFragment
import com.futo.music.fragments.main.ContentsFragment
import com.futo.music.fragments.main.HomeFragment
import com.futo.music.fragments.main.MainFragment
import com.futo.music.fragments.main.NotificationOverlayView
import com.futo.music.fragments.main.PlaybackFragment
import com.futo.music.fragments.main.SearchFragment
import com.futo.music.fragments.main.SetupFragment
import com.futo.music.fragments.top.GeneralTopBarFragment
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.fragments.top.TopFragment
import com.futo.music.logging.Logger
import com.futo.music.logic.PlayerManager
import com.futo.music.models.playable.IPlayable
import com.futo.music.services.PlaybackService
import com.futo.music.states.AnnouncementType
import com.futo.music.states.StateAnnouncement
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateLibrary
import com.futo.music.states.StateQueue
import com.futo.music.ui.views.playback.PlayableOptionOverlay
import com.futo.music.ui.views.toasts.ToastView
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.RequestBuilder
import com.futo.music.toGradientDrawable

class MainActivity : AppCompatActivity() {

    private lateinit var _rootView: ConstraintLayout;
    private lateinit var _fragContainerTopBar: FragmentContainerView;
    private lateinit var _fragContainerMain: FragmentContainerView;
    private lateinit var _fragContainerBotBar: FragmentContainerView;
    private lateinit var _toastView: ToastView;
    private lateinit var _overlayPlayable: PlayableOptionOverlay;

    private lateinit var _backgroundTop: ImageView;
    private lateinit var _backgroundBottom: ImageView;

    //Topbar
    private val _fragTopGeneral = GeneralTopBarFragment();
    private val _fragTopNavigation = NavigationTopBarFragment();

    //BottomBar
    private val _fragBotMenu = MenuBottomBarFragment();

    private val _fragHome = HomeFragment();
    private val _fragSearch = SearchFragment();
    private val _fragContents = ContentsFragment();
    private val _fragPlayer = PlaybackFragment();
    private val _fragNotifs = NotificationOverlayView.Frag();
    private val _fragArtist = ArtistFragment();

    //Main

    private lateinit var _rootInsetsController: RootInsetsController


    private val _queue: LinkedList<Pair<MainFragment, Any?>> = LinkedList();
    var fragCurrent: MainFragment? = null; private set;
    private var _parameterCurrent: Any? = null;

    var fragBeforeOverlay: MainFragment? = null; private set;

    val onNavigated = Event1<MainFragment>();

    var isConnectedTop: Boolean = false;

    val fragmentsMain = mapOf<KClassifier, FragmentDefinition>(
        Pair(HomeFragment::class, FragmentDefinition(_fragTopGeneral, _fragBotMenu, { _fragHome }, connectTop = true)),
        Pair(SearchFragment::class, FragmentDefinition(_fragTopGeneral, _fragBotMenu, { _fragSearch })),
        Pair(ContentsFragment::class, FragmentDefinition(_fragTopGeneral, _fragBotMenu, { _fragContents })),
        Pair(PlaybackFragment::class, FragmentDefinition(null, null, { _fragPlayer }, animExit = R.anim.slide_down)),
        Pair(NotificationOverlayView.Frag::class, FragmentDefinition(_fragTopGeneral, null, { _fragNotifs })),
        Pair(ArtistFragment::class, FragmentDefinition(null, _fragBotMenu, { _fragArtist }))
    );

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val writer = StringWriter();

            var excp = throwable;
            Logger.e("Application", "Uncaught", excp);

            //Resolve invocation chains
            while (excp is InvocationTargetException || excp is java.lang.RuntimeException) {
                val before = excp;

                if (excp is InvocationTargetException)
                    excp = excp.targetException ?: excp.cause ?: excp;
                else if (excp is java.lang.RuntimeException)
                    excp = excp.cause ?: excp;

                if (excp == before)
                    break;
            }
            writer.write((excp.message ?: "Empty error") + "\n\n");
            excp.printStackTrace(PrintWriter(writer));
            val message = writer.toString();
            Logger.e(TAG, message, excp);

            val exIntent = Intent(this, ExceptionActivity::class.java);
            exIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            exIntent.putExtra(ExceptionActivity.EXTRA_STACK, message);
            startActivity(exIntent);

            Runtime.getRuntime().exit(0);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT, Color.TRANSPARENT
            )
        )

        StateApp.instance.registerContext(this);
        StateApp.instance.registerScope(lifecycleScope);

        lifecycleScope.launch(Dispatchers.IO) {
            StateQueue.instance.restoreQueue(this@MainActivity);
        }

        setContentView(R.layout.activity_main);

        _rootView = findViewById(R.id.rootView);
        _fragContainerTopBar = findViewById(R.id.fragment_top_bar);
        _fragContainerMain = findViewById(R.id.fragment_main);
        _fragContainerBotBar = findViewById(R.id.fragment_bottom_bar);
        _toastView = findViewById(R.id.toast_view);
        _overlayPlayable = findViewById(R.id.overlay_playable)

        _backgroundTop = findViewById(R.id.image_background_top);
        _backgroundTop.post {
            _backgroundTop.pivotY = 0f;
        }
        _backgroundBottom = findViewById(R.id.image_background_bottom);
        _backgroundBottom.post {
            _backgroundBottom.pivotY = _backgroundBottom.height.toFloat();
        }

        _rootInsetsController = RootInsetsController.attach(this, _rootView);
        _rootInsetsController.setLightSystemBarAppearance(lightStatus = false, lightNav = false);
        _rootInsetsController.onPaddingChanged.subscribe { top, bottom ->
            _backgroundTop.updateLayoutParams {
                if(this is ViewGroup.MarginLayoutParams) {
                    this.topMargin = top * -1;
                }
            }
            _backgroundBottom.updateLayoutParams {
                if(this is ViewGroup.MarginLayoutParams) {
                    this.bottomMargin = bottom * -1;
                }
            }
        }

        for(frag in fragmentsMain) {
            frag.value.get().topBar = frag.value.topbar;
            frag.value.get().botBar = frag.value.botbar;
        }

        _fragTopGeneral.setTitleLongPress {
            UIDialogs.showDialogVertical(this, 0, false, "Hidden Menu", "Some hidden options for testing", null, null, null, -1,
                UIDialogs.Action("Rescan", {
                    sync(true)
                }, ActionStyle.PRIMARY));
        }

        createPlayer {
            it.subscribe("main", object: PlayerManager.Listener {
                override fun onPlayingChanged(isPlaying: Boolean) {

                }

                override fun onMediaItemChanged(player: Player, mediaItem: MediaItem?, reason: Int) {
                    if(mediaItem != null) {
                        val id = mediaItem.mediaId.toLongOrNull()
                        if(id != null && id > 0) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    StateDatabase.instance.setPlayedTrack(id);
                                } catch (ex: Throwable) {
                                    Logger.e(TAG, "Failed to update played date for track", ex);
                                }
                            }
                        }
                    }
                }

                override fun onMediaMetadataChanged(player: Player, mediaMetadata: MediaMetadata?) {

                }

                override fun onMediaClose() {
                }
            });

            _fragPlayer.setPlayer(it);
            _fragBotMenu.setPlayer(it);
        }


        fragCurrent?.onShown(null, false);


        hasAudioPermission {
            if(it) {
                fragCurrent = _fragHome;
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_top_bar, _fragTopGeneral)
                    .replace(R.id.fragment_main, _fragHome)
                    .replace(R.id.fragment_bottom_bar, _fragBotMenu)
                    .commitNow();

                _fragTopGeneral.onShowFragment(_fragHome);
                _fragHome.onShown(null, false);

                val definitionNew = fragmentsMain.values.find { it.get() == _fragHome };
                if(isConnectedTop != definitionNew?.connectTop) {
                    isConnectedTop = !isConnectedTop;
                    if(isConnectedTop) {
                        val constraintSet = ConstraintSet();
                        constraintSet.clone(_rootView);
                        constraintSet.connect(R.id.fragment_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID,ConstraintSet.TOP, 0);
                        constraintSet.applyTo(_rootView);
                    }
                    else {
                        val constraintSet = ConstraintSet();
                        constraintSet.clone(_rootView);
                        constraintSet.connect(R.id.fragment_main, ConstraintSet.TOP, R.id.fragment_top_bar,ConstraintSet.BOTTOM, 0);
                        constraintSet.applyTo(_rootView);
                    }
                }

                sync();
            }
            else {
                val setupFrag = SetupFragment();
                fragCurrent = setupFrag;
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_main, setupFrag)
                    .commitNow();
            }
        }


        if(Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                handleBack();
            }
        }
        else
            onBackPressedDispatcher.addCallback {
                handleBack();
            }
    }

    fun sync(force: Boolean = false) {
        if(force || StateLibrary.instance.requireSync(this)) {
            val announce = StateAnnouncement.instance.registerLoading("Syncing Mediastore", "Importing new music from your phone", null,
                "importing", true);
            UIDialogs.appToast("We're importing your music!\nGive us a minute.")
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val results = StateLibrary.instance.syncDatabase(applicationContext, { max, progress, type, text ->
                        if (max > 0) {
                            announce.setProgress(progress.toDouble() / max, text);
                        }
                    });
                    StateAnnouncement.instance.deleteAnnouncement(announce.id);
                    StateAnnouncement.instance.registerAnnouncement("import-success-" + UUID.randomUUID().toString() , "Import Success", "Imported ${results.albums} albums (${results.albumsNew} new), ${results.artists} artists (${results.artistsNew} new), ${results.tracks} tracks (${results.tracksNew} new)", AnnouncementType.SESSION);
                }
                catch(ex: Throwable) {
                    Logger.e(TAG, "Import failed", ex);
                    StateAnnouncement.instance.deleteAnnouncement(announce.id);
                    StateAnnouncement.instance.registerAnnouncement("import-failed-" + UUID.randomUUID().toString(), "Import failed", ex.message ?: "", AnnouncementType.SESSION);
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        _rootInsetsController.onConfigurationChanged();
    }

    override fun onBackPressed() {
        handleBack();
    }

    private val _backgroundTransitionDuration = 1000L;
    private var _lastTopAnimation: ViewPropertyAnimator? = null;
    fun setBackgroundTopGradient(color: Int, heightScale: Float = 0.5f, opacity: Float = 1.0f)
            = setBackgroundTop(color.toGradientDrawable(Color.BLACK,
        GradientDrawable.Orientation.TOP_BOTTOM), heightScale, opacity)
    fun setBackgroundTop(drawable: Drawable, heightScale: Float = 1f, opacity: Float = 1f, glideBuilder: ((RequestBuilder<Drawable>)->RequestBuilder<Drawable>)? = null) {
        _lastTopAnimation?.cancel();


        if(drawable is BitmapDrawable && _backgroundTop.scaleY != heightScale) {
            _lastTopAnimation = _backgroundTop.animate()
                .alpha(0f)
                .setDuration(_backgroundTransitionDuration)
                .setListener(object: Animator.AnimatorListener {
                    override fun onAnimationCancel(p0: Animator) { }
                    override fun onAnimationEnd(p0: Animator) {
                        _lastTopAnimation?.setListener(null);
                        Logger.i(TAG, "setBackgroundTop clear animation ended");
                        _backgroundTop.scaleY = heightScale;
                        _backgroundTop.setBackgroundColor(Color.BLACK);
                        setBackgroundTop(drawable, heightScale, opacity, glideBuilder);
                    }
                    override fun onAnimationRepeat(p0: Animator) {}
                    override fun onAnimationStart(p0: Animator) {}
                })
                .let {
                    it.start();
                    return@let it;
                }
        }
        else {
            var builder =  Glide.with(_backgroundTop)
                .load(drawable);

            if(glideBuilder != null)
                builder = glideBuilder(builder);
            builder
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(_backgroundTop);
            if(_backgroundTop.scaleY != heightScale || _backgroundTop.alpha != opacity)
                _lastTopAnimation = _backgroundTop.animate()
                    .scaleY(heightScale)
                    .setDuration(_backgroundTransitionDuration)
                    .alpha(opacity)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .let {
                        it.start();
                        return@let it;
                    }
        }
    }
    fun setBackgroundTopColor(color: Int, heightScale: Float = 1f) {
        if(heightScale == 0f && color == Color.BLACK) {
            hideBackgroundTop();
        }
        else {
            _lastTopAnimation?.cancel();
            Glide.with(_backgroundTop)
                .load(color.toDrawable())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(_backgroundTop);

            _lastTopAnimation = _backgroundTop.animate()
                .scaleY(heightScale)
                .setDuration(500)
                .alpha(1f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .let {
                    it.start();
                    return@let it;
                }
        }
    }
    fun hideBackgroundTop() {
        _lastTopAnimation?.cancel();
        if(_backgroundTop.scaleY != 0f) {
            if(_backgroundTop.scaleY <= 0.5f) {
                _backgroundTop.animate()
                    .scaleY(0f)
                    .alpha(0f)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationCancel(p0: Animator) {}
                        override fun onAnimationEnd(p0: Animator) {
                            _lastTopAnimation?.setListener(null);
                            _backgroundTop.setBackgroundColor(Color.BLACK);
                        }

                        override fun onAnimationRepeat(p0: Animator) {}
                        override fun onAnimationStart(p0: Animator) {}
                    })
                    .setDuration(_backgroundTransitionDuration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start();
            }
            else {
                _backgroundTop.animate()
                    .alpha(0f)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationCancel(p0: Animator) {}
                        override fun onAnimationEnd(p0: Animator) {
                            _lastTopAnimation?.setListener(null);
                            _backgroundTop.scaleY = 0f;
                            _backgroundTop.setBackgroundColor(Color.BLACK);
                        }

                        override fun onAnimationRepeat(p0: Animator) {}
                        override fun onAnimationStart(p0: Animator) {}
                    })
                    .setDuration(_backgroundTransitionDuration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start();
            }
        }
    }
    private var _lastBottomAnimation: ViewPropertyAnimator? = null;
    fun setBackgroundBottomGradient(color: Int, heightScale: Float = 0.5f, opacity: Float = 1f)
        = setBackgroundBottom(android.graphics.Color.TRANSPARENT.toGradientDrawable(color,
        GradientDrawable.Orientation.TOP_BOTTOM), heightScale, opacity)
    fun setBackgroundBottom(drawable: Drawable, heightScale: Float = 1f, opacity: Float = 1f) {
        _lastBottomAnimation?.cancel();
        Glide.with(_backgroundBottom)
            .load(drawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(_backgroundBottom);

       _lastBottomAnimation =  _backgroundBottom.animate()
            .scaleY(heightScale)
            .setDuration(_backgroundTransitionDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .alpha(opacity)
            .let {
                it.start();
                return@let it;
            }
    }
    fun setBackgroundBottomColor(color: Int, heightScale: Float = 1f) {
        if(heightScale == 0f && color == Color.BLACK) {
            hideBackgroundBottom();
        }
        else {
            _lastBottomAnimation?.cancel();
            Glide.with(_backgroundBottom)
                .load(color.toDrawable())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(_backgroundBottom);
            _lastBottomAnimation = _backgroundBottom.animate()
                .scaleY(heightScale)
                .setDuration(_backgroundTransitionDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .let {
                    it.start();
                    return@let it;
                }
        }
    }
    fun hideBackgroundBottom() {
        _lastBottomAnimation?.cancel();
        if(_backgroundBottom.scaleY != 0f)
            _lastBottomAnimation = _backgroundBottom.animate()
                .scaleY(0f)
                .alpha(0f)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object: Animator.AnimatorListener {
                    override fun onAnimationCancel(p0: Animator) { }
                    override fun onAnimationEnd(p0: Animator) {
                        _lastBottomAnimation?.setListener(null);
                        _backgroundBottom.setBackgroundColor(Color.BLACK);
                    }
                    override fun onAnimationRepeat(p0: Animator) {}
                    override fun onAnimationStart(p0: Animator) {}
                })
                .setDuration(500)
                .let {
                    it.start();
                    return@let it;
                }
    }


    fun handleBack() {
        Logger.i(TAG, "onBackPressed")

        //if (_fragBotBarMenu.onBackPressed())
        //    return;

        if (!(fragCurrent?.onBackPressed() ?: true))
            closeSegment();

    }


    //#region Navigation

    inline fun <reified T : Fragment> navigate(parameter: Any? = null, withHistory: Boolean = true, isBack: Boolean = false) {
        val fragment = getFragment<T>();
        if(fragment is MainFragment)
            navigate(fragment, parameter, withHistory, isBack);
    }

    /**
     * Navigate takes a MainFragment, and makes them the current main visible view
     * A parameter can be provided which becomes available in the onShow of said fragment
     */
    @SuppressLint("CommitTransaction")
    fun navigate(segment: MainFragment, parameter: Any? = null, withHistory: Boolean = true, isBack: Boolean = false) {
        //Logger.i(TAG, "Navigate to $segment (parameter=$parameter, withHistory=$withHistory, isBack=$isBack)")

        if (segment != fragCurrent) {
            fragCurrent?.onHide();

            setBackgroundTopColor(Color.BLACK, 0f);
            setBackgroundBottomColor(Color.BLACK, 0f);

            if (segment.isMainView) {
                var transaction = supportFragmentManager.beginTransaction();
                if (segment.topBar != null) {
                    if (segment.topBar != fragCurrent?.topBar) {
                        transaction = transaction
                            .show(segment.topBar as Fragment)
                            .replace(R.id.fragment_top_bar, segment.topBar as Fragment);
                        fragCurrent?.topBar?.onHide();
                    }
                } else if (fragCurrent?.topBar != null)
                    transaction.hide(fragCurrent?.topBar as Fragment);

                segment.topBar?.let {
                    it.onShowFragment(segment);
                }

                val definitionOld = fragmentsMain.values.find { it.get() == fragCurrent };
                val definitionNew = fragmentsMain.values.find { it.get() == segment };
                //if(definitionOld?.animExit != null || definitionNew?.animEnter != null)
                 transaction = transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);

                transaction = transaction.replace(R.id.fragment_main, segment);

                if(!segment.hasBottomBar) {
                    if(fragCurrent?.hasBottomBar ?: false)
                        transaction = transaction.hide(_fragBotMenu);
                }
                else {
                    if(fragCurrent?.hasBottomBar ?: false)
                        transaction = transaction
                            .hide(fragCurrent!!.botBar!!)
                            .show(segment.botBar!!)
                            .replace(R.id.fragment_bottom_bar, segment.botBar!!);
                    else if(segment.botBar != fragCurrent?.botBar)
                        transaction = transaction
                            .show(segment.botBar!!)
                            .replace(R.id.fragment_bottom_bar, segment.botBar!!)
                }
                transaction.commitNow();

                if(isConnectedTop != definitionNew?.connectTop) {
                    isConnectedTop = !isConnectedTop;
                    if(isConnectedTop) {
                        val constraintSet = ConstraintSet();
                        constraintSet.clone(_rootView);
                        constraintSet.connect(R.id.fragment_main, ConstraintSet.TOP, ConstraintSet.PARENT_ID,ConstraintSet.TOP, 0);
                        constraintSet.applyTo(_rootView);
                    }
                    else {
                        val constraintSet = ConstraintSet();
                        constraintSet.clone(_rootView);
                        constraintSet.connect(R.id.fragment_main, ConstraintSet.TOP, R.id.fragment_top_bar,ConstraintSet.BOTTOM, 0);
                        constraintSet.applyTo(_rootView);
                    }
                }
            } else {

                if (!segment.hasBottomBar) {
                    supportFragmentManager.beginTransaction()
                        .hide(_fragBotMenu)
                        .commitNow();
                }
            }

            if (fragCurrent?.isHistory ?: false && withHistory && _queue.lastOrNull() != fragCurrent)
                _queue.add(Pair(fragCurrent!!, _parameterCurrent));

            if (segment.isOverlay && !(fragCurrent?.isOverlay ?: false) && withHistory)// && fragCurrent.isHistory)
                fragBeforeOverlay = fragCurrent;

            fragCurrent = segment;
            _parameterCurrent = parameter;
        }

        segment.topBar?.onShown(parameter);
        segment.onShown(parameter, isBack);
        onNavigated.emit(segment);
    }

    /**
     * Called when the current segment (main) should be closed, if already at a root view (tab), close application
     * If called with a non-null fragment, it will only close if the current fragment is the provided one
     */
    fun closeSegment(fragment: MainFragment? = null) {
        if ((fragment?.isOverlay ?: false) && fragBeforeOverlay != null) {
            navigate(fragBeforeOverlay!!, null, false, true);
        } else {
            val last = _queue.lastOrNull();
            if (last != null) {
                _queue.remove(last);
                navigate(last.first, last.second, false, true);
            } else {
                if (false) { //Is not playing? kill
                    Logger.i(TAG, "Closing activity because _fragVideoDetail.state == closed");
                    finish();
                } else {
                    moveTaskToBack(false);
                }
            }
        }
    }
    inline fun <reified T : Fragment> getFragment(): T {
        val clazz = T::class;
        if(fragmentsMain.containsKey(clazz))
            return fragmentsMain[clazz]!!.get.invoke() as T;
        throw IllegalArgumentException("Fragment type ${T::class.java.name} is not available in MainActivity");
    }
    //#endregion

    //#region Permissions
    var _callbackPermissionAudio: ((Boolean)->Unit)? = null;
    val permissionReqAudio = registerForActivityResult(ActivityResultContracts.RequestPermission(), { isGranted ->
        //Clear syncs after
        StateLibrary.instance.resetSyncs();
        _callbackPermissionAudio?.invoke(isGranted);
    });
    fun requestPermissionAudio(cb: ((Boolean)->Unit)? = null) {
        _callbackPermissionAudio = cb;
        permissionReqAudio.launch(android.Manifest.permission.READ_MEDIA_AUDIO);

    }

    fun hasAudioPermission(handler: ((Boolean)->Unit)) {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                handler.invoke(true);
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_AUDIO) -> {
                requestPermissionAudio {
                    handler.invoke(it);
                }
            }
            else -> {
                requestPermissionAudio {
                    handler.invoke(it);
                }
            }
        }
    }
    fun requestPermissions(handler: ((Boolean)->Unit)?) {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                handler?.invoke(true);
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_AUDIO) -> {
                requestPermissionAudio {
                    handler?.invoke(it);
                }
            }
            else -> {
                requestPermissionAudio {
                    handler?.invoke(it);
                }
            }
        }
    }

    //#endregion

    //#region Overlay

    fun showPlayableOverlay(playable: IPlayable) {
        lifecycleScope.launch(Dispatchers.Main) {
            _overlayPlayable.show(playable);
        }
    }

    private val _toastQueue = ConcurrentLinkedQueue<ToastView.Toast>();
    private var _toastJob: Job? = null;
    fun showAppToast(toast: ToastView.Toast) {
        synchronized(_toastQueue) {
            _toastQueue.add(toast);
            if (_toastJob?.isActive != true)
                _toastJob = lifecycleScope.launch(Dispatchers.Default) {
                    launchAppToastJob();
                };
        }
    }
    private suspend fun launchAppToastJob() {
        Logger.i(TAG, "Starting appToast loop");
        while (!_toastQueue.isEmpty()) {
            val toast = _toastQueue.poll() ?: continue;
            Logger.i(TAG, "Showing next toast (${toast.msg})");

            lifecycleScope.launch(Dispatchers.Main) {
                if (!_toastView.isVisible) {
                    Logger.i(TAG, "First showing toast");
                    _toastView.setToast(toast);
                    _toastView.show(true);
                } else {
                    _toastView.setToastAnimated(toast);
                }
            }
            if (toast.long)
                delay(5000);
            else
                delay(2500);
        }
        Logger.i(TAG, "Ending appToast loop");
        lifecycleScope.launch(Dispatchers.Main) {
            _toastView.hide(true) {
            };
        }
    }
    //#endregion

    fun createPlayer(playerCallback: (PlayerManager)->Unit) {

        val sessionToken = SessionToken(this.applicationContext, ComponentName(this.applicationContext, PlaybackService::class.java))
        val mediacontrollerFuture = MediaController
            .Builder(this.applicationContext, sessionToken)
            .buildAsync()

        mediacontrollerFuture.addListener({
            val player = mediacontrollerFuture.get();
            val fullPlayer = PlaybackService.getLastPlayer();

            val playerManager = PlayerManager(fullPlayer ?: player);
            StateQueue.instance.setPlayer(playerManager);
            playerCallback.invoke(playerManager);
        }, MoreExecutors.directExecutor());

    }


    companion object {
        val TAG = "MainActivity";
    }

    class FragmentDefinition(
        val topbar: TopFragment?,
        val botbar: MenuBottomBarFragment?,
        val get: ()-> MainFragment,
        val animEnter: Int? = null,
        val animExit: Int? = null,
        val animePopEnter: Int? = null,
        val animPopExit: Int? = null,
        val connectTop: Boolean = false
    )
}