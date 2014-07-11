package fr.outadev.skinswitch.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;

import fr.outadev.skinswitch.R;
import fr.outadev.skinswitch.Util;
import fr.outadev.skinswitch.network.MojangConnectionHandler;
import fr.outadev.skinswitch.network.login.ChallengeRequirementException;
import fr.outadev.skinswitch.network.login.InvalidMojangCredentialsException;
import fr.outadev.skinswitch.skin.Skin;
import fr.outadev.skinswitch.skin.SkinsDatabase;
import fr.outadev.skinswitch.user.UsersManager;

/**
 * Created by outadoc on 06/07/14.
 */
public class DetailActivity extends Activity {

	private Skin skin;
	private int animTime;

	private ImageButton b_delete;
	private ImageButton b_wear;

	private FrameLayout b_upload_skin_container;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		skin = (Skin) getIntent().getSerializableExtra("skin");
		animTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);

		b_delete = (ImageButton) findViewById(R.id.b_delete);
		b_wear = (ImageButton) findViewById(R.id.b_upload_skin);

		b_upload_skin_container = (FrameLayout) findViewById(R.id.b_upload_skin_container);

		setupSkinPreviews();
		setupText();
		setupButtons();

		setOutlines();
		applySystemWindowsBottomInset(R.id.container);

		setLoading(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.skin_details, menu);

		MenuItem shareItem = menu.findItem(R.id.action_share);
		ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
		shareActionProvider.setShareIntent(getDefaultIntent());

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void setupSkinPreviews() {
		final ImageView img_skin_preview_front = (ImageView) findViewById(R.id.skin_preview_front);
		final ImageView img_skin_preview_back = (ImageView) findViewById(R.id.skin_preview_back);

		(new AsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... voids) {
				try {
					return skin.getFrontSkinPreview(DetailActivity.this);
				} catch(FileNotFoundException e) {
					e.printStackTrace();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Bitmap bmp) {
				if(bmp != null) {
					img_skin_preview_front.setImageBitmap(bmp);
					colorizeInterface(bmp);
				}

				(new AsyncTask<Void, Void, Bitmap>() {

					@Override
					protected Bitmap doInBackground(Void... voids) {
						try {
							return skin.getBackSkinPreview(DetailActivity.this);
						} catch(FileNotFoundException e) {
							e.printStackTrace();
						}

						return null;
					}

					@Override
					protected void onPostExecute(Bitmap bmp) {
						if(bmp != null) {
							img_skin_preview_back.setImageBitmap(bmp);
						}
					}

				}).execute();
			}

		}).execute();

		img_skin_preview_front.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Util.crossfade(img_skin_preview_front, img_skin_preview_back, animTime);
			}

		});

		img_skin_preview_back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				Util.crossfade(img_skin_preview_back, img_skin_preview_front, animTime);
			}

		});
	}

	private void setupText() {
		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(skin.getName());

		TextView descriptionView = (TextView) findViewById(R.id.description);
		descriptionView.setText(skin.getDescription());
	}

	private void setupButtons() {
		b_delete.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
				builder.setTitle("Delete " + skin.getName() + "?").setMessage("Do you really want to delete this skin?");

				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {
						(new AsyncTask<Void, Void, Void>() {

							@Override
							protected Void doInBackground(Void... voids) {
								SkinsDatabase db = new SkinsDatabase(DetailActivity.this);
								db.removeSkin(skin);
								skin.deleteAllSkinResFromFilesystem(DetailActivity.this);
								DetailActivity.this.finish();

								return null;
							}

						}).execute();
					}

				});

				builder.setNegativeButton(R.string.no, null);
				builder.create().show();
			}

		});

		b_wear.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				UsersManager usersManager = new UsersManager(DetailActivity.this);

				//if the user isn't logged in, pop up the login window
				if(!usersManager.isLoggedInSuccessfully()) {
					Intent intent = new Intent(DetailActivity.this, MojangLoginActivity.class);
					startActivity(intent);
					return;
				}

				//else, ask for a confirmation
				AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
				builder.setTitle("Wear " + skin.getName() + "?").setMessage("Do you really want to replace your current " +
						"Minecraft skin with " + skin.getName() + "?");

				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {
						(new AsyncTask<Void, Void, Exception>() {

							@Override
							protected void onPreExecute() {
								setLoading(true);
							}

							@Override
							protected Exception doInBackground(Void... voids) {
								MojangConnectionHandler handler = new MojangConnectionHandler();
								UsersManager um = new UsersManager(DetailActivity.this);

								try {
									handler.loginWithCredentials(um.getUser());
									handler.uploadSkinToMojang(skin.getRawSkinFile(DetailActivity.this));
								} catch(Exception e) {
									return e;
								}

								return null;
							}

							@Override
							protected void onPostExecute(Exception e) {
								if(e != null) {
									//display the error if any
									if(e.getMessage() != null && !e.getMessage().isEmpty()) {
										Toast.makeText(DetailActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
									}

									//if the user needs to fill in a challenge
									if(e instanceof ChallengeRequirementException) {
										Intent intent = new Intent(DetailActivity.this, MojangLoginActivity.class);
										intent.putExtra("step", MojangLoginActivity.Step.CHALLENGE);
										startActivity(intent);
									} else if(e instanceof InvalidMojangCredentialsException) {
										//if the user needs to relog in
										Intent intent = new Intent(DetailActivity.this, MojangLoginActivity.class);
										startActivity(intent);
									}

								} else {
									Toast.makeText(DetailActivity.this, "Skin uploaded successfully!",
											Toast.LENGTH_SHORT).show();
								}

								setLoading(false);
							}

						}).execute();
					}

				});

				builder.setNegativeButton(R.string.no, null);
				builder.create().show();
			}

		});
	}

	private void setOutlines() {
		int size = getResources().getDimensionPixelSize(R.dimen.floating_button_size);

		Outline outline = new Outline();
		outline.setOval(0, 0, size, size);

		b_delete.setOutline(outline);
		b_upload_skin_container.setOutline(outline);
	}

	private void applySystemWindowsBottomInset(int container) {
		View containerView = findViewById(container);
		containerView.setFitsSystemWindows(true);

		containerView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {

			@Override
			public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
				DisplayMetrics metrics = getResources().getDisplayMetrics();

				if(metrics.widthPixels < metrics.heightPixels) {
					view.setPadding(0, 0, 0, windowInsets.getSystemWindowInsetBottom());
				} else {
					view.setPadding(0, 0, windowInsets.getSystemWindowInsetRight(), 0);
				}

				return windowInsets.consumeSystemWindowInsets();
			}

		});
	}

	@SuppressWarnings("ConstantConditions")
	private void colorizeInterface(Bitmap skin) {
		Palette palette = Palette.generate(skin);

		if(palette.getVibrantColor() != null) {
			TextView titleView = (TextView) findViewById(R.id.title);
			titleView.setTextColor(palette.getVibrantColor().getRgb());
		}

		if(palette.getLightVibrantColor() != null) {
			TextView descriptionView = (TextView) findViewById(R.id.description);
			descriptionView.setTextColor(palette.getLightVibrantColor().getRgb());
		}

		colorRipple(R.id.b_delete, (palette.getVibrantColor() != null) ? palette.getVibrantColor().getRgb() : getResources()
				.getColor(R.color.loading_bar_one));
		colorRipple(R.id.b_upload_skin, (palette.getVibrantColor() != null) ? palette.getVibrantColor().getRgb() : getResources
				().getColor(R.color.loading_bar_one));
	}

	private void colorRipple(int id, int tintColor) {
		View buttonView = findViewById(id);

		RippleDrawable ripple = (RippleDrawable) buttonView.getBackground();
		GradientDrawable rippleBackground = (GradientDrawable) ripple.getDrawable(0);
		rippleBackground.setColor(getResources().getColor(R.color.colorPrimary));

		ripple.setColor(ColorStateList.valueOf(tintColor));
	}

	private Intent getDefaultIntent() {
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out " + skin.getName() + "! " + skin.getSource() + " #SkinSwitch");
		sendIntent.setType("text/plain");
		return sendIntent;
	}

	private void setLoading(boolean loading) {
		View bLoading = findViewById(R.id.b_loading);
		View bWear = findViewById(R.id.b_upload_skin);

		bLoading.setVisibility((loading) ? View.VISIBLE : View.GONE);
		bWear.setVisibility((loading) ? View.GONE : View.VISIBLE);
	}
}
