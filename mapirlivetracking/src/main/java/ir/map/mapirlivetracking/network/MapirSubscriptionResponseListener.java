package ir.map.mapirlivetracking.network;

import ir.map.mapirlivetracking.network.model.Subscription;

public interface MapirSubscriptionResponseListener {

    void onSubscribeSuccess(Subscription subscription);

    void onSubscribeFailure(Throwable error);
}
