package win.zhlicen.nfcscaner;

import android.graphics.Color;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.content.IntentFilter;
import android.content.Context;
import android.app.PendingIntent;
import android.widget.Toast;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import android.widget.TextSwitcher;
import android.widget.ViewSwitcher.ViewFactory;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.net.Uri;
import android.Manifest;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.os.Vibrator;
import android.widget.CheckBox;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import android.content.DialogInterface;
import android.app.AlertDialog;


public class MainActivity extends AppCompatActivity {
    TextView tvReading;
    TextSwitcher tsNotify;
    Context context;
    NfcAdapter nfcAdapter;
    int scanCount = 0;
    String filePath = "/sdcard/NfcScan/";
    String fileName = "history.txt";
    String currentCode = "";
    CheckBox chkSound;
    CheckBox chkVibrate;
    Map<String, String> mapAdded = new HashMap<>();
    static final int PERMISSION_REQ_CODE_READ_STORAGE = 0;
    static final int PERMISSION_REQ_CODE_WRITE_STORAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tsNotify = (TextSwitcher) findViewById(R.id.tsNotify);
        tsNotify.setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                TextView tv = new TextView(MainActivity.this);
                tv.setTextSize(29);
                tv.setTextColor(Color.GRAY);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                return tv;
            }
        });
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        tsNotify.setInAnimation(in);
        tsNotify.setOutAnimation(out);

        chkSound = (CheckBox) findViewById(R.id.chkSound);
        chkVibrate = (CheckBox) findViewById(R.id.chkVibrate);
        tvReading = (TextView) findViewById(R.id.tvReading);
        tvReading.setText("N/A");
        tsNotify.setText("Scan a tag!");
        this.context = this.getApplicationContext();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE_READ_STORAGE);
        } else {
            this.initialAddedMap();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQ_CODE_WRITE_STORAGE);
            }
        }




        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.context);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_CODE_READ_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initialAddedMap();
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSION_REQ_CODE_WRITE_STORAGE);
                    }

                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Warning")
                            .setMessage("App can not work properly if no storage permission is granted!")
                            .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finishAndRemoveTask();
                                }
                            })
                            .show();
                }
                return;
            }
            case PERMISSION_REQ_CODE_WRITE_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Warning")
                            .setMessage("App can not work properly if no storage permission is granted!")
                            .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finishAndRemoveTask();
                                }
                            })
                            .show();
                }
            }
        }
    }

    private void setReadingText(String text) {
        tvReading.setText(text);
    }

    private void initialAddedMap(){
        InputStream fis = null;
        try {
            fis = new FileInputStream(filePath + fileName);
            if (fis != null) {

                InputStreamReader chapterReader = new InputStreamReader(fis);
                BufferedReader buffReader = new BufferedReader(chapterReader);

                String line;

                // read every line of the file into the line-variable, on line at the time
                do {
                    line = buffReader.readLine();
                    if(!line.isEmpty() && !mapAdded.containsKey(line)) {
                        mapAdded.put(line, "");
                    }
                } while (line != null);

            }
        }
        catch (Exception e) {

        }
        finally {
            // close the file.
            if(fis != null) {
                    try {
                    fis.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Toast.makeText(this, mapAdded.size() + " local records loaded!", Toast.LENGTH_SHORT).show();
    }

    private void playRingtone(int type) {
        if(!chkSound.isChecked())
            return;
        try {
            Uri notification = RingtoneManager.getDefaultUri(type);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrate(int delay) {
        if(!chkVibrate.isChecked())
            return;
        Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        if(v != null)
            v.vibrate(delay);
    }
    private boolean onClearMenu(){
        mapAdded.clear();
        String historyPath = filePath + fileName;
        File file = new File(historyPath);
        if(file.exists() && file.delete()) {
            Toast.makeText(this, "Clear history success!", Toast.LENGTH_SHORT).show();
            scanCount = 0;
            tsNotify.setText("Scan a tag!");
        }
        else {
            Toast.makeText(this, "No history to clear!", Toast.LENGTH_SHORT).show();

        }
        return true;
    }


    private boolean onShareMenu(){
        String historyPath = filePath + fileName;
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        File file = new File(historyPath);

        if(file.exists()) {
            intentShareFile.setType("application/txt");
            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + historyPath));
            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
            intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
            startActivity(Intent.createChooser(intentShareFile, "Share File"));
        }
        else {
            Toast.makeText(this, "No history to share!", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void newTag(String text) {
        // handle
        scanCount++;
        setReadingText(text);
        saveTag(text);
        tsNotify.setText(scanCount + " tags scanned. Next tag!");
    }

    private void saveTag(String text) {
        writeTxtToFile(text, filePath, fileName);
        Toast.makeText(this, "Saved to file", Toast.LENGTH_SHORT).show();
    }

    protected void writeTxtToFile(String content, String filePath, String fileName) {
        makeFilePath(filePath, fileName);
        String strFilePath = filePath + fileName;

        String strContent = content + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        }
        catch (Exception e) {
        }
    }

    protected File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    protected void makeRootDirectory(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null && currentCode == "") {
                tsNotify.setText("Scaning...");
                for (int i = 0; i < rawMessages.length; i++) {
                    NdefMessage message = (NdefMessage) rawMessages[i];
                    NdefRecord[] records = message.getRecords();
                    for (int j = 0; j < records.length; j++) {

//                        byte[] payload = records[j].getPayload();
//                        if (payload != null && payload.length > 0) {
//                            StringBuffer stringBuf = new StringBuffer();
//                            for(int idx = 0; idx < payload.length; idx++)
//                                stringBuf.append(String.format("%02x", payload[idx]));
//                            currentCode += stringBuf.toString();
//                        }
                        byte[] id = records[j].getId();
                        if (id != null && id.length > 0) {
                            StringBuffer stringBuf = new StringBuffer();
                            for (int idx = 0; idx < id.length; idx++)
                                stringBuf.append(String.format("%02x", id[idx]));
                            currentCode += stringBuf.toString();
                        }
                    }
                }
            }
            playRingtone(RingtoneManager.TYPE_NOTIFICATION);
            playRingtone(RingtoneManager.TYPE_NOTIFICATION);
            vibrate(50);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentCode == "") {
                        vibrate(20);
                        tvReading.setText("N/A");
                        tsNotify.setText("No valid data, try a again!");
                        vibrate(20);
                    }
                    else if(mapAdded.containsKey(currentCode)) {
                        vibrate(20);
                        tsNotify.setText("Tag already exist, try a another!");
                        vibrate(10);
                    }
                    else {
                        newTag(currentCode);
                        mapAdded.put(currentCode, "");
                    }
                    currentCode = "";
                }
            }, 800);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            return onClearMenu();
        }
        else if (id == R.id.action_share) {
            return onShareMenu();
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume(){
        super.onResume();
        PendingIntent nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),  0);
        IntentFilter[] nfcFilter = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        };
        String[][] techList = new String[][]{{MainActivity.class.getName()}};
        if (nfcAdapter != null){
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, nfcFilter, techList);
            if (!nfcAdapter.isEnabled()){
                Toast.makeText(this, "Please enable your NFC", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Your device dose not support NFC.", Toast.LENGTH_SHORT).show();
        }
    }


}
