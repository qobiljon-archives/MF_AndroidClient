package kr.ac.inha.nsl.mindforecaster;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

public class AlarmReceiverIntervention extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent notificationIntent = new Intent(context, SignInActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(SignInActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        int notificaiton_id = (int) intent.getLongExtra("notification_id", 0);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(context.getString(R.string.intervention));
        inboxStyle.addLine(intent.getStringExtra("Content1"));
        inboxStyle.addLine(intent.getStringExtra("Content2"));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.notif_channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.intervention))
                .setTicker("New Message Alert!")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(intent.getStringExtra("Content1")).setStyle(inboxStyle);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(notificaiton_id, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = builder.setContentIntent(pendingIntent).build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificaiton_id, notification);
        }
    }
}
