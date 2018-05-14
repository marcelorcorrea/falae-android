package org.falaeapp.falae.fragment

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.falaeapp.falae.R
import org.falaeapp.falae.model.User

class UserInfoFragment : Fragment() {

    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = arguments?.getParcelable(USER_PARAM) ?: return
        onAttachFragment(parentFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user_info, container, false)
        val imageView = view.findViewById(R.id.user_photo) as ImageView
        val userName = view.findViewById(R.id.user_name) as TextView
        val userInfo = view.findViewById(R.id.user_information) as TextView

        val brokenImage: Drawable?
        val placeHolderImage: Drawable?

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            brokenImage = context?.resources?.getDrawable(R.drawable.ic_broken_image_black_48dp)
            placeHolderImage = context?.resources?.getDrawable(R.drawable.ic_person_black_24dp)
        } else {
            brokenImage = context?.getDrawable(R.drawable.ic_broken_image_black_48dp)
            placeHolderImage = context?.getDrawable(R.drawable.ic_person_black_24dp)
        }

        user.photo?.let {
            if (it.isNotEmpty()) {
                Picasso.with(context)
                        .load(it)
                        .placeholder(placeHolderImage)
                        .error(brokenImage!!)
                        .transform(CropCircleTransformation())
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .into(imageView)
            }
        }

        userName.text = user.name
        userInfo.text = user.profile

        return view
    }

    companion object {

        private const val USER_PARAM = "userParam"

        fun newInstance(user: User): UserInfoFragment {
            val fragment = UserInfoFragment()
            val args = Bundle()
            args.putParcelable(USER_PARAM, user)
            fragment.arguments = args
            return fragment
        }
    }
}
