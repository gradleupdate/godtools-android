package org.keynote.godtools.renderer.crureader.bo.GPage;

import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.keynote.godtools.renderer.crureader.R;
import org.keynote.godtools.renderer.crureader.bo.GPage.Base.GCoordinator;
import org.keynote.godtools.renderer.crureader.bo.GPage.Base.GBaseTextAttributes;
import org.keynote.godtools.renderer.crureader.bo.GPage.Compat.RenderViewCompat;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "input-field")
public class GInputField extends GCoordinator {

    private static final String TAG = "GInputField";

    @Attribute(name = "valid-format", required = false)
    public String validFormat;

    @Attribute
    public String type;

    @Attribute
    public String name;

    @Element(name = "input-label", required = false)
    public GBaseTextAttributes inputLabel;

    @Element(name = "input-placeholder", required = false)
    public GInputPlaceholder inputPlaceholder;

    @Override
    public int render(LayoutInflater inflater, ViewGroup viewGroup, int position) {



        View inflate = inflater.inflate(R.layout.g_input_field, viewGroup);
        final TextInputLayout textInputLayout = (TextInputLayout)inflate.findViewById(R.id.g_input_field_input_layout);
        textInputLayout.setId(RenderViewCompat.generateViewId());
        textInputLayout.setHint(inputLabel.content);
        final TextInputEditText textInputEditText = (TextInputEditText)inflate.findViewById(R.id.g_input_field_input_edit_text);
        textInputEditText.setTag(R.id.textinput_name, name);
        textInputEditText.setTag(inflater.getContext().getString(R.string.scannable_text_input));
        textInputEditText.setId(RenderViewCompat.generateViewId());
        updateBaseAttributes(textInputLayout);

        return textInputLayout.getId();
    }

}
