package com.futo.music.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.futo.futopay.PaymentManager
import com.futo.music.R
import com.futo.music.UIDialogs
import com.futo.music.fragments.MainFragView
import com.futo.music.states.StatePayment

class BuyFragment: MainFragment() {
    override val isMainView : Boolean = true;
    override val isTab: Boolean = true;
    override val hasBottomBar: Boolean get() = false;


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
        return view;
    }
    override fun onDestroyMainView() {
        super.onDestroyMainView();
        _view = null;
    }


    class FragView(frag: BuyFragment, inflater: LayoutInflater): MainFragView<BuyFragment>(frag, inflater, R.layout.fragment_buy) {

        private val _paymentManager: PaymentManager;

        val textPrice: TextView;
        val buttonNext: Button;

        private val _overlayPaying: FrameLayout;
        private val _overlayPaid: FrameLayout;

        init {
            textPrice = findViewById(R.id.text_price);
            buttonNext = findViewById(R.id.button_next);
            _overlayPaying = findViewById(R.id.overlay_paying);
            _overlayPaid = findViewById(R.id.overlay_paid);

            _paymentManager = PaymentManager(StatePayment.instance, fragment, _overlayPaying) { success, purchaseId, exception ->
                if(success) {
                    UIDialogs.showDialog(context, R.drawable.ic_check, "Payment succeeded", "Thanks for your purchase, a key will be sent to your email after your payment has been received!", null, 0,
                        UIDialogs.Action("Ok", {}, UIDialogs.ActionStyle.PRIMARY));
                    fragment.close(true);
                }
                else {
                    UIDialogs.showConfirmSheet(context, R.drawable.ic_error, "Payment Failed", exception?.message ?: "???", {

                    }, {

                    })
                }
            }

            buttonNext.setOnClickListener {
                _paymentManager.startPayment(StatePayment.instance, fragment.lifecycleScope, "futo-music");
            }
        }

        fun onShown(parameter: Any? = null) {

        }

        fun onHide() {
            
        }
    }
}