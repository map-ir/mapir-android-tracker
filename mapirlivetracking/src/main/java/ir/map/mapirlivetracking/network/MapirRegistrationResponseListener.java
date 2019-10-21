package ir.map.mapirlivetracking.network;

import ir.map.mapirlivetracking.network.model.Register;

public interface MapirRegistrationResponseListener {

    void onRegisterSuccess(Register register);

    void onRegisterFailure(Throwable error);
}
