# Focus Flow — R8 pravila za release build.
# Room, WorkManager, Compose i Activity/Service/Receiver iz manifesta imaju
# sopstvena (consumer/AAPT) pravila; ovde je samo ono što je specifično za app.

# Modeli pravila i alarma su java.io.Serializable (radna kopija forme u
# rememberSaveable). Java serijalizacija traži ova polja/metode po imenu.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Enum vrednosti se u Room/SharedPreferences čuvaju po imenu (name()/valueOf()).
-keepclassmembers enum com.example.screenmanager.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Čitljivi stack trace-ovi u Play Console (mapping.txt se pakuje u AAB).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
