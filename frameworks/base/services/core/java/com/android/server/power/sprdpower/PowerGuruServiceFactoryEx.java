package com.android.server.power.sprdpower;

import android.content.Context;

public class PowerGuruServiceFactoryEx extends PowerGuruServiceFactory {

    public PowerGuruServiceFactoryEx(){

    }

    public AbsPowerGuruService createPowerGuruService(Context context){
        return new PowerGuruService(context){};
    }

}
