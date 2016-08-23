Permiso [![Build Status](https://travis-ci.org/greysonp/permiso.svg?branch=master)](https://travis-ci.org/greysonp/permiso) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Permiso-green.svg?style=true)](https://android-arsenal.com/details/1/2818) [![Join the chat at https://gitter.im/permiso/Lobby](https://badges.gitter.im/permiso/Lobby.svg)](https://gitter.im/permiso/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
=======

Permiso is an Android library that makes requesting runtime permissions a whole lot easier.

Have you seen the [docs](http://developer.android.com/training/permissions/requesting.html) for how to request runtime permissions? Who wants to do *that* every time you request a permission? Let's clean this up!

Features
--------
* Localizes permission requests so you can handle everything using a simple callback mechanism.
* Can easily make permission requests outside of the context of an Activity.
* Simplifies showing the user your rationale for requesting a permission.
* Can request multiple permissions at once.
* Merges simultaneous requests for the same permission into a single request.

Usage
-----
If your Activity subclasses ```PermisoActivity```, requesting a permission is as simple as:

```java
Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
    @Override
    public void onPermissionResult(Permiso.ResultSet resultSet) {
        if (resultSet.areAllPermissionsGranted()) {
            // Permission granted!
        } else {
            // Permission denied.
        }
    }

    @Override
    public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
        Permiso.getInstance().showRationaleInDialog("Title", "Message", null, callback);
    }
}, Manifest.permission.READ_EXTERNAL_STORAGE);
```

### Requesting Multiple Permissions
Requesting multiple permissions at once is just as easy.

```java
Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
    @Override
    public void onPermissionResult(Permiso.ResultSet resultSet) {
        if (resultSet.isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
            // Contact permission granted!
        }
        if (resultSet.isPermissionGranted(Manifest.permission.READ_CALENDAR)) {
            // Calendar permission granted!
        }
    }

    @Override
    public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
        Permiso.getInstance().showRationaleInDialog("Title", "Message", null, callback);
    }
}, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR);
```

Gradle
------
### Latest Stable Version
```java
dependencies {
    compile 'com.greysonparrelli.permiso:permiso:0.3.0'
}
```

### Latest Dev Version
```java
// In your top-level build.gradle
repositories {
    maven { url "https://www.jitpack.io" }
}
// In your module's build.gradle
dependencies {
    compile 'com.github.greysonp:permiso:master-SNAPSHOT'
}
```
FAQ
---
**I don't want my Activity to subclass ```PermisoActivity```. Do I have to?**

Of course not! Permiso requires very little boilerplate, and therefore ```PermisoActivity``` does very little. If you don't want to subclass ```PermisoActivity```, all you have to do is make sure you do the two following things:

* In ```onCreate()``` and ```onResume()```, invoke ```Permiso.getInstance().setActivity(this)```.
* Forward the results of ```Activity.onRequestPermissionsResult()``` to ```Permiso.getInstance().onRequestPermissionResult()```.

Here's an example:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Permiso.getInstance().setActivity(this);
}

@Override
protected void onResume() {
    super.onResume();
    Permiso.getInstance().setActivity(this);
}

@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
}
```

**I don't want to show any rationale for my permissions.**

According to the Android Guidelines you probably should, but there's no hard requirement. If you don't want to show a rationale, simply invoke the callback and do nothing else:

```java
@Override
public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
    callback.onRationaleProvided();
}
```

**I want to do some complicated logic with the results of my permission request, but the ResultSet doesn't let me.**

Fear not! The ```ResultSet``` object has a method called ```toMap()```, which will give you back a mapping of permissions -> ```Permiso.Result``` that you can iterate over to your heart's content.

**What do you mean when you say that Permiso merges simultaneous requests for the same permission into a single request?**

If you request the same permission in two places simultaneously, Permiso will automatically merge them into one request. You might think this is a rare scenario, but before you know it, you have master and detail fragments that both need access to the user's contacts, and now you have to manage your permissions so their simultaneous requests don't cause two separate pop-ups! Don't worry, Permiso handles this for you.

**I request a permission but nothing happens? What's up?**

Did you make sure to declare your permissions in your ```AndroidManifest.xml```? If you don't, permission requests fail silently. That's an Android thing - not much Permiso can do there.
