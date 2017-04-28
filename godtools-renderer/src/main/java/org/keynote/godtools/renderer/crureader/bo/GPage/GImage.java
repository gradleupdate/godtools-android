package org.keynote.godtools.renderer.crureader.bo.GPage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.keynote.godtools.renderer.crureader.R;
import org.keynote.godtools.renderer.crureader.bo.GPage.Base.GCoordinator;
import org.keynote.godtools.renderer.crureader.bo.GPage.Compat.RenderViewCompat;
import org.keynote.godtools.renderer.crureader.bo.GPage.RenderHelpers.ImageAsyncTask;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "image")
public class GImage extends GCoordinator {

    private static final String TAG = "GImage";

    @Text(required = false)
    public String content;

    @Override
    public int render(LayoutInflater layoutInflater, ViewGroup viewGroup, int position) {
        View view
                = layoutInflater.inflate(R.layout.g_image, viewGroup);
        ImageView gImageView = (ImageView) view.findViewById(R.id.g_image_image_view);
        updateBaseAttributes(gImageView);
        ImageAsyncTask.setImageView(content, gImageView);
        gImageView.setId(RenderViewCompat.generateViewId());
        return gImageView.getId();
    }
}