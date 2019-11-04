package ir.map.tracker.network.model;

public class Data {

    public Data(String topic, String username, String password) {
        this.topic = topic;
        this.username = username;
        this.password = password;
    }

    private String topic;
    private String username;
    private String password;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
