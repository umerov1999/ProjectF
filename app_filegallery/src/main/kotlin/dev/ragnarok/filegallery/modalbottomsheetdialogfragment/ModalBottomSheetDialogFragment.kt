package dev.ragnarok.filegallery.modalbottomsheetdialogfragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso3.Callback
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.getParcelableArrayListCompat
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.picasso.transforms.PolyTransformation
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.util.Utils

class ModalBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        private const val KEY_EXCLUDES = "excludes"
        private const val KEY_OPTIONS = "options"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_HEADER = "header"
        private const val KEY_DRAWABLE_RES = "resource"
        private const val KEY_IMAGE_URL = "url"

        internal fun newInstance(
            listener: Listener?,
            builder: Builder
        ): ModalBottomSheetDialogFragment {
            val fragment = ModalBottomSheetDialogFragment()
            fragment.listener = listener
            val args = Bundle()
            args.putParcelableArrayList(KEY_OPTIONS, builder.options)
            args.putInt(KEY_COLUMNS, builder.columns)
            args.putInt(KEY_DRAWABLE_RES, builder.icon)
            args.putIntegerArrayList(KEY_EXCLUDES, builder.excludes)
            args.putString(KEY_HEADER, builder.header)
            args.putString(KEY_IMAGE_URL, builder.urlicon)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var list: RecyclerView
    private lateinit var adapter: Adapter
    private var listener: Listener? = null

    private val menuInflater by lazy {
        MenuInflater(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_bottom_sheet_dialog_fragment, container, false)
    }

    override fun onPause() {
        super.onPause()
        if (bindHost() == null) {
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list = view.findViewById(R.id.list)
        val arguments = checkArguments()

        val optionHolders =
            arguments.getParcelableArrayListCompat<OptionHolder>(KEY_OPTIONS) ?: return
        val excludes = arguments.getIntegerArrayList(KEY_EXCLUDES) ?: return

        val options = mutableListOf<Option>()

        val finalOptions = mutableListOf<Option>()

        optionHolders.forEach {
            val resource = it.resource
            val optionRequest = it.optionRequest
            if (resource != null) {
                inflate(resource, options)
            }
            if (optionRequest != null) {
                options.add(optionRequest.toOption(requireContext()))
            }
        }

        options.forEach {
            if (!excludes.contains(it.id))
                finalOptions.add(it)
        }

        adapter = Adapter {
            listener?.onModalOptionSelected(this@ModalBottomSheetDialogFragment.tag, it)
            dismissAllowingStateLoss()
        }
        adapter.header = arguments.getString(KEY_HEADER)
        adapter.icon = arguments.getInt(KEY_DRAWABLE_RES)
        adapter.urlicon = arguments.getString(KEY_IMAGE_URL)

        list.adapter = adapter
        val columns = arguments.getInt(KEY_COLUMNS)
        if (columns == 1) {
            list.layoutManager = LinearLayoutManager(requireActivity())
        } else {
            val layoutManager = GridLayoutManager(requireActivity(), columns)
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter.header != null && position == 0) {
                        columns
                    } else {
                        1
                    }
                }
            }
            list.layoutManager = layoutManager
        }

        adapter.set(finalOptions)
        if (listener == null) {
            listener = bindHost()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun inflate(menuRes: Int, options: MutableList<Option>) {
        val menu = MenuBuilder(context)
        menuInflater.inflate(menuRes, menu)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val option = Option(
                item.itemId,
                if (item.title == null) "" else item.title.toString(),
                item.icon,
                false
            )
            options.add(option)
        }
    }

    private fun bindHost(): Listener? {
        if (parentFragment != null) {
            if (parentFragment is Listener) {
                return parentFragment as Listener
            }
        }
        if (context is Listener) {
            return context as Listener
        }
        return null
    }

    private fun checkArguments(): Bundle {
        return arguments
            ?: throw IllegalStateException("You need to create this via the builder")

    }

    /**
     * Used to build a [ModalBottomSheetDialogFragment]
     */
    class Builder {

        internal var options = ArrayList<OptionHolder>()
        internal var excludes = ArrayList<Int>()
        internal var columns = 1
        internal var header: String? = null

        @DrawableRes
        internal var icon: Int = R.drawable.ic_error_toast_vector
        internal var urlicon: String? = null

        /**
         * Inflate the given menu resource to the options
         */
        fun add(@MenuRes menuRes: Int): Builder {
            options.add(OptionHolder(menuRes, null))
            return this
        }

        /**
         * Add an option to the sheet
         */
        fun add(option: OptionRequest): Builder {
            options.add(OptionHolder(null, option))
            return this
        }

        fun exclude(option: Int): Builder {
            excludes.add(option)
            return this
        }

        /**
         * Set the number of columns you want for your options
         */
        fun columns(columns: Int): Builder {
            this.columns = columns
            return this
        }

        /**
         * Add a custom header to the modal, using the custom layout if provided
         */
        fun header(header: String?, @DrawableRes icon: Int, url: String?): Builder {
            this.header = header
            this.icon = icon
            this.urlicon = url
            return this
        }

        /**
         * Build the [ModalBottomSheetDialogFragment]. You still need to call [ModalBottomSheetDialogFragment.show] when you want it to show
         */
        fun build(listener: Listener?): ModalBottomSheetDialogFragment {
            return newInstance(listener, this)
        }

        /**
         * Build and show the [ModalBottomSheetDialogFragment]
         */
        fun show(
            fragmentManager: FragmentManager,
            tag: String,
            listener: Listener
        ): ModalBottomSheetDialogFragment {
            val dialog = build(listener)
            dialog.show(fragmentManager, tag)
            return dialog
        }

        fun show(
            fragmentManager: FragmentManager,
            tag: String
        ): ModalBottomSheetDialogFragment {
            val dialog = build(null)
            dialog.show(fragmentManager, tag)
            return dialog
        }

        inline fun show(
            fragmentManager: FragmentManager,
            tag: String,
            crossinline granted: (String?, Option) -> Unit
        ): ModalBottomSheetDialogFragment {
            val dialog = build(object : Listener {
                override fun onModalOptionSelected(tag: String?, option: Option) {
                    granted.invoke(tag, option)
                }
            })
            dialog.show(fragmentManager, tag)
            return dialog
        }
    }

    /**
     * Listener for when the modal options are selected
     */
    interface Listener {
        /**
         * A modal option has been selected
         */
        fun onModalOptionSelected(tag: String?, option: Option)
    }

    internal class Adapter(private val callback: (option: Option) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val VIEW_TYPE_HEADER = 0
            const val VIEW_TYPE_ITEM = 1
        }

        private val options = mutableListOf<Option>()
        internal var header: String? = null

        @DrawableRes
        internal var icon: Int = R.drawable.ic_error_toast_vector
        internal var urlicon: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            when (viewType) {

                VIEW_TYPE_HEADER -> {
                    val view =
                        LayoutInflater.from(parent.context).inflate(
                            R.layout.modal_bottom_sheet_dialog_fragment_header,
                            parent,
                            false
                        )
                    return HeaderViewHolder(view)
                }

                VIEW_TYPE_ITEM -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.modal_bottom_sheet_dialog_fragment_item, parent, false)
                    val holder = ItemViewHolder(view)
                    view.setOnClickListener {
                        val position = if (header != null) {
                            holder.bindingAdapterPosition - 1
                        } else {
                            holder.bindingAdapterPosition
                        }
                        val option = options[position]
                        callback.invoke(option)
                    }
                    return holder
                }
            }

            throw IllegalStateException("Wht is this")
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val correctedPosition = if (header == null) position else position - 1
            if (holder is ItemViewHolder) {
                val option = options[correctedPosition]
                holder.bind(option)
            } else if (holder is HeaderViewHolder) {
                holder.bind(header, urlicon, icon)
            }
        }

        override fun getItemCount(): Int {
            return if (header == null) options.size else options.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            if (header != null) {
                if (position == 0) {
                    return VIEW_TYPE_HEADER
                }
            }
            return VIEW_TYPE_ITEM
        }

        fun set(options: List<Option>) {
            this.options.clear()
            this.options.addAll(options)
            notifyDataSetChanged()
        }
    }

    internal class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private var text: TextView = view.findViewById(R.id.item_option_text)
        private var icon: ImageView = view.findViewById(R.id.item_option_icon)

        fun bind(option: Option) {
            text.text = option.title
            if (option.singleLine) {
                text.maxLines = 1
            } else {
                text.maxLines = 3
            }
            icon.setImageDrawable(option.icon)
        }
    }

    internal class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private var text: TextView = view.findViewById(R.id.item_header_text)
        private var av: ShapeableImageView = view.findViewById(R.id.item_header_icon)

        fun bind(header: String?, url: String?, @DrawableRes res: Int) {
            text.text = header
            PicassoInstance.with().cancelRequest(av)
            if (url.nonNullNoEmpty()) {
                PicassoInstance.with()
                    .load(url)
                    .transform(PolyTransformation())
                    .into(av, object : Callback {
                        override fun onSuccess() {

                        }

                        override fun onError(t: Throwable) {
                            av.setImageResource(res)
                            Utils.setColorFilter(av, CurrentTheme.getColorPrimary(av.context))
                        }
                    })
            } else {
                av.setImageResource(res)
                Utils.setColorFilter(av, CurrentTheme.getColorPrimary(av.context))
            }
        }
    }
}
