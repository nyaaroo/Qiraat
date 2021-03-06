					/* In the name of GOD, the Most Gracious, the Most Merciful */
/*
 *	Date : 27th of May 2011
 *	Description : Lists the ayas in the selected Sura
 *
 * 	Author	: Jazarine Jamal
 *  E-Mail 	: jazarinester@gmail.com
 *  Web		: http://www.jazarine.org
 *  
 *  Updated: 20th May 2012 - Start from current position
 * */
package com.qiraat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.qiraat.R;

public class DisplaySuraActivity extends Activity
{
	private static ListView ayahListView;
	int suraPosition = 0;
	int numAyas = 0;
	public static int nReciterVoiceID = 0;
	public static ProgressDialog mProgressDialog;
	public static ProgressDialog mCalcSizeDialog;
	public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
	public static long totalDownloadSize = 0;
	public static long totalDownloaded = 0;
	public static MenuItem playStopMenuItem;
	public static int statTranslationVal = -1;
	public Handler sizeCalculatorHandler;
	public static final String LOG_TAG = "DisplaySuraActivity";
	public static final boolean isDEBUGLOG = true;
	public static final boolean isERRORLOG = true;
	public static String appAudioPath = "";
	boolean playBismillah = false;			//Jaz 20th May 2012 - Start from Current Position.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        try
        {
        	setContentView(R.layout.displaysurah);
        	ayahListView = (ListView)findViewById(R.id.ayahList);
        	ArrayList<String> ayahList = new ArrayList<String>();
            TextView suraHeaderView = (TextView)findViewById(R.id.suraHeader);
            Typeface externalFont=Typeface.createFromAsset(getAssets(), "fonts/me_quran_volt_newmet.ttf");
            suraHeaderView.setTypeface(externalFont);
            XmlPullParser xpp=this.getResources().getXml(R.xml.quransimple);
            
            Bundle bundle = this.getIntent().getExtras();
            suraHeaderView.setText(bundle.getString("suraName"));
            suraPosition = bundle.getInt("position");
            numAyas = bundle.getInt("numAyas");
            ayahList = SurahDataParser.getAyahList(this,xpp,suraPosition);
        	
            int nTranslationVal = bundle.getInt("translationValue");
            statTranslationVal = nTranslationVal;
            if(nTranslationVal == 1)
            {
            	xpp=this.getResources().getXml(R.xml.entransliteration);
            }
            else if(nTranslationVal == 2)
            {
            	xpp=this.getResources().getXml(R.xml.enyusufali);
            }
            else if(nTranslationVal == 3)
            {
            	xpp=this.getResources().getXml(R.xml.mlabdulhameed);
            }
            ArrayList<String> translatedAyaList = new ArrayList<String>();
            if(nTranslationVal != 0)
            {
            	translatedAyaList = TranslatedSuraDataParser.getTranslatedAyaList(this,xpp,suraPosition);
            }
            
            CustomAyaListAdapter customAdapter = new CustomAyaListAdapter(this, ayahList,translatedAyaList, nTranslationVal);
            ayahListView.setAdapter(customAdapter);
        }
        catch(Exception ex)
        {
        	AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
			adb.setTitle("Error!");
			adb.setMessage("Encountered Exception: "+ex.toString());
			adb.setNegativeButton("Cancel", null);
			adb.show();
        }
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.suramenu,menu);
		return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		playStopMenuItem = menu.getItem(0);
		if(Recitation.isPlaying())
		{
			playStopMenuItem.setTitle("Stop");
		}
		return super.onPrepareOptionsMenu(menu);
	}
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		Recitation.releasePlayer();
	}
	protected Dialog onCreateDialog(int id)
	{
		switch(id)
		{
		case DIALOG_DOWNLOAD_PROGRESS:
			mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Downloading Sura..");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
            return mProgressDialog;
		default:
        	return null;
		}
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem)
	{
			switch(menuItem.getItemId())
			{
			case R.id.recite_sura:
				//play sura
				playStopMenuItem = menuItem;
				if((menuItem.getTitle() != "Stop") &&(!Recitation.isPlaying()))
				{
					boolean mExternalStorageAvailable = false;
					boolean mExternalStorageWriteable = false;
					String state = Environment.getExternalStorageState();

					if (Environment.MEDIA_MOUNTED.equals(state)) {
					    // We can read and write the media
					    mExternalStorageAvailable = mExternalStorageWriteable = true;
					} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
					    // We can only read the media
					    mExternalStorageAvailable = true;
					    mExternalStorageWriteable = false;
					} else {
					    // Something else is wrong. It may be one of many other states, but all we need
					    //  to know is we can neither read nor write
					    mExternalStorageAvailable = mExternalStorageWriteable = false;
					}
					
					if((!mExternalStorageAvailable) || (!mExternalStorageWriteable))
					{
						AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
						adb.setTitle("Error Occured");
						adb.setMessage("Could not access SD Card.");
						adb.setPositiveButton("OK", null);
						adb.show();
						break;
					}
					getAudioPath(DisplaySuraActivity.this.getExternalFilesDir(null).getAbsolutePath());	//Just call this once and set the static var
					boolean isFoldersInitialized = initializeFolders();
					if(!isFoldersInitialized)
					{
						AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
						adb.setTitle("Error Occured");
						adb.setMessage("Recitation files could not be downloaded at this time.");
						adb.setPositiveButton("OK", null);
						adb.show();
						break;
					}
					boolean isAvailable = isSuraRecitationAvailable(DisplaySuraActivity.this.suraPosition,DisplaySuraActivity.this.numAyas);
					if (isAvailable)
					{
						int audhuBismiExists = getAudhuBismi(true);
						
						if(audhuBismiExists == 1)
						{
							playBismillah = true;
							if ((DisplaySuraActivity.this.suraPosition == 1) || (DisplaySuraActivity.this.suraPosition == 9))
							{
								playBismillah = false;
							}
							//+ Jaz 20th May 2012 - Start from Current Position
							if((Recitation.currentAyaPos != 0) && (Recitation.currentAyaPos != 1))
							{
								AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
								adb.setTitle("Start from beginning");
								adb.setMessage("Do you want to start playback from current position or start from beginning?");
								adb.setPositiveButton("Current Position", new OnClickListener() {
									
									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										// TODO Auto-generated method stub
										ayahListView.smoothScrollToPosition(0);
										Recitation.play(DisplaySuraActivity.this, DisplaySuraActivity.this.suraPosition,DisplaySuraActivity.this.numAyas,playBismillah);
										
										
									}
								});
								adb.setNegativeButton("Start from Beginning", new OnClickListener() {
									
									@Override
									public void onClick(DialogInterface arg0, int arg1) {
										// TODO Auto-generated method stub
										Recitation.currentAyaPos = 0;
										ayahListView.smoothScrollToPosition(0);
										Recitation.play(DisplaySuraActivity.this, DisplaySuraActivity.this.suraPosition,DisplaySuraActivity.this.numAyas,playBismillah);
										
										
									}
								});
								adb.show();
							}
							else
							{
								ayahListView.smoothScrollToPosition(0);
								Recitation.play(DisplaySuraActivity.this, DisplaySuraActivity.this.suraPosition,DisplaySuraActivity.this.numAyas,playBismillah);
							}
							//- Jaz
							menuItem.setTitle("Stop");
						}
					}
					else
					{
						AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
						adb.setTitle("No Recitation found");
						adb.setMessage("No recitation files found. Do you wish to download this Sura?");
						adb.setPositiveButton("Yes", new OnClickListener() {
							
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								// TODO Auto-generated method stub
								if(haveInternet())
								{
									int audhuBismiExists = getAudhuBismi(false);
									//+ Jaz 06/21/2011 - Show Progress circle when calculating size to download
									PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
									final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Stay awake on size computation");
									wl.acquire();
									final ProgressDialog dialog = ProgressDialog.show(
		                                    DisplaySuraActivity.this,
		                                    "Computing size of sura",
		                                    "This may take some time depending on the size of the sura.");
									dialog.setCancelable(true);
									dialog.show();
									DisplaySuraActivity.this.sizeCalculatorHandler = new Handler() {
										public void handleMessage(Message msg){
											switch(msg.what){
											case 100:
												dialog.dismiss();
												if(totalDownloadSize != 0)
			                            		{
			                            			wl.release();
													startDownload(DisplaySuraActivity.this.suraPosition,DisplaySuraActivity.this.numAyas);
			                            		}
												else
												{
													AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
													adb.setTitle("Cannot access everyayah.com");
													adb.setMessage("Could not make a connection to everyayah.com. The server may be down. Please try again later.");
													adb.setPositiveButton("OK",null);
													adb.show();
												}
											}
										}
									};
									//- JJ
									Thread background = new Thread (new Runnable() {
				                           public void run() {
				                               try {
				                            	   	totalDownloadSize = getTotalDownloadSize();
				                            	   	dialog.setProgress(100);
				                            	   	Message msg = new Message();
				                            	   	msg.what = 100;
				                            	   	DisplaySuraActivity.this.sizeCalculatorHandler.sendMessage(msg);
				                               }
				                               catch (Exception e) {
				                                   if (isERRORLOG) {
													// if something fails do something smart
													Log.e(LOG_TAG, "Error in sizeCalculatorThread" + e.toString());
													e.printStackTrace();
												}
				                               }
				                           }
									});
									background.setPriority(java.lang.Thread.MAX_PRIORITY);
									background.start();
									
								}
								else
								{
									AlertDialog.Builder adb1=new AlertDialog.Builder(DisplaySuraActivity.this);
									adb1.setTitle("No Internet Connection");
									adb1.setMessage("No Connection to the internet found. Please make sure you are connected to the Internet and try again.");
									adb1.show();
								}
								
							}
						});
						adb.setNegativeButton("No", null);
						adb.show();
					}
				}
				else
				{
					Recitation.pausePlayer();
					menuItem.setTitle("Recite Sura");
				}
				return true;
			case R.id.change_translation:
				openChangeTranslationDialog();
				return true;
			case R.id.change_reciterVoice:
				openChangeReciterVoiceDialog();
				return true;
			}
			return false;
	}
	protected boolean haveInternet() {
		// TODO Auto-generated method stub
		ConnectivityManager connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

		// ARE WE CONNECTED TO THE NET

		if ( connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED ||

		connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTING ||

		connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING ||

		connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED )
		{
			return true;
		}
		else if ( connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED ||  connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED )
		{
			return false;
		}
		return true;
	}
	private void openChangeReciterVoiceDialog() {
		// TODO Auto-generated method stub
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Reciter");
		builder.setSingleChoiceItems(R.array.recitervoices,nReciterVoiceID,
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						//changeReciterVoice(arg1);
						nReciterVoiceID = arg1;
					}
				});
		
		builder.setPositiveButton("OK", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				changeReciterVoice(nReciterVoiceID);
			}
		});
		AlertDialog alert = builder.create();		
		alert.show();
	
	}
	public void changeReciterVoice(int reciterVoiceID)
	{
		nReciterVoiceID = reciterVoiceID;		
	}
	public static String getAudioPath(String externalDir)
	{
		String reciterVoiceDir = "";
		switch(DisplaySuraActivity.nReciterVoiceID)
		{
		case 0:
			reciterVoiceDir = "Alafasy/";
			break;
		case 1:
			reciterVoiceDir = "Sudais/";
			break;
		case 2:
			reciterVoiceDir = "Shaatree/";
			break;
		}
		String applicationAudioPath = externalDir + "/audio/"+reciterVoiceDir;
		appAudioPath = applicationAudioPath;
		return applicationAudioPath;
	}
	private boolean isSuraRecitationAvailable(int suraPosition, int numAyas) {
		// TODO Auto-generated method stub
		String suraID = calculateSuraId(suraPosition);
		
		String ayaID = calculateAyaId(numAyas);
		
		//String applicationAudioPath = getAudioPath(DisplaySuraActivity.this.getExternalFilesDir(null).getAbsolutePath());
		String applicationAudioPath = appAudioPath;
		File file= new File(applicationAudioPath+suraID+ayaID+".mp3");
		return file.exists();
	}
	
	private boolean initializeFolders()
	{
		boolean retValue = true;
		//File audioDir = new File(getAudioPath(DisplaySuraActivity.this.getExternalFilesDir(null).getAbsolutePath()));
		File audioDir = new File(appAudioPath);
		// have the object build the directory structure, if needed.
		if(!audioDir.exists())
		{
			retValue = audioDir.mkdirs();
		}
		return retValue;
	}
	private int getAudhuBismi(boolean askUser)
	{
		//String applicationAudioPath = getAudioPath(DisplaySuraActivity.this.getExternalFilesDir(null).getAbsolutePath());
		String applicationAudioPath = appAudioPath;
		
		File file= new File(applicationAudioPath+"bismillah.mp3");
		File file2= new File(applicationAudioPath+"audhubillah.mp3");
		File file3= new File(applicationAudioPath+"001000.mp3");
		if(((!file.exists() || (!file2.exists()))) && (!file3.exists()))
		{
			if(askUser)
			{
				AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
				adb.setTitle("Initialazation files missing");
				adb.setMessage("Some small-sized initialization files for this reciter are missing. Do you want to download it now?");
				adb.setPositiveButton("Yes", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						if(haveInternet())
						{
							downloadAudhuBismi();
						}
						else
						{
							AlertDialog.Builder adb1=new AlertDialog.Builder(DisplaySuraActivity.this);
							adb1.setTitle("No Internet Connection");
							adb1.setMessage("No Connection to the internet found. Please make sure you are connected to the Internet and try again.");
							adb1.show();
						}
						
					}
				});
				adb.setNegativeButton("No", null);
				adb.show();
			}
			else
			{
				if(haveInternet())
				{
					downloadAudhuBismi();
				}
				else
				{
					AlertDialog.Builder adb1=new AlertDialog.Builder(DisplaySuraActivity.this);
					adb1.setTitle("No Internet Connection");
					adb1.setMessage("No Connection to the internet found. Please make sure you are connected to the Internet and try again.");
					adb1.show();
				}
			}
			return 0;
		}
		return 1;
	}
	private void downloadAudhuBismi()
	{
		String url = "";
		DownloadRecitation dwnloadRecit = new DownloadRecitation();
		
		if(DisplaySuraActivity.nReciterVoiceID == 1)		//Only for As Sudais
		{
			url = "http://www.everyayah.com/data/"+getNameInUrl()+"001000.mp3";
			dwnloadRecit.execute(url,"001","000");
		}
		else
		{
			url = "http://www.everyayah.com/data/"+getNameInUrl()+"bismillah.mp3";
			dwnloadRecit.execute(url,"bismillah","");
		}		
	}
	private String getNameInUrl()
	{
		String nameInUrl = "";
		switch(DisplaySuraActivity.nReciterVoiceID)
		{
		case 0:
			nameInUrl = "Alafasy_64kbps/";
			break;
		case 1:
			nameInUrl = "Abdurrahmaan_As-Sudais_64kbps/";
			break;
		case 2:
			nameInUrl = "Abu_Bakr_Ash-Shaatree_64kbps/";
			break;
		}
		return nameInUrl;
		
	}
	private void startDownload(int suraPosition, int numAyas) {
		// TODO Auto-generated method stub
		String suraID = calculateSuraId(suraPosition);
		String ayaID = "";
		String url = "";
		
		ayaID = calculateAyaId(1);
		url = "http://www.everyayah.com/data/"+getNameInUrl()+suraID+ayaID+".mp3";
		new DownloadRecitation().execute(url,suraID,ayaID);
	}
	
	private String calculateAyaId(int lastDownloadedAya) 
	{
		// TODO Auto-generated method stub
		String ayaID = "";
		int toDownload = lastDownloadedAya;
		if(toDownload<10)
		{
			ayaID = "00"+toDownload;
		}
		else if(toDownload<100)
		{
			ayaID = "0"+toDownload;
		}
		else
		{
			ayaID = toDownload + "";
		}
		return ayaID;
	}

	private String calculateSuraId(int suraPos) {
		// TODO Auto-generated method stub
		String suraID = "";
		if(suraPos<10)
		{
			suraID = "00"+suraPos;
		}
		else if(suraPos<100)
		{
			suraID = "0"+suraPos;
		}
		else
		{
			suraID = suraPos + "";
		}
		return suraID;
	}

	private void openChangeTranslationDialog() {
		// TODO Auto-generated method stub
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Translation");
		builder.setSingleChoiceItems(R.array.translations,statTranslationVal,
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						statTranslationVal = arg1;
					}
				});
		builder.setPositiveButton("OK", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				changeTranslation(statTranslationVal);
			}
		});
		AlertDialog alert = builder.create();		
		alert.show();
	}

	public static void goToNextAya(int ayaPos) {
		
		
		// TODO Auto-generated method stub
		ayahListView.smoothScrollToPosition(ayaPos);
		/*if(ayaPos == ayahListView.getChildCount())
		{
			ayaPos = ayaPos-1;
		}
		if(ayaPos!=0)
			ayahListView.getChildAt(ayaPos).setBackgroundColor(Color.TRANSPARENT);
		ayahListView.getChildAt(ayaPos).setBackgroundColor(Color.GRAY);*/
		//ayahListView.setItemChecked(ayaPos, true);
		//ayahListView.setFocusableInTouchMode(true);
		//ayahListView.setSelection(ayaPos);
	}
	
	public void changeTranslation(int nChangeTranslation)
	{
		Intent intent = getIntent();
	    overridePendingTransition(0, 0);
	    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
	    finish();

	    overridePendingTransition(0, 0);
	    Bundle bundle = new Bundle();
	    bundle.putInt("translationValue", nChangeTranslation);
	    intent.putExtras(bundle);
	    startActivity(intent);

	}
	public long getTotalDownloadSize()
	{
		String suraID = calculateSuraId(suraPosition);
		String ayaID = "";
		String sUrl = "";
		long totalSizeforDownload = 0;
		
		for(int nAyaCount=0;nAyaCount<numAyas;nAyaCount++)
		{
			ayaID = calculateAyaId(nAyaCount+1);
			sUrl = "http://www.everyayah.com/data/"+getNameInUrl()+suraID+ayaID+".mp3";
			try
			{
				if (isDEBUGLOG) {
					Log.d(LOG_TAG, "Checking URL: " + sUrl);
				}
				URL url = new URL(sUrl);
				URLConnection connection = url.openConnection();
				/*connection.setConnectTimeout(10000);
				connection.connect();*/
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				c.setRequestMethod("GET");
				c.setDoOutput(true);
				c.setConnectTimeout(10000);
				//c.connect();
				//totalSizeforDownload += connection.getContentLength();
				totalSizeforDownload += c.getContentLength();
				if(totalSizeforDownload < 0)
				{
					if (isDEBUGLOG) {
						Log.d(LOG_TAG, "getTotalDownloadSize: "
								+ "Size is negative. Returning");
					}
					return 0;
				}
				if (isDEBUGLOG) {
					Log.d(LOG_TAG, "Calculated download size till " + nAyaCount
							+ " : " + totalSizeforDownload);
				}
			}
			catch(Exception ex)
			{
				if (isERRORLOG) {
					Log.e(LOG_TAG,
							"Error in getTotalDownloadSize" + ex.toString());
				}
				return 0;
			}
		}
		return totalSizeforDownload;
	}
	class DownloadRecitation extends AsyncTask<String, String, String>
	{
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			showDialog(DIALOG_DOWNLOAD_PROGRESS);
		}
		
		@Override
		protected String doInBackground(String... aurl) {
			// TODO Auto-generated method stub
			int count;
			
			try
			{
				URL url = new URL(aurl[0]);
				URLConnection connection = url.openConnection();
				connection.connect();
				
				InputStream input = new BufferedInputStream(url.openStream());
				
				//String applicationAudioPath = getAudioPath(DisplaySuraActivity.this.getExternalFilesDir(null).getAbsolutePath());
				String applicationAudioPath = DisplaySuraActivity.appAudioPath;
				OutputStream output = new FileOutputStream(applicationAudioPath+aurl[1]+aurl[2]+".mp3");
				
				byte data[] = new byte[1024];
				
				//long total = 0;	//Commented out and made public static
				
				if((DisplaySuraActivity.nReciterVoiceID == 1) && (aurl[1] == "001") && (aurl[2] == "001"))		//For Sudais
				{
					int lengthOfFile = connection.getContentLength();
					long total = 0;
					while ((count = input.read(data)) != -1)
					{
						total+=count;
						publishProgress(""+(int)((total*100)/lengthOfFile));
						output.write(data,0,count);
					}
					return "sudaisbismi";
				}
				else if((aurl[2] != "") && (aurl[2]!="000"))
				{
					while ((count = input.read(data)) != -1)
					{
						totalDownloaded+=count;
						publishProgress(""+(int)((totalDownloaded*100)/totalDownloadSize));
						output.write(data,0,count);
					}
				}
				else
				{
					int lengthOfFile = connection.getContentLength();
					long total = 0;
					while ((count = input.read(data)) != -1)
					{
						total+=count;
						publishProgress(""+(int)((total*100)/lengthOfFile));
						output.write(data,0,count);
					}
				}
				output.flush();
				output.close();
				
			}
			catch (Exception e)
			{
				if (isERRORLOG) {
					// TODO: handle exception
					Log.e(LOG_TAG, "Download Error!: " + e.toString()
							+ "Link: " + aurl[0]);
					e.printStackTrace();
				}
				return "error!";
			}
			
			if(aurl[2] == "")
			{
				return aurl[1];
			}
			return aurl[2];
		}
		
		protected void onProgressUpdate(String... progress)
		{
			DisplaySuraActivity.mProgressDialog.setProgress(Integer.parseInt(progress[0]));
		}
		
		@Override
		protected void onPostExecute(String downloadedAya)
		{
			if((downloadedAya != "bismillah") && (downloadedAya != "audhubillah") && (downloadedAya != "error!") && (downloadedAya!="000") && (downloadedAya!="sudaisbismi"))
			{
				if (isDEBUGLOG) {
					Log.d(LOG_TAG, "Downloaded aya: " + downloadedAya);
				}
				String suraID = calculateSuraId(suraPosition);
				String ayaID = "";
				String url = "";
				
				if(Integer.parseInt(downloadedAya)+1 <= numAyas)
				{
					ayaID = calculateAyaId(Integer.parseInt(downloadedAya)+1);
					url = "http://www.everyayah.com/data/"+getNameInUrl()+suraID+ayaID+".mp3";
					if (isDEBUGLOG) {
						Log.d(LOG_TAG, "Downloading sura: " + suraID
								+ ", aya: " + ayaID);
					}
					new DownloadRecitation().execute(url,suraID,ayaID);
				}
				else
				{
					totalDownloaded = 0;
					totalDownloadSize = 0;
					dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
				}
			}
			else if(downloadedAya.equals("bismillah"))
			{
				String url = "http://www.everyayah.com/data/"+getNameInUrl()+"audhubillah.mp3";
				new DownloadRecitation().execute(url,"audhubillah","");
			}
			else if(downloadedAya.equals("000"))		//For Sudais
			{
				String url = "http://www.everyayah.com/data/"+getNameInUrl()+"001001.mp3";
				new DownloadRecitation().execute(url,"001","001");
			}
			else if(downloadedAya == "error!")
			{
				dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
				AlertDialog.Builder adb=new AlertDialog.Builder(DisplaySuraActivity.this);
				adb.setTitle("Cannot access everyayah.com");
				adb.setMessage("Could not make a connection to everyayah.com. The server may be down. Please try again later.");
				adb.setPositiveButton("OK", null);
				adb.show();
			}
			else
			{
				dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
			}
			//Recite Sura..
			/*boolean isAvailable = isSuraRecitationAvailable(DisplaySurahActivity.this.suraPosition,DisplaySurahActivity.this.numAyas);
			if (isAvailable)
			{
				ayahListView.smoothScrollToPosition(0);
				boolean playBismillah = true;
				if ((DisplaySurahActivity.this.suraPosition == 1) || (DisplaySurahActivity.this.suraPosition == 9))
				{
					playBismillah = false;
				}
				Recitation.play(DisplaySurahActivity.this, DisplaySurahActivity.this.suraPosition,DisplaySurahActivity.this.numAyas,playBismillah);
			}*/
		}
		
		
	}
	
}