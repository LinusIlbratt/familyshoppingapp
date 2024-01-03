package com.example.familyshoppingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class HiddenGemDetailFragment : Fragment() {
    private lateinit var hiddenGem: HiddenGem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            hiddenGem = it.getParcelable(ARG_HIDDEN_GEM) ?: throw IllegalArgumentException("Hidden Gem is required")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate din layout och visa information om hiddenGem här
    }

    companion object {
        private const val ARG_HIDDEN_GEM = "hidden_gem"

        fun newInstance(hiddenGem: HiddenGem): HiddenGemDetailFragment {
            val args = Bundle().apply {
                putParcelable(ARG_HIDDEN_GEM, hiddenGem)
            }
            return HiddenGemDetailFragment().apply {
                arguments = args
            }
        }
    }
}
