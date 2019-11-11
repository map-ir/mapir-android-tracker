package ir.map.tracker.network;

import ir.map.tracker.network.model.Subscription;

public interface SubscriptionResponseListener {

    void onSubscribeSuccess(Subscription subscription);

    void onSubscribeFailure(Throwable error);
}
