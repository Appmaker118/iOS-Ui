import os

def create_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content.strip())
    print(f"✅ Generated: {path}")

# 1. Root-Level Build Settings
create_file("Approot/settings.gradle", """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "iOS-Ui-Replica"
include ':app'
""")

# 2. Root-Level Build Rules
create_file("Approot/build.gradle", """
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.2'
    }
}
plugins {
    id 'com.android.application' version '8.2.2' apply false
}
tasks.register('clean', Delete) {
    delete rootProject.buildDir
}
""")

# 3. AndroidX Architecture Enforcements
create_file("Approot/gradle.properties", """
android.useAndroidX=true
android.enableJetifier=true
""")

# 4. App-Module Level Compilation Options
create_file("Approot/app/build.gradle", """
plugins {
    id 'com.android.application'
}
android {
    namespace 'com.replica.iosui'
    compileSdk 34
    defaultConfig {
        applicationId "com.replica.iosui"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    buildTypes {
        release {
            minifyEnabled false
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
}
""")

# 5. Master Manifest Interface Pipeline (With Explicit Global XML Namespaces)
create_file("Approot/app/src/main/AndroidManifest.xml", """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://android.com"
    xmlns:tools="http://android.com">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="iOS 27 UI"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:clearTaskOnLaunch="true"
            android:stateNotNeeded="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <service
            android:name=".services.IOSStatusBarService"
            android:enabled="true"
            android:exported="false" />

        <service
            android:name=".services.IOSLockScreenService"
            android:enabled="true"
            android:exported="false" />

    </application>
</manifest>
""")

# 6. Housekeeping: Removing non-XML compilation blockers
remove_target = "Approot/app/src/main/res/values/REMOVE"
if os.path.exists(remove_target):
    os.remove(remove_target)
    print(f"🧹 Removed compilation blocker: {remove_target}")

print("🚀 Workspace setup successfully synchronized!")
