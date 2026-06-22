package com.futo.music.fragments.main

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.futo.music.R
import com.futo.music.dp
import com.futo.music.fragments.MainFragView
import com.futo.music.fragments.top.NavigationTopBarFragment
import com.futo.music.models.playable.IPlayable
import com.futo.music.states.StateDatabase
import com.futo.music.ui.adapters.AnyAdapterView
import com.futo.music.ui.adapters.AnyAdapterView.Companion.asAny
import com.futo.music.ui.buttons.PillButton
import com.futo.music.ui.viewholders.PlayableRatingViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RatingsListFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = true;


    private var _view: FragView? = null;


    override fun onShownWithView(parameter: Any?, isBack: Boolean) {
        super.onShownWithView(parameter, isBack);
        _view?.onShown(parameter);

        topBar?.let {
            if(it is NavigationTopBarFragment) {

            }
        }
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


    class FragView(frag: RatingsListFragment, inflater: LayoutInflater): MainFragView<RatingsListFragment>(frag, inflater, R.layout.fragment_recycler) {

        val recycler: RecyclerView;
        val adapter: AnyAdapterView<IPlayable, PlayableRatingViewHolder>;

        val allPills = ArrayList<PillButton>();
        val pillRandom: PillButton;
        val pillRecent: PillButton;

        init {
            recycler = findViewById(R.id.recycler);
            adapter = recycler.asAny(onCreate = {

            });

            val containerBefore = findViewById<LinearLayout>(R.id.container_before);
            containerBefore.layoutParams = ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, 70.dp(resources));

            val buttonsContainer = LinearLayout(context);
            buttonsContainer.layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            buttonsContainer.orientation = LinearLayout.HORIZONTAL;
            buttonsContainer.gravity = Gravity.CENTER;

            pillRandom = PillButton(context, null)
                .withText("Random")
                .withIcon(R.drawable.ic_shuffle_white);
            pillRandom.onClick.subscribe {
                loadRandom();
            }
            buttonsContainer.addView(pillRandom);

            pillRecent = PillButton(context, null)
                .withText("Recent")
                .withIcon(R.drawable.ic_date);
            pillRecent.onClick.subscribe {
                loadRecent();
            }
            val dp10 = 10.dp(resources);
            pillRecent.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                this.setMargins(dp10, 0, 0, 0);
            }
            buttonsContainer.addView(pillRecent);

            containerBefore.addView(buttonsContainer);

            allPills.add(pillRandom);
            allPills.add(pillRecent);
        }


        fun onShown(parameter: Any? = null) {
            loadRandom();
        }

        fun loadRandom() {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val ratings = StateDatabase.instance.getTracksUnrated(300);
                withContext(Dispatchers.Main) {
                    adapter.setData(ratings);
                }
            }
            allPills.forEach { it.alpha = 0.4f };
            pillRandom.alpha = 1f;
        }
        fun loadRecent() {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val ratings = StateDatabase.instance.getTracksNewUnrated(300);
                withContext(Dispatchers.Main) {
                    adapter.setData(ratings);
                }
            }
            allPills.forEach { it.alpha = 0.4f };
            pillRecent.alpha = 1f;
        }

        fun onHide() {
            
        }
    }
}