package com.futo.music.fragments.main

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.updateTransition
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.dp
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.logging.Logger
import com.futo.music.setHeaderScrollFade
import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBDirectory
import com.futo.music.ui.adapters.AnyAdapterView
import com.futo.music.ui.adapters.AnyAdapterView.Companion.asAny
import com.futo.music.ui.viewholders.DirectoryRootViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime

class FilesFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;

    override val fragmentTitle: String? get() = "Files"

    private var _view: FragView? = null;


    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                requireContext().contentResolver
                    .takePersistableUriPermission(uri, flags)

                UIDialogs.toast("Access: " + uri.toString());

                lifecycleScope.launch(Dispatchers.IO) {
                    val mainThread1 = Looper.myLooper() == Looper.getMainLooper();
                    try {
                        val doc = DocumentFile.fromTreeUri(requireContext(), uri);
                        val mainThread2 = Looper.myLooper() == Looper.getMainLooper();

                        if (doc?.name == null)
                            UIDialogs.toast("No document found?");

                        val mainThread3 = Looper.myLooper() == Looper.getMainLooper();

                        val item = DBDirectory(
                            0,
                            doc!!.name!!,
                            OffsetDateTime.MIN,
                            OffsetDateTime.MIN,
                            uri.toString(), false
                        );
                        StateDatabase.instance.db.directoryDao().insert(item);
                        withContext(Dispatchers.Main) {
                            _view?.updateContent();
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
        }


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
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        _view = null;
    }


    class FragView(frag: FilesFragment, inflater: LayoutInflater): MainFragView<FilesFragment>(frag, inflater, R.layout.fragment_recycler) {

        val anyAdapter: AnyAdapterView<DBDirectory, DirectoryRootViewHolder>;

        init {
            val recycler = findViewById<RecyclerView>(R.id.recycler);

            val fadeOffset = 10.dp(resources);
            recycler.setFadingEdgeLength(fadeOffset);
            anyAdapter = recycler.asAny {

            }
        }

        fun updateContent() {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val items = StateDatabase.instance.db.directoryDao().getAll();
                withContext(Dispatchers.Main) {
                    anyAdapter.setData(items);
                }
            }
        }

        fun onShown(paramter: Any? = null) {
            UIDialogs.toast("This is still under construction");
            fragment.topBar?.let {
                if(it is NavigationTopBarFragment) {
                    it.setGeneralButton(R.drawable.ic_add) {
                        fragment.folderPicker.launch(null);
                    };
                }
            }
            updateContent();
        }

        fun onHide() {
            
        }
    }
}