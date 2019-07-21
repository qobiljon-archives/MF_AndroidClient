package kr.ac.inha.nsl.mindforecaster;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class AlarmReceiverIntervention extends BroadcastReceiver {
    // region Override
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent interventionSuggestionIntent = new Intent(context, ActivityDialogInterventionSuggestion.class);

        interventionSuggestionIntent.putExtra("intervention_reminder_title", intent.getStringExtra("intervention_reminder_title"));
        interventionSuggestionIntent.putExtra("intervention_reminder_description", intent.getStringExtra("intervention_reminder_description"));
        interventionSuggestionIntent.putExtra("intervention_reminder_event_period", intent.getStringExtra("intervention_reminder_event_period"));

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ActivitySignIn.class);
        stackBuilder.addNextIntent(interventionSuggestionIntent);

        int interventionId = intent.getIntExtra("notification_id", -1);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(intent.getStringExtra("intervention_reminder_title"));
        inboxStyle.addLine(intent.getStringExtra("intervention_reminder_description"));
        inboxStyle.addLine(intent.getStringExtra("intervention_reminder_event_period"));

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(interventionId, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(context, context.getString(R.string.notif_channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.intervention))
                .setTicker(intent.getStringExtra("intervention_reminder_title"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(intent.getStringExtra("intervention_reminder_description"))
                .setStyle(inboxStyle)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(interventionId, notification);
    }
    // endregion
}
