/*
 *     Copyright (C) 2014  Raymond Wisman
 * 			Indiana University SE
 * 			May 7, 2014
 * 
 * 	Harmonic Motion - Harmonic Motion analysis for Physics Toolbox Accelerometer.  

	The application is designed for use in science education experiments that:
		collect and analyze accelerometer data.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

  */

package edu.ius.harmonicmotion;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.view.Menu;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import java.io.BufferedReader;
import java.io.File; 
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import jxl.*; 
import jxl.write.*;
import jxl.write.Number;

public class AccolyzeActivity extends Activity {
	private final Activity activity = this;
	BufferedReader br = null;
	private File maxFile=null, xlsDir=null, accDir=null, copyFile=null, inFile=null;
	private boolean dismissed = false;
	private static final String DIRECTORY = "PhysicsToolboxAccelerometer";
	private static final String PROGRAM = "Physics Toolbox Accelerometer";
	private static final String PACKAGENAME="com.chrystianvieyra.android.physicstoolboxaccelerometer";
	private static String APP;
	
	private static int samples = 9;
	private static int axisColumn=3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accolyze);
		APP = getString(R.string.app_name);	

		((ImageButton) findViewById(R.id.buttonInfo)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				showInfo();
			}
		});

		((Button) findViewById(R.id.buttonCollect)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				collect();
			}
		});
				
		((Button) findViewById(R.id.buttonAnalyze)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {   
				analyze();
				generateXLSDialog();
			}
		});
		
        NumberPicker samplePicker = (NumberPicker) findViewById(R.id.samplepicker);
        samplePicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        samplePicker.setMaxValue(100);
        samplePicker.setMinValue(1);
        samplePicker.setValue(samples);
        samplePicker.setWrapSelectorWheel(true);
        samplePicker.setOnValueChangedListener( new NumberPicker.
            OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                samples = newVal;
            }
        });
        
        NumberPicker axisPicker = (NumberPicker) findViewById(R.id.axispicker);
        axisPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        axisPicker.setMaxValue(2);
        axisPicker.setMinValue(0);
        axisPicker.setValue(axisColumn-2);											// Don't use first two columns
        axisPicker.setDisplayedValues( new String[] { "X", "Y", "Z" } );
        axisPicker.setWrapSelectorWheel(true);
        axisPicker.setOnValueChangedListener( new NumberPicker.
            OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                axisColumn = newVal+2;												// Don't use first two columns
            }
        });

        Toast.makeText(this, "samples " + samples, Toast.LENGTH_SHORT).show();
	}

    protected void showInfo() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
 
        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        TextView textView = (TextView) messageView.findViewById(R.id.about);
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);
 
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setIcon(R.drawable.icon);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
	public void generateXLSDialog() {
		final ProgressDialog dialog = ProgressDialog.show(this, "Please wait.",	"Generating XLS file...", true);
        dialog.setCanceledOnTouchOutside(true);
		dialog.setCancelable(true);
		dismissed = false;
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(final DialogInterface dialog) {
    			dismissed = true;
			}
		});
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					generateXLS();
				} catch (Exception e) {

				}
				dialog.dismiss();
			}
		}).start();
	}

    private void errorDialog(final String msg){
	    runOnUiThread(new Runnable()   {
	        public void run()  {
	          	AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
	          	alertDialog.setTitle("Error");
	          	alertDialog.setMessage(msg);
	          	alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
	          		new DialogInterface.OnClickListener() {
	          			public void onClick(DialogInterface dialog, int i) {}
	          	});
	          	alertDialog.show();
	        }
	    });    
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void copyAssets(File dest) {
		AssetManager am = getAssets();
		try {
		    InputStream is = am.open( "input.xls");
		    
		    dest.createNewFile();	// Create new file to copy into.

		    copyFile(is, dest);

		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	public static void copyFile(InputStream in, File dst) {
	    OutputStream out = null;
	    try {
	    	
	        out = new FileOutputStream(dst);

	        byte[] buffer = new byte[1024];
	        int read;
	        while ((read = in.read(buffer)) != -1) {
	            out.write(buffer, 0, read);
	        }
	        in.close();
	        in = null;
	        out.flush();
	        out.close();
	        out = null;
	    } catch (Exception e) {	    }
	}

	private void collect() {
    	Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(PACKAGENAME);
    	startActivity(LaunchIntent);		
	}
	
	private void analyze() {
		Uri uri=null;
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    if (!action.equals(Intent.ACTION_VIEW)) {						// App started directly, try to find Accelerometer data directory and most recent file.	    	
	    	try {
				String filepath = Environment.getExternalStorageDirectory().getPath();
				accDir = new File(filepath, DIRECTORY);
				if (!accDir.exists()) {
					AlertDialog alertDialog = new AlertDialog.Builder(this).create();
					alertDialog.setTitle("Error");
					alertDialog.setMessage(DIRECTORY + " directory does not exist. Run "+PROGRAM+" program.");
					alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) { }
					});
					alertDialog.show();
	    			return;
				}
				// Obtain list of files in the directory. listFiles() returns a list of File objects to each file found.
				File[] files = accDir.listFiles();
			
				if (files == null || files.length==0) {
					AlertDialog alertDialog = new AlertDialog.Builder(this).create();
					alertDialog.setTitle("Error");
					alertDialog.setMessage("No files in "+DIRECTORY+" directory. Run "+PROGRAM+" program.");
					alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) { }
					});
					alertDialog.show();
	    			return;
				}

				long maxAge = 0;
				maxFile = files[0];
				// Loop through all files
				for (File f : files ) {		// Get the last modified date. 
			   		long lastmodified = f.lastModified();

					if(lastmodified > maxAge) { 
			      		maxAge = lastmodified;
			      		maxFile = f;
			   		}
				}
				br = new BufferedReader(new FileReader(maxFile));
			}
			catch(Exception e1) {}
	    }
	    else {
	    	uri = intent.getData();										// Started from another app
			ContentResolver cr = getContentResolver();	
			try {
				br = new BufferedReader(new InputStreamReader(cr.openInputStream(uri)));
			}
			catch(Exception e) {
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Error");
				alertDialog.setMessage("File cannot be opened: "+uri.toString());
				alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) { }
					});
				alertDialog.show();
				e.printStackTrace();
			}
	    }
	    
		try {
			String filepath = Environment.getExternalStorageDirectory().getPath();
			xlsDir = new File(filepath, APP);		
			
			if(!xlsDir.exists()) xlsDir.mkdirs();
			copyFile = new File(xlsDir.getAbsolutePath() + "/" + "copy.xls");
			inFile = new File(xlsDir.getAbsolutePath() + "/" + "input.xls");
			if (!inFile.exists())	
				copyAssets(inFile);	
			
			if (!inFile.exists()) {	
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Error");
				alertDialog.setMessage(APP + " directory must contain 'input.xls' spreadsheet to combine with "+PROGRAM+" data, creating 'input.xls'.");
				alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) { }
				});
				alertDialog.show();
				return;
			}

			for( int i=0; i<2; i++) br.readLine();								// First two lines are blank or comment

			if (copyFile.exists())
				copyFile.delete();
		}
		catch(Exception e){
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Error");
			alertDialog.setMessage(APP+" directory must contain 'input.xls' spreadsheet to combine with "+PROGRAM+" data, creating 'copy.xls'.");
			alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int i) { }
			});
			alertDialog.show();
			e.printStackTrace();
		}
	}
	
    private void generateXLS() {
    	String values[];
        double x[], y[];
        int peak=0;
        
        x = new double[samples];
        y = new double[samples];
        
//        System.out.println("Number of samples "+samples);

        boolean findPeaks=false, above=false;
        
        for(int i=0; i<samples; i++)
            y[i]=0;

		try {
			Workbook inworkbook = Workbook.getWorkbook(inFile);
 			WritableWorkbook copyworkbook = Workbook.createWorkbook(copyFile, inworkbook);
			WritableSheet sheet = copyworkbook.getSheet(0);

			int row=1;
			Number number;
			double d, time=0.0;
			String line;
			
			for( int i=0; i<2; i++) br.readLine();								// First two lines are blank or comment
            
			while((line=br.readLine()) != null) {
				values =line.split("\\s*(;|,)\\s*");
				if(values.length > 1){
					number = new Number(0, row, row);
					sheet.addCell(number);
					for(int col=1; col<5; col++) {
						d = Double.parseDouble(values[col]);
                        if(col == 1)
                            time = d;
                        if(col == axisColumn) {
                            if(d > 1.2) {
                                findPeaks = true;                               // Dropped event occured
                                above=false;
                            }
                    
                            if(findPeaks && peak < samples) {                     // Accelerometer bounces above and below 1g in axis of motion
                                if(d < 1.0) {  
                                    if(above)
                                        peak=peak+1;
                                    above = false;
                                }
                                if(d > 1.0) {
                                    above = true;
                                }
                                if(above && d > y[peak]) {
                                    y[peak]=d;
                                    x[peak]=time;
                                }
                                if(above && peak > 0 && y[peak] > y[peak-1]) {
                                    peak=peak-1;
                                    y[peak]=y[peak+1];
                                    x[peak]=x[peak+1];
                                    y[peak+1]=0.0;
                                }
                            }
                        }
						number = new Number(col, row, d);
						sheet.addCell(number);
					}
					row++;
				}
			}
            
            for(int i=0; i<samples; i++) 
                System.out.println(x[i]+" "+y[i]);

            for (int i=0; i<samples; i++)              
                y[i]=Math.log(y[i]);
            
            int n = samples;
            
            // first pass: read in data, compute xbar and ybar
            double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
            for(int i=0;i<n;i++) {
                sumx  += x[i];
                sumx2 += x[i] * x[i];
                sumy  += y[i];
            }
            double xbar = sumx / n;
            double ybar = sumy / n;
            
            // second pass: compute summary statistics
            double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
            for (int i = 0; i < n; i++) {
                xxbar += (x[i] - xbar) * (x[i] - xbar);
                yybar += (y[i] - ybar) * (y[i] - ybar);
                xybar += (x[i] - xbar) * (y[i] - ybar);
            }
            double beta1 = xybar / xxbar;
            double beta0 = ybar - beta1 * xbar;
            
            // analyze results
            int df = n - 2;
            double rss = 0.0;      // residual sum of squares
            double ssr = 0.0;      // regression sum of squares
            for (int i = 0; i < n; i++) {
                double fit = beta1*x[i] + beta0;
                rss += (fit - y[i]) * (fit - y[i]);
                ssr += (fit - ybar) * (fit - ybar);
            }
            double R2    = ssr / yybar;
            
            // print results
 //           System.out.println("y   = " + beta1 + " * x + " + beta0);
 //           System.out.println("R^2                 = " + R2);
            
            int COL = 6;
            for(row=1;row<=n;row++) {
                number = new Number(COL, row, x[row-1]);
                sheet.addCell(number);
//                System.out.print(x[row-1]+" ");
                number = new Number(COL+1, row, y[row-1]);
                sheet.addCell(number);
//               System.out.print(y[row-1]+" ");
                number = new Number(COL+2, row, Math.exp(y[row-1]));
                sheet.addCell(number);
//               System.out.print(Math.exp(y[row-1])+" ");
                number = new Number(COL+3, row, Math.log(Math.exp(beta0)*Math.exp(beta1*x[row-1])));
                sheet.addCell(number);
//               System.out.println(Math.log(Math.exp(beta0)*Math.exp(beta1*x[row-1]))+" ");
            }
            
            Label label = new Label(COL+4,0, "A = exp(" + String.format("%.3f", beta0)+")");
            sheet.addCell(label);
            label = new Label(COL+4,1, "b/2m = " + String.format("%.3f", beta1));
            sheet.addCell(label);
            
            label = new Label(COL+6,0, "y = exp(" + String.format("%.3f", beta1) + " * x + " + String.format("%.3f", beta0)+")");
            sheet.addCell(label);
            label = new Label(COL+6,1, "R^2 = " + String.format("%.3f", R2));
            sheet.addCell(label);

           
			if( !dismissed ) {
				copyworkbook.write(); 
				copyworkbook.close();
				inworkbook.close();
				
			    MimeTypeMap map = MimeTypeMap.getSingleton();
			    String ext = MimeTypeMap.getFileExtensionFromUrl(copyFile.getName());
			    String type = map.getMimeTypeFromExtension(ext);
	
			    if (type == null)
			        type = "*/*";
	
			    Intent intent = new Intent(Intent.ACTION_VIEW);
			    Uri dataFile = Uri.fromFile(copyFile);
	
			    intent.setDataAndType(dataFile, type); 
	
			    startActivity(intent);	
			}
		}
		catch(Exception e) {
			errorDialog(PROGRAM+" file incorrectly formatted.");
		}	
	}
}
