package com.example.edgesum.page.main;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesum.R;
import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.data.VideoViewModelFactory;
import com.example.edgesum.data.VideosRepository;
import com.example.edgesum.model.Video;
import com.example.edgesum.util.nearby.TransferCallback;
import com.example.edgesum.util.video.VideoDetailsLookup;
import com.example.edgesum.util.video.videoeventhandler.VideoEventHandler;
import com.example.edgesum.util.video.viewholderprocessor.VideoViewHolderProcessor;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class VideoFragment extends Fragment {
    private static final String ARG_COLUMN_COUNT = "column-count";

    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private VideoViewHolderProcessor videoViewHolderProcessor;
    private ActionButton actionButton;
    private VideosRepository repository;
    private VideoViewModel videoViewModel;
    private VideoEventHandler videoEventHandler;
    private VideoRecyclerViewAdapter adapter;

    private ActionMode actionMode;
    private SelectionTracker<Long> tracker;
    private TransferCallback transferCallback;

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.video_selection_menu, menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.send:
                    if (actionButton.equals(ActionButton.ADD)) {
                        adapter.sendVideos(tracker.getSelection());
                    } else {
                        adapter.uploadVideos(tracker.getSelection());
                    }

                    mode.finish();
                    return true;
                case R.id.select_all:
                    for (long i = 0L; i < adapter.getItemCount(); i++) {
                        tracker.select(i);
                    }

                    adapter.notifyDataSetChanged();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            tracker.clearSelection();
            actionMode = null;
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public VideoFragment() {
    }

    public static VideoFragment newInstance(int columnCount,
                                            VideoViewHolderProcessor videoViewHolderProcessor,
                                            ActionButton actionButton,
                                            VideoEventHandler handler,
                                            TransferCallback transferCallback) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        fragment.videoViewHolderProcessor = videoViewHolderProcessor;
        fragment.actionButton = actionButton;
        fragment.videoEventHandler = handler;
        fragment.transferCallback = transferCallback;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        // TODO replace with non-deprecated version
        //noinspection deprecation
        setVideoViewModel(ViewModelProviders.of(
                this, new VideoViewModelFactory(activity.getApplication(), repository)).get(VideoViewModel.class));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);

        EventBus.getDefault().register(videoEventHandler);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            adapter = new VideoRecyclerViewAdapter(
                    mListener,
                    this.getContext(),
                    this.actionButton.getText(),
                    videoViewHolderProcessor,
                    videoViewModel,
                    transferCallback);
            recyclerView.setAdapter(adapter);

            FragmentActivity activity = getActivity();
            if (activity == null) {
                return null;
            }
            videoViewModel.getVideos().observe(activity, videos -> adapter.setVideos(videos));

            ItemKeyProvider<Long> videoKeyProvider = new ItemKeyProvider<Long>(ItemKeyProvider.SCOPE_MAPPED) {
                @Override
                public Long getKey(int position) {
                    return adapter.getItemId(position);
                }

                @Override
                public int getPosition(@NonNull Long key) {
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForItemId(key);
                    return viewHolder == null ? RecyclerView.NO_POSITION : viewHolder.getLayoutPosition();
                }
            };

            tracker = new SelectionTracker.Builder<>(
                    "video_selection",
                    recyclerView,
                    videoKeyProvider,
                    new VideoDetailsLookup(recyclerView),
                    StorageStrategy.createLongStorage())
                    .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                    .build();
            tracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
                @Override
                public void onSelectionChanged() {
                    super.onSelectionChanged();

                    // Check that CAB isn't already active and that an item has been selected, prevents activation
                    // from calls to notifyDataSetChanged()
                    if (actionMode == null && tracker.hasSelection()) {
                        actionMode = view.startActionMode(actionModeCallback);
                    }
                }
            });
            adapter.setTracker(tracker);
        }
        return view;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        EventBus.getDefault().unregister(videoEventHandler);
        super.onDetach();
        mListener = null;
    }

    public void setRepository(VideosRepository repository) {
        this.repository = repository;
    }

    private void setVideoViewModel(VideoViewModel videoViewModel) {
        this.videoViewModel = videoViewModel;
    }

    public void setVideoEventHandler(VideoEventHandler handler) {
        this.videoEventHandler = handler;
    }

    void cleanRepository(Context context) {
        List<Video> videos = repository.getVideos().getValue();

        if (videos != null) {
            int videoCount = videos.size();

            for (int i = 0; i < videoCount; i++) {
                Video video = videos.get(0);
                context.getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.MediaColumns.DATA + "=?", new String[]{video.getData()});
                repository.delete(0);
            }
        }
        adapter.setVideos(new ArrayList<>());
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Video item);
    }
}
