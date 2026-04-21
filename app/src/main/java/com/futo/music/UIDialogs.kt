package com.futo.music

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.os.Build
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.extensions.assume
import com.futo.music.logging.Logger
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBAlbum
import com.futo.music.storage.db.DBArtist
import com.futo.music.storage.db.DBPlaylist
import com.futo.music.storage.db.DBTrack
import com.futo.music.ui.adapters.AnyInsertedAdapterView.Companion.asAnyWithViews
import com.futo.music.ui.buttons.PillButton
import com.futo.music.ui.dialogs.ProgressDialog
import com.futo.music.ui.viewholders.ListPlaylistToggleViewHolder
import com.futo.music.ui.viewholders.ListPlaylistViewHolder
import com.futo.music.ui.views.SheetBar
import com.futo.music.ui.views.containers.PlaylistsToggleView
import com.futo.music.ui.views.playback.PlayableOptionOverlay
import com.futo.music.ui.views.playback.PlayableOptionsView
import com.futo.music.ui.views.toasts.ToastView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.map
import kotlin.collections.toList
import kotlin.let

class UIDialogs {
    companion object {
        private val TAG = "Dialogs"

        private val _openDialogs = arrayListOf<AlertDialog>();

        private fun registerDialogOpened(dialog: AlertDialog) {
            _openDialogs.add(dialog);
        }

        private fun registerDialogClosed(dialog: AlertDialog) {
            _openDialogs.remove(dialog);
        }


        fun showDialogProgress(context: Context, handler: ((ProgressDialog)->Unit)) {
            val dialog = ProgressDialog(context, handler);
            registerDialogOpened(dialog);
            dialog.setOnDismissListener {
                registerDialogClosed(dialog)
            };
            dialog.show();
        }

        fun dismissAllDialogs() {
            for (openDialog in _openDialogs) {
                openDialog.dismiss();
            }

            _openDialogs.clear();
        }

        fun overlayPlayable(playable: IPlayable) {
            StateApp.instance.activity()?.let {
                var dialog: BottomSheetDialog? = null;
                val overlay = PlayableOptionsView(it);
                overlay.setPlayable(playable)
                overlay.onHide.subscribe {
                    dialog?.hide();
                }
                dialog = showSheet(it, overlay, addTopHandle = true)
            }
        }
        fun overlayPlayableOld(playable: IPlayable) {
            StateApp.instance.activity()?.let {
                it.showPlayableOverlay(playable);
            }
        }

        fun toast(context : Context, text : String, long : Boolean = false) {
            Toast.makeText(context, text, if(long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show();
        }
        fun toast(text : String, long : Boolean = false) {
            StateApp.instance.scopeOrNull?.launch(Dispatchers.Main) {
                try {
                    RootApplication.applicationContext.let {
                        toast(it, text, long);
                    }
                } catch (e: Throwable) {
                    Logger.e(TAG, "Failed to show toast.", e);
                }
            }
        }
        fun appToast(text: String, long: Boolean = false) {
            appToast(ToastView.Toast(text, long))
        }
        fun appToastError(text: String, long: Boolean) {
            RootApplication.applicationContext.let {
                appToast(ToastView.Toast(text, long, it.getColor(R.color.pastel_red)));
            };
        }
        fun appToast(toast: ToastView.Toast) {
            StateApp.instance.activity()?.let {
                it.showAppToast(toast);
            }
        }

        fun showRatingDialog(context: Context, scope: CoroutineScope, track: DBTrack? = null, playlist: DBPlaylist? = null, album: DBAlbum? = null, artist: DBArtist? = null, onDismissed: (()->Unit)? = null) {
            scope.launch(Dispatchers.IO) {
                if(track == null && playlist == null && album == null && artist == null) {
                    appToast("Unknown Item");
                    return@launch;
                }
                withContext(Dispatchers.Main) {
                    val builder = AlertDialog.Builder(context);
                    val view = LayoutInflater.from(context).inflate(R.layout.dialog_rating, null);
                    builder.setView(view);
                    builder.setCancelable(true);

                    val dialog = builder.create();
                    registerDialogOpened(dialog);


                    var item = if(track !=  null)
                        track
                    else if(playlist != null)
                        playlist
                    else if(album != null)
                        album
                    else if(artist != null)
                        artist;
                    else throw IllegalStateException("No item to rate?");

                    val textItem = view.findViewById<TextView>(R.id.text_item);

                    val buttonTrack = view.findViewById<Button>(R.id.button_track);
                    val buttonPlaylist = view.findViewById<Button>(R.id.button_playlist);
                    val buttonAlbum = view.findViewById<Button>(R.id.button_album);
                    val buttonArtist = view.findViewById<Button>(R.id.button_artist);
                    val buttonsTypes = listOf(buttonTrack, buttonPlaylist, buttonAlbum, buttonArtist);

                    val buttonStars = listOf(
                        view.findViewById<ImageButton>(R.id.button_star_1),
                        view.findViewById<ImageButton>(R.id.button_star_2),
                        view.findViewById<ImageButton>(R.id.button_star_3),
                        view.findViewById<ImageButton>(R.id.button_star_4),
                        view.findViewById<ImageButton>(R.id.button_star_5),
                    )

                    fun updateStarUI(rating: Int) {
                        buttonStars.forEachIndexed { index, button ->
                            button.apply {
                                if (rating > 0 && rating > index * 20)
                                    button.setImageResource(androidx.media3.session.R.drawable.media3_icon_star_filled);
                                else
                                    button.setImageResource(androidx.media3.session.R.drawable.media3_icon_star_unfilled);
                            }
                        }
                    }
                    fun updateSelectUI() {
                        val toSelect = if(item is DBTrack)
                            buttonTrack;
                        else if(item is DBPlaylist)
                            buttonPlaylist;
                        else if(item is DBAlbum)
                            buttonAlbum;
                        else if(item is DBArtist)
                            buttonArtist
                        else null;
                        buttonsTypes.forEach {
                            if(it == toSelect)
                                it.setBackgroundResource(R.drawable.background_button_accent);
                            else
                                it.setBackgroundResource(R.drawable.background_button_black);
                        }
                        textItem.text = item.name;
                    }

                    if(track == null)
                        buttonTrack.isVisible = false;
                    else
                    {
                        buttonTrack.isVisible = true;
                        buttonTrack.setOnClickListener {
                            item = track;
                            updateSelectUI();
                            updateStarUI(item.score ?: 0);
                        }
                    }
                    if(playlist == null)
                        buttonPlaylist.isVisible = false;
                    else
                    {
                        buttonPlaylist.isVisible = true;
                        buttonPlaylist.setOnClickListener {
                            item = playlist
                            updateSelectUI();
                            updateStarUI(item.score ?: 0);
                        }
                    }
                    if(album == null)
                        buttonAlbum.isVisible = false;
                    else
                    {
                        buttonAlbum.isVisible = true;
                        buttonAlbum.setOnClickListener {
                            item = album;
                            updateSelectUI();
                            updateStarUI(item.score ?: 0);
                        }
                    }
                    if(artist == null)
                        buttonArtist.isVisible = false;
                    else
                    {
                        buttonArtist.isVisible = true;
                        buttonArtist.setOnClickListener {
                            item = artist;
                            updateSelectUI();
                            updateStarUI(item.score ?: 0);
                        }
                    }

                    updateSelectUI();
                    updateStarUI(item.score ?: 0);
                    buttonStars.forEachIndexed { index, button ->
                        button.apply {
                            this.setOnClickListener {
                                val rating = (index + 1) * 20;
                                updateStarUI(rating);
                                val currentItem = item;
                                currentItem.score = rating;
                                scope.launch(Dispatchers.IO) {
                                    val result = if (currentItem is DBAlbum)
                                        StateDatabase.instance.setRatingAlbum(currentItem.id, rating);
                                    else if (currentItem is DBArtist)
                                        StateDatabase.instance.setRatingArtist(currentItem.id, rating);
                                    else if (currentItem is DBPlaylist)
                                        StateDatabase.instance.setRatingPlaylist(currentItem.id, rating);
                                    else if (currentItem is DBTrack)
                                        StateDatabase.instance.setRatingTrack(currentItem.id, rating) != null;
                                    else false
                                    if(!result) {
                                        appToast("Failed to update rating");
                                    }
                                }
                            }
                        }
                    }

                    dialog.setOnDismissListener {
                        registerDialogClosed(dialog);
                        onDismissed?.invoke();
                    }
                    dialog.show();
                }
            }
        }

        fun showAddToPlaylistDialog(context: Context, scope: CoroutineScope, reason: String, track: DBTrack) {
            scope.launch(Dispatchers.IO) {
                var dialog: BottomSheetDialog? = null;

                val trackArt = StateDatabase.instance.getTrackAlbumArt(track.id);
                val playlists = StateDatabase.instance.getPlaylistsByRecent()
                val partOf = StateDatabase.instance.getTrackPlaylists(track.id);
                val list = playlists.map { ListPlaylistToggleViewHolder.Item(it, partOf.any{ part -> part.id == it.id}) }.sortedBy { if(it.added) 1 else 0 };
                withContext(Dispatchers.Main) {
                    val togglesView = PlaylistsToggleView(context, "New Playlist", {
                        dialog?.hide();
                        showCreatePlaylistDialog(context, scope) {
                            showAddToPlaylistDialog(context, scope, reason, track);
                        }
                    });
                    togglesView.onPlaylistToggleChanged.subscribe { item ->
                        scope.launch(Dispatchers.IO) {
                            if(!item.added) {
                                StateDatabase.instance.removeTrackFromPlaylist(item.playlist.id, track.id);
                                StateDatabase.instance.updatePlaylistMetadata(item.playlist.id);
                                StateDatabase.instance.onLibraryUpdated.emit();
                            }
                            else {
                                val id = StateDatabase.instance.addTrackToPlaylist(item.playlist.id, track.id);
                                if (id > 0) {

                                    StateDatabase.instance.updatePlaylistMetadata(item.playlist.id);
                                    StateDatabase.instance.onLibraryUpdated.emit();
                                }
                            }
                            withContext(Dispatchers.Main) {

                            }
                        }
                    }
                    togglesView.setPlaylists(list);
                    togglesView.setTopViews(listOf(
                        SheetBar(context),
                        TextView(context).apply {
                            this.text = "Playlists"
                            this.textSize = 6.dp(resources).toFloat();
                            this.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            this.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                this.setMargins(0,0,0,10.dp(resources));
                            }
                        },
                        TextView(context).apply {
                            this.text = reason
                            this.textSize = 4.dp(resources).toFloat();
                            this.setTextColor(Color.rgb(150, 150, 150));
                            this.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            this.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                                this.setMargins(0,0,0,15.dp(resources));
                            }
                        }
                    ));
                    dialog = showSheet(context, togglesView);
                }
            }
        }
        fun showAddToPlaylistDialogOld(context: Context, scope: CoroutineScope, reason: String, track: DBTrack) {
            scope.launch(Dispatchers.IO) {
                val trackArt = StateDatabase.instance.getTrackAlbumArt(track.id);
                val playlists = StateDatabase.instance.getPlaylistsByRecent()
                val partOf = StateDatabase.instance.getTrackPlaylists(track.id);
                val list = playlists.map { ListPlaylistViewHolder.Item(it, partOf.any{ part -> part.id == it.id}) };
                withContext(Dispatchers.Main) {
                    var dialog: AlertDialog? = null;
                    dialog = showAdapterDialog(context, "Select a playlist", reason, {
                        it.asAnyWithViews<ListPlaylistViewHolder.Item, ListPlaylistViewHolder>( ArrayList(list), arrayListOf(), arrayListOf(
                            LinearLayout(context)
                                .apply {
                                    val button = PillButton(context, null)
                                        .withIcon(androidx.media3.session.R.drawable.media3_icon_plus)
                                        .withText("Add")
                                    this.addView(button);
                                    this.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                                    this.gravity = Gravity.RIGHT;

                                    button.onClick.subscribe {
                                        dialog?.hide();
                                        showCreatePlaylistDialog(context, scope) {
                                            showAddToPlaylistDialog(context, scope, reason, track);
                                        }
                                    }
                                }

                        ), RecyclerView.VERTICAL, false, { view ->
                            view.onClick.subscribe { view, item ->
                                scope.launch(Dispatchers.IO) {
                                    if(item.added) {
                                        StateDatabase.instance.removeTrackFromPlaylist(item.playlist.id, track.id);
                                        item.added = false;

                                        StateDatabase.instance.updatePlaylistMetadata(item.playlist.id);
                                        StateDatabase.instance.onLibraryUpdated.emit();
                                    }
                                    else {
                                        val id = StateDatabase.instance.addTrackToPlaylist(item.playlist.id, track.id);
                                        if (id > 0) {
                                            UIDialogs.appToast("Added to [${item.playlist.name}]");
                                            item.added = true;

                                            StateDatabase.instance.updatePlaylistMetadata(item.playlist.id);
                                            StateDatabase.instance.onLibraryUpdated.emit();
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        view.updateAdded();
                                    }
                                }
                            }
                        });
                    });
                }
            }
        }
        fun showCreatePlaylistDialog(context: Context, scope: CoroutineScope, onResult: (Long?)->Unit) {

            val dialog = UIDialogs.showDialog(context, R.drawable.ic_playlist, false, "New Playlist", "Enter a name for your new playlist", null, "", "Playlist name...", 0,
                UIDialogs.Action("Cancel", {
                    onResult(null);
                }, UIDialogs.ActionStyle.NONE, true),
                UIDialogs.Action.withInput("Create", { result ->

                    if(result?.text.isNullOrBlank()) {
                        UIDialogs.appToast("No name provided for playlist");
                        return@withInput;
                    }
                    scope.launch(Dispatchers.IO) {
                        val id = StateDatabase.instance.createPlaylist(result.text);

                        withContext(Dispatchers.Main) {
                            onResult(id);
                        }
                    }
                }, UIDialogs.ActionStyle.PRIMARY, true));
        }

        fun showSheet(context: Context, view: View, onClose: (()->Unit)? = null, addTopHandle: Boolean = false): BottomSheetDialog? {
            return StateApp.instance.activity()?.let {
                val dialog = BottomSheetDialog(context);

                var viewToUse = if(addTopHandle) {
                    val linear = LinearLayout(context);
                    linear.orientation = LinearLayout.VERTICAL;
                    linear.addView(SheetBar(context))
                    linear.addView(view);
                    linear;
                } else view;

                dialog.setContentView(viewToUse);
                dialog.show();

                //TODO: add to theme
                val background = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet);
                background?.setBackgroundResource(R.drawable.background_sheet);

                try {
                    dialog.window?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            it.setDecorFitsSystemWindows(false)
                        };
                    }
                }catch(ex: Throwable){}

                ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(0, 0, 0, systemBars.bottom) // Add the nav bar height as padding
                    insets
                }
                return@let dialog;
            }
        }

        fun showAdapterDialog(context: Context, title: String, textDetails: String, prepareRecycler: (RecyclerView)->Unit): AlertDialog {
            val builder = AlertDialog.Builder(context);
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_select_adapter_item, null);
            builder.setView(view);
            builder.setCancelable(true);

            val dialog = builder.create();
            registerDialogOpened(dialog);

            view.findViewById<TextView>(R.id.dialog_text).apply {
                if (title == null)
                    this.visibility = View.GONE;
                else {
                    this.text = title;
                }
            };
            view.findViewById<TextView>(R.id.dialog_text_details).apply {
                if (textDetails == null)
                    this.visibility = View.GONE;
                else {
                    this.text = textDetails;
                }
            };
            view.findViewById<RecyclerView>(R.id.recycler).apply {
                prepareRecycler(this);
            }


            dialog.setOnDismissListener {
                registerDialogClosed(dialog);
            }
            dialog.show();
            return dialog;
        }

        fun showDialog(context: Context, icon: Int, text: String, textDetails: String? = null, code: String? = null, defaultCloseAction: Int, vararg actions: Action): AlertDialog {
            return showDialog(context, icon, false, text, textDetails, code, defaultCloseAction, *actions);
        }
        fun showDialog(context: Context, icon: Int, animated: Boolean, text: String, textDetails: String? = null, code: String? = null, defaultCloseAction: Int, vararg actions: Action): AlertDialog
                = showDialog(context, icon, animated, text, textDetails, code, null, null, defaultCloseAction, *actions);

        fun showDialog(context: Context, icon: Int, animated: Boolean, text: String, textDetails: String? = null, code: String? = null, input: String?, placeholder: String?, defaultCloseAction: Int, vararg actions: Action): AlertDialog {
            val builder = AlertDialog.Builder(context);
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_multi_button, null);
            builder.setView(view);
            builder.setCancelable(defaultCloseAction > -2);
            val dialog = builder.create();
            registerDialogOpened(dialog);

            view.findViewById<ImageView>(R.id.dialog_icon).apply {
                this.setImageResource(icon);
                if(animated)
                    this.drawable.assume<Animatable, Unit> { it.start() };
            }
            view.findViewById<TextView>(R.id.dialog_text).apply {
                this.text = text;
            };
            view.findViewById<TextView>(R.id.dialog_text_details).apply {
                if (textDetails == null)
                    this.visibility = View.GONE;
                else {
                    this.text = textDetails;
                }
            };
            var inputView = view.findViewById<TextView>(R.id.dialog_text_input);
            fun hideKeyboard() {
                val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager?
                    ?: return;
                inputManager?.hideSoftInputFromWindow(inputView.windowToken, 0);
                inputView.clearFocus();
            }
            inputView.apply {
                if (input == null && placeholder == null) this.visibility = View.GONE;
                else {
                    this.text = input ?: "";
                    this.hint = placeholder ?: "";
                    this.visibility = View.VISIBLE;
                    this.textAlignment = if(actions.any { it.center }) View.TEXT_ALIGNMENT_CENTER else View.TEXT_ALIGNMENT_TEXT_START
                }
                this.addTextChangedListener {
                    if(it == null)
                        return@addTextChangedListener;
                    for (i in it.length - 1 downTo 0) {
                        if (it[i] === '\n') {
                            it.delete(i, i + 1)
                            hideKeyboard()
                            return@addTextChangedListener;
                        }
                    }
                }
            };
            view.findViewById<TextView>(R.id.dialog_text_code).apply {
                if (code == null) this.visibility = View.GONE;
                else {
                    this.text = code;
                    this.movementMethod = ScrollingMovementMethod.getInstance();
                    this.visibility = View.VISIBLE;
                    this.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
            };
            view.findViewById<LinearLayout>(R.id.dialog_buttons).apply {
                val center = actions.any { it?.center == true };
                val buttons = actions.map<Action, TextView> { act ->
                    val buttonView = TextView(context);
                    val dp10 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt();
                    val dp28 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics).toInt();
                    val dp14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14.0f, resources.displayMetrics).toInt();
                    buttonView.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        this.marginStart = if(actions.size >= 2) dp14 / 2 else dp28 / 2;
                        this.marginEnd = if(actions.size >= 2) dp14 / 2 else dp28 / 2;
                    };
                    buttonView.setTextColor(Color.WHITE);
                    buttonView.textSize = 14f;
                    buttonView.typeface = resources.getFont(R.font.inter_regular);
                    buttonView.text = act.text;
                    buttonView.setOnClickListener { act.invokeAction(DialogResult(inputView?.text?.toString())); dialog.dismiss(); };
                    when(act.style) {
                        ActionStyle.PRIMARY -> buttonView.setBackgroundResource(R.drawable.background_button_primary);
                        ActionStyle.ACCENT -> buttonView.setBackgroundResource(R.drawable.background_button_accent);
                        ActionStyle.DANGEROUS -> buttonView.setBackgroundResource(R.drawable.background_button_pred);
                        ActionStyle.DANGEROUS_TEXT -> buttonView.setTextColor(ContextCompat.getColor(context, R.color.pastel_red))
                        else -> buttonView.setTextColor(ContextCompat.getColor(context, R.color.white))
                    }
                    val paddingSpecialButtons = if(actions.size > 2) dp14 else dp28;
                    if(act.style != ActionStyle.NONE && act.style != ActionStyle.DANGEROUS_TEXT)
                        buttonView.setPadding(paddingSpecialButtons, dp10, paddingSpecialButtons, dp10);
                    else
                        buttonView.setPadding(dp10, dp10, dp10, dp10);

                    return@map buttonView;
                };
                if(actions.size <= 1 || center)
                    this.gravity = Gravity.CENTER;
                else
                    this.gravity = Gravity.END;
                for(button in buttons)
                    this.addView(button);
            };
            dialog.setOnCancelListener {
                if(defaultCloseAction >= 0 && defaultCloseAction < actions.size)
                    actions[defaultCloseAction].invokeAction(DialogResult(inputView?.text?.toString()));
            }
            dialog.setOnDismissListener {
                registerDialogClosed(dialog);
            }
            dialog.show();
            return dialog;
        }
        fun showDialogVertical(context: Context, icon: Int, animated: Boolean, text: String, textDetails: String? = null, code: String? = null, input: String?, placeholder: String?, defaultCloseAction: Int, vararg actions: Action): AlertDialog {
            val builder = AlertDialog.Builder(context);
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_multi_button, null);
            builder.setView(view);
            builder.setCancelable(defaultCloseAction > -2);
            val dialog = builder.create();
            registerDialogOpened(dialog);

            view.findViewById<ImageView>(R.id.dialog_icon).apply {
                this.setImageResource(icon);
                if(animated)
                    this.drawable.assume<Animatable, Unit> { it.start() };
                if(icon <= 0)
                    this.isVisible = false;
            }
            view.findViewById<TextView>(R.id.dialog_text).apply {
                this.text = text;
            };
            view.findViewById<TextView>(R.id.dialog_text_details).apply {
                if (textDetails == null)
                    this.visibility = View.GONE;
                else {
                    this.text = textDetails;
                }
            };
            var inputView = view.findViewById<TextView>(R.id.dialog_text_input);
            inputView.apply {
                if (input == null && placeholder == null) this.visibility = View.GONE;
                else {
                    this.text = input ?: "";
                    this.hint = placeholder ?: "";
                    this.visibility = View.VISIBLE;
                    this.textAlignment = if(actions.any { it.center }) View.TEXT_ALIGNMENT_CENTER else View.TEXT_ALIGNMENT_TEXT_START
                }
            };
            view.findViewById<TextView>(R.id.dialog_text_code).apply {
                if (code == null) this.visibility = View.GONE;
                else {
                    this.text = code;
                    this.movementMethod = ScrollingMovementMethod.getInstance();
                    this.visibility = View.VISIBLE;
                    this.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                }
            };
            view.findViewById<LinearLayout>(R.id.dialog_buttons).apply {

                val center = actions.any { it?.center == true };
                val buttons = actions.map<UIDialogs.Action, TextView> { act ->
                    val buttonView = TextView(context);
                    val dp10 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt();
                    val dp28 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics).toInt();
                    val dp14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14.0f, resources.displayMetrics).toInt();
                    buttonView.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        this.marginStart = if(actions.size >= 2) dp14 / 2 else dp28 / 2;
                        this.marginEnd = if(actions.size >= 2) dp14 / 2 else dp28 / 2;
                    };
                    buttonView.setTextColor(Color.WHITE);
                    buttonView.textSize = 14f;
                    buttonView.typeface = resources.getFont(R.font.inter_regular);
                    buttonView.text = act.text;
                    buttonView.setOnClickListener { act.invokeAction(DialogResult(inputView?.text?.toString())); dialog.dismiss(); };
                    when(act.style) {
                        UIDialogs.ActionStyle.PRIMARY -> buttonView.setBackgroundResource(R.drawable.background_button_primary);
                        UIDialogs.ActionStyle.ACCENT -> buttonView.setBackgroundResource(R.drawable.background_button_accent);
                        UIDialogs.ActionStyle.DANGEROUS -> buttonView.setBackgroundResource(R.drawable.background_button_pred);
                        UIDialogs.ActionStyle.DANGEROUS_TEXT -> buttonView.setTextColor(ContextCompat.getColor(context, R.color.pastel_red))
                        else -> buttonView.setTextColor(ContextCompat.getColor(context, R.color.white))
                    }
                    val paddingSpecialButtons = if(actions.size > 2) dp14 else dp28;
                    if(act.style != UIDialogs.ActionStyle.NONE && act.style != UIDialogs.ActionStyle.DANGEROUS_TEXT)
                        buttonView.setPadding(paddingSpecialButtons, dp10, paddingSpecialButtons, dp10);
                    else
                        buttonView.setPadding(dp10, dp10, dp10, dp10);

                    return@map buttonView;
                };
                this.orientation = LinearLayout.VERTICAL;
                this.gravity = Gravity.CENTER;
                for(button in buttons)
                    this.addView(button);
            };
            dialog.setOnCancelListener {
                if(defaultCloseAction >= 0 && defaultCloseAction < actions.size)
                    actions[defaultCloseAction].invokeAction(DialogResult(inputView?.text?.toString()));
            }
            dialog.setOnDismissListener {
                registerDialogClosed(dialog);
            }
            dialog.show();
            return dialog;
        }
    }

    class Descriptor(val icon: Int, val text: String, val textDetails: String? = null, val code: String? = null, val defaultCloseAction: Int, vararg acts: Action) {
        var shouldShow: ()->Boolean = {true};
        val actions: List<Action> = acts.toList();

        fun withCondition(shouldShow: () -> Boolean): Descriptor {
            this.shouldShow = shouldShow;
            return this;
        }
    }
    class Action {
        val text: String;
        val action: ((DialogResult?)->Unit);
        val style: ActionStyle;
        var center: Boolean;

        constructor(text: String, action: ()->Unit, style: ActionStyle = ActionStyle.NONE, center: Boolean = false) {
            this.text = text;
            this.action = { action() };
            this.style = style;
            this.center = center;
        }
        protected constructor(text: String, action: (DialogResult?)->Unit, style: ActionStyle = ActionStyle.NONE, center: Boolean = false) {
            this.text = text;
            this.action = action;
            this.style = style;
            this.center = center;
        }

        fun invokeAction(input: DialogResult? = null) {
            this.action(input);
        }

        companion object {
            fun withInput(text: String, action: (DialogResult?)->Unit, style: ActionStyle = ActionStyle.NONE, center: Boolean = false): Action {
                return Action(text, action, style, center);
            }
        }
    }
    class DialogResult(
      val text: String?
    );
    enum class ActionStyle {
        NONE,
        PRIMARY,
        ACCENT,
        DANGEROUS,
        DANGEROUS_TEXT
    }
}