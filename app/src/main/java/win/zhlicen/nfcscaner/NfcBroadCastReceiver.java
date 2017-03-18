package win.zhlicen.nfcscaner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import android.util.Log;
/**
 * Created by zhlic on 3/17/2017.
 */

public class NfcBroadCastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context , Intent intent) {
        // context.startActivity();
        intent.setClass(context, MainActivity.class);
        // Intent startIntent = new Intent(context, );
        // startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        //startIntent.setAction()
        //MainActivity.this.onNewIntent(intent);

    }
}
