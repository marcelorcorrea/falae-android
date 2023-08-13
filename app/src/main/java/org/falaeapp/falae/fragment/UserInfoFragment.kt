package org.falaeapp.falae.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat.getDrawable

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.falaeapp.falae.R
import org.falaeapp.falae.viewmodel.UserViewModel

class UserInfoFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        onAttachFragment(requireParentFragment())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user_info, container, false)
        val imageView = view.findViewById(R.id.user_photo) as ImageView
        val userName = view.findViewById(R.id.user_name) as TextView
        val userInfo = view.findViewById(R.id.user_information) as TextView

        val brokenImage = getDrawable(resources, R.drawable.ic_broken_image_black_48dp, null)
            ?: throw IllegalStateException("Could not retrieve drawable with id ${R.drawable.ic_broken_image_black_48dp}")
        val placeHolderImage = getDrawable(resources, R.drawable.ic_person_black_24dp, null)
            ?: throw IllegalStateException("Could not retrieve drawable with id ${R.drawable.ic_person_black_24dp}")

        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.apply {
                photo?.let { linkPhoto ->
                    if (linkPhoto.isNotEmpty() && context != null) {
                        Picasso.get()
                            .load(linkPhoto)
                            .placeholder(placeHolderImage)
                            .error(brokenImage)
                            .transform(CropCircleTransformation())
                            .memoryPolicy(MemoryPolicy.NO_CACHE)
                            .into(imageView)
                    }
                }
                userName.text = name
                userInfo.text = profile
            }
        }
        return view
    }

    companion object {

        fun newInstance(): UserInfoFragment {
            return UserInfoFragment()
        }
    }
}
