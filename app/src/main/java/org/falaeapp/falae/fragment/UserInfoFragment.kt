package org.falaeapp.falae.fragment

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.falaeapp.falae.R
import org.falaeapp.falae.databinding.FragmentUserInfoBinding
import org.falaeapp.falae.viewmodel.UserViewModel

class UserInfoFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)
        userViewModel = ViewModelProvider(activity!!, factory).get(UserViewModel::class.java)
        onAttachFragment(parentFragment!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        val view = binding.root

        val brokenImage: Drawable?
        val placeHolderImage: Drawable?

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            brokenImage = context?.resources?.getDrawable(R.drawable.ic_broken_image_black_48dp)
            placeHolderImage = context?.resources?.getDrawable(R.drawable.ic_person_black_24dp)
        } else {
            brokenImage = context?.getDrawable(R.drawable.ic_broken_image_black_48dp)
            placeHolderImage = context?.getDrawable(R.drawable.ic_person_black_24dp)
        }
        userViewModel.currentUser.observe(viewLifecycleOwner, Observer { user ->
            user?.apply {
                println("USER is $this")
                photo?.let { linkPhoto ->
                    if (linkPhoto.isNotEmpty() && context != null) {
                        Picasso.get()
                            .load(linkPhoto)
                            .placeholder(placeHolderImage!!)
                            .error(brokenImage!!)
                            .transform(CropCircleTransformation())
                            .memoryPolicy(MemoryPolicy.NO_CACHE)
                            .into(binding.userPhoto)
                    }
                }
                binding.userName.text = name
                binding.userInformation.text = profile
                binding.userEmail.text = email
            }
        })
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        fun newInstance(): UserInfoFragment {
            return UserInfoFragment()
        }
    }
}
