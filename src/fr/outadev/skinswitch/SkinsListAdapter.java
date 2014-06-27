package fr.outadev.skinswitch;

import java.io.FileNotFoundException;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import fr.outadev.skinswitch.skin.Skin;
import fr.outadev.skinswitch.skin.SkinRenderer;
import fr.outadev.skinswitch.skin.SkinRenderer.Side;

public class SkinsListAdapter extends ArrayAdapter<Skin> {

	public SkinsListAdapter(Context context, int resource, List<Skin> array) {
		super(context, resource, array);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.skin_icon, parent, false);
		}

		ImageView skinView = (ImageView) convertView.findViewById(R.id.img_skin_preview);
		TextView skinTitle = (TextView) convertView.findViewById(R.id.lbl_skin_title);

		Skin skin = getItem(position);

		skinTitle.setText(skin.getName());
		
		try {
	        skinView.setImageBitmap(SkinRenderer.getCroppedHead(SkinRenderer.getSkinPreview(skin.getRawSkinBitmap(getContext()),
	                Side.FRONT, 19)));
        } catch(FileNotFoundException e) {
	        e.printStackTrace();
	        skinView.setImageResource(R.drawable.ic_launcher);
        }

		return convertView;
	}

}
