package fr.outadev.skinswitch;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import fr.outadev.skinswitch.network.MojangConnectionHandler;
import fr.outadev.skinswitch.network.SkinUploadException;
import fr.outadev.skinswitch.network.login.ChallengeRequirementException;
import fr.outadev.skinswitch.network.login.InvalidMojangCredentialsException;
import fr.outadev.skinswitch.skin.BasicSkin;
import fr.outadev.skinswitch.skin.SkinsDatabase;
import fr.outadev.skinswitch.user.UsersManager;

/**
 * Android Wear listener.
 */
public class WearListenerService extends WearableListenerService {

	private static final String TAG = "SkinSwitch/Wear";

	private static Asset createAssetFromBitmap(Bitmap bitmap) {
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
		return Asset.createFromBytes(byteStream.toByteArray());
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		super.onMessageReceived(messageEvent);
		Log.d(TAG, "message received: " + messageEvent.getPath() + " / " + new String(messageEvent.getData()));

		if(messageEvent.getPath().equals("/getSkin")) {
			String skinName = new String(messageEvent.getData());
			SkinsDatabase db = new SkinsDatabase(this);
			List<BasicSkin> allSkins = db.getAllSkins();

			BasicSkin foundSkin = null;

			for(BasicSkin skin : allSkins) {
				if(skin.getName().toLowerCase().contains(skinName.toLowerCase())) {
					foundSkin = skin;
					break;
				}
			}

			if(foundSkin != null) {
				Log.d(TAG, "found a matching skin: " + foundSkin);

				GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
						.addApi(Wearable.API)
						.build();

				ConnectionResult connectionResult =
						googleApiClient.blockingConnect(10, TimeUnit.SECONDS);

				try {
					if(connectionResult.isSuccess()) {
						Log.d(TAG, "sending skin head back");
						Bitmap skinHead = foundSkin.getSkinHeadBitmap(this);
						Asset skinHeadAsset = createAssetFromBitmap(skinHead);
						skinHead.recycle();

						PutDataMapRequest dataMap = PutDataMapRequest.create("/skinHead");
						dataMap.getDataMap().putAsset("image", skinHeadAsset);
						dataMap.getDataMap().putString("name", foundSkin.getName());
						dataMap.getDataMap().putByte("skinId", (byte) foundSkin.getId());
						dataMap.getDataMap().putInt("id", (new Random()).nextInt(100));
						PutDataRequest request = dataMap.asPutDataRequest();
						Wearable.DataApi.putDataItem(googleApiClient, request);
					}
				} catch(FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} else if(messageEvent.getPath().equals("/sendSkin")) {
			int skinId = messageEvent.getData()[0];
			Log.d(TAG, "uploading skin with ID " + skinId);

			SkinsDatabase db = new SkinsDatabase(this);
			MojangConnectionHandler handler = new MojangConnectionHandler();
			UsersManager usersManager = new UsersManager(this);

			BasicSkin skin = db.getSkin(skinId);

			try {
				handler.loginWithCredentials(usersManager.getUser());
				handler.uploadSkinToMojang(skin.getRawSkinFile(this));
			} catch(SkinUploadException e) {
				e.printStackTrace();
			} catch(InvalidMojangCredentialsException e) {
				e.printStackTrace();
			} catch(ChallengeRequirementException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onPeerConnected(Node peer) {
		super.onPeerConnected(peer);
		Log.d(TAG, "connected to " + peer.getDisplayName());
	}

	@Override
	public void onPeerDisconnected(Node peer) {
		super.onPeerDisconnected(peer);
		Log.d(TAG, "disconnected from " + peer.getDisplayName());
	}
}
