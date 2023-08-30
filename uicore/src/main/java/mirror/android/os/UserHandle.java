package mirror.android.os;

import mirror.RefClass;
import mirror.RefStaticInt;

public class UserHandle {
    public static Class<?> TYPE = RefClass.load(UserHandle.class, android.os.UserHandle.class);
    public static RefStaticInt USER_SYSTEM;
}
