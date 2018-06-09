package michal.chabiera;
//https://github.com/mcialini/MM1
//http://simjs.com/queuing/index.html



public class Zdarzenie implements Comparable<Zdarzenie> {
    public enum typZdarzenia {PRZYJSCIE, WYJSCIE, NULL}
    private double czas;
    private typZdarzenia typ;
    private int id;

    public static int globalId = 0;

    public Zdarzenie( typZdarzenia typ, double czas){
            this.typ = typ;
            this.czas = czas;
    }
    public Zdarzenie(typZdarzenia typ){
        this.typ = typ;
    } //uzywane dla zdarzenia null - praktycnzie niepotrzebne
    public double getCzas(){
        return this.czas;
    }
    public typZdarzenia getTyp(){
        return this.typ;
    }
    public int compareTo(Zdarzenie zdarzenie){
        if(this.getCzas() > zdarzenie.getCzas()){
            return 1;
        }
        else if (this.getCzas() == zdarzenie.getCzas()){
            return 0;
        }
        else{
            return -1;
        }
    }
}
