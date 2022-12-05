package dev.ragnarok.fenrir.fragment.marketview

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityFeatures
import dev.ragnarok.fenrir.activity.ActivityUtils.supportToolbarFor
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachmentsFor
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory.withSpans
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getTmpSourceGalleryPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils.getDateFromUnixTime
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.AspectRatioImageView
import dev.ragnarok.fenrir.view.CircleCounterButton

class MarketViewFragment : BaseMvpFragment<MarketViewPresenter, IMarketViewView>(),
    IMarketViewView {
    private var photo: AspectRatioImageView? = null
    private var fave_button: CircleCounterButton? = null
    private var share_button: CircleCounterButton? = null
    private var marketer_button: MaterialButton? = null
    private var price: TextView? = null
    private var title: TextView? = null
    private var available: TextView? = null
    private var description: TextView? = null
    private var time: TextView? = null
    private var sku: TextView? = null
    private var weight: TextView? = null
    private var dimensions: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_market_view, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        photo = root.findViewById(R.id.item_market_image)
        fave_button = root.findViewById(R.id.fave_button)
        share_button = root.findViewById(R.id.share_button)
        marketer_button = root.findViewById(R.id.item_messaging_marketer)
        available = root.findViewById(R.id.item_available)
        price = root.findViewById(R.id.item_price)
        title = root.findViewById(R.id.item_title)
        description = root.findViewById(R.id.item_description)
        time = root.findViewById(R.id.item_time)
        sku = root.findViewById(R.id.item_sku)
        dimensions = root.findViewById(R.id.item_dimensions)
        weight = root.findViewById(R.id.item_weight)
        return root
    }

    override fun displayMarket(market: Market, accountId: Int) {
        if (market.thumb_photo != null) {
            photo?.visibility = View.VISIBLE
            displayAvatar(photo, null, market.thumb_photo, Constants.PICASSO_TAG)
            photo?.setOnClickListener {
                getTmpSourceGalleryPlace(
                    accountId,
                    ParcelNative.createParcelableList(market.photos),
                    0
                ).tryOpenWith(requireActivity())
                /*
                getSingleURLPhotoPlace(
                    market.thumb_photo,
                    market.title,
                    "market"
                ).tryOpenWith(requireActivity())
                 */
            }
        } else {
            photo?.setOnClickListener { }
            photo?.let { with().cancelRequest(it) }
            photo?.visibility = View.GONE
        }
        share_button?.setOnClickListener {
            presenter?.fireSendMarket(
                market
            )
        }
        fave_button?.setIcon(if (market.isIs_favorite) R.drawable.favorite else R.drawable.star)
        marketer_button?.setOnClickListener {
            presenter?.fireWriteToMarketer(
                market,
                requireActivity()
            )
        }
        fave_button?.setOnClickListener {
            presenter?.fireFaveClick()
        }
        when (market.availability) {
            0 -> {
                available?.setTextColor(CurrentTheme.getColorOnSurface(requireActivity()))
                available?.setText(R.string.markets_available)
            }
            2 -> {
                available?.setTextColor(Color.parseColor("#ffaa00"))
                available?.setText(R.string.markets_not_available)
            }
            else -> {
                available?.setTextColor(Color.parseColor("#ff0000"))
                available?.setText(R.string.markets_deleted)
            }
        }
        title?.text = market.title
        if (market.price.isNullOrEmpty()) price?.visibility = View.GONE else {
            price?.visibility = View.VISIBLE
            price?.text = market.price
        }
        if (market.description.isNullOrEmpty()) description?.visibility = View.GONE else {
            description?.visibility = View.VISIBLE
            description?.text = withSpans(
                requireActivity().getString(
                    R.string.markets_description,
                    market.description
                ), owners = true, topics = false, listener = object : LinkActionAdapter() {
                    override fun onOwnerClick(ownerId: Int) {
                        getOwnerWallPlace(accountId, ownerId, null).tryOpenWith(requireActivity())
                    }
                })
        }
        if (market.date == 0L) time?.visibility = View.GONE else {
            time?.visibility = View.VISIBLE
            time?.text = getDateFromUnixTime(requireActivity(), market.date)
        }
        if (market.sku.isNullOrEmpty()) sku?.visibility = View.GONE else {
            sku?.visibility = View.VISIBLE
            sku?.text = requireActivity().getString(R.string.markets_sku, market.sku)
        }
        if (market.weight == 0) weight?.visibility = View.GONE else {
            weight?.visibility = View.VISIBLE
            weight?.text = requireActivity().getString(R.string.markets_weight, market.weight)
        }
        if (market.dimensions.isNullOrEmpty()) dimensions?.visibility = View.GONE else {
            dimensions?.visibility = View.VISIBLE
            dimensions?.text =
                requireActivity().getString(R.string.markets_dimensions, market.dimensions)
        }
    }

    override fun sendMarket(accountId: Int, market: Market) {
        startForSendAttachments(requireActivity(), accountId, market)
    }

    override fun onWriteToMarketer(accountId: Int, market: Market, peer: Peer) {
        startForSendAttachmentsFor(requireActivity(), accountId, peer, market)
    }

    override fun displayLoading(loading: Boolean) {}
    override fun onResume() {
        super.onResume()
        val actionBar = supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.product)
            actionBar.subtitle = null
        }
        ActivityFeatures.Builder()
            .begin()
            .setHideNavigationMenu(false)
            .setBarsColored(requireActivity(), true)
            .build()
            .apply(requireActivity())
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<MarketViewPresenter> {
        return object : IPresenterFactory<MarketViewPresenter> {
            override fun create(): MarketViewPresenter {
                val aid = requireArguments().getInt(Extra.ACCOUNT_ID)
                val market: Market = requireArguments().getParcelableCompat(Extra.MARKET)!!
                return MarketViewPresenter(aid, market, saveInstanceState)
            }
        }
    }

    companion object {
        fun buildArgs(aid: Int, market: Market?): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(Extra.MARKET, market)
            bundle.putInt(Extra.ACCOUNT_ID, aid)
            return bundle
        }

        fun newInstance(bundle: Bundle?): MarketViewFragment {
            val fragment = MarketViewFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}