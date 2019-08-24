package trelico.ru.allcastmvvm.data_sources.remote;

public class AudioRequestBody{

    private String text;

    public AudioRequestBody(String text){
        this.text = text;
    }

    public AudioRequestBody(){
    }

    public String getText(){
        return text;
    }

    public void setText(String text){
        this.text = text;
    }
}
