package ir.map.tracker.network;

import ir.map.tracker.network.model.Register;

public interface RegistrationResponseListener {

    void onRegisterSuccess(Register register);

    void onRegisterFailure(Throwable error);
}
