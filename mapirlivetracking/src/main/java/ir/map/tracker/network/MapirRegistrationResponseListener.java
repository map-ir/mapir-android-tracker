package ir.map.tracker.network;

import ir.map.tracker.network.model.Register;

public interface MapirRegistrationResponseListener {

    void onRegisterSuccess(Register register);

    void onRegisterFailure(Throwable error);
}
