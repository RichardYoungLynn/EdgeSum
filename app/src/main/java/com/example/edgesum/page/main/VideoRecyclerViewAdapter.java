package com.example.edgesum.page.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesum.R;
import com.example.edgesum.data.VideoViewModel;
import com.example.edgesum.event.AddEvent;
import com.example.edgesum.event.RemoveEvent;
import com.example.edgesum.event.Type;
import com.example.edgesum.model.Video;
import com.example.edgesum.page.main.VideoFragment.OnListFragmentInteractionListener;
import com.example.edgesum.util.file.FileManager;
import com.example.edgesum.util.nearby.TransferCallback;
import com.example.edgesum.util.video.clouduploader.S3Uploader;
import com.example.edgesum.util.video.summariser.SummariserIntentService;
import com.example.edgesum.util.video.viewholderprocessor.VideoViewHolderProcessor;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Video} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class VideoRecyclerViewAdapter extends RecyclerView.Adapter<VideoRecyclerViewAdapter.VideoViewHolder> {
    private static final String TAG = VideoRecyclerViewAdapter.class.getSimpleName();
    private final OnListFragmentInteractionListener listFragmentInteractionListener;
    private final String BUTTON_ACTION_TEXT;
    private final TransferCallback transferCallback;

    private final Context context;
    private List<Video> videos;
    private SelectionTracker<Long> tracker;
    private final VideoViewHolderProcessor videoViewHolderProcessor;
    private final VideoViewModel viewModel;

    VideoRecyclerViewAdapter(OnListFragmentInteractionListener listener,
                             Context context,
                             String buttonText,
                             VideoViewHolderProcessor videoViewHolderProcessor,
                             VideoViewModel videoViewModel,
                             TransferCallback transferCallback) {
        this.listFragmentInteractionListener = listener;
        this.context = context;
        this.BUTTON_ACTION_TEXT = buttonText;
        this.videoViewHolderProcessor = videoViewHolderProcessor;
        this.viewModel = videoViewModel;
        this.transferCallback = transferCallback;
        setHasStableIds(true);
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        this.tracker = tracker;
    }

    void sendVideos(Selection<Long> positions) {
        transferCallback.printPreferences(false);
        if (transferCallback.isConnected()) {
            for (Long pos : positions) {
                transferCallback.addVideo(videos.get(pos.intValue()));
            }
            transferCallback.nextTransfer();
        } else {
            for (Long pos : positions) {
                Video video = videos.get(pos.intValue());
                EventBus.getDefault().post(new AddEvent(video, Type.PROCESSING));
                EventBus.getDefault().post(new RemoveEvent(video, Type.RAW));

                final String output = String.format("%s/%s", FileManager.getSummarisedDirPath(), video.getName());
                Intent summariseIntent = new Intent(context, SummariserIntentService.class);
                summariseIntent.putExtra(SummariserIntentService.VIDEO_KEY, video);
                summariseIntent.putExtra(SummariserIntentService.OUTPUT_KEY, output);
                summariseIntent.putExtra(SummariserIntentService.TYPE_KEY, SummariserIntentService.LOCAL_TYPE);
                context.startService(summariseIntent);
            }
        }
    }

    void uploadVideos(Selection<Long> positions) {
        Log.v(TAG, String.format("List size: %d", positions.size()));
        List<String> selectedVideos = StreamSupport.stream(positions.spliterator(), false)
                .map(i -> videos.get(i.intValue()).getData()).collect(Collectors.toList());
        S3Uploader s3 = new S3Uploader(selectedVideos);
        s3.uploadVideos(context);
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video_list_item, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final VideoViewHolder holder, final int position) {
        holder.video = videos.get(position);
        holder.thumbnailView.setImageBitmap(getThumbnail(videos.get(position).getId()));
        holder.videoFileNameView.setText(videos.get(position).getName());
        holder.actionButton.setText(BUTTON_ACTION_TEXT);

        videoViewHolderProcessor.process(context, viewModel, holder, position);

        holder.view.setOnClickListener(v -> {
            if (null != listFragmentInteractionListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                listFragmentInteractionListener.onListFragmentInteraction(holder.video);
            }
        });

        if (tracker.isSelected(getItemId(position))) {
            holder.layout.setBackgroundResource(android.R.color.darker_gray);
        } else {
            holder.layout.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private Bitmap getThumbnail(String id) {
        return MediaStore.Video.Thumbnails.getThumbnail(
                this.context.getContentResolver(),
                Integer.parseInt(id),
                MediaStore.Video.Thumbnails.MICRO_KIND,
                null);
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
        notifyDataSetChanged();
    }


    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        final ImageView thumbnailView;
        final TextView videoFileNameView;
        public final Button actionButton;
        public Video video;
        final LinearLayout layout;

        VideoViewHolder(View view) {
            super(view);
            this.view = view;
            thumbnailView = view.findViewById(R.id.thumbnail);
            videoFileNameView = view.findViewById(R.id.content);
            actionButton = view.findViewById(R.id.actionButton);
            layout = itemView.findViewById(R.id.video_row);
        }

        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<Long>() {
                @Override
                public int getPosition() {
                    return getAdapterPosition();
                }

                @NonNull
                @Override
                public Long getSelectionKey() {
                    return getItemId();
                }
            };
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + videoFileNameView.getText() + "'";
        }
    }
}
