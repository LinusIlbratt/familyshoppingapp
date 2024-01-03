package com.example.familyshoppingapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class HiddenGemDetailFragment : Fragment() {
    private lateinit var hiddenGem: HiddenGem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("detail", "onCreate called")
        arguments?.let {
            hiddenGem = it.getParcelable(HIDDEN_GEM) ?: throw IllegalArgumentException("Hidden Gem is required")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("detail", "onCreateView called")
        return inflater.inflate(R.layout.fragment_hidden_gem_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.detail_titel).text = hiddenGem.name
        view.findViewById<EditText>(R.id.detail_description_edit)
        view.findViewById<Button>(R.id.btn_save_desc)

    }

    companion object {
        private const val HIDDEN_GEM = "hidden_gem"

        fun newInstance(hiddenGem: HiddenGem): HiddenGemDetailFragment {
            val args = Bundle().apply {
                putParcelable(HIDDEN_GEM, hiddenGem)
            }
            return HiddenGemDetailFragment().apply {
                arguments = args
            }
        }
    }
}
