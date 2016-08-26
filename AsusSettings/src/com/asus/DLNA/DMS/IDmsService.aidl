package com.asus.DLNA.DMS;

import android.os.Bundle;

interface IDmsService
{
    IBinder getMessenger();
    Bundle doDmsFunction(int funcKey, in Bundle input);
}
