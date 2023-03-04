/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.device.display.wm_samples.twonote

import Defines.GET_STARTED_FRAGMENT
import Defines.INODE
import Defines.LIST_FRAGMENT
import Defines.NOTE
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ReactiveGuide
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.example.device.display.wm_samples.twonote.fragments.GetStartedFragment
import com.example.device.display.wm_samples.twonote.fragments.NoteDetailFragment
import com.example.device.display.wm_samples.twonote.fragments.NoteListFragment
import com.example.device.display.wm_samples.twonote.fragments.viewmodels.DualScreenViewModel
import com.example.device.display.wm_samples.twonote.fragments.viewmodels.GraphViewModel
import com.example.device.display.wm_samples.twonote.models.DirEntry
import com.example.device.display.wm_samples.twonote.models.INode
import com.example.device.display.wm_samples.twonote.models.Note
import com.example.device.display.wm_samples.twonote.utils.DataProvider
import com.example.device.display.wm_samples.twonote.utils.FileSystem
import com.example.device.display.wm_samples.twonote.utils.buildDetailTag
import com.example.device.display.wm_samples.twonote.utils.graph.NotesGraphHelper
import com.example.device.display.wm_samples.twonote.utils.isRotated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Activity that manages fragments and preservation of data through the app's lifecycle
 */
class MainActivity : AppCompatActivity(), NoteDetailFragment.OnFragmentInteractionListener {
    private var savedNote: Note? = null
    private var savedINode: INode? = null
    private var dualScreenVM = DualScreenViewModel()
    private var graphViewModel = GraphViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dualScreenVM = ViewModelProvider(this)[DualScreenViewModel::class.java]
        dualScreenVM.isDualScreen = false

        graphViewModel = ViewModelProvider(this)[GraphViewModel::class.java]
        graphViewModel.graphHelper = NotesGraphHelper.getInstance(
            context = applicationContext,
            onCreated = graphViewModel::loadAccount
        )

        // Get data from previously selected note (if available)
        savedNote = savedInstanceState?.getSerializable(NOTE) as? Note
        savedINode = savedInstanceState?.getSerializable(INODE) as? INode

        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect {
                        onWindowLayoutInfoChanged(it)
                    }
            }
        }
    }

    /**
     * Select which fragment should be inflated in single-screen mode
     *
     * @param noteSelected: true if savedInstanceState contained a specific note/inode, false otherwise
     * @param note: note from savedInstanceState
     * @param inode: inode from savedInstanceState
     */
    private fun selectSingleScreenFragment(noteSelected: Boolean, note: Note?, inode: INode?) {
        if (!supportFragmentManager.isDestroyed) {
            // Remove fragment from second container if it exists
            removeSecondFragment()

            if (noteSelected) {
                startNoteDetailFragment(R.id.primary_fragment_container, note!!, inode!!)
            } else {
                startNoteListFragment()
            }
        }
    }

    /**
     * Select which fragment(s) should be inflated in dual-screen mode
     *
     * @param noteSelected: true if savedInstanceState contained a specific note/inode, false otherwise
     * @param note: note from savedInstanceState
     * @param inode: inode from savedInstanceState
     */
    private fun selectDualScreenFragments(noteSelected: Boolean, note: Note?, inode: INode?) {
        if (!supportFragmentManager.isDestroyed) {
            // If rotated, use extended canvas pattern, otherwise use list-detail pattern
            if (isRotated(this, dualScreenVM.isDualScreen)) {
                // Remove fragment from second container if it exists
                removeSecondFragment()

                if (noteSelected) {
                    startNoteDetailFragment(R.id.primary_fragment_container, note!!, inode!!)
                } else {
                    startNoteListFragment()
                }
            } else {
                if (noteSelected) {
                    startNoteDetailFragment(R.id.secondary_fragment_container, note!!, inode!!)
                } else {
                    startGetStartedFragment()
                }
                startNoteListFragment()
            }
        }
    }

    /**
     * Remove fragment from second container
     */
    private fun removeSecondFragment() {
        supportFragmentManager.findFragmentById(R.id.secondary_fragment_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    /**
     * Start note list view fragment in first container
     */
    private fun startNoteListFragment() {
        val listFragment = supportFragmentManager.findFragmentByTag(LIST_FRAGMENT)
        if (listFragment == null || !listFragment.isAdded) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.primary_fragment_container, NoteListFragment(), LIST_FRAGMENT)
                .commit()
        }
    }

    /**
     * Start note detail view fragment in specified container
     *
     * @param container: container to start fragment in
     * @param note: note to display in fragment
     * @param inode: inode associated with note to display in fragment
     */
    private fun startNoteDetailFragment(container: Int, note: Note, inode: INode) {
        val tag = buildDetailTag(container, inode.id, note.id)
        val detailFragment = supportFragmentManager.findFragmentByTag(tag)
        if (detailFragment == null || !detailFragment.isAdded) {
            supportFragmentManager.beginTransaction()
                .replace(container, NoteDetailFragment.newInstance(inode, note), tag)
                .commit()
        }
    }

    /**
     * Start welcome fragment in second container
     */
    private fun startGetStartedFragment() {
        if (supportFragmentManager.findFragmentByTag(GET_STARTED_FRAGMENT) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.secondary_fragment_container, GetStartedFragment(), GET_STARTED_FRAGMENT)
                .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val firstFrag = supportFragmentManager.findFragmentById(R.id.primary_fragment_container)
        val secondFrag = supportFragmentManager.findFragmentById(R.id.secondary_fragment_container)

        // Save data from note detail view for configuration changes
        if (secondFrag is NoteDetailFragment) {
            if (secondFrag.deleted)
                outState.clear()
            else
                saveCurrentNote(outState, secondFrag)
        } else if (secondFrag is GetStartedFragment) {
            outState.clear()
        } else if (firstFrag is NoteDetailFragment) {
            if (firstFrag.deleted)
                outState.clear()
            else
                saveCurrentNote(outState, firstFrag)
        }
    }

    /**
     * Save NoteDetailFragment's note and inode data to outState bundle
     *
     * @param outState: bundle to save data in
     * @param frag: NoteDetailFragment to extract note/inode data from
     */
    private fun saveCurrentNote(outState: Bundle, frag: NoteDetailFragment) {
        outState.putSerializable(NOTE, frag.arguments?.getSerializable(NOTE))
        outState.putSerializable(INODE, frag.arguments?.getSerializable(INODE))
    }

    /**
     * Communicate from NoteFragment to NoteListFragment that a note/inode has been edited
     */
    override fun onINodeUpdate() {
        // Write change to file system
        FileSystem.writeDirEntry(
            applicationContext,
            DataProvider.getActiveSubDirectory(),
            DirEntry(DataProvider.getINodes())
        )

        // Notify NoteListFragment (if it exists)
        (supportFragmentManager.findFragmentByTag(LIST_FRAGMENT) as? NoteListFragment)?.updateNotesList()
    }

    // -------------------------- Window Manager Section --------------------------- \\
    private fun onWindowLayoutInfoChanged(windowLayoutInfo: WindowLayoutInfo) {
        dualScreenVM.isDualScreen = false
        var noteSelected = savedNote != null && savedINode != null

        // If no note/inode were passed through the saved instance, check if a
        // NoteDetailFragment exists and extract its note/inode
        if (!noteSelected) {
            try {
                val detailFragment =
                    supportFragmentManager.fragments.first { fragment -> fragment as? NoteDetailFragment != null }
                savedNote = detailFragment.arguments?.getSerializable(NOTE) as? Note
                savedINode = detailFragment.arguments?.getSerializable(INODE) as? INode
                noteSelected = true
            } catch (e: NoSuchElementException) {
                Log.e(this::class.java.toString(), e.message.toString())
            }
        }

        val fold = windowLayoutInfo.displayFeatures.firstOrNull { it is FoldingFeature && it.isSeparating }
        (fold as? FoldingFeature)?.let {
            dualScreenVM.isDualScreen = true

            when (it.orientation) {
                FoldingFeature.Orientation.VERTICAL -> setBoundsTwoVerticalPanes(it.bounds)
                FoldingFeature.Orientation.HORIZONTAL -> setBoundsOnePane()
            }

            selectDualScreenFragments(noteSelected, savedNote, savedINode)
        }

        if (!dualScreenVM.isDualScreen) {
            setBoundsOnePane()
            selectSingleScreenFragment(noteSelected, savedNote, savedINode)
        }

        savedNote = null
        savedINode = null
        supportFragmentManager.executePendingTransactions()
    }

    /**
     * Set the bounding rectangle for a configuration with two vertical panes of content
     *
     * @param dividerBounds bounds of the divider that will separate the two vertical panes
     */
    private fun setBoundsTwoVerticalPanes(dividerBounds: Rect) {
        val dividerWidth = dividerBounds.right - dividerBounds.left

        val boundingRect: View = findViewById(R.id.bounding_rect)
        val params: ViewGroup.LayoutParams = boundingRect.layoutParams
        params.width = dividerWidth
        boundingRect.layoutParams = params

        // left fragment is aligned with the right side of the divider and vice-versa
        // add padding to ensure fragments do not overlap the divider
        val leftFragment: FragmentContainerView = findViewById(R.id.primary_fragment_container)
        leftFragment.setPadding(0, 0, dividerWidth, 0)

        val rightFragment: FragmentContainerView = findViewById(R.id.secondary_fragment_container)
        rightFragment.setPadding(dividerWidth, 0, 0, 0)
        rightFragment.visibility = View.VISIBLE
    }

    /**
     * Set the bounding rectangle for a configuration with one pane of content
     */
    private fun setBoundsOnePane() {
        val boundingRect: View = findViewById(R.id.bounding_rect)
        val params: ViewGroup.LayoutParams = boundingRect.layoutParams

        // fill parent
        params.height = -1
        params.width = -1
        boundingRect.layoutParams = params

        val guide: ReactiveGuide = findViewById(R.id.horiz_guide)
        guide.setGuidelineEnd(0)
    }
}
