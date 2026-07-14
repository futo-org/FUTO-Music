package com.futo.music.fragments.main

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.PlaySettings
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.dp
import com.futo.music.files.DocumentDirectoryItem
import com.futo.music.files.DocumentFileItem
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.logging.Logger
import com.futo.music.models.ImageVariable
import com.futo.music.models.playable.Vibe
import com.futo.music.openPlayable
import com.futo.music.states.StateApp
import com.futo.music.states.StateDatabase
import com.futo.music.states.StateFiles
import com.futo.music.storage.db.DBDirectory
import com.futo.music.ui.adapters.FilesAdapter
import com.futo.music.ui.adapters.IFileItem
import com.futo.music.ui.buttons.ListButton
import com.futo.music.ui.buttons.StandardButton
import com.futo.music.ui.viewholders.FilesFileViewHolder
import com.futo.music.ui.views.NoResultsView
import com.futo.music.withSettings
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import kotlin.system.measureTimeMillis


class FilesFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = false;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String? get() = "Files"

    private var _view: FragView? = null;



    override fun onShownWithView(parameter: Any?, isBack: Boolean) {
        super.onShownWithView(parameter, isBack);
        _view?.onShown(parameter);
    }

    override fun onHide() {
        super.onHide();
        _view?.onHide();
    }

    override fun onCreateMainView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = FragView(this, inflater);
        _view = view;
        StateFiles.instance.onScanning.subscribe(this) {
            _view?.setScanning(it);
        }
        StateFiles.instance.onScanningFinished.subscribe(this) {
            _view?.setScanning(null);
        }
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        StateFiles.instance.onScanning.remove(this);
        StateFiles.instance.onScanningFinished.remove(this);
        _view = null;
    }


    override fun onBackPressed(): Boolean {
        val view = _view ?: return super.onBackPressed();

        if(view.stack.isEmpty() || view.stack.size == 1)
            return super.onBackPressed();

        view.back();

        return true;
    }


    class FragView(frag: FilesFragment, inflater: LayoutInflater): MainFragView<FilesFragment>(frag, inflater, R.layout.fragment_recycler) {

        val filesAdapter: FilesAdapter;

        val stack = mutableListOf<Pair<IFileItem?, List<IFileItem>>>();
        val buttons: LinearLayout;
        val progressBar: ProgressBar;

        val emptyView: NoResultsView;

        init {
            val recycler = findViewById<RecyclerView>(R.id.recycler);

            recycler.setPadding(0, 5.dp(resources), 0, 100.dp(resources));
            recycler.clipToPadding = false;

            buttons = LinearLayout(context).apply {
                this.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 100.dp(resources));

                val buttonPlayAll = StandardButton(context, null)
                    .withIcon(R.drawable.ic_play)
                    .withText("Play All")
                    .withBackground(R.drawable.background_button_primary)
                    .apply {
                        val params = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT);
                        params.weight = 1f;
                        this.layoutParams = params;
                    };
                val buttonShuffle = StandardButton(context, null)
                    .withIcon(R.drawable.ic_shuffle_white)
                    .withText("Shuffle All")
                    .withBackground(R.drawable.background_button_accent)
                    .apply {
                        val params = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT);
                        params.weight = 1f;
                        this.layoutParams = params;
                    };

                buttonPlayAll.onClick.subscribe {
                    val current = stack.lastOrNull() ?: return@subscribe;
                    if(current.first == null)
                        return@subscribe;
                    val allMusic = current?.second?.filter { it is DocumentFileItem && it.track != null }?.map { (it as DocumentFileItem).track!! } ?: return@subscribe;
                    val vibe = Vibe("Folder: " + current.first!!.name, ImageVariable.fromResource(0), listOf(), listOf(), allMusic);
                    fragment.navigate<PlaybackFragment>(vibe);
                }
                buttonShuffle.onClick.subscribe {
                    val current = stack.lastOrNull() ?: return@subscribe;
                    if(current.first == null)
                        return@subscribe;
                    val allMusic = current?.second?.filter { it is DocumentFileItem && it.track != null }?.map { (it as DocumentFileItem).track!! } ?: return@subscribe;
                    val vibe = Vibe("Folder: " + current.first!!.name, ImageVariable.fromResource(0), listOf(), listOf(), allMusic);
                    fragment.navigate<PlaybackFragment>(vibe.withSettings(PlaySettings(shuffle = true)))
                }

                this.addView(buttonPlayAll);
                this.addView(buttonShuffle);
            }
            buttons.isVisible = false;
            findViewById<LinearLayout>(R.id.container_before).addView(buttons);

            progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                this.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 5.dp(resources));
            }
            progressBar.isVisible = false;
            findViewById<LinearLayout>(R.id.container_after).addView(progressBar);

            emptyView = NoResultsView(context, "No Directories Yet", "Add directories using the + icon.\nOnly the root directory of your music has to be added (or multiple).\n\nAdding directories also adds support for:",
                R.drawable.ic_files,
                listOf(
                    LinearLayout(context).apply {
                        this.gravity = Gravity.CENTER;
                        this.orientation = LinearLayout.VERTICAL;
                        addView(UIDialogs.Companion.GuideItemOption(context, "Album Thumbnails (eg. folder.jpg)"));
                        addView(UIDialogs.Companion.GuideItemOption(context, "Playlists (eg. myPlaylist.m3u)"));
                    }
                ))
            emptyView.isVisible = false;
            emptyView.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                this.setMargins(0, 50.dp(resources), 0, 0);
            }
            findViewById<LinearLayout>(R.id.container_before).addView(emptyView);

            val fadeOffset = 10.dp(resources);
            recycler.setFadingEdgeLength(fadeOffset);
            filesAdapter = FilesAdapter({
                it.view.onClick.subscribe { view, item ->
                    open(item);
                }
                it.view.onOptions.subscribe { view, item ->
                    if(item is DBDirectory) {
                        var sheet: BottomSheetDialog? = null;
                        sheet = UIDialogs.showSheetVertical(context, {

                        },
                            ListButton(context).withData(R.drawable.ic_trash, "Delete") {
                                sheet?.dismiss();
                                UIDialogs.showConfirmSheet(context, R.drawable.ic_trash, "Remove [${item.name}]", "Would you like to remove ${item.name} from the app?", {
                                   fragment.lifecycleScope.launch(Dispatchers.IO) {
                                       StateDatabase.instance.db.directoryDao().delete(item.id);
                                       withContext(Dispatchers.Main) {
                                           updateContent();
                                       }
                                   }
                                });
                            }.withMarginBottom(5),
                            ListButton(context).withData(R.drawable.ic_scan, "Rescan") {
                                sheet?.dismiss();
                                fragment.lifecycleScope.launch(Dispatchers.IO) {
                                    val dirs = StateDatabase.instance.db.directoryDao().getAll();
                                    for(scan in dirs)
                                        StateFiles.instance.scanAndProcessDirectory(context, scan);
                                }
                            }.withMarginBottom(5)
                        );
                    }
                    else if(item is DocumentDirectoryItem) {

                    }
                    else if(item is DocumentFileItem) {
                        if(item.track != null) {
                            item.track!!.openPlayable(fragment, true);
                        }
                    }
                }
                if(it.view is FilesFileViewHolder) {
                    it.view.onTrackChanged.subscribe { view, item ->
                        if(!buttons.isVisible && item is DocumentFileItem && item.track != null)
                            buttons.isVisible = true;
                    }
                }
            });
            val mLayoutManager = LinearLayoutManager(context)
            recycler.setLayoutManager(mLayoutManager)
            recycler.adapter = filesAdapter;
        }



        fun updateContent(item: Pair<IFileItem?, List<IFileItem>>? = null) {
            if(item != null) {
                stack.add(item);
                filesAdapter.setData(item.second);
                updateOtherUI();
                emptyView.isVisible = item.second.size == 0
                return;
            }

            val current = stack.removeLastOrNull();
            if(current == null || current.first == null)
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    val items = StateDatabase.instance.db.directoryDao().getAll();
                    stack.add(Pair(null, items));
                    withContext(Dispatchers.Main) {
                        filesAdapter.setData(items);
                    }
                    emptyView.isVisible = items.size == 0
                }
            else if(current.first is DocumentDirectoryItem) {
                val childs = (current.first as DocumentDirectoryItem).getFiles();
                stack.add(Pair(current.first, childs));
                filesAdapter.setData(childs);
                emptyView.isVisible = childs.size == 0
            }
            else if(current.first is DBDirectory) {
                val childs = (current.first as DBDirectory).getFiles(context);
                stack.add(Pair(current.first, childs));
                filesAdapter.setData(childs);
                emptyView.isVisible = childs.size == 0
            }
            updateOtherUI();
        }

        fun updateOtherUI(){
            val hasMusic = stack.lastOrNull()?.second?.any { it is DocumentFileItem && it.track != null } ?: false;
            buttons.isVisible = hasMusic;
        }

        private var lastScanningSet: Long? = null;
        fun setScanning(str: String?) {
            val now = System.currentTimeMillis();
            if(str != null && lastScanningSet != null && ((now - lastScanningSet!!) < 300))
                return;
            lastScanningSet = System.currentTimeMillis();
            fragment.lifecycleScope.launch(Dispatchers.Main) {
                if(str == null)
                    progressBar.isVisible = false;
                else {
                    progressBar.isIndeterminate = true;
                    progressBar.isVisible = true;
                }
            }
        }

        fun open(item: IFileItem) {
            if (item is DBDirectory || item is DocumentDirectoryItem) {
                val childs = if (item is DBDirectory) item.getFiles(context) else if (item is DocumentDirectoryItem) item.getFiles() else return;
                updateContent(Pair(item, childs));
            }
            else if(item is DocumentFileItem) {
                if(item.track != null) {
                    item.track!!.openPlayable(fragment);
                }
            }
            else {
                UIDialogs.appToast("Not implemented yet");
            }
        }
        fun back() {
            stack.removeLast();
            updateContent();
        }

        fun onShown(paramter: Any? = null) {
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setGeneralButton2(R.drawable.ic_help) {
                        UIDialogs.showGuideDialog(context, fragment.lifecycleScope, listOf(
                            UIDialogs.Companion.GuideItem("File Directories", "Access your media from the filesystem directly, as well as providing the app with additional folder metadata.", R.drawable.ic_files),
                            UIDialogs.Companion.GuideItem("Album Thumbnails", "Adding directories that contain your media files allows for scanning for thumbnail images (eg. folder.jpg) to display in the app.", R.drawable.ic_album),
                            UIDialogs.Companion.GuideItem("Playlist Files", "Playlist files are automatically found and synced on startup.\nExpected format:\n\nSomePlaylist.playlist.txt", R.drawable.ic_playlist_add,
                                listOf(
                                    TextView(context).apply {
                                        setBackgroundResource(R.drawable.background_darken_round_4dp_22)
                                        setTypeface(Typeface.MONOSPACE)
                                        setText("//NAME: Some Playlist\nfile1.mp3\nfile2.mp3\n...");
                                        val dp5 = 5.dp(resources);
                                        layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                                            this.setMargins(10.dp(resources), dp5 * -1, 10.dp(resources), 0);
                                        }
                                        setPadding(dp5, dp5, dp5, dp5);
                                    }
                                ))
                        ))
                    }
                    it.setGeneralButton(R.drawable.ic_add) {
                        StateApp.instance.activity()?.pickFolder { uri ->
                            if(uri == null)
                                return@pickFolder;
                            fragment.lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val doc = DocumentFile.fromTreeUri(context, uri);

                                    if (doc?.name == null)
                                        UIDialogs.toast("No document found?");

                                    val item = DBDirectory(
                                        0,
                                        doc!!.name!!,
                                        OffsetDateTime.now(),
                                        OffsetDateTime.now(),
                                        uri.toString(), false
                                    );
                                    StateDatabase.instance.db.directoryDao().insert(item);
                                    withContext(Dispatchers.Main) {
                                        updateContent();
                                    }
                                }
                                catch(ex: Throwable) {
                                    withContext(Dispatchers.Main) {
                                        UIDialogs.appToast("Failed to get document:\n" + ex.message);
                                        Logger.e("FilesFragment", "Failed to get document", ex);
                                    }
                                }
                            }
                        }

                    };
                }
            }
            updateContent();
        }

        fun onHide() {
            
        }
    }
}