package ir.map.tracker.network.model;

public class Register {

    public Register(String message, Data data) {
        this.message = message;
        this.data = data;
    }

    private Data data;

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}
