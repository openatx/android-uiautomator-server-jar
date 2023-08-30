package mirror.android.app;

import mirror.RefClass;
import mirror.RefMethod;
import mirror.RefStaticInt;
import mirror.RefStaticMethod;

public class ActivityManager {
    public static Class<?> TYPE = RefClass.load(ActivityManager.class, android.app.ActivityManager.class);
    public static RefStaticInt START_SUCCESS;
    public static RefStaticInt START_INTENT_NOT_RESOLVED;
    public static RefStaticInt START_TASK_TO_FRONT;
    public static RefStaticInt START_NOT_CURRENT_USER_ACTIVITY;

    public static RefStaticMethod<Object> getService;
}
