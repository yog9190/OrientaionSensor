package com.customc.orientaionsensor;



import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.customc.orientaionsensor.OrientationSenor.SensorData;

public class MainActivity extends Activity implements OrientationSenor.SensorListener  {
	Context ctx;
    OrientationSenor objOrientationSenor;
    TextView  textView1,textView2,textView3;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ctx = this;
		objOrientationSenor=new OrientationSenor(ctx);
		objOrientationSenor.addSensorListener(this);
		textView1=(TextView)findViewById(R.id.textView1);
		textView2=(TextView)findViewById(R.id.textView2);
		textView3=(TextView)findViewById(R.id.textView3);
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		try{
			objOrientationSenor.register();
		}catch(Exception e){
			Toast toast = Toast.makeText(ctx,
					e.getMessage(), Toast.LENGTH_SHORT);
			toast.show();		
		}	
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		 objOrientationSenor.unregister();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//etMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onSensorChanged(SensorData objSensorData) {
		// TODO Auto-generated method stub
		textView1.setText("Azimuth :  "+objSensorData.angleAzimuth);
		textView2.setText("Pitch :  "+objSensorData.anglePitch);
		textView3.setText("Roll :  "+objSensorData.angleRoll);
	}

}
