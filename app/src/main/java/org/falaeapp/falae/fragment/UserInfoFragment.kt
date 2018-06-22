package org.falaeapp.falae.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.falaeapp.falae.R
import org.falaeapp.falae.viewmodel.UserViewModel

class UserInfoFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProviders.of(activity!!).get(UserViewModel::class.java)
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

        userViewModel.currentUser.observe(activity!!, Observer { user ->
            user?.let {
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
                userName.text = it.name
                userInfo.text = it.profile
            }
        })



        return view
    }

    companion object {

        private const val USER_PARAM = "userParam"

        fun newInstance(): UserInfoFragment {
            return UserInfoFragment()
        }
    }
}
