package org.falaeapp.falae.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user_info, container, false)
        val imageView: ImageView = view.findViewById(R.id.user_photo)
        val userName: TextView = view.findViewById(R.id.user_name)
        val userInfo: TextView = view.findViewById(R.id.user_information)

        val brokenImage: Drawable? = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_broken_image_black_48dp)
        val placeHolderImage: Drawable? = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_person_black_24dp)

        userViewModel.currentUser.observe(viewLifecycleOwner, Observer { user ->
            user?.apply {
                photo?.let { linkPhoto ->
                    if (linkPhoto.isNotEmpty() && context != null) {
                        Picasso.get()
                            .load(linkPhoto)
                            .placeholder(placeHolderImage!!)
                            .error(brokenImage!!)
                            .transform(CropCircleTransformation())
                            .memoryPolicy(MemoryPolicy.NO_CACHE)
                            .into(imageView)
                    }
                }
                userName.text = name
                userInfo.text = profile
            }
        })
        return view
    }

    companion object {

        fun newInstance(): UserInfoFragment {
            return UserInfoFragment()
        }
    }
}
